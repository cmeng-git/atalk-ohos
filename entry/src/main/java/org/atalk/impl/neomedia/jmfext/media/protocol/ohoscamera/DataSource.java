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

import org.atalk.impl.neomedia.jmfext.media.protocol.AbstractPushBufferCaptureDevice;
import org.atalk.impl.neomedia.jmfext.media.protocol.AbstractPushBufferStream;
import org.atalk.service.neomedia.codec.Constants;

import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.control.FormatControl;
import javax.media.format.VideoFormat;

/**
 * Camera data source. Creates <code>PreviewStream</code> or <code>SurfaceStream</code> based on the used encode format.
 *
 * @author Eng Chong Meng
 */
public class DataSource extends AbstractPushBufferCaptureDevice
{
	public DataSource()
	{
	}

	public DataSource(MediaLocator locator)
	{
		super(locator);
	}

	@Override
	protected AbstractPushBufferStream<?> createStream(int i, FormatControl formatControl)
	{
		String encoding = formatControl.getFormat().getEncoding();
		if (encoding.equals(Constants.OHOS_SURFACE)) {
			return new SurfaceStream(this, formatControl);
		}
		else {
			return new PreviewStream(this, formatControl);
		}
	}

	@Override
	protected Format setFormat(int streamIndex, Format oldValue, Format newValue)
	{
		// This DataSource VideoFormat supports setFormat.
		if (newValue instanceof VideoFormat) {
			return newValue;
		}
		else
			return super.setFormat(streamIndex, oldValue, newValue);
	}
}
