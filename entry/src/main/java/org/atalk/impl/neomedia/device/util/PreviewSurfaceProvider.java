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
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.surfaceprovider.SurfaceProvider;
import ohos.agp.graphics.Surface;
import ohos.agp.graphics.SurfaceOps;
import ohos.agp.window.service.Display;
import ohos.agp.window.service.DisplayManager;
import ohos.media.camera.device.Camera;

import timber.log.Timber;

/**
 * The class exposes methods for managing preview surfaceView state which must be synchronized with
 * currently used {@link Camera} state.
 * The surface must be present before the camera is started and for this purpose
 * {@link #obtainObject()} method shall be used.
 * <p>
 * When the call is ended, before the <code>Ability</code> is finished we should ensure that the camera
 * has been stopped (which is done by video telephony internals), so we should wait for it to be
 * disposed by invoking method {@link #waitForObjectRelease()}. It will block current
 * <code>Thread</code> until it happens or an <code>Exception</code> will be thrown if timeout occurs.
 */
public class PreviewSurfaceProvider extends ViewDependentProvider<SurfaceProvider> implements SurfaceOps.Callback {
    private AutoFitSurfaceView mSurfaceView;

    /**
     * Flag indicates whether {@link SurfaceProvider#pinToZTop(boolean)} should be called on created <code>AutoFitSurfaceView</code>.
     */
    private final boolean setZMediaOverlay;

    /**
     * Create a new instance of <code>PreviewSurfaceProvider</code>.
     *
     * @param parent parent <code>OSGiAbility</code> instance.
     * @param container the <code>ComponentContainer</code> that will hold maintained <code>SurfaceView</code>.
     * @param zMediaOverlay if set to <code>true</code> then the <code>SurfaceView</code> will be
     * displayed on the top of other surfaces e.g. local camera surface preview
     */
    public PreviewSurfaceProvider(Ability parent, ComponentContainer container, boolean zMediaOverlay) {
        super(parent, container);
        setZMediaOverlay = zMediaOverlay;
    }

    @Override
    protected SurfaceProvider createViewInstance() {
        mSurfaceView = new AutoFitSurfaceView(mAbility);
        mSurfaceView.getSurfaceOps().get().addCallback(this);
        mSurfaceView.pinToZTop(setZMediaOverlay);
        return mSurfaceView;
    }

    public void setAspectRatio(int width, int height) {
        if (mSurfaceView != null) {
            mSurfaceView.setAspectRatio(width, height);
        }
        else {
            Timber.w(" setAspectRatio for mSurfaceView is null");
        }
    }

    /**
     * Method is called before <code>Camera</code> is started and shall return non <code>null</code>
     * {@link Surface} instance. The is also used by ohos decoder.
     *
     * @return {@link SurfaceOps} instance that will be used for local video preview
     */
    @Override
    public SurfaceProvider obtainObject() {
        // Timber.e(new Exception("Obtain Object for testing only"));
        return super.obtainObject();
    }

    /**
     * Method is called when <code>Camera</code> is stopped and it's safe to release the {@link Surface} object.
     */
    @Override
    public void onObjectReleased() {
        super.onObjectReleased();
    }

    /**
     * Should return current {@link Display} rotation as defined in {@link Display#getRotation()}.
     *
     * @return current {@link Display} rotation as one of values:
     * {@link Display# ROTATION_0}, {@link CameraUtils#ROTATION_90}, {@link CameraUtils#ROTATION_180}, {@link CameraUtils#ROTATION_270}.
     */
    public int getDisplayRotation() {
        DisplayManager dm = DisplayManager.getInstance();
        return dm.getDefaultDisplay(mAbility).isPresent() ? dm.getDefaultDisplay(mAbility).get().getRotation() : 0;
    }

    // ============== SurfaceHolder.Callback ================== //

    /**
     * This is called immediately after the surface is first created. Implementations of this should
     * start up whatever rendering code they desire. Note that only one thread can ever draw into a
     * {@link Surface}, so you should not draw into the Surface here if your normal rendering will
     * be in another thread.
     * <p>
     * Must setFixedSize() to the user selected video size, to ensure local preview is in correct aspect ratio
     * <a href="https://developer.android.com/reference/android/hardware/camera2/CameraDevice.html#createCaptureSession(android.hardware.camera2.params.SessionConfiguration)">...</a>
     *
     * @param surfaceOps The SurfaceHolder whose surface is being created.
     */
    @Override
    public void surfaceCreated(SurfaceOps surfaceOps) {
        // Timber.d("SurfaceHolder created setFixedSize: %s", mVideoSize);
        if (mVideoSize != null) {
            surfaceOps.setFixedSize(mVideoSize.width, mVideoSize.height);
        }
        onObjectCreated(mSurfaceView);
    }

    /**
     * This is called immediately after any structural changes (format or size) have been made to
     * the surface. You should at this point update the imagery in the surface. This method is
     * always called at least once, after {@link #surfaceCreated}.
     *
     * @param surfaceOps The SurfaceHolder whose surface has changed.
     * @param format The new PixelFormat of the surface.
     * @param width The new width of the surface.
     * @param height The new height of the surface.
     */
    @Override
    public void surfaceChanged(SurfaceOps surfaceOps, int format, int width, int height) {
        /*
         * surfaceChange event is mainly triggered by local video preview change by user;
         * currently not implemented in android aTalk. Hence no action is required.
         * Note: the event get trigger whenever there is an init of the local video preview e.g. init or toggle camera
         * Timber.w("Preview surface change size: %s x %s", width, height);
         */
        // Timber.d("SurfaceHolder size changed: [%s x %s]; %s", width, height, holder);
        // preview surface does not exist
        // if (mHolder.getSurface() == null){
        //     return;
        // }
        //
        // // stop preview before making changes
        // try {
        //     mCamera.stopPreview();
        // } catch (Exception e){
        // ignore: tried to stop a non-existent preview
        // }

        // set preview size and make any resize, rotate or reformatting changes here
        // start preview with new settings
        // try {
        //     mCamera.setPreviewDisplay(mHolder);
        //     mCamera.startPreview();
        // } catch (Exception e){
        //     Timber.e("Error starting camera preview: %s", e.getMessage());
        // }
    }

    /**
     * This is called immediately before a surface is being destroyed. After returning from this
     * call, you should no longer try to access this surface. If you have a rendering thread that
     * directly accesses the surface, you must ensure that thread is no longer touching the Surface
     * before returning from this function.
     *
     * @param surfaceOps The SurfaceHolder whose surface is being destroyed.
     */
    @Override
    public void surfaceDestroyed(SurfaceOps surfaceOps) {
        onObjectDestroyed();
    }
}
