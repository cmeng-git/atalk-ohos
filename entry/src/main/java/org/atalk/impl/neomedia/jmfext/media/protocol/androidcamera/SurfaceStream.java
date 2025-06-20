/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.impl.neomedia.jmfext.media.protocol.androidcamera;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.media.MediaCodec;
import android.view.Surface;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Collections;

import javax.media.Buffer;
import javax.media.control.FormatControl;

import org.atalk.impl.neomedia.NeomediaServiceUtils;
import org.atalk.impl.neomedia.codec.video.AndroidEncoder;
import org.atalk.impl.neomedia.device.DeviceConfiguration;
import org.atalk.impl.neomedia.device.util.CameraSurfaceRenderer;
import org.atalk.impl.neomedia.device.util.CodecInputSurface;
import org.atalk.impl.neomedia.device.util.OpenGLContext;
import org.atalk.impl.neomedia.device.util.OpenGlCtxProvider;
import org.atalk.impl.timberlog.TimberLog;

import org.atalk.ohos.BaseActivity;
import org.atalk.ohos.gui.call.VideoCallActivity;
import org.atalk.ohos.gui.call.VideoHandlerFragment;

import timber.log.Timber;

/**
 * Camera stream that uses <code>Surface</code> to capture video data. First input <code>Surface</code> is
 * obtained from <code>MediaCodec</code>. Then it is passed as preview surface to the camera init.
 * Note: <code>Surface</code> instance is passed through buffer objects in read method;
 * this stream #onInitPreview() won't start until it is provided.
 * <p>
 * In order to display local camera preview in the app, <code>TextureView</code> is created in video
 * call <code>Activity</code>. It is used to create Open GL context that shares video texture and can
 * render it. Rendering is done here on camera capture <code>Thread</code>.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class SurfaceStream extends CameraStreamBase {
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
    private SurfaceTexture mSurfaceTexture;
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
     * @see AndroidEncoder#configureMediaCodec(MediaCodec, String)
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
        VideoHandlerFragment videoFragment = VideoCallActivity.getVideoFragment();
        myCtxProvider = videoFragment.mLocalPreviewGlCtxProvider;
        myCtxProvider.setVideoSize(videoSize);
        videoFragment.initLocalPreviewContainer(myCtxProvider);
        mDisplayTV = myCtxProvider.obtainObject(); // this will create a new TextureView

        // Init the encoder inputSurface for remote video streaming only once; do not recreate openGL surface.
        mEncoderSurface = new CodecInputSurface(surface, mDisplayTV.getContext());
        mEncoderSurface.makeCurrent();

        // Init the surface for capturing the camera image for remote video streaming, and local preview display
        mSurfaceRender = new CameraSurfaceRenderer();
        mSurfaceRender.surfaceCreated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onInitPreview() {
        try {
            // Init capturing parameters for camera image for remote video streaming, and local preview display
            // @see also initSurfaceConsumer();
            // Need to do initLocalPreviewContainer here to take care of device orientation change
            // https://developer.android.com/reference/android/hardware/camera2/CameraDevice.html#createCaptureSession(android.hardware.camera2.params.SessionConfiguration)
            myCtxProvider.setVideoSize(optimizedSize);
            VideoCallActivity.getVideoFragment().initLocalPreviewContainer(myCtxProvider);

            // Init the surface for capturing the camera image for remote video streaming, and local preview display
            mSurfaceTexture = new SurfaceTexture(mSurfaceRender.getTextureId());
            mSurfaceTexture.setDefaultBufferSize(optimizedSize.width, optimizedSize.height);
            mSurfaceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener, null);
            Surface mPreviewSurface = new Surface(mSurfaceTexture);

            mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureBuilder.addTarget(mPreviewSurface);
            Timber.d("Camera stream update preview: %s; %s; size %s (%s)", mFormat, mPreviewSurface, mPreviewSize, optimizedSize);

            // Has problem with this
            // mCaptureBuilder.addTarget(mEncoderSurface.getSurface());
            // mCameraDevice.createCaptureSession(Arrays.asList(mEncoderSurface.getSurface(), mPreviewSurface), //Collections.singletonList(mPreviewSurface),
            mCameraDevice.createCaptureSession(Collections.singletonList(mPreviewSurface), mSessionStateCallBack, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Timber.w("Surface stream onInitPreview exception: %s", e.getMessage());
        }
    }

    /**
     * Capture thread loop.
     */
    private void captureLoop() {
        // Wait for input surface to be returned before proceed;
        // Post an empty frame to init encoder surface, and get the surface that is provided in read() method
        while (run && (mCameraDevice == null)) {
            transferHandler.transferData(this);
        }

        while (run) {
            // loop if camera switching is in progress or capture session is setting up
            if (mCaptureSession == null || inTransition)
                continue;

            // Start the image acquire process from the surfaceView
            acquireNewImage();

            /*
             * Renders the preview on main thread for the local preview, return only on paintDone;
             */
            paintLocalPreview();

            // Calculate statistics for average frame rate if enable
            if (TimberLog.isTraceEnable)
                calcStats();

            /*
             * Push the received image frame to the android encoder; must be executed within onFrameAvailable
             * paintLocalPreview#mTextureRender.drawFrame(mSurfaceTexture) must not happen while in read();
             * else the new local preview video is streaming instead.
             */
            pushEncoderData();
        }
    }

    /*
     * The SurfaceTexture uses this to signal the availability of a new frame.  The
     * thread that "owns" the external texture associated with the SurfaceTexture (which,
     * by virtue of the context being shared, *should* be either one) needs to call
     * updateTexImage() to latch the buffer i.e. the acquireNewImage in captureThread.
     *
     * @param surfaceTexture the SurfaceTexture that set for this callback
     */
    private final SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener = surfaceTexture -> {
        synchronized (frameSyncObject) {
            frameAvailable = true;
            frameSyncObject.notifyAll();
        }
    };

    /**
     * Latches the next buffer frame data into the texture, notifies by mOnFrameAvailableListener.
     * Must be called from the thread that created the OutputSurface object.
     * Wait for a max of 2.5s Timer before abort.
     */
    private void acquireNewImage() {
        final int TIMEOUT_MS = 2500;

        // Timber.d("Waiting for onFrameAvailable!");
        synchronized (frameSyncObject) {
            while (!frameAvailable) {
                // Wait for TIMEOUT_MS for timeout to avoid stalling the test if it doesn't arrive.
                try {
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
        mSurfaceTexture.updateTexImage();
    }


    /**
     * Paints the local preview on UI thread by posting paint job and waiting for the UI handler to complete its job.
     */
    private void paintLocalPreview() {
        paintDone = false;
        BaseActivity.uiHandler.post(() -> {
            try {
                // OpenGLContext mDisplayTV = myCtxProvider.tryObtainObject();
                /*
                 * Must wait until local preview frame is posted to the TextureSurface#onSurfaceTextureUpdated,
                 * otherwise we will freeze on trying to set the current context. We skip the frame in this case.
                 */
                if (!myCtxProvider.textureUpdated) {
                    Timber.w("Skipped preview frame, previewCtx: %s", mDisplayTV);
                }
                else {
                    // myCtxProvider.configureTransform(myCtxProvider.getView().getWidth(), myCtxProvider.getView().getHeight());
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
     * Pushes the received image frame to the android encoder; to be retrieve in ...
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
        mEncoderSurface.setPresentationTime(mSurfaceTexture.getTimestamp());
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
        if (mCaptureSession != null) {
            buffer.setFormat(mFormat);
            buffer.setTimeStamp(mSurfaceTexture.getTimestamp());
        }
        // Init mEncoderSurface only once; else EGL error: 0x3003: connect: already connected to another API?
        else if (mCameraDevice == null && (surface = (Surface) buffer.getData()) != null) {
            if (mEncoderSurface == null) {
                initSurfaceConsumer(surface);
                startImpl();
            }
            else {
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
            mSurfaceTexture.setOnFrameAvailableListener(null);
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }

        // null if the graph realization cannot proceed due to unsupported codec
        if (myCtxProvider != null)
            myCtxProvider.onObjectReleased();
    }
}
