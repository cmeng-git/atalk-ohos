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
package org.atalk.impl.neomedia.jmfext.media.protocol.ohoscamera;

import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;
import ohos.media.camera.CameraKit;
import ohos.media.camera.device.Camera;
import ohos.media.camera.device.CameraAbility;
import ohos.media.camera.device.CameraConfig;
import ohos.media.camera.device.CameraInfo;
import ohos.media.camera.device.CameraInfo.FacingType;
import ohos.media.camera.device.CameraStateCallback;
import ohos.media.camera.device.FrameConfig;
import ohos.media.image.Image;
import ohos.media.image.ImageReceiver;
import ohos.media.image.common.ImageFormat;
import ohos.media.image.common.Size;

import org.atalk.impl.neomedia.NeomediaServiceUtils;
import org.atalk.impl.neomedia.device.DeviceConfiguration;
import org.atalk.impl.neomedia.device.util.CameraUtils;
import org.atalk.impl.neomedia.device.util.OhosCamera;
import org.atalk.impl.neomedia.jmfext.media.protocol.AbstractPushBufferStream;
import org.atalk.impl.timberlog.TimberLog;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.call.VideoCallAbility;
import org.atalk.ohos.gui.call.VideoHandlerSlice;

import java.awt.Dimension;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.control.FormatControl;
import javax.media.format.VideoFormat;
import timber.log.Timber;

import static ohos.media.camera.params.PropertyKey.SENSOR_ORIENTATION;

/**
 * Base class for ohos camera streams.
 *
 * @author Eng Chong Meng
 */
public abstract class CameraStreamBase extends AbstractPushBufferStream<DataSource> {
    private static CameraStreamBase mInstance;

    /**
     * ID of the current {@link Camera}.
     */
    protected String mCameraId;

    /**
     * A reference to the opened {@link Camera}.
     */
    protected static Camera mCamera;

    /**
     * The fixed properties for a given CameraDevice, and can be queried through the CameraManager interface
     * with CameraManager.getCameraCharacteristics.
     */
    protected CameraAbility mCameraAbility;

    /**
     * In use camera rotation, adjusted for camera lens facing direction  and device orientation - for video streaming
     */
    protected int mPreviewOrientation;

    /**
     * {@link CameraConfig.Builder} for the camera preview
     */
    protected CameraConfig.Builder mCameraConfigBuilder;

    /**
     * A {@link FrameConfig.Builder } for camera preview.
     */
    protected FrameConfig.Builder mFrameConfigBuilder;

    /**
     * An {@link ImageReceiver} that handles still image capture.
     */
    protected ImageReceiver mImageReceiver;

    /**
     * Format of this stream: must use a clone copy of the reference format given in streamFormats[];
     * i.e. mFormat = (VideoFormat) streamFormats[0].clone(); otherwise mFormat.setVideoSize(mPreviewSize) will change
     * the actual item in the formatControl.getSupportedFormats(); causing problem in VideoMediaStreamImpl#selectVideoSize()
     * to fail with no matched item, and androidCodec to work on first instance only
     */
    protected VideoFormat mFormat;

    /**
     * Best closer match for the user selected to the camera available resolution
     */
    protected Dimension optimizedSize = null;

    /**
     * Final previewSize (with respect to orientation) use for streaming
     */
    protected Dimension mPreviewSize = null;

    /**
     * The swap and flip state for the preview transformation for video streaming
     */
    protected boolean mSwap = true;
    protected boolean mFlip = true;

    /**
     * Flag indicates the system is in the process of shutting down the camera and ImageReader:
     * Do not access the ImageReader else: <a href="https://issuetracker.google.com/issues/203238264">...</a>
     */
    protected boolean inTransition = true;

    // protected ViewDependentProvider<?> mPreviewSurfaceProvider;

    protected DataSource dataSource;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private EventRunner backgroundThread;

