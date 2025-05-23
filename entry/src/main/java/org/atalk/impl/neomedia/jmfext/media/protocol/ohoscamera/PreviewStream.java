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

import ohos.agp.components.surfaceprovider.SurfaceProvider;
import ohos.agp.graphics.Surface;
import ohos.media.camera.device.Camera.FrameConfigType;
import ohos.media.camera.device.CameraConfig;
import ohos.media.image.Image;
import ohos.media.image.ImageReceiver;
import ohos.media.image.common.ImageFormat;
import ohos.media.image.common.ImageFormat.ComponentType;

import org.atalk.impl.neomedia.codec.AbstractCodec2;
import org.atalk.impl.neomedia.device.util.PreviewSurfaceProvider;
import org.atalk.impl.timberlog.TimberLog;
import org.atalk.ohos.gui.call.VideoCallAbility;
import org.atalk.ohos.gui.call.VideoHandlerSlice;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.media.Buffer;
import javax.media.control.FormatControl;
import timber.log.Timber;

import static ohos.media.camera.params.Metadata.AfMode.AF_MODE_CONTINUOUS;

/**
 * The video stream captures frames using camera2 OnImageAvailableListener callbacks in YUV420_888 format
 * as input; and covert it from multi-plane to single plane. The output is transformed/rotated according
 * to the camera orientation See {@link #YUV420PlanarRotate(Image, byte[], int, int)}.
 *
 * @author Eng Chong Meng
 */
public class PreviewStream extends CameraStreamBase {
    private static final int NUMBER_INT_2 = 2;
    private static final int NUMBER_INT_3 = 3;
    private static final int NUMBER_INT_5 = 5;

    /**
     * Buffers queue for camera2 YUV420_888 multi plan image buffered data
     */
    final private LinkedList<Image> bufferQueue = new LinkedList<>();

    private PreviewSurfaceProvider mSurfaceProvider;

    protected Surface mPreviewSurface = null;

