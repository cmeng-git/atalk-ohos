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
package org.atalk.ohos.gui.controller;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.animation.Animator;
import ohos.agp.animation.AnimatorProperty;
import ohos.agp.animation.AnimatorScatter;
import ohos.agp.components.Component;
import ohos.agp.components.DirectionalLayout;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.ResourceTable;

import java.util.Timer;
import java.util.TimerTask;
import timber.log.Timber;

/**
 * The Slice is a controller which hides the given <code>Component.</code> after specified delay interval.
 * To reset and prevent from hiding for another period, call <code>show</code> method.
 * This method will also instantly display controlled <code>Component</code> if it's currently hidden.
 *
 * @author Eng Chong Meng
 */
public class AutoHideController extends BaseSlice implements Animator.StateChangedListener {
    /**
     * Argument key for the identifier of <code>Component</code> that will be auto hidden.
     * It must exist in the parent <code>Ability</code> view hierarchy.
     */
    private static final String ARG_COMPONENT_ID = "component_id";

    /**
     * Argument key for the delay interval, before the <code>Component</code> will be hidden
     */
    private static final String ARG_HIDE_TIMEOUT = "hide_timeout";

    /**
     * Controlled <code>Component</code>
     */
    private DirectionalLayout component;
    private AnimatorProperty animatorProperty;

    /**
     * Timer used for the hide task scheduling
     */
    private Timer autoHideTimer;

    /**
     * Hide <code>Component</code> timeout
     */
    private long hideTimeout;

    /**
     * Listener object
     */
    private AutoHideListener listener;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        Ability activity = getAbility();

        if (activity instanceof AutoHideListener) {
            listener = (AutoHideListener) getAbility();
        }

        component = activity.findComponentById(intent.getIntParam(ARG_COMPONENT_ID, 0));
        if (component == null)
            throw new NullPointerException("The view is null");
        hideTimeout = intent.getLongParam(ARG_HIDE_TIMEOUT, -1);

        AnimatorScatter scatter = AnimatorScatter.getInstance(getContext());
        Animator animator = scatter.parse(ResourceTable.Animation_hide_to_bottom);
        if (animator instanceof AnimatorProperty) {
            animatorProperty = (AnimatorProperty) animator;
            animatorProperty.moveFromX(0).moveFromY(0)
                    .moveToX(0).moveToY(100)
                    .alphaFrom(1.0f).alpha(0.5f)
                    .setLoopedCount(1);
            animatorProperty.setTarget(component);
        }
        animatorProperty.setStateChangedListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActive() {
        super.onActive();
        show();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInactive() {
        super.onInactive();
        cancelAutoHideTask();
    }

    /**
     * Makes sure that hide task is scheduled. Cancels the previous one if is currently scheduled.
     */
    private void reScheduleAutoHideTask() {
        // Cancel pending task if exists
        cancelAutoHideTask();

        autoHideTimer = new Timer();
        autoHideTimer.schedule(new AutoHideTask(), hideTimeout);
    }

    /**
     * Makes sure the hide task is cancelled.
     */
    private void cancelAutoHideTask() {
        if (autoHideTimer != null) {
            autoHideTimer.cancel();
            autoHideTimer = null;
        }
    }

    /**
     * Hides controlled <code>Component</code>
     */
    public void hide() {
        if (!isViewVisible())
            return;

        // This call is required to clear the timer task
        cancelAutoHideTask();
        // Starts hide animation
        animatorProperty.start();
    }

    /**
     * Shows controlled <code>Component</code> and/or resets hide delay timer.
     */
    public void show() {
        if (animatorProperty == null) {
            Timber.e("The view has not been created yet");
            return;
        }
        // This means that the Component is hidden or animation is in progress
        if (autoHideTimer == null) {
            animatorProperty.reset();
            // Need to re-layout the Component
            component.setVisibility(Component.HIDE);
            component.setVisibility(Component.VISIBLE);

            if (listener != null) {
                listener.onAutoHideStateChanged(this, Component.VISIBLE);
            }
        }
        reScheduleAutoHideTask();
    }

    /**
     * Returns <code>true</code> if controlled <code>Component</code> is currently visible.
     *
     * @return <code>true</code> if controlled <code>Component</code> is currently visible.
     */
    private boolean isViewVisible() {
        return component.getVisibility() == Component.VISIBLE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(Animator animator) {
        component.setVisibility(Component.VISIBLE);
        reScheduleAutoHideTask();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onEnd(Animator animator) {
        // If it's hide animation and the task wasn't cancelled
        if (animator.isRunning() && autoHideTimer == null) {
            animator.stop();
            component.setVisibility(Component.HIDE);

            if (listener != null) {
                listener.onAutoHideStateChanged(this, Component.HIDE);
            }
        }
    }

    @Override
    public void onResume(Animator animator) {

    }

    @Override
    public void onStop(Animator animator) {
    }

    @Override
    public void onCancel(Animator animator) {
    }

    @Override
    public void onPause(Animator animator) {
    }

    /**
     * Hide <code>Component</code> timer task class.
     */
    class AutoHideTask extends TimerTask {
        @Override
        public void run() {
            BaseAbility.runOnUiThread(AutoHideController.this::hide);
        }
    }

    /**
     * Interface which can be used for listening to controlled view visibility state changes. Must be implemented by
     * the parent <code>Ability</code>, which will be registered as a listener when this fragment is created.
     */
    public interface AutoHideListener {
        /**
         * Fired when controlled <code>Component</code> visibility is changed by this controller.
         *
         * @param source the source <code>AutoHideController</code> of the event.
         * @param visibility controlled <code>Component</code> visibility state.
         */
        void onAutoHideStateChanged(AutoHideController source, int visibility);
    }

    /**
     * Creates new parametrized instance of <code>AutoHideController</code>.
     *
     * @param viewId identifier of the <code>Component</code> that will be auto hidden
     * @param hideTimeout auto hide delay in ms
     */
    public static void getInstance(int viewId, long hideTimeout) {
        AutoHideController ahCtrl = new AutoHideController();
        Intent intent = new Intent();
        Operation operation = new Intent.OperationBuilder()
                .withDeviceId("")
                .withBundleName("")
                .withAbilityName(ahCtrl.getLocalClassName())
                .build();

        intent.setOperation(operation);
        intent.setParam(ARG_COMPONENT_ID, viewId);
        intent.setParam(ARG_HIDE_TIMEOUT, hideTimeout);

        ahCtrl.present(ahCtrl, intent);
    }
}
