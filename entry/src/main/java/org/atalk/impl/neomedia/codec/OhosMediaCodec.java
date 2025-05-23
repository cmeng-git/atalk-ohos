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
package org.atalk.impl.neomedia.codec;

import ohos.agp.graphics.Surface.PixelFormat;
import ohos.media.codec.Codec;
import ohos.media.codec.CodecDescription;
import ohos.media.codec.CodecDescriptionList;
import ohos.media.common.BufferInfo;

import org.atalk.impl.neomedia.codec.video.AVFrame;
import org.atalk.impl.neomedia.codec.video.AVFrameFormat;
import org.atalk.impl.neomedia.codec.video.ByteBufferFmj;
import org.atalk.impl.neomedia.jmfext.media.protocol.ByteBufferPool;
import org.atalk.service.neomedia.codec.Constants;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.VideoFormat;
import timber.log.Timber;

import static org.atalk.ohos.gui.call.VideoHandlerSlice.DEFAULT_VIDEO_SIZE;

/**
 * Implements an FMJ <code>Codec</code> using Ohos {@link Codec}.
 *
 * @author Eng Chong Meng
 */
public class OhosMediaCodec extends AbstractCodec2 {
    /**
     * The map of FMJ <code>Format</code> encodings to Ohos Codec Format mime types
     * which allows converting between the two.
     */
    protected static final Map<String, String> FMJ_ENCODINGS_TO_MEDIA_CODEC_TYPES;

    /**
     * The map of <code>FFmpeg</code> pixel formats to ohos <code>PixelFormat</code>
     * which allows converting between the two.
     */
    protected static final Map<Integer, Integer> PIX_FMTS_TO_MEDIA_CODEC_COLOR_FORMATS;

    /**
     * The list of <code>Format</code>s of media data supported as input by <code>OhosMediaCodec</code> instances.
     */
    private static final Format[] SUPPORTED_INPUT_FORMATS;

    /**
     * The list of <code>Format</code>s of media data supported as output by <code>OhosMediaCodec</code> instances.
     */
    private static final Format[] SUPPORTED_OUTPUT_FORMATS;

    /**
     * List of color formats supported by subject <code>Codec</code>.
     */
    protected static final PixelFormat[] mPixelFormats = PixelFormat.values();

    /**
     * The constant defined by OpenMAX IL to signify that a <code>colorFormat</code> value defined in
     * the terms of Android's <code>MediaCodec</code> class is unknown.
     */
    private static final int OMX_COLOR_FormatUnused = 0;

