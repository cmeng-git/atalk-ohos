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

import ohos.agp.graphics.Surface;
import ohos.agp.graphics.TextureHolder;
import ohos.media.camera.device.Camera.FrameConfigType;
import ohos.media.camera.device.CameraConfig;

import org.atalk.impl.neomedia.NeomediaServiceUtils;
import org.atalk.impl.neomedia.codec.video.MediaEncoder;
import org.atalk.impl.neomedia.device.DeviceConfiguration;
import org.atalk.impl.neomedia.device.util.CameraSurfaceRenderer;
import org.atalk.impl.neomedia.device.util.CodecInputSurface;
import org.atalk.impl.neomedia.device.util.OpenGLContext;
import org.atalk.impl.neomedia.device.util.OpenGlCtxProvider;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.gui.call.VideoCallAbility;
import org.atalk.ohos.gui.call.VideoHandlerSlice;

import java.awt.Dimension;
import java.io.IOException;
import javax.media.Buffer;
import javax.media.control.FormatControl;
import timber.log.Timber;

import static ohos.media.camera.params.Metadata.AfMode.AF_MODE_CONTINUOUS;

/**
 * Camera stream that uses <code>Surface</code> to capture video data. First input <code>Surface</code> is
 * obtained from <code>Codec</code>. Then it is passed as preview surface to the camera init.
 * Note: <code>Surface</code> instance is passed through buffer objects in read method;
 * this stream #onInitPreview() won't start until it is provided.
 * <p>
 * In order to display local camera preview in the app, <code>TextureView</code> is created in video
 * call <code>Ability</code>. It is used to create Open GL context that shares video texture and can
 * render it. Rendering is done here on camera capture <code>Thread</code>.
 *
 * @author Eng Chong Meng
 */
public class SurfaceStream extends CameraStreamBase implements TextureHolder.OnNewFrameCallback {
    /**
     * <code>OpenGlCtxProvider</code> used by this instance.
     */
    private OpenGlCtxProvider myCtxProvider;

    /**
     * TextureView for local preview display
     */
    private OpenGLContext mDisplayTV;

    /**
     * Codec input surface obtained from <code>MediaCodec</code> for remote video streaming.
     */
    private CodecInputSurface mEncoderSurface;

    private CameraSurfaceRenderer mSurfaceRender;

    /**
     * SurfaceTexture that receives the output from the camera preview
     */
    private TextureHolder mSurfaceTexture;

    private Surface mPreviewSurface;

    /**
     * Capture thread.
     */
    private Thread captureThread;

    /**
     * Flag used to stop capture thread.
     */
    private boolean run = false;

    /**
     * guards frameAvailable
     */
    private final Object frameSyncObject = new Object();

    private boolean frameAvailable;

    /**
     * Object used to synchronize local preview painting.
     */
    private final Object paintLock = new Object();

    /**
     * Flag indicates that the local preview has been painted.
     */
    private boolean paintDone;