    /**
     * Creates a new instance of <code>PreviewStream</code>.
     *
     * @param dataSource parent <code>DataSource</code>.
     * @param formatControl format control used by this instance.
     */
    public PreviewStream(DataSource dataSource, FormatControl formatControl) {
        super(dataSource, formatControl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start()
            throws IOException {
        super.start();
        startImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop()
            throws IOException {
        super.stop();
        // close the local video preview surface
        if (mSurfaceProvider != null)
            mSurfaceProvider.onObjectReleased();
    }

    /**
     * {@inheritDoc}
     * aTalk native camera acquired YUV420 preview is always in landscape mode
     */
    @Override
    protected void onInitPreview() {
        /*
         * set up the target surfaces for local video preview display; Before calling obtainObject(),
         * must setViewSize() for use in surfaceHolder.setFixedSize() on surfaceCreated
         * Then only set the local previewSurface size by calling initLocalPreviewContainer()
         * Note: Do not change the following execution order
         */
        VideoHandlerSlice videoSlice = VideoCallAbility.getVideoSlice();
        mSurfaceProvider = videoSlice.localPreviewSurface;
        mSurfaceProvider.setVideoSize(optimizedSize);
        Timber.d("Set surfaceSize (PreviewStream): %s", optimizedSize);

        SurfaceProvider surfaceProvider = mSurfaceProvider.obtainObject(); // this will create the surfaceView
        videoSlice.initLocalPreviewContainer(mSurfaceProvider);
        if (surfaceProvider.getSurfaceOps().isPresent())
            mPreviewSurface = surfaceProvider.getSurfaceOps().get().getSurface();

        // Setup ImageReader to retrieve image data for remote video streaming; maxImages = 4 and acquireLatestImage();
        // to fix problem with android camera2 API implementation in throwing waitForFreeSlotThenRelock on fast android devices.
        mImageReceiver = ImageReceiver.create(optimizedSize.width, optimizedSize.height, ImageFormat.YUV420_888, 4);
        mImageReceiver.setImageArrivalListener(new MyImageArrivalListener());

        // Need to add both the surface and the ImageReader surface as targets to the preview capture request:
        mCameraConfigBuilder = mCamera.getCameraConfigBuilder();
        if (mCameraConfigBuilder != null) {
            CameraConfig cameraConfig = mCameraConfigBuilder
                    .addSurface(mPreviewSurface)
                    .addSurface(mImageReceiver.getRecevingSurface())
                    .build();
            mCamera.configure(cameraConfig);
        }
    }

    /**
     * Update the camera capture request.
     * Start the camera capture session with repeating request for smoother video streaming.
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
                .setAfMode(AF_MODE_CONTINUOUS,null);

//        if (isRecording() && recorderSurface != null) {
//            mFrameConfigBuilder.addSurface(recorderSurface);
//            videoRecorder.start();
//        }

        mCamera.triggerLoopingCapture(mFrameConfigBuilder.build());
//        if (cameraCallback != null) {
//            cameraCallback.onCameraConfigured(CameraController.this);
//        }
            inTransition = false;
    }

    /*
     * To fix problem with android camera2 API implementation in throwing waitForFreeSlotThenRelock on fast android devices;
     * Setup ImageReader to retrieve image data for remote video streaming; maxImages = 3 and acquireLatestImage();
     * Use try wth resource in reader.acquireLatestImage() for any IllegalStateException;
     * Call #close to release buffer before camera can acquiring more.
     * Note: The acquired image is always in landscape mode e.g. 1280x720.
     */

    /**
     * MyImageArrivalListener to retrieve image onImageArrival.
     */
    private class MyImageArrivalListener implements ImageReceiver.IImageArrivalListener {
        private static final int BUFFER_NUM = 5;

        private final List<byte[]> buffers = new ArrayList<>(NUMBER_INT_5);

        private int index;

        MyImageArrivalListener() {
            for (int i = 0; i < BUFFER_NUM; i++) {
                buffers.add(new byte[optimizedSize.width * optimizedSize.height * NUMBER_INT_3 / NUMBER_INT_2]);
            }
        }

        private byte[] getBuffer() {
            index++;
            if (index == BUFFER_NUM) {
                index = 0;
            }
            return buffers.get(index);
        }

        @Override
        public void onImageArrival(ImageReceiver receiver) {
            Timber.d("onImageArrival is called");

            Image image = mImageReceiver.readNextImage();
            if (image != null && ImageFormat.YUV420_888 == image.getFormat()) {
//                byte[] bytes = getBuffer();
//                Image.Component component = image.getComponent(ImageFormat.ComponentType.YUV_Y);
//                component.read(bytes, 0, component.remaining());
//                component = image.getComponent(ImageFormat.ComponentType.YUV_U);
//                ByteBuffer mBuffer = component.getBuffer();
//                mBuffer.get(bytes, optimizedSize.width * optimizedSize.height,
//                        Math.min(mBuffer.remaining(), optimizedSize.width * optimizedSize.height / NUMBER_INT_2));
//                YUV420PlanarRotate(image, bytes, optimizedSize.width, optimizedSize.height);

//                if (isFrontCamera()) {
//                    bytes = isMirror
//                            ? Nv21Handler.rotateYuvDegree270AndMirror(bytes, resoluteX, resoluteY)
//                            : Nv21Handler.rotateYuv420Degree270(bytes, resoluteX, resoluteY);
//                }
//                else {
//                    bytes = Nv21Handler.rotateYuv420Degree90(bytes, resoluteX, resoluteY);
//                }

//                if (cameraCallback != null) {
//                    cameraCallback.onGetFrameResult(bytes);
//                }
//                image.release();

                // Calculate statistics for average frame rate if enable
                if (TimberLog.isTraceEnable)
                    calcStats();
                synchronized (bufferQueue) {
                    bufferQueue.addFirst(image);
                }
                transferHandler.transferData(PreviewStream.this);
            }
            else {
                Timber.e("onImageArrival is null!");
            }
        }
    }

    /**
     * Pop the oldest image in the bufferQueue for processing; i.e.
     * transformation and copy into the buffer for remote video data streaming
     * Note: Sync problem between device rotation with new swap/flip; inTransition get clear with old image data in process.
     * (PreviewStream.java:188)#lambda$new$0$PreviewStream: OnImage available exception: index=345623 out of bounds (limit=345600)
     *
     * @param buffer streaming data buffer to be filled
     *
     * @throws IOException on image buffer not accessible
     */
    @Override
    public void read(Buffer buffer)
            throws IOException {
        Image image;
        synchronized (bufferQueue) {
            image = bufferQueue.removeLast();
        }

        if (inTransition) {
            Timber.w("Discarded acquired image in transition @ packet read!");
            buffer.setDiscard(true);
        }
        else {
            // Camera actual preview dimension may not necessary be same as set remote video format when rotated
            int w = mFormat.getSize().width;
            int h = mFormat.getSize().height;
            int outLen = (w * h * 12) / 8;

            // Set the buffer timeStamp before YUV processing, as it may take some times
            // On J7: Timestamp seems implausible relative to expectedPresent if performs after the process
            buffer.setFormat(mFormat);
            buffer.setLength(outLen);
            buffer.setFlags(Buffer.FLAG_LIVE_DATA | Buffer.FLAG_RELATIVE_TIME);
            buffer.setTimeStamp(System.currentTimeMillis());

            byte[] bytes = AbstractCodec2.validateByteArraySize(buffer, outLen, false);
            try {
                YUV420PlanarRotate(image, bytes, w, h);
            } catch (Exception e) {
                Timber.w("YUV420Planar Rotate exception: %s", e.getMessage());
                buffer.setDiscard(true);
            }
        }
        image.release();
    }

    /**
     * <a href="http://www.wordsaretoys.com/2013/10/25/roll-that-camera-zombie-rotation-and-coversion-from-yv12-to-yuv420planar/">...</a>
     * Original code has been modified for camera2 UV420_888 and optimised for aTalk rotation without stretching the image
     * <p>
     * Transform android YUV420_888 image orientation according to camera orientation.
     * ## Swap: means swapping the x & y coordinates, which provides a 90-degree anticlockwise rotation,
     * ## Flip: means mirroring the image for a 180-degree rotation, adjusted for inversion by for camera2
     * Note: Android does have condition with Swap && Flip in display orientation
     *
     * @param image input image with multi-plane YUV428_888 format.
     * @param width final output stream image width.
     * @param height final output stream image height.
     */
    protected void YUV420PlanarRotate(Image image, byte[] output, int width, int height) {
        // Init w * h parameters: Assuming input preview buffer dimension is always in landscape mode
        int wi = width - 1;
        int hi = height - 1;

        // Output buffer: I420uvSize is a 1/4 of the Y size
        int ySize = width * height;
        int I420uvSize = ySize >> 2;

        // Input image buffer parameters
        ByteBuffer yBuffer = image.getComponent(ComponentType.YUV_Y).getBuffer();
        ByteBuffer uBuffer = image.getComponent(ComponentType.YUV_U).getBuffer();
        ByteBuffer vBuffer = image.getComponent(ComponentType.YUV_V).getBuffer();

        int yRowStride = image.getComponent(ComponentType.YUV_Y).rowStride;
        int yPixelStride = image.getComponent(ComponentType.YUV_Y).pixelStride;

        // The U/V planes are guaranteed to have the same row stride and pixel stride.
        int uvRowStride = image.getComponent(ComponentType.YUV_U).rowStride;
        int uvPixelStride = image.getComponent(ComponentType.YUV_U).pixelStride;

        // Performance input to output buffer transformation iterate over output buffer;
        // input index xi & yi are transformed according to swap & flip
        for (int yo = 0; yo < height; yo++) {
            for (int xo = 0; xo < width; xo++) {
                // default input index for direct 1:1 transform
                int xi = xo, yi = yo;

                // The video frame: w and h are swapped at input frame - no image stretch required
                if (mSwap && mFlip) {
                    xi = yo;
                    yi = wi - xo;
                }
                else if (mSwap) {
                    xi = hi - yo;
                    yi = xo;
                }
                else if (mFlip) {
                    xi = wi - xi;
                    yi = hi - yi;
                }
                // Transform Y luminous data bytes from input to output
                output[width * yo + xo] = yBuffer.get(yRowStride * yi + yPixelStride * xi);

                /*
                 * ## Transform UV data bytes - UV has only 1/4 of Y bytes:
                 * To locate a pixel in these planes, divide all the xi and yi coordinates by two;
                 * and using the UV parameters i.e. uvRowStride and uvPixelStride
                 */
                if ((yo % 2) + (xo % 2) == 0) // 1 UV byte for 2x2 Y data bytes
                {
                    int uv420YIndex = ySize + (width >> 1) * (yo >> 1);
                    int uo = uv420YIndex + (xo >> 1);
                    int vo = I420uvSize + uo;

                    int uvIdx = (uvRowStride * (yi >> 1)) + (uvPixelStride * (xi >> 1));

                    output[uo] = uBuffer.get(uvIdx); // Cb (U)
                    output[vo] = vBuffer.get(uvIdx); // Cr (V)
                }
            }
        }
    }
}