    /**
     * A {@link EventHandler} for running tasks in the background.
     */
    protected EventHandler mBackgroundHandler;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Fps statistics
     */
    private long last = System.currentTimeMillis();
    private final long[] avg = new long[10];
    private int idx = 0;

    /**
     * Create a new instance of <code>CameraStreamBase</code>.
     *
     * @param parent parent <code>DataSource</code>.
     * @param formatControl format control used by this stream.
     */
    CameraStreamBase(DataSource parent, FormatControl formatControl) {
        super(parent, formatControl);
        dataSource = parent;
        mCameraId = OhosCamera.getCameraId(parent.getLocator());
        mInstance = this;
    }

    public static CameraStreamBase getInstance() {
        return mInstance;
    }

    /**
     * Method should be called by extending classes in order to start the camera.
     * Obtain optimized dimension from the device supported preview sizes with the given desired size.
     * a. always set camera preview captured dimension in its native orientation (landscape) - otherwise may not be supported.
     * b. Local preview dimension must follow current display orientation to maintain image aspect ratio
     * and width and height is interchanged if necessary. Transformation of preview stream video for sending
     * is carried out in:
     *
     * @throws IOException IO exception
     * @see PreviewStream#YUV420PlanarRotate(Image, byte[], int, int)
     */
    protected void startImpl()
            throws IOException {
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            // Get user selected default video resolution
            DeviceConfiguration deviceConfig = NeomediaServiceUtils.getMediaServiceImpl().getDeviceConfiguration();
            Dimension videoSize = deviceConfig.getVideoSize();

            CameraKit cameraManager = aTalkApp.getCameraManager();
            mCameraAbility = cameraManager.getCameraAbility(mCameraId);
            startBackgroundThread();

            // Find optimised video resolution with user selected against device support image format sizes
            List<Size> supportedPreviewSizes = mCameraAbility.getSupportedSizes(ImageFormat.YUV420_888);
            optimizedSize = CameraUtils.getOptimalPreviewSize(videoSize, supportedPreviewSizes);

            Format[] streamFormats = getStreamFormats();
            mFormat = (VideoFormat) streamFormats[0].clone();
            Timber.d("Camera data stream format #2: %s=>%s", videoSize, mFormat);
            initPreviewOrientation(true);

            cameraManager.createCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (SecurityException e) {
            Timber.e("openCamera: %s", e.getMessage());
        } catch (IllegalStateException e) {
            Timber.e("openCamera: Cannot access the camera.");
        } catch (NullPointerException e) {
            Timber.e("Camera2API is not supported on the device.");
        } catch (InterruptedException e) {
            // throw new RuntimeException("Interrupted while trying to lock camera opening.");
            Timber.e("Exception in start camera init: %s", e.getMessage());
        }
    }

    /**
     * Update swap and flip for YUV420PlanarRotate();
     * Set local preview display orientation according to device rotation and sensor orientation
     * Currently android phone device has 90/270 for back and front cameras native orientation
     * <p>
     * Note: valid Sensor orientations: 0, 90, 270; 180 is not reported by android camera sensors
     *
     * @param initFormat Sending video orientation always in upright position when set to true;
     * Set to false on device rotation requires the remote device to rotate accordingly to view image upright
     */
    public void initPreviewOrientation(boolean initFormat) {
        // Set preview display orientation according to device rotation
        if (initFormat) {
            mPreviewOrientation = CameraUtils.getPreviewOrientation(mCameraId);
        }
        else {
            mPreviewOrientation = mCameraAbility.getPropertyValue(SENSOR_ORIENTATION);
        }

        // Streaming video always send in the user selected dimension and image view in upright orientation
        mSwap = (mPreviewOrientation == 90) || (mPreviewOrientation == 270);

        if (mSwap) {
            mPreviewSize = new Dimension(optimizedSize.height, optimizedSize.width);
        }
        else {
            mPreviewSize = optimizedSize;
        }
        mFormat.setVideoSize(mPreviewSize);

        // front-facing camera; take care android flip the video for front facing lens camera
        CameraKit cameraManager = aTalkApp.getCameraManager();
        CameraInfo cameraInfo = cameraManager.getCameraInfo(mCameraId);
        int facing = cameraInfo.getFacingType();
        if (FacingType.CAMERA_FACING_FRONT == facing) {
            mFlip = (mPreviewOrientation == 180) || (!aTalkApp.isPortrait && mPreviewOrientation == 270);
        }
        else {
            mFlip = (mPreviewOrientation == 90 || mPreviewOrientation == 180);
        }
        Timber.d("Camera preview orientation: %s; portrait: %s; swap: %s; flip: %s; format: %s",
                mPreviewOrientation, aTalkApp.isPortrait, mSwap, mFlip, mFormat);
    }

