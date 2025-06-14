/*
 * aTalk, android VoIP and Instant Messaging client
 * Copyright 2014 Eng Chong Meng
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
package net.java.sip.communicator.impl.protocol.jabber;

import java.io.File;

import net.java.sip.communicator.service.protocol.AbstractFileTransfer;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.event.FileTransferStatusChangeEvent;

import org.atalk.ohos.R;
import org.atalk.ohos.aTalkApp;
import org.jivesoftware.smackx.jingle.component.JingleSessionImpl;
import org.jivesoftware.smackx.jingle.component.JingleSessionImpl.SessionState;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle_filetransfer.controller.JingleFileTransferController;
import org.jivesoftware.smackx.jingle_filetransfer.listener.ProgressListener;

import timber.log.Timber;

/**
 * The Jabber protocol extension of the <code>AbstractFileTransfer</code> to handle Jingle File Offer.
 *
 * @author Eng Chong Meng
 */
public class IncomingFileTransferJingleImpl extends AbstractFileTransfer {
    private final String mId;
    private final Contact mSender;
    private final File mFile;
    private int mByteRead;

    private final JingleSessionImpl mJingleSession;

    /**
     * The Jingle incoming file offer.
     */
    private final IncomingFileOfferJingleImpl mIfoJingle;

    /**
     * Creates an <code>IncomingFileTransferJingleImpl</code>.
     *
     * @param inFileOffer the Jingle incoming file offer
     * @param file the file
     */
    public IncomingFileTransferJingleImpl(IncomingFileOfferJingleImpl inFileOffer, File file) {
        mId = inFileOffer.getId();
        mSender = inFileOffer.getSender();
        mFile = file;
        mIfoJingle = inFileOffer;
        mByteRead = 0;
        JingleFileTransferController jftController = mIfoJingle.getController();
        jftController.addProgressListener(inFileProgressListener);
        mJingleSession = jftController.getJingleSession();
        mJingleSession.addJingleSessionListener(jingleSessionListener);
    }

    /**
     * User declines or cancels an incoming file transfer request during initial or during the active state.
     */
    @Override
    public void cancel() {
        try {
            onCanceled();
            mIfoJingle.declineFile();
        } catch (OperationFailedException e) {
            Timber.e("Exception: %s", e.getMessage());
        }
    }

    private void onCanceled() {
        String reason = aTalkApp.getResString(R.string.file_transfer_canceled);
        fireStatusChangeEvent(FileTransferStatusChangeEvent.CANCELED, reason);
    }

    /**
     * Remove IFO Listener:
     * a. Own IncomingFileOfferJingleImpl#declineFile(); nothing returns from remote
     * b. onSessionTerminated() received from remote; user cancels prior to accept or while in active transfer
     */
    public void removeIfoListener() {
        // Timber.d("Remove Ifo Listener");
        mIfoJingle.getController().removeProgressListener(inFileProgressListener);
        mJingleSession.removeJingleSessionListener(jingleSessionListener);
    }

    /**
     * Returns the number of bytes already received from the recipient.
     *
     * @return the number of bytes already received from the recipient
     */
    @Override
    public long getTransferredBytes() {
        return mByteRead;
    }

    /**
     * The direction is incoming.
     *
     * @return IN
     */
    public int getDirection() {
        return IN;
    }

    /**
     * Returns the sender of the file.
     *
     * @return the sender of the file
     */
    public Contact getContact() {
        return mSender;
    }

    /**
     * Returns the identifier of this file transfer.
     *
     * @return the identifier of this file transfer
     */
    public String getID() {
        return mId;
    }

    /**
     * Returns the local file that is being transferred.
     *
     * @return the file
     */
    public File getLocalFile() {
        return mFile;
    }

    ProgressListener inFileProgressListener = new ProgressListener() {
        @Override
        public void onStarted() {
            fireStatusChangeEvent(FileTransferStatusChangeEvent.IN_PROGRESS, "Byte receiving started");
        }

        /**
         * OperationSetFileTransferJabberImp#FileTransferProgressThread is not enabled for JingleFile
         * Transfer; so fireProgressChangeEvent to update display incoming file progressBar in UI
         *
         * @param rwBytes progressive byte count for byte-stream sent/received
         */
        @Override
        public void progress(int rwBytes) {
            mByteRead = rwBytes;
            fireProgressChangeEvent(System.currentTimeMillis(), rwBytes);
        }

        @Override
        public void onFinished() {
            Timber.d("Jingle session state: File received completed");
            fireStatusChangeEvent(FileTransferStatusChangeEvent.COMPLETED, "File received completed");
        }

        @Override
        public void onError(JingleReason reason) {
            jingleSessionListener.onSessionTerminated(reason);
        }
    };

    JingleSessionImpl.JingleSessionListener jingleSessionListener = new JingleSessionImpl.JingleSessionListener() {
        @Override
        public void sessionStateUpdated(SessionState oldState, SessionState newState) {
            String sessionState = newState.toString();
            Timber.d("Jingle session state: %s => %s (%s)", oldState, newState, mId);
            switch (newState) {
                case fresh:
                    fireStatusChangeEvent(FileTransferStatusChangeEvent.PREPARING, sessionState);
                    break;

                case pending:
                    // Currently not use in FileReceiveConversation
                    fireStatusChangeEvent(FileTransferStatusChangeEvent.WAITING, sessionState);
                    break;

                case active:
                    fireStatusChangeEvent(FileTransferStatusChangeEvent.IN_PROGRESS, sessionState);
                    break;

                case cancelled:
                    fireStatusChangeEvent(FileTransferStatusChangeEvent.CANCELED, sessionState);
                    break;

                case ended:
                    // Valid for sender only. Rely onFinished() instead.
                    // fireStatusChangeEvent(FileTransferStatusChangeEvent.COMPLETED, sessionState);
                    break;
            }
        }

        @Override
        public void onSessionAccepted() {
            // For sender only, nothing to do here for jingle responder;
            // both accept and decline actions are handled in IncomingFileOfferJingleImpl
        }

        @Override
        public void onSessionTerminated(JingleReason reason) {
            fireStatusChangeEvent(reason);
            removeIfoListener();
        }
    };
}
