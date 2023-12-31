/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.service.neomedia;

import ch.imvs.sdes4j.srtp.SrtpCryptoAttribute;

/**
 * SDES based SRTP MediaStream encryption control.
 *
 * @author Ingo Bauersachs
 * @author Eng Chong Meng
 */
public interface SDesControl extends SrtpControl
{
    /**
     * Name of the config setting that supplies the default enabled cipher suites. Cipher suites are
     * comma-separated.
     */
    String SDES_CIPHER_SUITES = "neomedia.SDES_CIPHER_SUITES";

    /**
     * Gets the crypto attribute of the incoming MediaStream.
     *
     * @return the crypto attribute of the incoming MediaStream.
     */
    SrtpCryptoAttribute getInAttribute();

    /**
     * Returns the crypto attributes enabled on this computer.
     *
     * @return The crypto attributes enabled on this computer.
     */
    SrtpCryptoAttribute[] getInitiatorCryptoAttributes();

    /**
     * Gets the crypto attribute of the outgoing MediaStream.
     *
     * @return the crypto attribute of the outgoing MediaStream.
     */
    SrtpCryptoAttribute getOutAttribute();

    /**
     * Gets all supported cipher suites.
     *
     * @return all supported cipher suites.
     */
    Iterable<String> getSupportedCryptoSuites();

    /**
     * Selects the local crypto attribute from the initial offering (
     * {@link #getInitiatorCryptoAttributes()}) based on the peer's first matching cipher suite.
     *
     * @param peerAttributes The peer's crypto offers.
     * @return A SrtpCryptoAttribute when a matching cipher suite was found; <code>null</code>, otherwise.
     */
    SrtpCryptoAttribute initiatorSelectAttribute(Iterable<SrtpCryptoAttribute> peerAttributes);

    /**
     * Chooses a supported crypto attribute from the peer's list of supplied attributes and creates
     * the local crypto attribute. Used when the control is running in the role as responder.
     *
     * @param peerAttributes The peer's crypto attribute offering.
     * @return The local crypto attribute for the answer of the offer or <code>null</code> if no
     * matching cipher suite could be found.
     */
    SrtpCryptoAttribute responderSelectAttribute(Iterable<SrtpCryptoAttribute> peerAttributes);

    /**
     * Sets the enabled SDES ciphers.
     *
     * @param ciphers The list of enabled ciphers.
     */
    void setEnabledCiphers(Iterable<String> ciphers);
}
