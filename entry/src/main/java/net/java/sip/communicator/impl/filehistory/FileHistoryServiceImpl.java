/*
 * aTalk, ohos VoIP and Instant Messaging client
 * Copyright 2024 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.filehistory;

import java.io.IOException;
import java.util.Comparator;
import java.util.Date;
import java.util.EventObject;
import java.util.Iterator;

import ohos.data.rdb.RdbPredicates;
import ohos.data.rdb.RdbStore;
import ohos.data.rdb.ValuesBucket;
import ohos.data.resultset.ResultSet;

import net.java.sip.communicator.impl.protocol.jabber.OutgoingFileSendEntityImpl;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.filehistory.FileHistoryService;
import net.java.sip.communicator.service.filehistory.FileRecord;
import net.java.sip.communicator.service.history.HistoryService;
import net.java.sip.communicator.service.msghistory.MessageHistoryService;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.FileTransfer;
import net.java.sip.communicator.service.protocol.IMessage;
import net.java.sip.communicator.service.protocol.IncomingFileTransferRequest;
import net.java.sip.communicator.service.protocol.OperationSetFileTransfer;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.FileTransferCreatedEvent;
import net.java.sip.communicator.service.protocol.event.FileTransferRequestEvent;
import net.java.sip.communicator.service.protocol.event.ScFileTransferListener;
import net.java.sip.communicator.util.ServiceUtils;

import org.apache.commons.lang3.StringUtils;
import org.atalk.impl.timberlog.TimberLog;
import org.atalk.ohos.gui.chat.ChatMessage;
import org.atalk.ohos.gui.chat.ChatSession;
import org.atalk.ohos.gui.chat.filetransfer.FileReceiveConversation;
import org.atalk.ohos.gui.chat.filetransfer.FileSendConversation;
import org.atalk.persistance.DatabaseBackend;
import org.jxmpp.util.XmppStringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import timber.log.Timber;

/**
 * File History Service stores info for file transfers from various protocols.
 * It handles both the outgoing and incoming file transfer events.
 * ScFileTransferListener = To handle Incoming fileTransfer request callbacks;
 * except fileTransferCreated which take care of both incoming and outgoing file creation.
 *
 * @author Eng Chong Meng
 */
public class FileHistoryServiceImpl implements FileHistoryService, ServiceListener, ScFileTransferListener {
    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext bundleContext = null;

    private RdbStore mRdbStore;
    private MessageHistoryService mhs;

    /**
     * Starts the service. Check the current registered protocol providers which supports
     * FileTransfer and adds a listener to them.
     *
     * @param bc BundleContext
     */
    public void start(BundleContext bc) {
        Timber.d("Starting the file history implementation.");
        this.bundleContext = bc;
        mRdbStore = DatabaseBackend.getRdbStore();

        // start listening for newly register or removed protocol providers
        bc.addServiceListener(this);
        ServiceReference[] ppsRefs = null;
        try {
            ppsRefs = bc.getServiceReferences(ProtocolProviderService.class.getName(), null);
        } catch (InvalidSyntaxException e) {
            Timber.w("PPS service reference (add): %s", e.getMessage());
        }

        // in case we found any
        if ((ppsRefs != null) && (ppsRefs.length != 0)) {
            Timber.d("Found %s installed providers.", ppsRefs.length);
            for (ServiceReference<ProtocolProviderService> ppsRef : ppsRefs) {
                ProtocolProviderService pps = bc.getService(ppsRef);
                handleProviderAdded(pps);
            }
        }
    }

    /**
     * Stops the service.
     *
     * @param bc BundleContext
     */
    public void stop(BundleContext bc) {
        bc.removeServiceListener(this);
        ServiceReference[] ppsRefs = null;
        try {
            ppsRefs = bc.getServiceReferences(ProtocolProviderService.class.getName(), null);
        } catch (InvalidSyntaxException e) {
            Timber.w("PPS service reference (remove): %s", e.getMessage());
        }

        // in case we found any
        if ((ppsRefs != null) && (ppsRefs.length != 0)) {
            for (ServiceReference<ProtocolProviderService> ppsRef : ppsRefs) {
                ProtocolProviderService pps = bc.getService(ppsRef);
                handleProviderRemoved(pps);
            }
        }
    }

