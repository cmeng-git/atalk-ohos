/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.service.neomedia;

import org.atalk.service.neomedia.event.CsrcAudioLevelListener;
import org.atalk.service.neomedia.event.DTMFListener;
import org.atalk.service.neomedia.event.SimpleAudioLevelListener;

/**
 * Extends the <code>MediaStream</code> interface and adds methods specific to audio streaming.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public interface AudioMediaStream extends MediaStream
{
	/**
	 * The name of the property which controls whether handling of RFC4733 DTMF packets should be
	 * disabled or enabled. If disabled, packets will not be processed or dropped (regardless of
	 * whether there is a payload type number registered for the telephone-event format).
	 */
	public static String DISABLE_DTMF_HANDLING_PNAME
				= AudioMediaStream.class.getName() + ".DISABLE_DTMF_HANDLING";

	/**
	 * Registers a listener that would receive notification events if the remote party starts
	 * sending DTMF tones to us.
	 *
	 * @param listener
	 *        the <code>DTMFListener</code> that we'd like to register.
	 */
	public void addDTMFListener(DTMFListener listener);

	/**
	 * Removes <code>listener</code> from the list of <code>DTMFListener</code>s registered to receive
	 * events for incoming DTMF tones.
	 *
	 * @param listener
	 *        the listener that we'd like to unregister
	 */
	public void removeDTMFListener(DTMFListener listener);

	/**
	 * Registers <code>listener</code> as the <code>CsrcAudioLevelListener</code> that will receive
	 * notifications for changes in the levels of conference participants that the remote party
	 * could be mixing.
	 *
	 * @param listener
	 *        the <code>CsrcAudioLevelListener</code> that we'd like to register or <code>null</code> if
	 *        we'd like to stop receiving notifications.
	 */
	public void setCsrcAudioLevelListener(CsrcAudioLevelListener listener);

	/**
	 * Sets <code>listener</code> as the <code>SimpleAudioLevelListener</code> registered to receive
	 * notifications for changes in the levels of the audio that this stream is sending out.
	 *
	 * @param listener
	 *        the <code>SimpleAudioLevelListener</code> that we'd like to register or <code>null</code> if
	 *        we want to stop local audio level measurements.
	 */
	public void setLocalUserAudioLevelListener(SimpleAudioLevelListener listener);

	/**
	 * Sets the <code>VolumeControl</code> which is to control the volume (level) of the audio received
	 * in/by this <code>AudioMediaStream</code> and played back.
	 *
	 * @param outputVolumeControl
	 *        the <code>VolumeControl</code> which is to control the volume (level) of the audio
	 *        received in this <code>AudioMediaStream</code> and played back
	 */
	public void setOutputVolumeControl(VolumeControl outputVolumeControl);

	/**
	 * Sets <code>listener</code> as the <code>SimpleAudioLevelListener</code> registered to receive
	 * notifications for changes in the levels of the party that's at the other end of this stream.
	 *
	 * @param listener
	 *        the <code>SimpleAudioLevelListener</code> that we'd like to register or <code>null</code> if
	 *        we want to stop stream audio level measurements.
	 */
	public void setStreamAudioLevelListener(SimpleAudioLevelListener listener);

	/**
	 * Starts sending the specified <code>DTMFTone</code> until the <code>stopSendingDTMF()</code> method is
	 * called (Excepts for INBAND DTMF, which stops by itself this is why where there is no need to
	 * call the stopSendingDTMF). Callers should keep in mind the fact that calling this method
	 * would most likely interrupt all audio transmission until the corresponding stop method is
	 * called. Also, calling this method successively without invoking the corresponding stop method
	 * between the calls will simply replace the <code>DTMFTone</code> from the first call with that
	 * from the second.
	 *
	 * @param tone
	 *        the <code>DTMFTone</code> to start sending.
	 * @param dtmfMethod
	 *        The kind of DTMF used (RTP, SIP-INOF or INBAND).
	 * @param minimalToneDuration
	 *        The minimal DTMF tone duration.
	 * @param maximalToneDuration
	 *        The maximal DTMF tone duration.
	 * @param volume
	 *        The DTMF tone volume. Describes the power level of the tone, expressed in dBm0 after
	 *        dropping the sign.
	 */
	public void startSendingDTMF(DTMFTone tone, DTMFMethod dtmfMethod, int minimalToneDuration,
		int maximalToneDuration, int volume);

	/**
	 * Interrupts transmission of a <code>DTMFTone</code> started with the <code>startSendingDTMF</code>
	 * method. This method has no effect if no tone is being currently sent.
	 *
	 * @param dtmfMethod
	 *        the <code>DTMFMethod</code> to stop sending.
	 */
	public void stopSendingDTMF(DTMFMethod dtmfMethod);
}
