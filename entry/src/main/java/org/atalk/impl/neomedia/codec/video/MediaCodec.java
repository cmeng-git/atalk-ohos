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
package org.atalk.impl.neomedia.codec.video;

import ohos.agp.graphics.Surface;
import ohos.media.codec.Codec;
import ohos.media.common.BufferInfo;
import ohos.media.common.Source.VideoSource;

import org.atalk.impl.neomedia.codec.AbstractCodec2;
import org.atalk.service.neomedia.codec.Constants;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import timber.log.Timber;

/**
 * Abstract codec class uses ohos <code>Codec</code> for video decoding/encoding.
 * The codec is always run with surface enabled.
 *
 * @author Eng Chong Meng
 */
abstract class MediaCodec extends AbstractCodec2 {
    /**
     * <code>Codec</code> used by this instance.
     */
    private Codec codec;

    /**
     * Indicates that this instance is used for encoding(and not for decoding).
     */
    private final boolean isEncoder;

    /**
     * Input <code>Codec</code> buffer.
     */
    ByteBuffer codecInputBuf;

    /**
     * Output <code>Codec</code> buffer.
     */
    ByteBuffer codecOutputBuf;

    /**
     * <code>BufferInfo</code> object that stores codec buffer information.
     */
    BufferInfo mBufferInfo = new BufferInfo();

    /**
     * Creates a new instance of <code>MediaCodec</code>.
     *
     * @param name the <code>PlugIn</code> name of the new instance
     * @param formatClass the <code>Class</code> of input and output <code>Format</code>s supported by the new instance
     * @param supportedOutputFormats the list of <code>Format</code>s supported by the new instance as output.
     * @param isEncoder true if codec is encoder.
     */
    protected MediaCodec(String name, Class<? extends Format> formatClass, Format[] supportedOutputFormats, boolean isEncoder) {
        super(name, formatClass, supportedOutputFormats);
        this.isEncoder = isEncoder;
    }

    /**
     * Returns <code>Surface</code> used by this instance for encoding or decoding.
     *
     * @return <code>Surface</code> used by this instance for encoding or decoding.
     */
    protected abstract Surface getSurface();

    /**
     * Template method used to configure <code>Codec</code> instance. Called before starting the codec.
     *
     * @param codec <code>Codec</code> instance to be configured.
     * @param codecType string codec media type.
     *
     * @throws ResourceUnavailableException Resource Unavailable Exception if not supported
     */
    protected abstract void configureMediaCodec(Codec codec, String codecType)
            throws ResourceUnavailableException;