    static {
        /*
         * OhosMediaCodec is an FMJ Codec and, consequently, defines the various formats of
         * media in FMJ terms. Ohos Codec is defined in its own terms. Make it possible
         * to translate between the two domains of terms.
         */
        FMJ_ENCODINGS_TO_MEDIA_CODEC_TYPES = new HashMap<String, String>() {{
            put(Constants.H264, ohos.media.common.Format.VIDEO_AVC);
            put(Constants.VP8, ohos.media.common.Format.VIDEO_VP8);
            put(Constants.VP9, ohos.media.common.Format.VIDEO_VP9);
        }};

        /*
         * The map of <code>FFmpeg</code> pixel formats to ohos <code>PixelFormat</code>
         * which allows converting between the two.
         */
        PIX_FMTS_TO_MEDIA_CODEC_COLOR_FORMATS = new HashMap<Integer, Integer>() {{
            put(FFmpeg.PIX_FMT_YUYV422, PixelFormat.PIXEL_FORMAT_YCBCR_422_I.value());
            put(FFmpeg.PIX_FMT_YUV420P, PixelFormat.PIXEL_FORMAT_YCRCB_420_SP.value());
            put(FFmpeg.PIX_FMT_NV12, PixelFormat.PIXEL_FORMAT_YV12.value());
        }};

        /*
         * The Formats supported by OhosMediaCodec as input and output are the mime types and
         * colorFormats (in the cases of video) supported by the Codecs available on the Ohos system.
         *
         * We'll keep the list of FMJ VideoFormats equivalent to MediaCodecInfo.CodecCapabilities
         * colorFormats out of the loop bellow in order to minimize the production of garbage.
         */
        List<Format> bSupportedFormats = null;
        List<Format> supportedInputFormats = new ArrayList<>();
        List<Format> supportedOutputFormats = new ArrayList<>();

        List<CodecDescription> codecDescriptions = new CodecDescriptionList().getSupportedCodecs();
        for (CodecDescription codecDescription : codecDescriptions) {
            String[] mimeTypes = codecDescription.getMimeTypes();
            for (String mimeType : mimeTypes) {
                /*
                 * Represent Ohos Codec mime type in the terms of FMJ (i.e. as an
                 * FMJ Format) because OhosMediaCodec implements an FMJ Codec.
                 */
                Format aSupportedFormat = getFmjFormatFromMediaCodecType(mimeType);

                if (aSupportedFormat == null)
                    continue;

                /*
                 * Ohos's mime type will determine either the supported input Format or the
                 * supported output Format of OhosMediaCodec. The colorFormats will determine the
                 * other half of the information related to the supported Formats. Of course, that
                 * means that we will not utilize Ohos MediaCodec for audio just yet.
                 */

                if (bSupportedFormats != null)
                    bSupportedFormats.clear();

                for (PixelFormat pixelFormat : mPixelFormats) {
                    Format bSupportedFormat = getFmjFormatFromMediaCodecColorFormat(pixelFormat.value());

                    if (bSupportedFormat == null)
                        continue;

                    if (bSupportedFormats == null) {
                        bSupportedFormats = new ArrayList<>(mPixelFormats.length);
                    }
                    bSupportedFormats.add(bSupportedFormat);
                }
                if ((bSupportedFormats == null) || bSupportedFormats.isEmpty())
                    continue;

                /*
                 * Finally, we know the FMJ Formats supported by Android's MediaCodec as input and output.
                 */
                List<Format> a, b;

                if (codecDescription.isEncoder()) {
                    /*
                     * Android's supportedType i.e. aSupportedFormat specifies the output Format of
                     * the MediaCodec. Respectively, Android's colorFormats i.e. bSupportedFormats
                     * define the input Formats supported by the MediaCodec.
                     */
                    a = supportedOutputFormats;
                    b = supportedInputFormats;
                }
                else {
                    /*
                     * Android's supportedType i.e. aSupportedFormat specifies the input Format of
                     * the MediaCodec. Respectively, Android's colorFormats i.e. bSupportedFormats
                     * define the output Formats supported by the MediaCodec.
                     */
                    a = supportedInputFormats;
                    b = supportedOutputFormats;
                }
                if (!a.contains(aSupportedFormat))
                    a.add(aSupportedFormat);
                for (Format bSupportedFormat : bSupportedFormats) {
                    if (!b.contains(bSupportedFormat))
                        b.add(bSupportedFormat);
                }

                StringBuilder s = new StringBuilder();

                s.append("Supported MediaCodec:");
                s.append(" name= ").append(codecDescription.getName()).append(';');
                s.append(" mime= ").append(mimeType).append(';');
                s.append(" colorFormats= ").append(Arrays.toString(mPixelFormats)).append(';');

//                MediaCodecInfo.CodecProfileLevel[] profileLevels = capabilities.profileLevels;
//
//                if ((profileLevels != null) && (profileLevels.length != 0)) {
//                    s.append(" profileLevels= [");
//                    for (int i = 0; i < profileLevels.length; i++) {
//                        if (i != 0)
//                            s.append("; ");
//
//                        MediaCodecInfo.CodecProfileLevel profileLevel = profileLevels[i];
//
//                        s.append("profile= ").append(profileLevel.profile).append(", level= ")
//                                .append(profileLevel.level);
//                    }
//                    s.append("];");
//                }
                Timber.d("%s", s);
            }
        }

        SUPPORTED_INPUT_FORMATS = supportedInputFormats.toArray(EMPTY_FORMATS);
        SUPPORTED_OUTPUT_FORMATS = supportedOutputFormats.toArray(EMPTY_FORMATS);
    }