    /**
     * {@link CameraStateCallback} is called when {@link Camera} changes its status.
     */
    private final CameraStateCallback mStateCallback = new CameraStateCallback() {
        @Override
        public void onConfigured(Camera camera) {
            updateCaptureRequest();
        }

        @Override
        public void onConfigureFailed(Camera camera, int errorCode) {
            onCreateFailed(camera.getCameraId(), errorCode);
        }

        @Override
        public void onCreated(Camera camera) {
            super.onCreated(camera);
            mCamera = camera;
            onInitPreview();
            mCameraOpenCloseLock.release();
        }

        @Override
        public void onCreateFailed(String cameraId, int errorCode) {
            String errMessage;
            switch (errorCode) {
                case ErrorCode.ERROR_CAMERA_ALREADY_IN_USE:
                    errMessage = "Camera already in use";
                    break;
                case ErrorCode.ERROR_CAMERA_DEVICE_DISABLED:
                    errMessage = "Device policy";
                    break;
                case ErrorCode.ERROR_CAMERA_DEVICE_FATAL:
                    errMessage = "Fatal (device)";
                    break;
                case ErrorCode.ERROR_CAMERA_RESOURCE_LIMITED:
                    errMessage = "Cameras resource limited";
                    break;
                case ErrorCode.ERROR_CAMERA_SERVICE_FATAL:
                    errMessage = "Fatal (service)";
                    break;
                default:
                    errMessage = "Camera unKnown";
            }
            Timber.e("Set camera preview failed: %s", errMessage);
            aTalkApp.showGenericError(ResourceTable.String_video_format_not_supported, mPreviewSize, errMessage);

            mCamera.release();
            mCamera = null;
            mCameraOpenCloseLock.release();
        }

        @Override
        public void onFatalError(Camera camera, int errorCode) {
            super.onFatalError(camera, errorCode);
            onCreateFailed(camera.getCameraId(), errorCode);
        }

        @Override
        public void onReleased(Camera camera) {
            mCamera.release();
            mCamera = null;
            mCameraOpenCloseLock.release();
        }
    };

    /**
     * Method called before camera preview is started. Extending classes should configure preview at this point.
     */
    protected abstract void onInitPreview();

    /**
     * Method called to start camera image capture.
     */
    protected abstract void updateCaptureRequest();

    /**
     * Selects stream formats.
     *
     * @return stream formats.
     */
    private Format[] getStreamFormats() {
        FormatControl[] formatControls = dataSource.getFormatControls();
        final int count = formatControls.length;
        Format[] streamFormats = new Format[count];

        for (int i = 0; i < count; i++) {
            FormatControl formatControl = formatControls[i];
            Format format = formatControl.getFormat();

            if (format == null) {
                Format[] supportedFormats = formatControl.getSupportedFormats();
                if ((supportedFormats != null) && (supportedFormats.length > 0)) {
                    format = supportedFormats[0];
                }
            }
            // Timber.d("getStreamFormats Idx: %s/%s; format: %s", i, count, format);
            streamFormats[i] = format;
        }
        return streamFormats;
    }

