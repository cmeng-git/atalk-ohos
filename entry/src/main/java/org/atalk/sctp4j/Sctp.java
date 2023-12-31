/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atalk.sctp4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import timber.log.Timber;

/**
 * Class encapsulates native SCTP counterpart.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class Sctp
{
    /**
     * FIXME Remove once usrsctp_finish is fixed
     */
    private static boolean initialized;

    /**
     * SCTP notification
     */
    public static final int MSG_NOTIFICATION = 0x2000;

    /**
     * Track the number of currently running SCTP engines.
     * Each engine calls {@link #init()} on startup and {@link #finish()} on shutdown. We want
     * {@link #init()} to be effectively called only when there are 0 engines currently running
     * and {@link #finish()} when the last one is performing a shutdown.
     */
    private static int sctpEngineCount;

    /**
     * List of instantiated <code>SctpSockets</code> mapped by native pointer.
     */
    private static final Map<Long, SctpSocket> sockets = new ConcurrentHashMap<>();

    static {
        String lib = "jnsctp";

        try {
            System.loadLibrary(lib);
        } catch (Throwable t) {
            Timber.e("Failed to load native library %s: %s", lib, t.getMessage());
            if (t instanceof Error)
                throw (Error) t;
            else
                throw (RuntimeException) t;
        }
    }

    /**
     * Closes SCTP socket addressed by given native pointer.
     *
     * @param ptr native socket pointer.
     */
    static void closeSocket(long ptr)
    {
        usrsctp_close(ptr);
        sockets.remove(ptr);
    }

    /**
     * Creates new <code>SctpSocket</code> for given SCTP port. Allocates native resources bound to
     * the socket.
     *
     * @param localPort local SCTP socket port.
     * @return new <code>SctpSocket</code> for given SCTP port.
     */
    public static SctpSocket createSocket(int localPort)
    {
        long ptr = usrsctp_socket(localPort);
        SctpSocket socket;

        if (ptr == 0) {
            socket = null;
        }
        else {
            socket = new SctpSocket(ptr, localPort);
            sockets.put(ptr, socket);
        }
        return socket;
    }

    /**
     * Disposes of the resources held by native counterpart.
     *
     * @throws IOException if usrsctp stack has failed to shutdown.
     */
    public static synchronized void finish()
            throws IOException
    {
        // Skip if we're not the last one
        //if(--sctpEngineCount > 0)
        //  return;

        //try
        //{
        // FIXME fix this loop?
        // it comes from SCTP samples written in C

        // Retry limited amount of times
			/*
			  FIXME usrsctp issue:
              SCTP stack is now never deinitialized in order to prevent deadlock
              in usrsctp_finish.
              https://code.google.com/p/webrtc/issues/detail?id=2749

            final int CLOSE_RETRY_COUNT = 20;

            for(int i=0; i < CLOSE_RETRY_COUNT; i++)
            {
                if(usrsctp_finish())
                    return;

                Thread.sleep(50);
            }*/

        //FIXME after throwing we might end up with other SCTP users broken
        // (or stack not disposed) at this point because engine count will
        // be out of sync for the purpose of calling init() and finish()
        // methods.
        //    throw new IOException("Failed to shutdown usrsctp stack" +
        //                              " after 20 retries");
        //}
        //catch(InterruptedException e)
        //{
        //    Timber.e(e, "Finish interrupted");
        //    Thread.currentThread().interrupt();
        //}
    }

    /**
     * Initializes native SCTP counterpart.
     */
    public static synchronized void init()
    {
        // Skip if we're not the first one
        //if(sctpEngineCount++ > 0)
        //    return;
        if (!initialized) {
            Timber.e("Init'ing brian's patched usrsctp");
            usrsctp_init(0);
            initialized = true;
        }
    }

    /**
     * Passes network packet to native SCTP stack counterpart.
     *
     * @param ptr native socket pointer.
     * @param pkt buffer holding network packet data.
     * @param off the position in the buffer where packet data starts.
     * @param len packet data length.
     */
    private static native void on_network_in(
            long ptr, byte[] pkt, int off, int len);

    /**
     * Used by {@link SctpSocket} to pass received network packet to native
     * counterpart.
     *
     * @param socketPtr native socket pointer.
     * @param packet network packet data.
     * @param offset position in the buffer where packet data starts.
     * @param len length of packet data in the buffer.
     */
    static void onConnIn(long socketPtr, byte[] packet, int offset, int len)
    {
        on_network_in(socketPtr, packet, offset, len);
    }

    /**
     * Method fired by native counterpart to notify about incoming data.
     *
     * @param socketAddr native socket pointer
     * @param data buffer holding received data
     * @param sid stream id
     * @param ssn
     * @param tsn
     * @param ppid payload protocol identifier
     * @param context
     * @param flags
     */
    public static void onSctpInboundPacket(
            long socketAddr, byte[] data, int sid, int ssn, int tsn, long ppid,
            int context, int flags)
    {
        SctpSocket socket = sockets.get(socketAddr);

        if (socket == null) {
            Timber.e("No SctpSocket found for ptr: %s", socketAddr);
        }
        else {
            socket.onSctpInboundPacket(data, sid, ssn, tsn, ppid, context, flags);
        }
    }

    /**
     * Method fired by native counterpart when SCTP stack wants to send network packet.
     *
     * @param socketAddr native socket pointer
     * @param data buffer holding packet data
     * @param tos type of service???
     * @param set_df use IP don't fragment option
     * @return 0 if the packet has been successfully sent or -1 otherwise.
     */
    public static int onSctpOutboundPacket(
            long socketAddr, byte[] data, int tos, int set_df)
    {
        // FIXME handle tos and set_df

        SctpSocket socket = sockets.get(socketAddr);
        int ret;

        if (socket == null) {
            ret = -1;
            Timber.e("No SctpSocket found for ptr: %s", socketAddr);
        }
        else {
            ret = socket.onSctpOut(data, tos, set_df);
        }
        return ret;
    }

    /**
     * Waits for incoming connection.
     *
     * @param ptr native socket pointer.
     */
    static native boolean usrsctp_accept(long ptr);

    /**
     * Closes SCTP socket.
     *
     * @param ptr native socket pointer.
     */
    private static native void usrsctp_close(long ptr);

    /**
     * Connects SCTP socket to remote socket on given SCTP port.
     *
     * @param ptr native socket pointer.
     * @param remotePort remote SCTP port.
     * @return <code>true</code> if the socket has been successfully connected.
     */
    static native boolean usrsctp_connect(long ptr, int remotePort);

    /**
     * Disposes of the resources held by native counterpart.
     *
     * @return <code>true</code> if stack successfully released resources.
     */
    native private static boolean usrsctp_finish();

    /**
     * Initializes native SCTP counterpart.
     *
     * @param port UDP encapsulation port.
     * @return <code>true</code> on success.
     */
    private static native boolean usrsctp_init(int port);

    /**
     * Makes socket passive.
     *
     * @param ptr native socket pointer.
     */
    static native void usrsctp_listen(long ptr);

    /**
     * Sends given <code>data</code> on selected SCTP stream using given payload
     * protocol identifier.
     * FIXME add offset and length buffer parameters.
     *
     * @param ptr native socket pointer.
     * @param data the data to send.
     * @param off the position of the data inside the buffer
     * @param len data length.
     * @param ordered should we care about message order ?
     * @param sid SCTP stream identifier
     * @param ppid payload protocol identifier
     * @return sent bytes count or <code>-1</code> in case of an error.
     */
    static native int usrsctp_send(
            long ptr, byte[] data, int off, int len, boolean ordered, int sid, int ppid);

    /**
     * Creates native SCTP socket and returns pointer to it.
     *
     * @param localPort local SCTP socket port.
     * @return native socket pointer or 0 if operation failed.
     */
    private static native long usrsctp_socket(int localPort);

    /*
    FIXME to be added?
    int usrsctp_shutdown(struct socket *so, int how);
    */
}