    /**
     * Selects color format used for the codec.
     * VideoSource.SURFACE: indicates that the surface is used as the video source
     *
     * @return used <code>MediaFormat</code> color format.
     */
    protected int getColorFormat() {
        return VideoSource.SURFACE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doClose() {
        if (codec != null) {
            try {
                // Throws IllegalStateException – if in the Released state.
                codec.stopVideoSurface();
                codec.stop();
                codec.release();
            } catch (IllegalStateException e) {
                Timber.w("Codec stop exception: %s", e.getMessage());
            } finally {
                codec = null;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doOpen()
            throws ResourceUnavailableException {
        String codecType;
        String encoding = isEncoder ? outputFormat.getEncoding() : inputFormat.getEncoding();

        switch (encoding) {
            case Constants.VP9:
                codecType = Format.VIDEO_VP9;
                break;
            case Constants.VP8:
                codecType = Format.VIDEO_VP8;
                break;
            case Constants.H264:
                codecType = Format.VIDEO_AVC;
                break;
            default:
                throw new RuntimeException("Unsupported encoding: " + encoding);
        }

        CodecInfo codecInfo = CodecInfo.getCodecForType(codecType, isEncoder);
        if (codecInfo == null) {
            throw new ResourceUnavailableException("No " + getStrName() + " found for type: " + codecType);
        }
        codec = isEncoder ? Codec.createEncoder() : Codec.createDecoder();
        configureMediaCodec(codec, codecType);
        codec.start();
    }

    private String getStrName() {
        return isEncoder ? "encoder" : "decoder";
    }

    /**
     * {@inheritDoc}
     * <p>
     * Exception: IllegalStateException thrown by codec.dequeueOutputBuffer or codec.dequeueInputBuffer
     * Any RuntimeException will close remote view container.
     */
    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer) {
        try {
            return doProcessImpl(inputBuffer, outputBuffer);
        } catch (Exception e) {
            Timber.e(e, "Do process for codec: %s; Exception: %s", codec.getClass(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Process the video stream:
     * We will first process the output data from the mediaCodec; then we will feed input into the decoder.
     *
     * @param inputBuffer input buffer
     * @param outputBuffer output buffer
     *
     * @return process status
     */
    private int doProcessImpl(Buffer inputBuffer, Buffer outputBuffer) {
        Format outputFormat = this.outputFormat;
        int processed = INPUT_BUFFER_NOT_CONSUMED | OUTPUT_BUFFER_NOT_FILLED;

        // Process the output data from the codec
        // Returns the index of an output buffer that has been successfully decoded or one of the INFO_* constants.
        int outputBufferIdx = 0; // codec.dequeueOutputBuffer(mBufferInfo, 0);

        if (outputBufferIdx == INFO_OUTPUT_FORMAT_CHANGED) {
            Format outFormat = (Format) codec.getBufferFormat(codecOutputBuf);
            Timber.d("Codec output format changed (encoder: %s): %s", isEncoder, outFormat);
            // Video size should be known at this point
            Dimension videoSize = new Dimension(outFormat.getIntValue(Format.KEY_WIDTH_SCOPE), outFormat.getIntValue(Format.KEY_HEIGHT_SCOPE));
            onSizeChanged(videoSize);
        }
        else if (outputBufferIdx >= 0) {
            // Timber.d("Reading output: %s:%s flag: %s", mBufferInfo.offset, mBufferInfo.size, mBufferInfo.flags);
            int outputLength = 0;
            codecOutputBuf = null;
            try {
                if (!isEncoder) {
                    processed &= ~OUTPUT_BUFFER_NOT_FILLED;
                    outputBuffer.setFormat(outputFormat);
                    // Timber.d("Codec output format: %s", outputFormat);
                }
                else if ((outputLength = mBufferInfo.size) > 0) {
                    codecOutputBuf = codec.getAvailableBuffer(0);
                    codecOutputBuf.position(mBufferInfo.offset);
                    codecOutputBuf.limit(mBufferInfo.offset + mBufferInfo.size);

                    byte[] out = AbstractCodec2.validateByteArraySize(outputBuffer, mBufferInfo.size, false);
                    codecOutputBuf.get(out, 0, mBufferInfo.size);

                    outputBuffer.setFormat(outputFormat);
                    outputBuffer.setLength(outputLength);
                    outputBuffer.setOffset(0);

                    processed &= ~OUTPUT_BUFFER_NOT_FILLED;
                }
            } finally {
                if (codecOutputBuf != null)
                    codecOutputBuf.clear();
                /*
                 * releaseOutputBuffer: the output buffer data will be forwarded to SurfaceView for render if true.
                 * see https://developer.android.com/reference/android/media/MediaCodec
                 */
                codec.flush();
            }
            /*
             * We will first exhaust the output of the mediaCodec, and then we will feed input into it.
             */
            if (outputLength > 0)
                return processed;
        }

        else if (outputBufferIdx != INFO_TRY_AGAIN_LATER) {
            Timber.w("Codec output reports: %s", outputBufferIdx);
        }

        // Feed more data to the decoder.
        if (isEncoder) {
            inputBuffer.setData(getSurface());
            processed &= ~INPUT_BUFFER_NOT_CONSUMED;
        }
        else {
            int inputBufferIdx = codec.getInputBufferIndex(0);
            if (inputBufferIdx >= 0) {
                byte[] buf_data = (byte[]) inputBuffer.getData();
                int buf_offset = inputBuffer.getOffset();
                int buf_size = inputBuffer.getLength();

                codecInputBuf = codec.getAvailableBuffer(0);
                if (codecInputBuf.capacity() < buf_size) {
                    throw new RuntimeException("Input buffer too small: " + codecInputBuf.capacity() + " < " + buf_size);
                }

                codecInputBuf.clear();
                codecInputBuf.put(buf_data, buf_offset, buf_size);
                // codec.queueInputBuffer(inputBufferIdx, 0, buf_size, inputBuffer.getTimeStamp(), 0);
                mBufferInfo.setInfo(0, buf_size, inputBuffer.getTimeStamp(), 0, inputBufferIdx);
                codec.writeBuffer(codecInputBuf, mBufferInfo);

                Timber.d("Fed input with %s bytes of data; Offset: %s.", buf_size, buf_offset);
                processed &= ~INPUT_BUFFER_NOT_CONSUMED;
            }
            else if (inputBufferIdx != MediaCodec.INFO_TRY_AGAIN_LATER) {
                Timber.w("Codec input reports: %s", inputBufferIdx);
            }
        }
        return processed;
    }

    /**
     * Method fired when <code>Codec</code> detects video size.
     *
     * @param dimension video dimension.
     *
     * @see MediaDecoder#onSizeChanged(Dimension)
     */
    protected void onSizeChanged(Dimension dimension) {
    }
}
