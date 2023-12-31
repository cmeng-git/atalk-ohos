/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.service.neomedia.event;

/**
 * Represents a listener (to be) notified about changes in the volume level/value maintained by a
 * <code>VolumeControl</code> .
 *
 * @author Damian Minkov
 */
public interface VolumeChangeListener
{
	/**
	 * Notifies this instance that the volume level/value maintained by a source
	 * <code>VolumeControl</code> (to which this instance has previously been added) has changed.
	 *
	 * @param volumeChangeEvent
	 *        a <code>VolumeChangeEvent</code> which details the source <code>VolumeControl</code> which has
	 *        fired the notification and the volume level/value
	 */
	public void volumeChange(VolumeChangeEvent volumeChangeEvent);
}
