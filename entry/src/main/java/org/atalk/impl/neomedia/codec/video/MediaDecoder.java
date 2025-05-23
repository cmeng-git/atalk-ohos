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
import ohos.agp.graphics.SurfaceOps;
import ohos.media.codec.Codec;

import org.atalk.impl.neomedia.device.DeviceConfiguration;
import org.atalk.impl.neomedia.device.util.PreviewSurfaceProvider;
import org.atalk.impl.neomedia.format.ParameterizedVideoFormat;
import org.atalk.impl.neomedia.format.VideoMediaFormatImpl;
import org.atalk.ohos.gui.call.RemoteVideoLayout;
import org.atalk.service.libjitsi.LibJitsi;
import org.atalk.service.neomedia.codec.Constants;

import java.awt.Dimension;
import java.util.Optional;
import javax.media.Format;
import javax.media.format.VideoFormat;
import javax.media.format.YUVFormat;
import timber.log.Timber;

/**
 * The video decoder based on <code>MediaCodec</code>.
 *
 * @author Eng Chong Meng
 */
public class MediaDecoder extends MediaCodec {
    /**
     * Name of configuration property that enables hardware decoding.
     */
    public static final String HW_DECODING_ENABLE_PROPERTY = "neomedia.ohos.hw_decode";

    /**
     * Name of configuration property that enables decoding directly into provided <code>Surface</code> object.
     */
    public static final String DIRECT_SURFACE_DECODE_PROPERTY = "neomedia.ohos.surface_decode";

    /**
     * Remembers if this instance is using decoding into the <code>Surface</code>.
     */
    private final boolean useOutputSurface;

    /**
     * Output video size.
     */
    private Dimension outputSize;

    /**
     * Surface provider used to obtain <code>SurfaceView</code> object that will be used for decoded remote video rendering.
     */
    public static PreviewSurfaceProvider renderSurfaceProvider;

    /**
     * Default Input formats  supported by android decoder.
     * <a href="https://developer.android.com/guide/topics/media/media-formats#video-formats">...</a>
     */
    private static final VideoFormat[] INPUT_FORMATS = new VideoFormat[]{
            new VideoFormat(Constants.VP9),
            new VideoFormat(Constants.VP8),
            new VideoFormat(Constants.H264),
            new ParameterizedVideoFormat(Constants.H264, VideoMediaFormatImpl.H264_PACKETIZATION_MODE_FMTP, "0")
    };

    /**
     * Create a new instance of <code>MediaDecoder</code>.
     */
    public MediaDecoder() {
        super("MediaDecoder", VideoFormat.class, getOutputFormats(), false);
        if (isHwDecodingEnabled())
            inputFormats = INPUT_FORMATS;
        else
            inputFormats = EMPTY_FORMATS;

        useOutputSurface = isDirectSurfaceEnabled();
    }

    /**
     * {@inheritDoc}
     * inputFormat is not used to set video size; as the video dimension is not defined prior to received video
     *
     * @see RemoteVideoLayout#setVideoPreferredSize(Dimension, boolean)
     */
    @Override
    protected void configureMediaCodec(Codec codec, String codecType) {
        Format format = new Format(codecType);
        format.putIntValue(Format.KEY_WIDTH_SCOPE, DeviceConfiguration.DEFAULT_VIDEO_WIDTH);
        format.putIntValue(Format.KEY_HEIGHT_SCOPE, DeviceConfiguration.DEFAULT_VIDEO_HEIGHT);
        format.putIntValue(Format.KEY_BIT_RATE_SCOPE, 8000000);
        format.putIntValue(Format.KEY_FRAME_RATE_SCOPE, 30);
        format.putIntValue(Format.FRAME_INTERVAL, 30);
        format.putIntValue(Format.COLOR_MODEL, getColorFormat());

        codec.setCodecFormat(format);
    }

