/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.impl.neomedia.rtp.translator;

import org.atalk.service.neomedia.MediaStream;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.LinkedList;
import java.util.List;

import javax.media.protocol.PushSourceStream;
import javax.media.rtp.OutputDataStream;
import javax.media.rtp.RTPConnector;

/**
 * Implements the <code>RTPConnector</code> with which this instance initializes its <code>RTPManager</code>
 * . It delegates to the <code>RTPConnector</code> of the various <code>StreamRTPManager</code>s.
 *
 * @author Lyubomir Marinov
 */
class RTPConnectorImpl implements RTPConnector
{
    /**
     * The <code>RTPConnector</code>s this instance delegates to.
     */
    private final List<RTPConnectorDesc> connectors = new LinkedList<RTPConnectorDesc>();

    private PushSourceStreamImpl controlInputStream;

    private OutputDataStreamImpl controlOutputStream;

    private PushSourceStreamImpl dataInputStream;

    private OutputDataStreamImpl dataOutputStream;

    public final RTPTranslatorImpl translator;

    /**
     * The indicator which determines whether {@link #close()} has been invoked on this instance.
     */
    private boolean closed = false;

    public RTPConnectorImpl(RTPTranslatorImpl translator)
    {
        this.translator = translator;
    }

    public synchronized void addConnector(RTPConnectorDesc connector)
    {
        // XXX Could we use a read/write lock instead of a synchronized here?
        // We acquire a write lock and as soon as add the connector to the
        // connectors we downgrade to a read lock.
        if (!connectors.contains(connector)) {
            connectors.add(connector);
            if (this.controlInputStream != null) {
                PushSourceStream controlInputStream = null;

                try {
                    controlInputStream = connector.connector.getControlInputStream();
                } catch (IOException ioe) {
                    throw new UndeclaredThrowableException(ioe);
                }
                if (controlInputStream != null) {
                    this.controlInputStream.addStream(connector, controlInputStream);
                }
            }
            if (this.controlOutputStream != null) {
                OutputDataStream controlOutputStream = null;

                try {
                    controlOutputStream = connector.connector.getControlOutputStream();
                } catch (IOException ioe) {
                    throw new UndeclaredThrowableException(ioe);
                }
                if (controlOutputStream != null) {
                    this.controlOutputStream.addStream(connector, controlOutputStream);
                }
            }
            if (this.dataInputStream != null) {
                PushSourceStream dataInputStream = null;

                try {
                    dataInputStream = connector.connector.getDataInputStream();
                } catch (IOException ioe) {
                    throw new UndeclaredThrowableException(ioe);
                }
                if (dataInputStream != null) {
                    this.dataInputStream.addStream(connector, dataInputStream);
                }
            }
            if (this.dataOutputStream != null) {
                OutputDataStream dataOutputStream = null;

                try {
                    dataOutputStream = connector.connector.getDataOutputStream();
                } catch (IOException ioe) {
                    throw new UndeclaredThrowableException(ioe);
                }
                if (dataOutputStream != null) {
                    this.dataOutputStream.addStream(connector, dataOutputStream);
                }
            }
        }
    }

    @Override
    public synchronized void close()
    {
        if (controlInputStream != null) {
            controlInputStream.close();
            controlInputStream = null;
        }
        if (controlOutputStream != null) {
            controlOutputStream.close();
            controlOutputStream = null;
        }
        if (dataInputStream != null) {
            dataInputStream.close();
            dataInputStream = null;
        }
        if (dataOutputStream != null) {
            dataOutputStream.close();
            dataOutputStream = null;
        }

        this.closed = true;

        for (RTPConnectorDesc connectorDesc : connectors)
            connectorDesc.connector.close();
    }

    @Override
    public synchronized PushSourceStream getControlInputStream()
            throws IOException
    {
        if (this.controlInputStream == null) {
            this.controlInputStream = new PushSourceStreamImpl(this, false);
            for (RTPConnectorDesc connectorDesc : connectors) {
                PushSourceStream controlInputStream = connectorDesc.connector.getControlInputStream();
                if (controlInputStream != null) {
                    this.controlInputStream.addStream(connectorDesc, controlInputStream);
                }
            }
        }
        return this.controlInputStream;
    }

