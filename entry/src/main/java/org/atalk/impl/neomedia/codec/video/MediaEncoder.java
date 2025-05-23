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

import org.atalk.impl.neomedia.MediaServiceImpl;
import org.atalk.impl.neomedia.VideoMediaStreamImpl;
import org.atalk.impl.neomedia.format.ParameterizedVideoFormat;
import org.atalk.impl.neomedia.format.VideoMediaFormatImpl;
import org.atalk.ohos.aTalkApp;
import org.atalk.service.libjitsi.LibJitsi;
import org.atalk.service.neomedia.codec.Constants;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;
import javax.media.protocol.DataSource;
import timber.log.Timber;

/**
 * Video encoder based on <code>MediaCodec</code>.
 *
 * @author Eng Chong Meng
 */
public class MediaEncoder extends MediaCodec {
    /**
     * Name of configuration property that enables this encoder.
     */
    public static final String HW_ENCODING_ENABLE_PROPERTY = "neomedia.ohos.hw_encode";

    /**
     * Name of configuration property that enables usage of <code>Surface</code> object as a source of video data.
     */
    public static final String DIRECT_SURFACE_ENCODE_PROPERTY = "neomedia.ohos.surface_encode";

    /**
     * Indicates if this instance is using <code>Surface</code> for data source.
     */
    private final boolean useInputSurface;

    /**
     * Input <code>Surface</code> object.
     */
    private Surface mInputSurface;

    /**
     * Default output formats supported by this android encoder
     * see: <a href="https://developer.android.com/guide/topics/media/media-formats#video-formats">...</a>
     */
    private static final VideoFormat[] SUPPORTED_OUTPUT_FORMATS;

    /**
     * List of vFormats supported by this android device. VP9 encoder only supported on certain android device.
     */
    private static final List<VideoFormat> vFormats = new ArrayList<>();

    static {
        if (CodecInfo.getCodecForType(Format.VIDEO_VP9, true) != null) {
            vFormats.add(new VideoFormat(Constants.VP9));
        }
        vFormats.add(new VideoFormat(Constants.VP8));
        vFormats.add(new ParameterizedVideoFormat(Constants.H264, VideoMediaFormatImpl.H264_PACKETIZATION_MODE_FMTP, "0"));
        vFormats.add(new ParameterizedVideoFormat(Constants.H264, VideoMediaFormatImpl.H264_PACKETIZATION_MODE_FMTP, "1"));
        SUPPORTED_OUTPUT_FORMATS = vFormats.toArray(vFormats.toArray(new VideoFormat[0]));
    }

