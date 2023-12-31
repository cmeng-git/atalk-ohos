/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.media;

import org.atalk.service.neomedia.SrtpControl;
import org.atalk.service.neomedia.SrtpControlType;
import org.atalk.util.MediaType;

/**
 * Represents a sorted set of <code>SrtpControl</code> implementations.
 *
 * @author Lyubomir Marinov
 * @author MilanKral
 */
public class SrtpControls
{
    private static final SrtpControlType[] SORTED_SRTP_CONTROL_TYPES = {
            SrtpControlType.ZRTP,
            SrtpControlType.DTLS_SRTP,
            SrtpControlType.MIKEY,
            SrtpControlType.SDES};

    /**
     * The <code>SrtpControl</code> implementations which are the elements of this sorted set.
     */
    private final SrtpControl[][] elements
            = new SrtpControl[MediaType.values().length][SrtpControlType.values().length];

    /**
     * Initializes a new <code>SrtpControls</code> instance.
     */
    public SrtpControls()
    {
    }

    public SrtpControl findFirst(MediaType mediaType)
    {
        SrtpControl element = null;

        for (SrtpControlType srtpControlType : SORTED_SRTP_CONTROL_TYPES) {
            element = get(mediaType, srtpControlType);
            if (element != null)
                break;
        }
        return element;
    }

    public SrtpControl get(MediaType mediaType, SrtpControlType srtpControlType)
    {
        return elements[mediaType.ordinal()][srtpControlType.ordinal()];
    }

    public SrtpControl getOrCreate(MediaType mediaType, SrtpControlType srtpControlType, final byte[] myZid)
    {
        SrtpControl[] elements = this.elements[mediaType.ordinal()];
        int index = srtpControlType.ordinal();
        SrtpControl element = elements[index];

        if (element == null) {
            element = ProtocolMediaActivator.getMediaService().createSrtpControl(srtpControlType, myZid);
            if (element != null)
                elements[index] = element;
        }
        return element;
    }

    public SrtpControl remove(MediaType mediaType, SrtpControlType srtpControlType)
    {
        SrtpControl[] elements = this.elements[mediaType.ordinal()];
        int index = srtpControlType.ordinal();
        SrtpControl element = elements[index];

        elements[index] = null;
        return element;
    }

    public void set(MediaType mediaType, SrtpControl element)
    {
        SrtpControlType srtpControlType = element.getSrtpControlType();
        elements[mediaType.ordinal()][srtpControlType.ordinal()] = element;
    }
}