    /*
      * protected Format doGetFormat()
      * cmeng: mFormat is always null in all doGetFormat; serving no purpose
      * Instead via buffer.setFormat(mFormat) in PreviewStream#read(Buffer buffer); a safer approach
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop()
            throws IOException {
        closeCamera();
        stopBackgroundThread();
        super.stop();
    }

    /**
     * Closes the current {@link Camera}.
     */
    protected void closeCamera() {
        if (mCamera != null) {
            try {
                inTransition = true;
                mCameraOpenCloseLock.acquire();
                mCamera.stopLoopingCapture();
                mCamera.release();
                mCamera = null;

                // (PreviewStream.java:165): OnImage available exception: buffer is inaccessible
                if (null != mImageReceiver) {
                    mImageReceiver.release();
                    mImageReceiver = null;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to close camera.", e);
            } finally {
                mCameraOpenCloseLock.release();
            }
        }
    }

    /**
     * Triggered on device rotation to init the remote video orientation sending;
     * initFormat == true has synchronised problem between imageReader data and YUV swap if not handled properly
     * <p>
     * On Samsung J7 implementation, seems at times mCaptureSession and mCameraDevice can be null etc;
     * If exception happen, then reInit the whole camera sequence
     *
     * @param initFormat Sending video orientation always in upright position when set to true;
     * Set to false on device rotation requires the remote device to rotate accordingly to view image upright
     */
    public void initPreviewOnRotation(boolean initFormat) {
        if (initFormat && mCamera != null) {
            inTransition = true;
            try {
                mCamera.stopLoopingCapture();
                initPreviewOrientation(true);
                updateCaptureRequest();
                return;
            } catch (Exception e) {
                Timber.e("Close capture session exception: %s", e.getMessage());
            }
            reInitCamera();
        }
        else {
            initPreviewOrientation(false);
        }
    }

    private void reInitCamera() {
        VideoHandlerSlice videoFragment = VideoCallAbility.getVideoSlice();
        closeCamera();

        if (videoFragment.isLocalVideoEnabled()) {
            try {
                start();
            } catch (IOException e) {
                aTalkApp.showToastMessage(ResourceTable.String_video_format_not_supported, mCameraId, e.getMessage());
            }
        }
    }

    /**
     * Switch to the user selected lens facing camera. Start data streaming only if local video is enabled
     * User needs to enable the local video to send the video stream to remote user.
     *
     * @param cameraLocator MediaLocator
     * @param isLocalVideoEnable true is local video is enabled for sending
     */
    public void switchCamera(MediaLocator cameraLocator, boolean isLocalVideoEnable) {
        OhosCamera.setSelectedCamera(cameraLocator);
        mCameraId = OhosCamera.getCameraId(cameraLocator);

        // Stop preview and release the current camera if any before switching, otherwise app will crash
        Timber.d("Switching camera: %s", cameraLocator.toString());
        closeCamera();
        if (isLocalVideoEnable) {
            try {
                start();
            } catch (IOException e) {
                aTalkApp.showToastMessage(ResourceTable.String_video_format_not_supported, cameraLocator, e.getMessage());
            }
        }
    }

    /**
     * Calculates fps statistics.
     *
     * @return time elapsed in millis between subsequent calls to this method.
     */
    protected long calcStats() {
        // Measure moving average
        long current = System.currentTimeMillis();
        long delay = (current - last);
        last = System.currentTimeMillis();
        avg[idx] = delay;
        if (++idx == avg.length)
            idx = 0;
        long movAvg = 0;
        for (long anAvg : avg) {
            movAvg += anAvg;
        }
        Timber.log(TimberLog.FINER, "Avg frame rate: %d", (1000 / (movAvg / avg.length)));
        return delay;
    }

    // ===============================================================
    private void startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = EventRunner.create("CameraBackground");
            backgroundThread.run();
            mBackgroundHandler = new EventHandler(backgroundThread);
        }
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.stop();
            backgroundThread = null;
            mBackgroundHandler = null;
            // Timber.e(e, "Stop background thread exception: %s", e.getMessage());
        }
    }
}
