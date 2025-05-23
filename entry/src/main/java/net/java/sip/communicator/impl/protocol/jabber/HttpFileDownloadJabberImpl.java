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
package net.java.sip.communicator.impl.protocol.jabber;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Hashtable;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

import ohos.app.Context;
import ohos.miscservices.download.DownloadConfig;
import ohos.miscservices.download.DownloadSession;
import ohos.miscservices.download.IDownloadListener;
import ohos.utils.net.Uri;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.persistance.FileBackend;
import org.atalk.persistance.FilePathHelper;
import org.jivesoftware.smackx.omemo_media_sharing.AesgcmUrl;

import net.java.sip.communicator.service.protocol.AbstractFileTransfer;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.IMessage;
import net.java.sip.communicator.service.protocol.event.FileTransferStatusChangeEvent;
import timber.log.Timber;

/**
 * The Jabber protocol HttpFileDownloadJabberImpl extension of the <code>AbstractFileTransfer</code>.
 *
 * @author Eng Chong Meng
 */
public class HttpFileDownloadJabberImpl extends AbstractFileTransfer {
    private final static Context mContext = aTalkApp.getInstance();

    /* DownloadManager Broadcast Receiver Handler */
    private DownloadSession dnSession;
    private IDownloadListener dnListener = null;

    /* previousDownloads <DownloadJobId, Download Link> */
    private final Hashtable<Long, String> previousDownloads = new Hashtable<>();

    private final String msgUuid;
    private final Contact mSender;

    /*
     * The advertised downloadable file info:
     * mFile: server url link last segment: File
     * mFileName: mFile filename
     * dnLink: server url link for download
     * mFileSize: the query size of the dnLink file
     */
    private final File mFile;
    private final String mFileName;
    private final String dnLink;
    // https download uri link; extracted from dnLink if it is AesgcmUrl
    private final Uri mUri;
    private long mFileSize;

    // Downloading tmp file.
    private File tmpFile = null;

    /**
     * The transfer file full path for saving the received file.
     */
    protected File mXferFile;

    /*
     * Transfer file encryption type, default to ENCRYPTION_NONE.
     */
    protected int mEncryption;

    /**
     * Creates an <code>IncomingFileTransferJabberImpl</code>.
     *
     * @param sender the sender of the file
     * @param id the message Uuid uniquely identify  record in DB
     * @param dnLinkDescription the download link may contains other options e.g. file.length()
     */
    public HttpFileDownloadJabberImpl(Contact sender, String id, String dnLinkDescription) {
        mSender = sender;

        // Create a new msg Uuid if none provided
        msgUuid = (id == null) ? String.valueOf(System.currentTimeMillis()) + hashCode() : id;

        String[] dnLinkInfos = dnLinkDescription.split("\\s+|,|\\t|\\n");
        dnLink = dnLinkInfos[0];
        String url;
        if (dnLink.matches("^aesgcm:.*")) {
            AesgcmUrl aesgcmUrl = new AesgcmUrl(dnLink);
            url = aesgcmUrl.getDownloadUrl().toString();
            mEncryption = IMessage.ENCRYPTION_OMEMO;
        }
        else {
            url = dnLink;
            mEncryption = IMessage.ENCRYPTION_NONE;
        }

        mUri = Uri.parse(url);
        mFileName = mUri.getLastPathSegment();
        mFile = (mFileName != null) ? new File(mFileName) : null;

        if (dnLinkInfos.length > 1 && "fileSize".matches(dnLinkInfos[1])) {
            mFileSize = Long.parseLong(dnLinkInfos[1].split("[:=]")[1]);
        }
        else
            mFileSize = -1;
    }

    /**
     * Unregister the HttpDownload transfer downloadReceiver.
     */
    @Override
    public void cancel() {
        doCleanup(-1);
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
        return msgUuid;
    }

    /**
     * Returns the local file that is being transferred or to which we transfer.
     *
     * @return the file
     */
    public File getLocalFile() {
        return mFile;
    }

