/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.impl.neomedia.transform.sdes;

import org.atalk.impl.neomedia.transform.PacketTransformer;
import org.atalk.impl.neomedia.transform.srtp.SRTCPTransformer;
import org.atalk.impl.neomedia.transform.srtp.SRTPTransformer;
import org.atalk.impl.neomedia.transform.srtp.SrtpContextFactory;
import org.atalk.impl.neomedia.transform.srtp.SrtpPolicy;
import org.atalk.service.neomedia.SrtpControl;

import ch.imvs.sdes4j.srtp.SrtpCryptoAttribute;
import ch.imvs.sdes4j.srtp.SrtpCryptoSuite;
import ch.imvs.sdes4j.srtp.SrtpSessionParam;

/**
 * TransformEngine for SDES based SRTP encryption.
 *
 * @author Ingo Bauersachs
 * @author Eng Chong Meng
 */
public class SDesTransformEngine implements SrtpControl.TransformEngine
{
    private SRTPTransformer srtpTransformer;
    private SRTCPTransformer srtcpTransformer;
    private SrtpCryptoAttribute inAttribute;
    private SrtpCryptoAttribute outAttribute;
    private SrtpContextFactory reverseCtx;
    private SrtpContextFactory forwardCtx;

    /**
     * Creates a new instance of this class.
     *
     * @param inAttribute Key material for the incoming stream.
     * @param outAttribute Key material for the outgoing stream.
     */
    public SDesTransformEngine(SrtpCryptoAttribute inAttribute, SrtpCryptoAttribute outAttribute)
    {
        update(inAttribute, outAttribute);
    }

    /**
     * Updates this instance with new key materials.
     *
     * @param inAttribute Key material for the incoming stream.
     * @param outAttribute Key material for the outgoing stream.
     */
    public void update(SrtpCryptoAttribute inAttribute, SrtpCryptoAttribute outAttribute)
    {
        // Only reset the context if the new keys are really different, otherwise the ROC of an active
        // but paused (on hold) stream will be reset. A new stream (with a fresh context) should be
        // advertised by a new SSRC and thus receive a ROC of zero.
        boolean changed = false;
        if (!inAttribute.equals(this.inAttribute)) {
            this.inAttribute = inAttribute;
            reverseCtx = getTransformEngine(inAttribute, false /* receiver */);
            changed = true;
        }

        if (!outAttribute.equals(this.outAttribute)) {
            this.outAttribute = outAttribute;
            forwardCtx = getTransformEngine(outAttribute, true /* sender */);
            changed = true;
        }

        if (changed) {
            if (srtpTransformer == null) {
                srtpTransformer = new SRTPTransformer(forwardCtx, reverseCtx);
            }
            else {
                srtpTransformer.setContextFactory(forwardCtx, true);
                srtpTransformer.setContextFactory(reverseCtx, false);
            }

            if (srtcpTransformer == null) {
                srtcpTransformer = new SRTCPTransformer(forwardCtx, reverseCtx);
            }
            else {
                srtcpTransformer.updateFactory(forwardCtx, true);
                srtcpTransformer.updateFactory(reverseCtx, false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cleanup()
    {
        if (srtpTransformer != null)
            srtpTransformer.close();
        if (srtcpTransformer != null)
            srtcpTransformer.close();

        srtpTransformer = null;
        srtcpTransformer = null;
    }

    private static SrtpContextFactory getTransformEngine(SrtpCryptoAttribute attribute, boolean sender)
    {
        SrtpSessionParam[] sessionParams = attribute.getSessionParams();

        if ((sessionParams != null) && (sessionParams.length > 0)) {
            throw new IllegalArgumentException("session parameters are not supported");
        }

        SrtpCryptoSuite cryptoSuite = attribute.getCryptoSuite();

        return new SrtpContextFactory(
                sender,
                getKey(attribute),
                getSalt(attribute),
                new SrtpPolicy(
                        getEncryptionCipher(cryptoSuite),
                        cryptoSuite.getEncKeyLength() / 8,
                        getHashAlgorithm(cryptoSuite),
                        cryptoSuite.getSrtpAuthKeyLength() / 8,
                        cryptoSuite.getSrtpAuthTagLength() / 8,
                        cryptoSuite.getSaltKeyLength() / 8),
                new SrtpPolicy(
                        getEncryptionCipher(cryptoSuite),
                        cryptoSuite.getEncKeyLength() / 8,
                        getHashAlgorithm(cryptoSuite),
                        cryptoSuite.getSrtcpAuthKeyLength() / 8,
                        cryptoSuite.getSrtcpAuthTagLength() / 8,
                        cryptoSuite.getSaltKeyLength() / 8));
    }

    private static byte[] getKey(SrtpCryptoAttribute attribute)
    {
        int length = attribute.getCryptoSuite().getEncKeyLength() / 8;
        byte[] key = new byte[length];
        System.arraycopy(attribute.getKeyParams()[0].getKey(), 0, key, 0, length);
        return key;
    }

    private static byte[] getSalt(SrtpCryptoAttribute attribute)
    {
        int keyLength = attribute.getCryptoSuite().getEncKeyLength() / 8;
        int saltLength = attribute.getCryptoSuite().getSaltKeyLength() / 8;
        byte[] salt = new byte[saltLength];
        System.arraycopy(attribute.getKeyParams()[0].getKey(), keyLength, salt, 0, saltLength);
        return salt;
    }

    private static int getEncryptionCipher(SrtpCryptoSuite cs)
    {
        switch (cs.getEncryptionAlgorithm()) {
            case SrtpCryptoSuite.ENCRYPTION_AES128_CM:
            case SrtpCryptoSuite.ENCRYPTION_AES192_CM:
            case SrtpCryptoSuite.ENCRYPTION_AES256_CM:
                return SrtpPolicy.AESCM_ENCRYPTION;
            case SrtpCryptoSuite.ENCRYPTION_AES128_F8:
                return SrtpPolicy.AESF8_ENCRYPTION;
            default:
                throw new IllegalArgumentException("Unsupported cipher");
        }
    }

    private static int getHashAlgorithm(SrtpCryptoSuite cs)
    {
        switch (cs.getHashAlgorithm()) {
            case SrtpCryptoSuite.HASH_HMAC_SHA1:
                return SrtpPolicy.HMACSHA1_AUTHENTICATION;
            default:
                throw new IllegalArgumentException("Unsupported hash");
        }
    }

    public PacketTransformer getRTPTransformer()
    {
        return srtpTransformer;
    }

    public PacketTransformer getRTCPTransformer()
    {
        return srtcpTransformer;
    }
}
