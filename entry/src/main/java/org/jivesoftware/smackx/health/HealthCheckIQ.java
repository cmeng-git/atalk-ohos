/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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
package org.jivesoftware.smackx.health;

import org.jivesoftware.smack.packet.IQ;

/**
 * The health check IQ used to trigger health checks on the Jitsi Videobridge.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class HealthCheckIQ extends IQ
{
	/**
	 * Health check IQ element name.
	 */
	final static public String ELEMENT = "healthcheck";

	/**
	 * XML namespace name for health check IQs.
	 */
	final static public String NAMESPACE = "http://jitsi.org/protocol/healthcheck";

	public HealthCheckIQ()
	{
		super(ELEMENT, NAMESPACE);
	}

	/**
	 * /** {@inheritDoc}
	 */
	@Override
	protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml)
	{
		xml.setEmptyElement();
		return xml;
	}
}
