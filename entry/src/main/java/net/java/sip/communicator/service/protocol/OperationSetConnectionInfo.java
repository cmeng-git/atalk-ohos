/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.net.InetSocketAddress;

/**
 * An <code>OperationSet</code> that allows access to connection information used by the protocol provider.
 *
 * @author Markus Kilas
 * @author Eng Chong Meng
 */
public interface OperationSetConnectionInfo extends OperationSet
{
    /**
     * @return The address of the server.
     */
    InetSocketAddress getServerAddress();
}
