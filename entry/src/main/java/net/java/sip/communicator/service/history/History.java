/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.history;

import net.java.sip.communicator.service.history.records.HistoryRecordStructure;

/**
 * @author Alexander Pelov
 * @author Yana Stamcheva
 */
public interface History {
    /**
     * @return Returns the ID of this history.
     */
    HistoryID getID();

    /**
     * @return Returns the structure of the history records in this history.
     */
    HistoryRecordStructure getHistoryRecordsStructure();

    /**
     * Sets the given <code>structure</code> to be the new history records structure used in this history implementation.
     *
     * @param structure the new <code>HistoryRecordStructure</code> to use
     */
    void setHistoryRecordsStructure(HistoryRecordStructure structure);
}