    /**
     * Returns the name of the file corresponding to this request.
     *
     * @return the name of the file corresponding to this request
     */
    public String getFileName() {
        return mFileName;
    }

    /**
     * Returns the size of the file corresponding to this request.
     *
     * @return the size of the file corresponding to this request
     */
    public long getFileSize() {
        return mFileSize;
    }

    /**
     * Returns the description of the file corresponding to this request.
     *
     * @return the description of the file corresponding to this request
     */
    public String getDnLink() {
        return dnLink;
    }

    /**
     * Returns the encryption of the file corresponding to this request.
     *
     * @return the encryption of the file corresponding to this request
     */
    public int getEncryptionType() {
        return mEncryption;
    }

    // ********************************************************************************************//
    // Routines supporting HTTP File Download

    /**
     * Method fired when the HttpFileDownload message is received.
     */
    public void initHttpFileDownload() {
        if (previousDownloads.contains(dnLink))
            return;

        // queryFileSize will also trigger onReceived; just ignore
        if (dnListener == null) {
            dnListener = new DownloadListener();
        }
        if (mFileSize == -1) {
            mFileSize = queryFileSize();
        }
        // Timber.d("Download receiver registered %s: file size: %s", downloadReceiver, mFileSize);
    }

    /**
     * Query the http uploaded file size for auto download.
     */
    private long queryFileSize() {
        mFileSize = -1;
        dnSession = new DownloadSession(mContext, mUri);
        DownloadSession.DownloadInfo dnInfo = dnSession.query();

        // allow loop for 3 seconds for slow server. Server may return size == 0 ?
        int wait = 3;
        while ((wait-- > 0) && (mFileSize <= 0)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Timber.w("Download Manager query file size exception: %s", e.getMessage());
                return -1;
            }
        }