    /**
     * When new protocol provider is registered we check does it supports FileTransfer and if so add a listener to it
     *
     * @param serviceEvent ServiceEvent
     */
    public void serviceChanged(ServiceEvent serviceEvent) {
        Object sService = bundleContext.getService(serviceEvent.getServiceReference());
        Timber.log(TimberLog.FINER, "Received a service event for: %s", sService.getClass().getName());

        // we don't care if the source service is not a protocol provider
        if (!(sService instanceof ProtocolProviderService)) {
            return;
        }

        Timber.d("Service is a protocol provider.");
        if (serviceEvent.getType() == ServiceEvent.REGISTERED) {
            Timber.d("Handling registration of a new Protocol Provider.");

            this.handleProviderAdded((ProtocolProviderService) sService);
        }
        else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING) {
            this.handleProviderRemoved((ProtocolProviderService) sService);
        }
    }

    /**
     * Used to attach the File History Service to existing or just registered protocol provider.
     * Checks if the provider has implementation of OperationSetFileTransfer
     *
     * @param provider ProtocolProviderService
     */
    private void handleProviderAdded(ProtocolProviderService provider) {
        Timber.d("Adding protocol provider %s", provider.getProtocolName());

        // check whether the provider has a file transfer operation set
        OperationSetFileTransfer opSetFileTransfer = provider.getOperationSet(OperationSetFileTransfer.class);
        if (opSetFileTransfer != null) {
            opSetFileTransfer.addFileTransferListener(this);
        }
        else {
            Timber.log(TimberLog.FINER, "Service did not have a file transfer op. set.");
        }
    }

    /**
     * Removes the specified provider from the list of currently known providers
     *
     * @param provider the ProtocolProviderService that has been unregistered.
     */
    private void handleProviderRemoved(ProtocolProviderService provider) {
        OperationSetFileTransfer opSetFileTransfer = provider.getOperationSet(OperationSetFileTransfer.class);
        if (opSetFileTransfer != null) {
            opSetFileTransfer.removeFileTransferListener(this);
        }
    }

    /**
     * Set the history service.
     *
     * @param historyService HistoryService
     */
    public void setHistoryService(HistoryService historyService) {
    }

    private MessageHistoryService getMHS() {
        if (mhs == null)
            mhs = ServiceUtils.getService(bundleContext, MessageHistoryService.class);
        return mhs;
    }

    /* ============= File Transfer Handlers - ScFileTransferListener callbacks implementations ============= */
    /**
     * Receive fileTransfer requests.
     *
     * @param event FileTransferRequestEvent
     */
    public void fileTransferRequestReceived(FileTransferRequestEvent event) {
        IncomingFileTransferRequest req = event.getRequest();
        String fileName = req.getFileName();
        insertRecordToDB(event, ChatMessage.MESSAGE_FILE_TRANSFER_RECEIVE, fileName);
    }

    /**
     * New file transfer was created; callback from both IncomingFileTransfer, OutgoingFileTransfer and
     *
     * @param event FileTransferCreatedEvent for all FileTransfers
     *
     * @see FileSendConversation#createFileSendRecord()
     */
    public void fileTransferCreated(FileTransferCreatedEvent event) {
        FileTransfer fileTransfer = event.getFileTransfer();
        ValuesBucket values = new ValuesBucket();
        try {
            String fileName = fileTransfer.getLocalFile().getCanonicalPath();
            Timber.d("File Transfer record created in DB: %s: %s", fileTransfer.getDirection(), fileName);

            if (fileTransfer.getDirection() == FileTransfer.IN) {
                values.putString(ChatMessage.FILE_PATH, fileName);

                RdbPredicates rdbPredicates = new RdbPredicates(ChatMessage.TABLE_NAME)
                        .equalTo(ChatMessage.UUID, fileTransfer.getID());
                mRdbStore.update(values, rdbPredicates);
            }
            else if (fileTransfer.getDirection() == FileTransfer.OUT) {
                insertRecordToDB(event, ChatMessage.MESSAGE_FILE_TRANSFER_SEND, fileName);
            }
        } catch (IOException e) {
            Timber.e(e, "Could not add file transfer log to history");
        }
    }

    /**
     * Called when a new <code>IncomingFileTransferRequest</code> has been rejected.
     *
     * @param event the <code>FileTransferRequestEvent</code> containing the received request which was rejected.
     *
     * @see FileReceiveConversation#fileTransferRequestRejected(FileTransferRequestEvent)
     */
    public void fileTransferRequestRejected(FileTransferRequestEvent event) {
        // Event is being handled by FileReceiveConversation; need to update both the DB and msgCache
    }

    /**
     * Called when a new <code>IncomingFileTransferRequest</code> has been cancel by the sender.
     *
     * @param event the <code>FileTransferRequestEvent</code> containing the received request which was rejected.
     *
     * @see FileReceiveConversation#fileTransferRequestCanceled(FileTransferRequestEvent)
     */
    public void fileTransferRequestCanceled(FileTransferRequestEvent event) {
        // Event is being handled by FileReceiveConversation; need to update both the DB and msgCache
    }

    /**
     * Create new fileTransfer record in DB when file transfer has started
     * Also use as conversion for http file upload link message to file transfer message
     *
     * @param evt FileTransferRequestEvent or FileTransferCreatedEvent
     * @param msgType file record message type i.e MESSAGE_FILE_TRANSFER_SEND, MESSAGE_FILE_TRANSFER_RECEIVE, MESSAGE_HTTP_FILE_DOWNLOAD
     * @param fileName Name of the file to received or send
     */
    public void insertRecordToDB(EventObject evt, int msgType, String fileName) {
        Date timeStamp = new Date();
        String uuid = null;
        String mJid, mEntityJid;
        String direction = FileRecord.OUT;
        Object entityJid = null;
        String serverMsgId = null;
        String remoteMsgId = null;
        ValuesBucket valuesBucket = new ValuesBucket();

        if (evt instanceof FileTransferRequestEvent) {
            FileTransferRequestEvent event = (FileTransferRequestEvent) evt;
            IncomingFileTransferRequest req = event.getRequest();
            uuid = req.getId();
            entityJid = req.getSender();
            timeStamp = event.getTimestamp();
            direction = FileRecord.IN;
            valuesBucket.putString(ChatMessage.MSG_BODY, fileName);
        }
        else if (evt instanceof FileTransferCreatedEvent) {
            FileTransferCreatedEvent event = (FileTransferCreatedEvent) evt;
            timeStamp = event.getTimestamp();
            FileTransfer fileTransfer = event.getFileTransfer();
            uuid = fileTransfer.getID();

            if (fileTransfer.getDirection() == FileTransfer.IN) {
                direction = FileRecord.IN;
                remoteMsgId = uuid;
            } else {
                direction = FileRecord.OUT;
                serverMsgId = uuid;
            }

            if (fileTransfer instanceof OutgoingFileSendEntityImpl) {
                entityJid = ((OutgoingFileSendEntityImpl) fileTransfer).getEntityJid();
            }
            else {
                entityJid = fileTransfer.getContact();
            }
        }

        String sessionUuid;
        if (entityJid instanceof Contact) {
            Contact contact = (Contact) entityJid;
            sessionUuid = getMHS().getSessionUuidByJid(contact);
            mEntityJid = contact.getAddress();
            mJid = contact.getProtocolProvider().getOurJid().toString();
        }
        else {
            ChatRoom chatroom = (ChatRoom) entityJid;
            sessionUuid = getMHS().getSessionUuidByJid(chatroom);
            mJid = chatroom.getParentProvider().getOurJid().asEntityBareJidString();
            mEntityJid = XmppStringUtils.parseLocalpart(mJid);
        }

        valuesBucket.putString(ChatMessage.UUID, uuid);
        valuesBucket.putString(ChatMessage.SESSION_UUID, sessionUuid);
        valuesBucket.putLong(ChatMessage.TIME_STAMP, timeStamp);
        valuesBucket.putString(ChatMessage.ENTITY_JID, mEntityJid);
        valuesBucket.putString(ChatMessage.JID, mJid);
        valuesBucket.putInteger(ChatMessage.ENC_TYPE, IMessage.ENCODE_PLAIN);
        valuesBucket.putInteger(ChatMessage.MSG_TYPE, msgType);
        valuesBucket.putString(ChatMessage.DIRECTION, direction);
        valuesBucket.putInteger(ChatMessage.STATUS, FileRecord.STATUS_WAITING);
        valuesBucket.putString(ChatMessage.FILE_PATH, fileName);
        valuesBucket.putString(ChatMessage.SERVER_MSG_ID, serverMsgId);
        valuesBucket.putString(ChatMessage.REMOTE_MSG_ID, remoteMsgId);

        mRdbStore.insert(ChatMessage.TABLE_NAME, valuesBucket);
        getMHS().setMamDate(sessionUuid, timeStamp);
    }

    /* ============= File Transfer Handlers - Update file transfer status ============= */
    /**
     * Update new status and fileName to the fileTransfer record in dataBase.
     * Keep file uri; for retry if not converted to MESSAGE_FILE_TRANSFER_HISTORY.
     *
     * @param msgUuid message UUID
     * @param status New status for update
     * @param fileName local fileName path for http downloaded file; null => no change and keep the link in MSG_BODY
     * @param encType IMessage.ENCRYPTION_NONE, ENCRYPTION_OMEMO
     * @param msgType File Transfer message type
     *
     * @return the number of records being updated; zero means there is no record to update historyLog disabled
     */
    public int updateFTStatusToDB(String msgUuid, int status, String fileName, int encType, int msgType) {
        // Timber.w(new Exception("### File in/out transfer status changes to: " + status));
        ValuesBucket valuesBucket = new ValuesBucket();
        valuesBucket.putInteger(ChatMessage.STATUS, status);

        if (StringUtils.isNotEmpty(fileName)) {
            valuesBucket.putString(ChatMessage.FILE_PATH, fileName);
        }
        valuesBucket.putInteger(ChatMessage.ENC_TYPE, encType);
        valuesBucket.putInteger(ChatMessage.MSG_TYPE, msgType);

        RdbPredicates rdbPredicates = new RdbPredicates(ChatMessage.TABLE_NAME)
                .equalTo(ChatMessage.UUID, msgUuid);
        return mRdbStore.update(valuesBucket, rdbPredicates);
    }

    /**
     * Permanently removes locally stored chatRoom messages (need cleanup - not used)
     */
    public void eraseLocallyStoredHistory() {
        String[] columns = {ChatSession.SESSION_UUID};
        RdbPredicates rdbPredicates = new RdbPredicates(ChatSession.TABLE_NAME)
                .equalTo(ChatSession.MODE, ChatSession.MODE_MULTI);
        ResultSet resultSet = mRdbStore.query(rdbPredicates, columns);

        while (resultSet.goToNextRow()) {
            purgeLocallyStoredHistory(null, resultSet.getString(0));
        }
        resultSet.close();

        mRdbStore.delete(new RdbPredicates(ChatSession.TABLE_NAME));
    }

    /**
     * Permanently removes locally stored message history for the sessionUuid.
     * - Remove only chatMessages for metaContacts
     * - Remove both chatSessions and chatMessages for muc
     */
    public void eraseLocallyStoredHistory(MetaContact metaContact) {
        getMHS();
        Iterator<Contact> contacts = metaContact.getContacts();
        while (contacts.hasNext()) {
            Contact contact = contacts.next();
            String sessionUuid = mhs.getSessionUuidByJid(contact);

            purgeLocallyStoredHistory(contact, sessionUuid);
        }
    }

    /**
     * Permanently removes locally stored message history for the sessionUuid.
     * - Remove only chatMessages for metaContacts
     * - Remove both chatSessions and chatMessages for muc
     */
    private void purgeLocallyStoredHistory(Contact contact, String sessionUuid) {
        if (contact != null) {
            mRdbStore.delete(new RdbPredicates(ChatMessage.TABLE_NAME)
                    .equalTo(ChatMessage.SESSION_UUID, sessionUuid));
        }
        else {
            mRdbStore.delete(new RdbPredicates(ChatSession.TABLE_NAME)
                    .equalTo(ChatSession.SESSION_UUID, sessionUuid));
        }
    }

    /**
     * Used to compare FileRecords and to be ordered in TreeSet according their timestamp
     */
    private static class FileRecordComparator implements Comparator<FileRecord> {
        public int compare(FileRecord o1, FileRecord o2) {
            Date date1 = o1.getDate();
            Date date2 = o2.getDate();
            return date1.compareTo(date2);
        }
    }
}