    /**
     * Gets an FMJ <code>VideoFormat</code> instance which represents the same information about media
     * data as a specific <code>colorFormat</code> defined in the terms of Android's <code>MediaCodec</code> class.
     *
     * @param colorFormat the <code>colorFormat</code> value in the terms of Android's <code>MediaCodec</code> class to
     * get an FMJ <code>VideoFormat</code> equivalent of
     *
     * @return an FMJ <code>VideoFormat</code> instance which represents the same information about
     * media data as (i.e. is equivalent to) the specified <code>colorFormat</code>
     */
    private static VideoFormat getFmjFormatFromMediaCodecColorFormat(int colorFormat) {
        int pixFmt = PIX_FMTS_TO_MEDIA_CODEC_COLOR_FORMATS.get(colorFormat);
        return (pixFmt == -1) ? null : new AVFrameFormat(pixFmt);
    }

    /**
     * Gets an FMJ <code>Format</code> instance which represents the same information about media data
     * as a specific mime type defined in the terms of Android's <code>MediaCodec</code> class.
     *
     * @param type the mime type in the terms of Android's <code>MediaCodec</code> class to get an FMJ
     * <code>Format</code> equivalent of
     *
     * @return an FMJ <code>Format</code> instance which represents the same information about media
     * data as (i.e. is equivalent to) the specified <code>type</code>
     */
    private static Format getFmjFormatFromMediaCodecType(String type) {
        String encoding = FMJ_ENCODINGS_TO_MEDIA_CODEC_TYPES.get(type);
        return (encoding == null) ? null : new VideoFormat(encoding);
    }

    /**
     * Gets a <code>colorFormat</code> value defined in the terms of Android's <code>MediaCodec</code> class
     * which is equivalent to a specific FMJ <code>Format</code>.
     *
     * @param format the FMJ <code>Format</code> to get the equivalent to
     *
     * @return a <code>colorFormat</code> value defined in the terms of Android's <code>MediaCodec</code>
     * class which is equivalent to the specified <code>format</code> or
     * {@link #OMX_COLOR_FormatUnused} if no equivalent is known to <code>OhosMediaCodec</code>
     */
    private static int getMediaCodecColorFormatFromFmjFormat(Format format) {
        Integer pixfmt = null;
        if (format instanceof AVFrameFormat) {
            pixfmt = ((AVFrameFormat) format).getPixFmt();
        }
        return PIX_FMTS_TO_MEDIA_CODEC_COLOR_FORMATS.getOrDefault(pixfmt, OMX_COLOR_FormatUnused);
    }

    /**
     * Gets a mime type defined in the terms of Android's <code>MediaCodec</code> class which is
     * equivalent to a specific FMJ <code>Format</code>.
     *
     * @param format the FMJ <code>Format</code> to get the equivalent to
     *
     * @return a mime type defined in the terms of Android's <code>MediaCodec</code> class which is
     * equivalent to the specified <code>format</code> or <code>null</code> if no equivalent is
     * known to <code>OhosMediaCodec</code>
     */
    private static String getMediaCodecTypeFromFmjFormat(Format format) {
        return FMJ_ENCODINGS_TO_MEDIA_CODEC_TYPES.get(format.getEncoding());
    }

    /**
     * Determines whether a specific FMJ <code>Format</code> matches (i.e. is equivalent to) a specific
     * <code>colorFormat</code> defined in the terms of Android's <code>MediaCodec</code> class.
     *
     * @param format the FMJ <code>Format</code> to be compared to the specified <code>colorFormat</code>
     * @param colorFormat the <code>colorFormat</code> defined in the terms of Android's <code>MediaCodec</code> class
     * to be compared to the specified <code>format</code>
     *
     * @return <code>true</code> if the specified <code>format</code> matches (i.e. is equivalent to) the
     * specified <code>colorFormat</code>; otherwise, <code>false</code>
     */
    private static boolean matchesMediaCodecColorFormat(Format format, int colorFormat) {
        int formatColorFormat = getMediaCodecColorFormatFromFmjFormat(format);

        return (formatColorFormat != OMX_COLOR_FormatUnused) && (formatColorFormat == colorFormat);
    }

