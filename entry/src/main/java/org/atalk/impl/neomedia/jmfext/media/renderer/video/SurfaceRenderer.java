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
package org.atalk.impl.neomedia.jmfext.media.renderer.video;

import ohos.agp.graphics.Surface;
import ohos.agp.utils.Rect;

import org.atalk.impl.neomedia.codec.video.MediaDecoder;
import org.atalk.impl.neomedia.jmfext.media.renderer.AbstractRenderer;
import org.atalk.ohos.agp.components.JComponent;
import org.atalk.service.neomedia.codec.Constants;

import java.awt.Dimension;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.VideoFormat;
import javax.media.renderer.VideoRenderer;
import timber.log.Timber;

/**
 * Dummy renderer used only to construct valid codec graph when decoding into <code>Surface</code> is enabled.
 * The actual video rendering is performed by MediaCodec, i.e. codec.configure(format, surface, null, 0)
 *
 * @author Eng Chong Meng
 * @see MediaDecoder# configureMediaCodec(Codec, String)
 */
@SuppressWarnings("unused")
public class SurfaceRenderer extends AbstractRenderer<VideoFormat> implements VideoRenderer {
    private final static Format[] INPUT_FORMATS = new Format[]{
            new VideoFormat(
                    Constants.OHOS_SURFACE,
                    null,
                    Format.NOT_SPECIFIED,
                    Surface.class,
                    Format.NOT_SPECIFIED)
    };

    private JComponent component;

    public SurfaceRenderer() {
    }

    @Override
    public Format[] getSupportedInputFormats() {
        return INPUT_FORMATS;
    }

    @Override
    public int process(Buffer buffer) {
        return 0;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void close() {
    }

    @Override
    public String getName() {
        return "SurfaceRenderer";
    }

    @Override
    public void open()
            throws ResourceUnavailableException {
    }

    @Override
    public Format setInputFormat(Format format) {
        VideoFormat newFormat = (VideoFormat) super.setInputFormat(format);
        Timber.d("Set input format: %s = > %s", format, newFormat);
        if (newFormat.getSize() != null) {
            getComponent().setPreferredSize(new Dimension(newFormat.getSize()));
        }
        return newFormat;
    }

    @Override
    public Rect getBounds() {
        return null;
    }

    @Override
    public void setBounds(Rect rectangle) {
    }

    @Override
    public boolean setComponent(JComponent component) {
        return false;
    }

    @Override
    public JComponent getComponent() {
        if (component == null) {
            component = new JComponent(component.getContext());
        }
        return component;
    }
}