    /**
     * Creates new instance of <code>MediaEncoder</code>.
     */
    public MediaEncoder() {
        super("MediaEncoder", VideoFormat.class, isHwEncodingEnabled()
                ? SUPPORTED_OUTPUT_FORMATS : EMPTY_FORMATS, true);

        useInputSurface = isDirectSurfaceEnabled();
        if (useInputSurface) {
            inputFormats = new VideoFormat[]{new VideoFormat(
                    Constants.OHOS_SURFACE,
                    null,
                    Format.NOT_SPECIFIED,
                    Surface.class,
                    Format.NOT_SPECIFIED)};
        }
        else {
            inputFormats = new VideoFormat[]{new YUVFormat(
                    null,
                    Format.NOT_SPECIFIED,
                    javax.media.Format.byteArray,
                    Format.NOT_SPECIFIED,
                    YUVFormat.YUV_420,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED,
                    Format.NOT_SPECIFIED
            )};
        }
        inputFormat = null;
        outputFormat = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configureMediaCodec(Codec codec, String codecType)
            throws ResourceUnavailableException {
        if (inputFormat == null)
            throw new ResourceUnavailableException("Output format not set");

        Dimension size = ((VideoFormat) inputFormat).getSize();
        if (size == null)
            throw new ResourceUnavailableException("Size not set");

        if (aTalkApp.isPortrait) {
            size = new Dimension(size.height, size.width);
        }
        Timber.d("Encoder video input format: %s => %s", inputFormat, size);

        // Setup encoder properties.  Failing to specify some of these can cause the Codec
        // configure() call to throw an unhelpful exception.

        Format format = new Format(codecType);
        format.putIntValue(Format.KEY_WIDTH_SCOPE, size.width);
        format.putIntValue(Format.KEY_HEIGHT_SCOPE, size.height);
        format.putIntValue(Format.COLOR_MODEL, getColorFormat());

        int bitrate = ((MediaServiceImpl) LibJitsi.getMediaService()).getDeviceConfiguration().getVideoBitrate() * 1024;
        format.putIntValue(Format.KEY_BIT_RATE_SCOPE, bitrate);
        format.putIntValue(Format.KEY_FRAME_RATE_SCOPE, 30);
        format.putIntValue(Format.FRAME_INTERVAL, 30);

        codec.setCodecFormat(format);
        if (useInputSurface) {
            mInputSurface = codec.createLastingVideoSurface();
        }
    }

    /**
     * Returns <code>true</code> if hardware encoding is enabled.
     *
     * @return <code>true</code> if hardware encoding is enabled.
     */
    private static boolean isHwEncodingEnabled() {
        return LibJitsi.getConfigurationService().getBoolean(HW_ENCODING_ENABLE_PROPERTY, true);
    }

    /**
     * Returns <code>true</code> if input <code>Surface</code> mode is enabled.
     *
     * @return <code>true</code> if input <code>Surface</code> mode is enabled.
     */
    public static boolean isDirectSurfaceEnabled() {
        return isHwEncodingEnabled()
                && LibJitsi.getConfigurationService().getBoolean(DIRECT_SURFACE_ENCODE_PROPERTY, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Surface getSurface() {
        return mInputSurface;
    }

    /**
     * Check if the specified hardware encoder is supported on this device
     *
     * @param codec Encoder name
     *
     * @return true if supported
     *
     * @see VideoMediaStreamImpl#selectVideoSize(DataSource, int, int)
     */
    public static boolean isCodecSupported(String codec) {
        return isDirectSurfaceEnabled() && vFormats.toString().contains(codec);
    }

    /**
     * Gets the matching output formats for a specific format.
     *
     * @param inputFormat input format
     *
     * @return array of formats matching input format
     */
    @Override
    protected Format[] getMatchingOutputFormats(Format inputFormat) {
        if (!(inputFormat instanceof VideoFormat) || !isHwEncodingEnabled())
            return EMPTY_FORMATS;

        VideoFormat inputVideoFormat = (VideoFormat) inputFormat;
        Dimension size = inputVideoFormat.getSize();
        float frameRate = inputVideoFormat.getFrameRate();

        return new VideoFormat[]{
                new VideoFormat(Constants.VP9, size, /* maxDataLength */
                        Format.NOT_SPECIFIED, Format.byteArray, frameRate),
                new VideoFormat(Constants.VP8, size, /* maxDataLength */
                        Format.NOT_SPECIFIED, javax.media.Format.byteArray, frameRate),
                new ParameterizedVideoFormat(Constants.H264, size, Format.NOT_SPECIFIED,
                        javax.media.Format.byteArray, frameRate, ParameterizedVideoFormat.toMap(
                        VideoMediaFormatImpl.H264_PACKETIZATION_MODE_FMTP, "0")),
                new ParameterizedVideoFormat(Constants.H264, size, Format.NOT_SPECIFIED,
                        javax.media.Format.byteArray, frameRate, ParameterizedVideoFormat.toMap(
                        VideoMediaFormatImpl.H264_PACKETIZATION_MODE_FMTP, "1"))};
    }

    /**
     * Sets the input format.
     *
     * @param format format to set
     *
     * @return the selected inputFormat
     */
    @Override
    public javax.media.Format setInputFormat(javax.media.Format format) {
        if (!(format instanceof VideoFormat) || (matches(format, inputFormats) == null))
            return null;

        inputFormat = format;
        // Timber.d(new Exception(),"Encoder video input format set: %s", inputFormat);

        // Return the selected inputFormat
        return inputFormat;
    }

    /**
     * Sets the <code>Format</code> in which this <code>Codec</code> is to output media data.
     *
     * @param format the <code>Format</code> in which this <code>Codec</code> is to output media data
     *
     * @return the <code>Format</code> in which this <code>Codec</code> is currently configured to output
     * media data or <code>null</code> if <code>format</code> was found to be incompatible with this <code>Codec</code>
     */
    @Override
    public javax.media.Format setOutputFormat(javax.media.Format format) {
        if (!(format instanceof VideoFormat)
                || (matches(format, getMatchingOutputFormats(inputFormat)) == null))
            return null;

        // Timber.d(new Exception(),"Encoder video output format set: %s", inputFormat);
        VideoFormat videoFormat = (VideoFormat) format;
        /*
         * An Encoder translates raw media data in (en)coded media data. Consequently, the size of
         * the output is equal to the size of the input.
         */
        Dimension size = null;

        if (inputFormat != null)
            size = ((VideoFormat) inputFormat).getSize();
        if ((size == null) && format.matches(outputFormat))
            size = ((VideoFormat) outputFormat).getSize();

        outputFormat = new VideoFormat(videoFormat.getEncoding(), size,
                Format.NOT_SPECIFIED, videoFormat.getDataType(), videoFormat.getFrameRate());

        // Return the selected outputFormat
        return outputFormat;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doClose() {
        super.doClose();

        if (mInputSurface != null) {
            mInputSurface = null;
        }
    }
}