    /**
     * Determines whether a specific FMJ <code>Format</code> matches (i.e. is equivalent to) a specific
     * mime type defined in the terms of Android's <code>MediaCodec</code> class.
     *
     * @param format the FMJ <code>Format</code> to be compared to the specified <code>type</code>
     * @param type the media type defined in the terms of Android's <code>MediaCodec</code> class to be
     * compared to the specified <code>format</code>
     *
     * @return <code>true</code> if the specified <code>format</code> matches (i.e. is equal to) the
     * specified <code>type</code>; otherwise, <code>false</code>
     */
    private static boolean matchesMediaCodecType(Format format, String type) {
        String formatType = getMediaCodecTypeFromFmjFormat(format);
        return (formatType != null) && formatType.equals(type);
    }

    /**
     * The <code>AVFrame</code> instance into which this <code>Codec</code> outputs media (data) if the
     * <code>outputFormat</code> is an <code>AVFrameFormat</code> instance.
     */
    private AVFrame avFrame;

    /**
     * A <code>byte</code> in the form of an array which is used to copy the bytes of a
     * <code>java.nio.ByteBufferFmj</code> into native memory (because the <code>memcpy</code> implementation
     * requires an array. Allocated once to reduce garbage collection.
     */
    private final byte[] b = new byte[1];

    private final ByteBufferPool byteBufferPool = new ByteBufferPool();

    /**
     * The <code>colorFormat</code> value defined in the terms of Android's <code>MediaCodec</code> class
     * with which {@link #mediaCodec} is configured.
     */
    private int mColorFormat = OMX_COLOR_FormatUnused;

    /**
     * Ohos <code>MediaCodec</code> instance which is wrapped by this instance and which performs
     * the very media processing (during the execution of {@link #doProcess(Buffer, Buffer)}).
     */
    private Codec mediaCodec;

    /**
     * The indicator which determines whether {@link #mediaCodec} is configured to encode or decode media (data).
     */
    private boolean isEncoder;

    private ByteBuffer inputBuffer;

    private ByteBuffer outputBuffer;

    /**
     * The <code>MediaCodec.BufferInfo</code> instance which is populated by {@link #mediaCodec} to
     * describe the offset and length/size of the <code>java.nio.ByteBufferFmj</code>s it utilizes.
     * Allocated once to reduce garbage collection at runtime.
     */
    private final BufferInfo mBufferInfo = new BufferInfo();

    /**
     * The mime type defined in the terms of Android's <code>MediaCodec</code> class with which
     * {@link #mediaCodec} is configured.
     */
    private String mType;