    @Override
    public synchronized OutputDataStreamImpl getControlOutputStream()
            throws IOException
    {
        if (this.closed) {
            throw new IllegalStateException("Connector closed.");
        }

        if (this.controlOutputStream == null) {
            this.controlOutputStream = new OutputDataStreamImpl(this, false);
            for (RTPConnectorDesc connectorDesc : connectors) {
                OutputDataStream controlOutputStream = connectorDesc.connector.getControlOutputStream();
                if (controlOutputStream != null) {
                    this.controlOutputStream.addStream(connectorDesc, controlOutputStream);
                }
            }
        }
        return this.controlOutputStream;
    }

    @Override
    public synchronized PushSourceStream getDataInputStream()
            throws IOException
    {
        if (this.dataInputStream == null) {
            this.dataInputStream = new PushSourceStreamImpl(this, true);
            for (RTPConnectorDesc connectorDesc : connectors) {
                PushSourceStream dataInputStream = connectorDesc.connector.getDataInputStream();
                if (dataInputStream != null) {
                    this.dataInputStream.addStream(connectorDesc, dataInputStream);
                }
            }
        }
        return this.dataInputStream;
    }

    @Override
    public synchronized OutputDataStreamImpl getDataOutputStream()
            throws IOException
    {
        if (this.closed) {
            throw new IllegalStateException("Connector closed.");
        }

        if (this.dataOutputStream == null) {
            this.dataOutputStream = new OutputDataStreamImpl(this, true);
            for (RTPConnectorDesc connectorDesc : connectors) {
                OutputDataStream dataOutputStream = connectorDesc.connector.getDataOutputStream();
                if (dataOutputStream != null) {
                    this.dataOutputStream.addStream(connectorDesc, dataOutputStream);
                }
            }
        }
        return this.dataOutputStream;
    }

    /**
     * Not implemented because there are currently no uses of the underlying functionality.
     */
    @Override
    public int getReceiveBufferSize()
    {
        return -1;
    }

    /**
     * Not implemented because there are currently no uses of the underlying functionality.
     */
    @Override
    public double getRTCPBandwidthFraction()
    {
        return -1;
    }

    /**
     * Not implemented because there are currently no uses of the underlying functionality.
     */
    @Override
    public double getRTCPSenderBandwidthFraction()
    {
        return -1;
    }

    /**
     * Not implemented because there are currently no uses of the underlying functionality.
     */
    @Override
    public int getSendBufferSize()
    {
        return -1;
    }

    public synchronized void removeConnector(RTPConnectorDesc connector)
    {
        if (connectors.contains(connector)) {
            if (controlInputStream != null)
                controlInputStream.removeStreams(connector);
            if (controlOutputStream != null)
                controlOutputStream.removeStreams(connector);
            if (dataInputStream != null)
                dataInputStream.removeStreams(connector);
            if (dataOutputStream != null)
                dataOutputStream.removeStreams(connector);
            connectors.remove(connector);
        }
    }

    /**
     * Not implemented because there are currently no uses of the underlying functionality.
     */
    @Override
    public void setReceiveBufferSize(int receiveBufferSize)
            throws IOException
    {
        // TODO Auto-generated method stub
    }

    /**
     * Not implemented because there are currently no uses of the underlying functionality.
     */
    @Override
    public void setSendBufferSize(int sendBufferSize)
            throws IOException
    {
        // TODO Auto-generated method stub
    }

    /**
     * Writes an <code>RTCPFeedbackMessage</code> into a destination identified by a specific
     * <code>MediaStream</code>.
     *
     * @param controlPayload
     * @param destination
     * @return <code>true</code> if the <code>controlPayload</code> was written into the
     * <code>destination</code>; otherwise, <code>false</code>
     */
    boolean writeControlPayload(Payload controlPayload, MediaStream destination)
    {
        OutputDataStreamImpl controlOutputStream = this.controlOutputStream;

        return (controlOutputStream == null) ? false
                : controlOutputStream.writeControlPayload(controlPayload, destination);
    }
}