    /**
     * Creates a new instance of <code>SurfaceStream</code>.
     *
     * @param parent parent <code>DataSource</code>.
     * @param formatControl format control used by this instance.
     */
    SurfaceStream(DataSource parent, FormatControl formatControl) {
        super(parent, formatControl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start()
            throws IOException {
        super.start();
        if (captureThread == null)
            startCaptureThread();
        else
            startImpl();
    }

    // Start the captureThread
    private void startCaptureThread() {
        captureThread = new Thread() {
            @Override
            public void run() {
                captureLoop();
            }
        };
        run = true;
        captureThread.start();
    }

    /**
     * Create all the renderSurfaces for the EGL drawFrame. The creation must be performed in the
     * same captureThread for GL and CameraSurfaceRenderer to work properly
     * Note: onInitPreview is executed in the mBackgroundHandler thread
     *
     * @param surface Encoder Surface object obtained from <code>MediaCodec</code> via read().
     *
     * @see MediaEncoder #configureMediaCodec(Codec, String)
     * link: https://www.khronos.org/registry/EGL/sdk/docs/man/html/eglMakeCurrent.xhtml
     */
    private void initSurfaceConsumer(Surface surface) {
        // Get user selected default video resolution
        DeviceConfiguration deviceConfig = NeomediaServiceUtils.getMediaServiceImpl().getDeviceConfiguration();
        Dimension videoSize = deviceConfig.getVideoSize();

        /*
         * Init the TextureView / SurfaceTexture for the local preview display:
         * Must setup the local preview container size before proceed to obtainObject;
         * Otherwise onSurfaceTextureAvailable()#SurfaceTexture will not have the correct aspect ratio;
         * and the local preview image size is also not correct even with setAspectRatio()
         *
         * Need to setViewSize() for use in initLocalPreviewContainer
         * Note: Do not change the following execution order
         */
        VideoHandlerSlice videoSlice = VideoCallAbility.getVideoSlice();
        myCtxProvider = videoSlice.mLocalPreviewGlCtxProvider;
        myCtxProvider.setVideoSize(videoSize);
        videoSlice.initLocalPreviewContainer(myCtxProvider);
        mDisplayTV = myCtxProvider.obtainObject(); // this will create a new TextureView

        // Init the encoder inputSurface for remote video streaming
        mEncoderSurface = new CodecInputSurface(surface, mDisplayTV.getEGLContext());
        mEncoderSurface.makeCurrent();

        // Init the surface for capturing the camera image for remote video streaming, and local preview display
        mSurfaceRender = new CameraSurfaceRenderer();
        mSurfaceRender.surfaceCreated();
        mSurfaceTexture = new TextureHolder(mSurfaceRender.getTextureId());
        // mSurfaceTexture.bindToGPUContext(mSurfaceRender.getTextureId());
        mSurfaceTexture.setOnNewFrameCallback(this);

        mPreviewSurface = new Surface();
        mPreviewSurface.bindToTextureHolder(mSurfaceTexture);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onInitPreview() {
        // Init capturing parameters for camera image for remote video streaming, and local preview display
        // @see also initSurfaceConsumer();
        // Need to do initLocalPreviewContainer here to take care of device orientation change
        // https://developer.android.com/reference/android/hardware/camera2/CameraDevice.html#createCaptureSession(android.hardware.camera2.params.SessionConfiguration)
        myCtxProvider.setVideoSize(optimizedSize);
        VideoCallAbility.getVideoSlice().initLocalPreviewContainer(myCtxProvider);
        mSurfaceTexture.setBufferDimension(optimizedSize.width, optimizedSize.height);

        mCameraConfigBuilder = mCamera.getCameraConfigBuilder();
        if (mCameraConfigBuilder != null) {
            CameraConfig cameraConfig = mCameraConfigBuilder
                    .addSurface(mPreviewSurface)
                    .build();
            mCamera.configure(cameraConfig);
        }
        Timber.d("Camera stream update preview: %s; %s; size %s (%s)", mFormat, mPreviewSurface, mPreviewSize, optimizedSize);
    }

    /**
     * Update the camera preview. {@link # startPreview()} needs to be called in advance.
     */
    protected void updateCaptureRequest() {
        // Abort if the camera is already closed.
        if (null == mCamera) {
            Timber.e("Camera capture session config - camera closed, return");
            return;
        }

        // Auto focus should be continuous for camera preview.
        mFrameConfigBuilder = mCamera.getFrameConfigBuilder(FrameConfigType.FRAME_CONFIG_PREVIEW);
        mFrameConfigBuilder.addSurface(mPreviewSurface)
                // .setImageRotation(90)
                .setAfMode(AF_MODE_CONTINUOUS, null);

        mCamera.triggerLoopingCapture(mFrameConfigBuilder.build());
    }

    /**
     * Capture thread loop.
     */
    private void captureLoop() {
        // Wait for input surface to be returned before proceed
        // Post an empty frame to init encoder, and get the surface that is provided in read() method
        while (run && (mCamera == null)) {
            transferHandler.transferData(this);
        }

        while (run) {
            // loop if camera switching is in progress or capture session is setting up
            if (mFrameConfigBuilder == null || inTransition)
                continue;

            // Start the image acquire process from the surfaceView
            acquireNewImage();

            /*
             * Renders the preview on main thread for the local preview, return only on paintDone;
             */
            paintLocalPreview();

            long delay = calcStats();
            if (delay < 80) {
                try {
                    long wait = 80 - delay;
                    // Timber.d("Delaying frame: %s", wait);
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            /*
             * Push the received image frame to the android encoder; must be executed within onFrameAvailable
             * paintLocalPreview#mTextureRender.drawFrame(mSurfaceTexture) must not happen while in read();
             * else the new local preview video is streaming instead.
             */
            pushEncoderData();
        }
    }

    /**
     * Latches the next buffer into the texture. Must be called from the thread that created the OutputSurface object.
     * Wait for a max of 2.5s Timer
     */
    private void acquireNewImage() {
        final int TIMEOUT_MS = 2500;

        // Timber.d("Waiting for onFrameAvailable!");
        synchronized (frameSyncObject) {
            while (!frameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us. Use a timeout to avoid stalling the test if it doesn't arrive.
                    frameSyncObject.wait(TIMEOUT_MS);
                    if (!frameAvailable) {
                        throw new RuntimeException("Camera frame wait timed out");
                    }
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }
            }
            frameAvailable = false;
        }
        mSurfaceRender.checkGlError("before updateTexImage");
        mSurfaceTexture.refreshTextureImage();
    }

    /**
     * The SurfaceTexture uses this to signal the availability of a new frame.  The
     * thread that "owns" the external texture associated with the SurfaceTexture (which,
     * by virtue of the context being shared, *should* be either one) needs to call
     * updateTexImage() to latch the buffer i.e. the acquireNewImage in captureThread.
     *
     * @param st the SurfaceTexture that set for this callback
     */
    @Override
    public void onNewFrame(TextureHolder st) {
        synchronized (frameSyncObject) {
            frameAvailable = true;
            frameSyncObject.notifyAll();
        }
    }

    /**
     * Paints the local preview on UI thread by posting paint job and waiting for the UI handler to complete its job.
     */
    private void paintLocalPreview() {
        paintDone = false;
        BaseAbility.runOnUiThread(() -> {
            try {
                // OpenGLContext mDisplayTV = myCtxProvider.tryObtainObject();
                /*
                 * Must wait until local preview frame is posted to the TextureSurface#onSurfaceTextureUpdated,
                 * otherwise we will freeze on trying to set the current context. We skip the frame in this case.
                 */
                if (!myCtxProvider.textureUpdated) {
                    Timber.w("Skipped preview frame, previewCtx: %s textureUpdated: %s", mDisplayTV, false);
                }
                else {
                    // myCtxProvider.configureTransform(myCtxProvider.getComponent().getWidth(), myCtxProvider.getComponent().getHeight());
                    mDisplayTV.makeCurrent();
                    mSurfaceRender.drawFrame(mSurfaceTexture);
                    mDisplayTV.swapBuffers();

                    /*
                     * If current context is not unregistered the main thread will freeze: at
                     * com.google.android.gles_jni.EGLImpl.eglMakeCurrent(EGLImpl.java:-1) at
                     * android.view.HardwareRenderer$GlRenderer.checkRenderContextUnsafe(HardwareRenderer.java:1767) at
                     * android.view.HardwareRenderer$GlRenderer.draw(HardwareRenderer.java:1438)
                     * at android.view.ViewRootImpl.draw(ViewRootImpl.java:2381) .... at
                     * com.android.internal.os.ZygoteInit.main(ZygoteInit.java:595) at
                     * dalvik.system.NativeStart.main(NativeStart.java:-1)
                     * cmeng: not required in camera2
                     */
                    // mDisplayTV.releaseEGLSurfaceContext();
                    myCtxProvider.textureUpdated = false;
                }
            } finally {
                synchronized (paintLock) {
                    paintDone = true;
                    paintLock.notifyAll();
                }
            }
        });

        // Wait for the main thread to finish painting before return to caller
        synchronized (paintLock) {
            if (!paintDone) {
                try {
                    paintLock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Pushes the received image frame to the ohos encoder; to be retrieve in ...
     *
     * @see #read(Buffer)
     * this must be executed within the SurfaceTextureManage#onFrameAvailable() thread;
     * Only happen in camera2 implementation
     */
    private void pushEncoderData() {
        // Pushes the received image frame to the android encoder input surface
        // mEncoderSurface.makeCurrent();
        // myCtxProvider.configureTransform(mPreviewSize.width, mPreviewSize.height);

        mSurfaceRender.drawFrame(mSurfaceTexture);
        mEncoderSurface.setPresentationTime(mSurfaceTexture.getLastRefreshTextureImageTime());
        mEncoderSurface.swapBuffers();
        transferHandler.transferData(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void read(Buffer buffer)
            throws IOException {
        Surface surface;
        if (mFrameConfigBuilder != null) {
            buffer.setFormat(mFormat);
            buffer.setTimeStamp(mSurfaceTexture.getLastRefreshTextureImageTime());
        }
        // Init mEncoderSurface only once; else EGL error: 0x3003: connect: already connected to another API?
        else if (mCamera == null && (surface = (Surface) buffer.getData()) != null) {
            if (mEncoderSurface == null) {
                initSurfaceConsumer(surface);
                startImpl();
            } else {
                Timber.w("Skip encoder surface re-creation: %s", mEncoderSurface);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop()
            throws IOException {
        run = false;
        if (captureThread != null) {
            try {
                captureThread.join();
                captureThread = null;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        super.stop();
        if (mSurfaceRender != null) {
            mSurfaceRender.release();
            mSurfaceRender = null;
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.setOnNewFrameCallback(null);
            // mSurfaceTexture.unbindFromGPUContext();
            mSurfaceTexture.abandon();
            mSurfaceTexture = null;
        }

        // null if the graph realization cannot proceed due to unsupported codec
        if (myCtxProvider != null)
            myCtxProvider.onObjectReleased();
    }
}
