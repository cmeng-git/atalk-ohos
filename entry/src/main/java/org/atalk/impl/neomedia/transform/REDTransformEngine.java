/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.impl.neomedia.transform;

import net.sf.fmj.media.rtp.RTPHeader;

import org.atalk.service.neomedia.RawPacket;

import timber.log.Timber;

/**
 * Implements a {@link PacketTransformer} and
 * {@link TransformEngine} for RED (RFC2198).
 *
 * @author Boris Grozev
 * @author Eng Chong Meng
 */
public class REDTransformEngine implements TransformEngine, PacketTransformer
{
    /**
     * The RED payload type for incoming packets. Only RTP packets with this payload type will be
     * reverse-transformed by this <code>PacketTransformer</code>.
     *
     * The special value "-1" is used to effectively disable reverse-transforming packets by this
     * <code>PacketTransformer</code>.
     */
    private byte incomingPT;

    /**
     * The payload type to set when constructing RED packets (e.g. for outgoing) packets.
     *
     * The special value "-1" is used to effectively disable transforming packets by this
     * <code>PacketTransformer</code>.
     */
    private byte outgoingPT;

    /**
     * Initializes a new <code>REDTransformEngine</code> instance.
     *
     * @param incomingPT the RED payload type number for incoming packets.
     * @param outgoingPT the RED payload type number for outgoing packets.
     */
    public REDTransformEngine(byte incomingPT, byte outgoingPT)
    {
        setIncomingPT(incomingPT);
        setOutgoingPT(outgoingPT);
    }

    /**
     * Initializes a new <code>REDTransformEngine</code> instance.
     */
    public REDTransformEngine()
    {
        this((byte) -1, (byte) -1);
    }

    /**
     * Sets the RED payload type for incoming red packets.
     *
     * @param incomingPT the payload type to set.
     */
    public void setIncomingPT(byte incomingPT)
    {
        this.incomingPT = incomingPT;
        Timber.i("Set incoming payload type %s", incomingPT);
    }