        mFileSize = dnInfo.getTotalBytes();
        // Timber.d("Download Manager file size query id: %s %s (%s)", id, mFileSize, wait);
        return mFileSize;
    }

    /**
     * Schedules media file download.
     *
     * @param xferFile The saved filename on download completed.
     */
    public void download(File xferFile) {
        mXferFile = xferFile;

        try {
            String dir = FileBackend.getaTalkStore(FileBackend.TMP, true).toString();
            DownloadConfig config = new DownloadConfig.Builder(mContext, mUri)
                    .setPath(dir, mFileName)
                    .build();
            dnSession = new DownloadSession(mContext, config);
            dnSession.addListener(new DownloadListener());
            long jobId = dnSession.start();

            if (jobId > 0) {
                previousDownloads.put(jobId, dnLink);

                mFileSize = dnSession.query().getTotalBytes();
                // Timber.d("Download Manager HttpFileDownload Size: %s %s", mFileSize, previousDownloads.toString());
                fireStatusChangeEvent(FileTransferStatusChangeEvent.IN_PROGRESS, null);

                // Send a fake progressChangeEvent to show progressBar
                fireProgressChangeEvent(System.currentTimeMillis(), 100);
            }
        } catch (SecurityException e) {
            aTalkApp.showToastMessage(e.getMessage());
        } catch (Exception e) {
            aTalkApp.showToastMessage(ResourceTable.String_file_does_not_exist);
        }
    }

    private class DownloadListener implements IDownloadListener {
        public void onCompleted() {
            // Fetching the download id received with the broadcast and
            // if the received broadcast is for our enqueued download by matching download id
            long lastDownloadId = dnSession.query().getDownloadId();

            // Just ignore all unrelated download JobId.
            if (previousDownloads.containsKey(lastDownloadId)) {
                dnSession.attach(lastDownloadId);
                DownloadSession.DownloadInfo dnInfo = dnSession.query();
                int lastJobStatus = dnInfo.getStatus();
                // Timber.d("Download receiver %s (%s): %s", lastDownloadId, previousDownloads, lastJobStatus);

                if (lastJobStatus == DownloadSession.SESSION_SUCCESSFUL) {
                    String dnLink = previousDownloads.get(lastDownloadId);

                    Uri fileUri = dnInfo.getPath();
                    File inFile = new File(FilePathHelper.getFilePath(mContext, fileUri));

                    // update fileSize for progress bar update, in case it is still not updated by download Manager
                    mFileSize = inFile.length();
                    if (inFile.exists()) {
                        // OMEMO media file sharing - need to decrypt file content.
                        if ((dnLink != null) && dnLink.matches("^aesgcm:.*")) {
                            try {
                                AesgcmUrl aesgcmUrl = new AesgcmUrl(dnLink);
                                Cipher decryptCipher = aesgcmUrl.getDecryptionCipher();

                                FileInputStream fis = new FileInputStream(inFile);
                                FileOutputStream outputStream = new FileOutputStream(mXferFile);
                                CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, decryptCipher);

                                int count;
                                byte[] buffer = new byte[4096];
                                while ((count = fis.read(buffer)) != -1) {
                                    cipherOutputStream.write(buffer, 0, count);
                                }

                                fis.close();
                                outputStream.flush();
                                cipherOutputStream.close();
                                // inFile.delete();

                                fireStatusChangeEvent(FileTransferStatusChangeEvent.COMPLETED, null);
                            } catch (Exception e) {
                                fireStatusChangeEvent(FileTransferStatusChangeEvent.FAILED,
                                        "Failed to decrypt OMEMO media file: " + inFile);
                            }
                        }
                        else {
                            // Plain media file sharing; rename will move the infile to outfile dir.
                            if (inFile.renameTo(mXferFile)) {
                                fireStatusChangeEvent(FileTransferStatusChangeEvent.COMPLETED, null);
                            }
                        }

                        // Timber.d("Downloaded fileSize: %s (%s)", outFile.length(), fileSize);
                        previousDownloads.remove(lastDownloadId);
                        // Remove lastDownloadId from downloadManager record and delete the tmp file
                        dnSession.remove();
                    }
                }
                doCleanup();
            }

            if (dnListener != null) {
                dnSession.removeListener(dnListener);
                dnListener = null;
            }
        }

        public void onProgress(long receivedSize, long totalSize) {
            if (System.currentTimeMillis() >= lastUpdateTime) {
                lastUpdateTime += PROGRESS_DELAY;
                waitTime--;
            }
            if (waitTime < 0) {
                Timber.d("Downloaded fileSize (failed): %s (%s)", receivedSize, mFileSize);
                fireStatusChangeEvent(FileTransferStatusChangeEvent.FAILED, null);
                onFailed(-1);
                return;
            }

            if (receivedSize > previousProgress) {
                waitTime = MAX_IDLE_TIME;
                previousProgress = receivedSize;
                fireProgressChangeEvent(System.currentTimeMillis(), receivedSize);
            }
        }

        public void onFailed(int errorCode) {
            fireStatusChangeEvent(FileTransferStatusChangeEvent.FAILED, dnLink);
            dnSession.remove();
        }

        public void onRemoved() {
            dnSession.remove();
        }
    }

    /**
     * Get the jobId for the given dnLink
     *
     * @param dnLink previously download link
     *
     * @return jobId for the dnLink if available else -1
     */
    private long getJobId(String dnLink) {
        for (Map.Entry<Long, String> entry : previousDownloads.entrySet()) {
            if (entry.getValue().equals(dnLink)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    /**
     * Perform cleanup at end of http file transfer process: passed, failed or cancel.
     */
    private void doCleanup() {
        long jobId = getJobId(dnLink);
        if (jobId != -1) {
            previousDownloads.remove(jobId);
            dnSession.attach(jobId);

            // Unregister the HttpDownload transfer downloadReceiver.
            // Receiver not registered exception - may occur if window is refreshed while download is in progress?
            if (dnListener != null) {
                try {
                    dnSession.removeListener(dnListener);
                } catch (IllegalArgumentException ie) {
                    Timber.w("Unregister download receiver exception: %s", ie.getMessage());
                }
                dnListener = null;
            }
            // Timber.d("Download Manager for JobId: %s; File: %s (status: %s)", jobId, dnLink, status);
            dnSession.remove();
        }
    }
}
