/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.atalk.impl.neomedia.rtp.translator;

import javax.media.Buffer;

/**
 * Privately used by {@link PushSourceStreamImpl} at the time of this writing and extracted into its
 * own file for the sake of readability.
 *
 * @author Lyubomir Marinov
 */
class SourcePacket extends Buffer
{
	private byte[] buf;

	public PushSourceStreamDesc streamDesc;

	public SourcePacket(byte[] buf, int off, int len)
	{
		setData(buf);
		setOffset(off);
		setLength(len);
	}

	public byte[] getBuffer()
	{
		return buf;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setData(Object data)
	{
		super.setData(data);

		buf = (byte[]) data;
	}
}
