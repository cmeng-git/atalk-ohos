/*
 * aTalk, ohos VoIP and Instant Messaging client
 * Copyright 2024 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atalk.impl.neomedia.device.util;

import ohos.aafwk.ability.Ability;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;

import org.atalk.ohos.BaseAbility;

import java.awt.Dimension;
import timber.log.Timber;

/**
 * <code>ViewDependentProvider</code> is used to implement classes that provide objects dependent on
 * <code>JComponent</code> visibility state. It means that they can provide it only when <code>JComponent</code> is
 * visible, and they have to release the object before <code>JComponent</code> is hidden.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public abstract class ViewDependentProvider<T> {
    /**
     * Timeout for dispose surface operation
     */
    private static final long REMOVAL_TIMEOUT = 5000L;

    /**
     * Timeout for create surface operation
     */
    private static final long CREATE_TIMEOUT = 5000L;

    /**
     * <code>Ability</code> context.
     */
    protected final Ability mAbility;

    /**
     * The container that will hold maintained view.
     */
    private final ComponentContainer mContainer;

    /**
     * The view (can either be SurfaceView or TextureView) maintained by this instance.
     */
    protected Component mComponent;

    /**
     * Use for surfaceCreation to set surface holder size for correct camera local preview aspect ratio
     * This size is the user selected video resolution independent of the device orientation
     */
    protected Dimension mVideoSize;

    /**
     * Provided object created when <code>JComponent</code> is visible.
     */
    protected T providedObject;

    /**
     * Factory method that creates new <code>JComponent</code> instance.
     *
     * @return new <code>JComponent</code> instance.
     */
    protected abstract Component createViewInstance();

    public abstract void setAspectRatio(int width, int height);

    /**
     * Create a new instance of <code>ViewDependentProvider</code>.
     *
     * @param ability parent <code>Ability</code> that manages the <code>container</code>.
     * @param container the container that will hold maintained <code>JComponent</code>.
     */
    public ViewDependentProvider(Ability ability, ComponentContainer container) {
        mAbility = ability;
        mContainer = container;
    }

    /**
     * Checks if the view is currently created. If not creates new <code>JComponent</code> and adds it to the
     * <code>container</code>.
     */
    protected void ensureViewCreated() {
        if (mComponent == null) {
            BaseAbility.runOnUiThread(() -> {
                mComponent = createViewInstance();
                ComponentContainer.LayoutConfig params = new ComponentContainer.LayoutConfig(
                        ComponentContainer.LayoutConfig.MATCH_PARENT, ComponentContainer.LayoutConfig.MATCH_PARENT);
                mContainer.addComponent((Component) mComponent, params);
                mContainer.setVisibility(Component.VISIBLE);
            });
        }
    }

    /**
     * Returns maintained <code>JComponent</code> object.
     *
     * @return maintained <code>JComponent</code> object.
     */
    public Component getComponent() {
        return mComponent;
    }

    /**
     * Set the {@link #mVideoSize} with the video size selected by user for this instance
     *
     * @param size user selected video size independent of the device orientation
     */
    public void setVideoSize(Dimension size) {
        mVideoSize = size;
    }

    public Dimension getVideoSize() {
        return mVideoSize;
    }

    /**
     * Checks if maintained view exists and removes if from the <code>container</code>.
     */
    protected void ensureViewDestroyed() {
        if (mComponent != null) {
            final Component viewToRemove = mComponent;
            mComponent = null;

            BaseAbility.runOnUiThread(() -> {
                mContainer.removeComponent((Component) viewToRemove);
                mContainer.setVisibility(Component.HIDE);
            });
        }
    }

    /**
     * Must be called by subclasses when provided object is created.
     *
     * @param obj provided object instance.
     */
    synchronized protected void onObjectCreated(T obj) {
        providedObject = obj;
        notifyAll();
    }

    /**
     * Should be called by consumer to obtain the object. It is causing hidden <code>JComponent</code> to be
     * displayed and eventually {@link #onObjectCreated(Object)} method to be called which results
     * in object creation.
     *
     * @return provided object.
     */
    synchronized public T obtainObject() {
        ensureViewCreated();
        if (providedObject == null) {
            try {
                Timber.i("Waiting for object...%s", hashCode());
                this.wait(CREATE_TIMEOUT);
                if (providedObject == null) {
                    throw new RuntimeException("Timeout waiting for surface");
                }
                Timber.i("Returning object! %s", hashCode());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return providedObject;
    }

    /**
     * Checks if provider has already the object and returns it immediately. If there is no object,
     * and we would have to wait for it, then the <code>null</code> is returned.
     *
     * @return the object if it is currently held by this provider or <code>null</code> otherwise.
     */
    synchronized public T tryObtainObject() {
        return providedObject;
    }

    /**
     * Should be called by subclasses when object is destroyed.
     */
    synchronized protected void onObjectDestroyed() {
        releaseObject();
    }

    /**
     * Should be called by the consumer to release the object.
     */
    public void onObjectReleased() {
        releaseObject();
        // Remove the view once it's released
        ensureViewDestroyed();
    }

    /**
     * Releases the subject object and notifies all threads waiting on the lock.
     */
    synchronized protected void releaseObject() {
        if (providedObject != null) {
            providedObject = null;
            this.notifyAll();
        }
    }

    /**
     * Blocks the current thread until subject object is released. It should be used to block UI thread
     * before the <code>JComponent</code> is hidden.
     */
    synchronized public void waitForObjectRelease() {
        if (providedObject != null) {
            try {
                Timber.i("Waiting for object release... %s", hashCode());
                this.wait(REMOVAL_TIMEOUT);
                if (providedObject != null) {
                    // cmeng - do not throw, as this hangs the video call screen
                    // throw new RuntimeException("Timeout waiting for preview surface removal");
                    Timber.w("Timeout waiting for preview surface removal!");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        ensureViewDestroyed();
    }
}