    /**
     * Sets the RED payload type for outgoing red packets.
     *
     * @param outgoingPT the payload type to set.
     */
    public void setOutgoingPT(byte outgoingPT)
    {
        this.outgoingPT = outgoingPT;
        Timber.i("Set outgoing payload type %s", outgoingPT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close()
    {
    }

    /**
     * {@inheritDoc}
     *
     * Reverse-transform a RED (RFC2198) packet.
     */
    @Override
    public RawPacket[] reverseTransform(RawPacket[] pkts)
    {
        if (incomingPT == -1)
            return pkts;

        // XXX: in the general case we should transform each packet in pkts and
        // then merge all the results somehow. However, for performance(*) and
        // simplicity, we assume that there is at most a single packet in pkts,
        // and the rest is null. This is a valid assumption with the currently
        // available PacketTransformers in libjitsi.
        //
        // (*) in the majority of packets there will be a single packet as a
        // result, and thus we get to reuse both pkts[0] and pkts itself.

        if (pkts != null && pkts.length > 0) {
            if (pkts[0] != null && pkts[0].getPayloadType() == incomingPT)
                return reverseTransformSingle(pkts[0], pkts);
        }

        return pkts;
    }

    /**
     * {@inheritDoc}
     *
     * Encapsulates the packets in <code>pkts</code> with RED (RFC2198).
     *
     * Effectively inserts the following 1-byte RED header right after the
     * RTP header (where "Block PT" is the payload type of the original packet)
     * and changes the payload type of the packet to <code>outgoingPT</code>
     *
     * 0 1 2 3 4 5 6 7
     * +-+-+-+-+-+-+-+-+
     * |0|   Block PT  |
     * +-+-+-+-+-+-+-+-+
     */
    @Override
    public RawPacket[] transform(RawPacket[] pkts)
    {
        if (outgoingPT == -1)
            return pkts;

        for (RawPacket pkt : pkts) {
            if (pkt != null && pkt.getVersion() == RTPHeader.VERSION) {
                byte[] buf = pkt.getBuffer();
                int len = pkt.getLength();
                int off = pkt.getOffset();
                int hdrLen = pkt.getHeaderLength();

                byte[] newBuf = buf; // try to reuse
                if (newBuf.length < len + 1) {
                    newBuf = new byte[len + 1];
                }
                System.arraycopy(buf, off, newBuf, 0, hdrLen);
                System.arraycopy(buf, off + hdrLen, newBuf, hdrLen + 1, len - hdrLen);
                newBuf[hdrLen] = pkt.getPayloadType();

                pkt.setBuffer(newBuf);
                pkt.setOffset(0);
                pkt.setLength(len + 1);

                pkt.setPayloadType(outgoingPT);
            }
        }
        return pkts;
    }

    /**
     * Transforms the RFC2198 packet <code>pkt</code> into an array of RTP packets.
     */
    private RawPacket[] reverseTransformSingle(RawPacket pkt, RawPacket[] pkts)
    {
        byte[] buf = pkt.getBuffer();
        int off = pkt.getOffset();

        int hdrLen = pkt.getHeaderLength();
        int idx = off + hdrLen; // beginning of RTP payload
        int pktCount = 1; // number of packets inside RED

        // 0 1 2 3
        // 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
        // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        // |F| block PT | timestamp offset | block length |
        // +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        while ((buf[idx] & 0x80) != 0) {
            pktCount++;
            idx += 4;
        }
        idx = off + hdrLen; // back to beginning of RTP payload

        if (pkts.length < pktCount)
            pkts = new RawPacket[pktCount];
        if (pktCount != 1)
            Timber.i("Received a RED packet with more than one packet inside");

        int payloadOffset = idx + (pktCount - 1) * 4 + 1 /* RED headers */;

        // write non-primary packets, keep pkts[0] for the primary
        for (int i = 1; i < pktCount; i++) {
            int blockLen = (buf[idx + 2] & 0x03) << 8 | (buf[idx + 3] & 0xFF);

            // XXX: we might need to optimize
            byte[] newBuf = new byte[hdrLen + blockLen];
            // XXX: might be wrong but this doesn't look right -- do we really
            // want to copy the RTP header from inside the payload?
            System.arraycopy(buf, payloadOffset, newBuf, 0, hdrLen + blockLen);

            // XXX: we might need to optimize
            if (pkts[i] == null)
                pkts[i] = new RawPacket();

            pkts[i].setBuffer(newBuf);
            pkts[i].setOffset(0);
            pkts[i].setLength(hdrLen + blockLen);

            pkts[i].setPayloadType((byte) (buf[idx] & 0xf7));
            // TODO: update timestamp

            idx += 4; // next RED header
            payloadOffset += blockLen;
        }

        // idx is now at the "primary encoding block header":
        // 0 1 2 3 4 5 6 7
        // +-+-+-+-+-+-+-+-+
        // |0| Block PT |
        // +-+-+-+-+-+-+-+-+

        // write primary packet: reuse pkt
        pkt.setPayloadType((byte) (buf[idx] & 0x7f));

        // reuse the buffer, move the payload "left". Moving the payload
        // right doesn't work because apparently we've got an offset
        // bug somewhere in libjitsi (in the SRTP transformer?) that
        // corrupts the authentication tag of outgoing RTP packets
        // resulting in RTP packets being discarded at the clients (both
        // FF and Chrome) because the RTP packet authentication fails.
        System.arraycopy(buf, off, buf, off + payloadOffset - hdrLen, hdrLen);
        pkt.setOffset(off + payloadOffset - hdrLen);
        pkt.setLength(pkt.getLength() - (payloadOffset - hdrLen));

        pkts[0] = pkt;
        return pkts;
    }

    /**
     * {@inheritDoc}
     *
     * Return the single <code>PacketTransformer</code> for this <code>TransformEngine</code>
     */
    @Override
    public PacketTransformer getRTPTransformer()
    {
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * We don't touch RTCP
     */
    @Override
    public PacketTransformer getRTCPTransformer()
    {
        return null;
    }
}