    /**
     * Returns <code>true</code> if hardware decoding is supported and enabled.
     *
     * @return <code>true</code> if hardware decoding is supported and enabled.
     */
    public static boolean isHwDecodingEnabled() {
        return LibJitsi.getConfigurationService().getBoolean(HW_DECODING_ENABLE_PROPERTY, true);
    }

    /**
     * Returns <code>true</code> if decoding into the <code>Surface</code> is enabled.
     *
     * @return <code>true</code> if decoding into the <code>Surface</code> is enabled.
     */
    public static boolean isDirectSurfaceEnabled() {
        return isHwDecodingEnabled()
                && LibJitsi.getConfigurationService().getBoolean(DIRECT_SURFACE_DECODE_PROPERTY, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Surface getSurface() {
        Optional<SurfaceOps> surfaceOps = renderSurfaceProvider.obtainObject().getSurfaceOps();
        return surfaceOps.map(SurfaceOps::getSurface).orElse(null);
    }

    /**
     * Obtain the video output video format based on user defined option:
     * a. None
     * b. Direct surface
     * c. YUV format
     *
     * @return video format as per User selected options
     */
    static Format[] getOutputFormats() {
        if (!isHwDecodingEnabled())
            return EMPTY_FORMATS;

        if (isDirectSurfaceEnabled()) {
            return new javax.media.Format[]{
                    new VideoFormat(Constants.OHOS_SURFACE)
            };
        }
        else {
            return new Format[]{new YUVFormat(
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
    }

    /**
     * Gets the matching output formats for a specific format.
     *
     * @param inputFormat input format
     *
     * @return array of formats matching input format
     */
    @Override
    protected javax.media.Format[] getMatchingOutputFormats(javax.media.Format inputFormat) {
        if (!(inputFormat instanceof VideoFormat))
            return EMPTY_FORMATS;

        VideoFormat inputVideoFormat = (VideoFormat) inputFormat;
        if (useOutputSurface) {
            return new VideoFormat[]{
                    new VideoFormat(
                            Constants.OHOS_SURFACE,
                            inputVideoFormat.getSize(),
                            Format.NOT_SPECIFIED,
                            Surface.class,
                            Format.NOT_SPECIFIED
                    )
            };
        }
        else {
            return new VideoFormat[]{
                    new YUVFormat(
                            inputVideoFormat.getSize(),
                            Format.NOT_SPECIFIED,
                            javax.media.Format.byteArray,
                            Format.NOT_SPECIFIED,
                            YUVFormat.YUV_420,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED,
                            Format.NOT_SPECIFIED)
            };
        }
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
    public Format setOutputFormat(Format format) {
        if (!(format instanceof VideoFormat)
                || (matches(format, getMatchingOutputFormats(inputFormat)) == null))
            return null;

        VideoFormat videoFormat = (VideoFormat) format;
        if (format instanceof YUVFormat) {
            YUVFormat yuvFormat = (YUVFormat) videoFormat;
            outputFormat = new YUVFormat(
                    outputSize,
                    videoFormat.getMaxDataLength(),
                    Format.byteArray,
                    videoFormat.getFrameRate(),
                    YUVFormat.YUV_420,
                    yuvFormat.getStrideY(),
                    yuvFormat.getStrideUV(),
                    yuvFormat.getOffsetY(),
                    yuvFormat.getOffsetU(),
                    yuvFormat.getOffsetV());
        }
        else {
            outputFormat = new VideoFormat(videoFormat.getEncoding(), outputSize,
                    videoFormat.getMaxDataLength(), videoFormat.getDataType(),
                    videoFormat.getFrameRate());
        }
        // Return the selected outputFormat
        return outputFormat;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doClose() {
        super.doClose();
        renderSurfaceProvider.onObjectReleased();
    }

    @Override
    protected void onSizeChanged(Dimension dimension) {
        Timber.d("Set decode outputFormat on video dimension change: %s", dimension);
        outputSize = dimension;
        setOutputFormat(outputFormat);
    }
}