    /**
     * Initializes a new <code>OhosMediaCodec</code> instance.
     */
    public OhosMediaCodec() {
        super("MediaCodec", Format.class, SUPPORTED_OUTPUT_FORMATS);
        inputFormats = SUPPORTED_INPUT_FORMATS;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Stops and releases {@link #mediaCodec} i.e. prepares it to be garbage collected.
     */
    protected void doClose() {
        if (mediaCodec != null) {
            try {
                try {
                    mediaCodec.stop();
                } finally {
                    mediaCodec.release();
                }
            } finally {
                mediaCodec = null;

                /*
                 * The following are properties of mediaCodec which are not exposed by the
                 * MediaCodec class. The encoder property is of type boolean and either of its
                 * domain values is significant so clearing it is impossible.
                 */
                mColorFormat = OMX_COLOR_FormatUnused;
                inputBuffer = null;
                outputBuffer = null;
                mType = null;
            }
        }

        if (avFrame != null) {
            avFrame.free();
            avFrame = null;
        }
        byteBufferPool.drain();
    }

    /**
     * {@inheritDoc}
     */
    protected void doOpen()
            throws ResourceUnavailableException {
        /*
         * If the inputFormat and outputFormat properties of this Codec have already been assigned
         * suitable values, initialize a MediaCodec instance, configure it and start it. Otherwise,
         * the procedure will be performed later on when the properties in question do get assigned
         * suitable values.
         */
        try {
            maybeConfigureAndStart();
        } catch (Throwable t) {
            /*
             * PlugIn#open() (and, consequently, AbstractCodecExt#doOpen()) is supposed to throw
             * ResourceUnavailableException but maybeConfigureAndStart() does not throw such an
             * exception for the sake of ease of use.
             */
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else {
                if (t instanceof InterruptedException)
                    Thread.currentThread().interrupt();

                Timber.e(t, "Failed to open %s", getName());
                ResourceUnavailableException rue = new ResourceUnavailableException();

                rue.initCause(t);
                throw rue;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected int doProcess(Buffer inputBuffer, Buffer outputBuffer) {
        /*
         * Note: The implementation of OhosMediaCodec is incomplete by relying on inputFormat
         * having Format.byteArray dataType.
         */
        if ((inputFormat == null) || !Format.byteArray.equals(inputFormat.getDataType())) {
            return BUFFER_PROCESSED_FAILED;
        }

        /*
         * Note: The implementation of OhosMediaCodec is incomplete by relying on outputFormat
         * being an AVFrameFormat instance.
         */
        Format outputFormat = this.outputFormat;
        if (!(outputFormat instanceof AVFrameFormat))
            return BUFFER_PROCESSED_FAILED;

        int mediaCodecOutputIndex = mediaCodec.getInputBufferIndex(10);
        /*
         * We will first exhaust the output of mediaCodec and then we will feed input into it.
         */
        int processed = INPUT_BUFFER_NOT_CONSUMED | OUTPUT_BUFFER_NOT_FILLED;

        if (INFO_OUTPUT_BUFFERS_CHANGED == mediaCodecOutputIndex) {
            this.outputBuffer = mediaCodec.getAvailableBuffer(0);
        }
        else if (0 <= mediaCodecOutputIndex) {
            int outputLength = 0;
            java.nio.ByteBuffer byteBuffer = null;

            try {
                if ((outputLength = mBufferInfo.size) > 0) {
                    byteBuffer = this.outputBuffer;

                    ByteBufferFmj avFrameData = avFrame.getData();
                    AVFrameFormat avFrameFormat = (AVFrameFormat) outputFormat;

                    if ((avFrameData == null) || (avFrameData.getCapacity() < outputLength)) {
                        avFrameData = byteBufferPool.getBuffer(outputLength);
                        if ((avFrameData == null)
                                || (avFrame.avpicture_fill(avFrameData, avFrameFormat) < 0)) {
                            processed = BUFFER_PROCESSED_FAILED;
                        }
                    }
                    if (processed != BUFFER_PROCESSED_FAILED) {
                        long ptr = avFrameData.getPtr();

                        for (int i = mBufferInfo.offset, end = i + outputLength; i < end; i++) {
                            b[0] = byteBuffer.get(i);
                            FFmpeg.memcpy(ptr, b, 0, b.length);
                            ptr++;
                        }

                        outputBuffer.setData(avFrame);
                        outputBuffer.setFormat(outputFormat);
                        outputBuffer.setLength(outputLength);
                        outputBuffer.setOffset(0);

                        processed &= ~OUTPUT_BUFFER_NOT_FILLED;
                    }
                }
            } finally {
                // Well, it was recommended by the Internet.
                if (byteBuffer != null)
                    byteBuffer.clear();
                mediaCodec.release();
            }
            /*
             * We will first exhaust the output of mediaCodec and then we will feed input into it.
             */
            if ((processed == BUFFER_PROCESSED_FAILED) || (outputLength > 0))
                return processed;
        }

        int codecInputIdx = INFO_TRY_AGAIN_LATER;// mediaCodec.dequeueInputBuffer(DEQUEUE_INPUT_BUFFER_TIMEOUT);
        if (0 <= codecInputIdx) {
            int codecInputOff = 0;
            int codecInputLen = 0;

            ByteBuffer inByteBuffer = this.inputBuffer;
            int fmjLength = inputBuffer.getLength();
            codecInputLen = Math.min(inByteBuffer.capacity(), fmjLength);

            byte[] bytes = (byte[]) inputBuffer.getData();
            int fmjOffset = inputBuffer.getOffset();

            for (int dst = 0, src = fmjOffset; dst < codecInputLen; dst++, src++) {
                inByteBuffer.put(dst, bytes[src]);
            }

            if (codecInputLen == fmjLength)
                processed &= ~INPUT_BUFFER_NOT_CONSUMED;
            else {
                inputBuffer.setLength(fmjLength - codecInputLen);
                inputBuffer.setOffset(fmjOffset + codecInputLen);
            }

            // mediaCodec.queueInputBuffer(codecInputIdx, codecInputOff, codecInputLen, 0, 0);
            mBufferInfo.setInfo(codecInputOff, codecInputLen, inputBuffer.getTimeStamp(), BufferInfo.BUFFER_TYPE_PARTIAL_FRAME, codecInputIdx);
            mediaCodec.writeBuffer(inByteBuffer, mBufferInfo);
        }
        return processed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Format[] getMatchingOutputFormats(Format inputFormat) {
        /*
         * An input Format may be supported by multiple MediaCodecs and, consequently, the output
         * Formats supported by this OhosMediaCodec is the set of the output formats supported by
         * the multiple MediaCodecs in question.
         */

        List<Format> outputFormats = new LinkedList<>();
        List<CodecDescription> supportedCodecs = new CodecDescriptionList().getSupportedCodecs();
        for (CodecDescription codecDescription : supportedCodecs) {
            String[] supportedTypes = codecDescription.getMimeTypes();

            if (codecDescription.isEncoder()) {
                /* The supported input Formats are the colorFormats. */
                for (String mimeType : supportedTypes) {
                    boolean matches = false;
                    for (PixelFormat pixelFormat : mPixelFormats) {
                        if (matchesMediaCodecColorFormat(inputFormat, pixelFormat.value())) {
                            matches = true;
                            break;
                        }
                    }
                    if (matches) {
                        /*
                         * The supported input Formats are the supportedTypes.
                         */
                        Format outputFormat = getFmjFormatFromMediaCodecType(mimeType);

                        if ((outputFormat != null) && !outputFormats.contains(outputFormat)) {
                            outputFormats.add(outputFormat);
                        }
                    }
                }
            }
            else {
                /* The supported input Formats are the supportedTypes. */
                for (String supportedType : supportedTypes) {
                    if (matchesMediaCodecType(inputFormat, supportedType)) {
                        for (PixelFormat colorFormat : mPixelFormats) {
                            Format outputFormat = getFmjFormatFromMediaCodecColorFormat(colorFormat.value());

                            if ((outputFormat != null)
                                    && !outputFormats.contains(outputFormat)) {
                                outputFormats.add(outputFormat);
                            }
                        }
                    }
                }
            }
        }
        return outputFormats.toArray(EMPTY_FORMATS);
    }

    /**
     * Configures and starts Codec if the <code>inputFormat</code> and <code>outputFormat</code>
     * properties of this <code>Codec</code> have already been assigned suitable values.
     */
    private void maybeConfigureAndStart() {
        /*
         * We can discover an appropriate MediaCodec to be wrapped by this instance only if the
         * inputFormat and outputFormat are suitably set.
         */
        if ((inputFormat == null) || (outputFormat == null))
            return;

        /*
         * If the inputFormat and outputFormat are supported by the current MediaCodec,
         * there is no need to bring it in accord with them.
         */
        if (mediaCodec != null) {
            Format typeFormat, colorFormat;

            if (isEncoder) {
                typeFormat = outputFormat;
                colorFormat = inputFormat;
            }
            else {
                typeFormat = inputFormat;
                colorFormat = outputFormat;
            }

            if (matchesMediaCodecType(typeFormat, mType)
                    && matchesMediaCodecColorFormat(colorFormat, mColorFormat)) {
                return;
            }
            // close the not matching current codec; continue to setup and start.
            else {
                doClose();
            }
        }

        /*
         * Find a MediaCodecInfo which supports the specified inputFormat and outputFormat of this instance,
         * initialize a MediaCodec from it to be wrapped by this instance, configure and start it.
         */
        CodecDescription codecInfo = null;
        Format typeFormat, colorFormat;

        List<CodecDescription> supportedCodecs = new CodecDescriptionList().getSupportedCodecs();
        for (CodecDescription codecDescription : supportedCodecs) {
            isEncoder = codecDescription.isEncoder();
            codecInfo = codecDescription;

            if (isEncoder) {
                typeFormat = outputFormat;
                colorFormat = inputFormat;
            }
            else {
                typeFormat = inputFormat;
                colorFormat = outputFormat;
            }

            String[] supportedTypes = codecDescription.getMimeTypes();
            for (String supportedType : supportedTypes) {
                if (!matchesMediaCodecType(typeFormat, supportedType))
                    continue;

                for (PixelFormat cFormat : mPixelFormats) {
                    if (matchesMediaCodecColorFormat(colorFormat, cFormat.value())) {
                        // We have found a CodecDescription which supports inputFormat and outputFormat.
                        mColorFormat = cFormat.value();
                        mType = supportedType;
                        break;
                    }
                }

                // Have we found a CodecDescription which supports inputFormat and outputFormat yet?
                if ((mColorFormat != OMX_COLOR_FormatUnused) && (mType != null)) {
                    break;
                }
            }

            // Have we found a CodecDescription which supports inputFormat and outputFormat yet?
            if ((mColorFormat != OMX_COLOR_FormatUnused) && (mType != null)) {
                break;
            }
        }

        // Have we found a MediaCodecInfo which supports inputFormat and outputFormat yet?
        if ((mColorFormat != OMX_COLOR_FormatUnused) && (mType != null)) {

            Codec mediaCodec = isEncoder ? Codec.createEncoder() : Codec.createDecoder();
            Format format = new Format(mType);
            if (isEncoder) {
                format.putIntValue(Format.COLOR_MODEL, mColorFormat);
            }

            // Well, this Codec is either an encoder or a decoder so it
            // seems like only its inputFormat may specify the size/Dimension.
            if (inputFormat instanceof VideoFormat) {
                Dimension size = ((VideoFormat) inputFormat).getSize();
                if (size == null)
                    size = DEFAULT_VIDEO_SIZE;

                format.putIntValue(ohos.media.common.Format.HEIGHT, size.height);
                format.putIntValue(ohos.media.common.Format.WIDTH, size.width);
            }

            mediaCodec.setCodecFormat(format);
            mediaCodec.start();

            inputBuffer = mediaCodec.getAvailableBuffer(0);
            outputBuffer = mediaCodec.getAvailableBuffer(0);

            if (avFrame == null)
                avFrame = new AVFrame();
        }

        // At this point, mediaCodec should have successfully been initialized,
        // configured and started.
        if (mediaCodec == null)
            throw new IllegalStateException("mediaCodec");
    }

    /**
     * Makes sure that {@link #mediaCodec} is in accord in terms of properties with the
     * <code>inputFormat</code> set on this <code>Codec</code>.
     */
    @Override
    public Format setInputFormat(Format format) {
        Format oldValue = inputFormat;
        super.setInputFormat(format);

        if ((oldValue != inputFormat) && opened)
            maybeConfigureAndStart();
        return inputFormat;
    }

    /**
     * Makes sure that {@link #mediaCodec} is in accord in terms of properties with the
     * <code>outputFormat</code> set on this <code>Codec</code>.
     */
    @Override
    public Format setOutputFormat(Format format) {
        if (format instanceof AVFrameFormat) {
            AVFrameFormat avFrameFormat = (AVFrameFormat) format;

            if (avFrameFormat.getSize() == null) {
                format = new AVFrameFormat(DEFAULT_VIDEO_SIZE, avFrameFormat.getFrameRate(),
                        avFrameFormat.getPixFmt(), avFrameFormat.getDeviceSystemPixFmt());
            }
        }

        Format oldValue = outputFormat;
        super.setOutputFormat(format);

        if ((oldValue != outputFormat) && opened)
            maybeConfigureAndStart();
        return outputFormat;
    }
}
