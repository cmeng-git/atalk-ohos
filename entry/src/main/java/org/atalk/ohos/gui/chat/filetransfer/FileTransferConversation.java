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
package org.atalk.ohos.gui.chat.filetransfer;

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.Image;
import ohos.agp.components.Image.ScaleMode;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Slider;
import ohos.agp.components.element.FrameAnimationElement;
import ohos.agp.utils.Color;
import ohos.event.commonevent.CommonEventData;
import ohos.event.commonevent.CommonEventManager;
import ohos.event.commonevent.CommonEventSubscribeInfo;
import ohos.event.commonevent.CommonEventSubscriber;
import ohos.event.commonevent.MatchingSkills;
import ohos.media.image.PixelMap;
import ohos.media.image.common.Size;
import ohos.rpc.RemoteException;
import ohos.utils.net.Uri;

import net.java.sip.communicator.service.filehistory.FileRecord;
import net.java.sip.communicator.service.protocol.FileTransfer;
import net.java.sip.communicator.service.protocol.IMessage;
import net.java.sip.communicator.service.protocol.event.FileTransferProgressEvent;
import net.java.sip.communicator.service.protocol.event.FileTransferProgressListener;
import net.java.sip.communicator.service.protocol.event.FileTransferStatusChangeEvent;
import net.java.sip.communicator.util.ByteFormat;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.GuiUtils;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.MyGlideApp;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.chat.ChatAbility;
import org.atalk.ohos.gui.chat.ChatSlice;
import org.atalk.ohos.plugin.audioservice.AudioBgService;
import org.atalk.ohos.util.AppImageUtil;
import org.atalk.ohos.util.LogUtil;
import org.atalk.persistance.FileBackend;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.httpfileupload.UploadProgressListener;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import timber.log.Timber;

import static org.atalk.ohos.BaseAbility.uiHandler;
import static org.atalk.persistance.FileBackend.getMimeType;

/**
 * The <code>FileTransferConversationComponent</code> is the parent of all
 * file conversation components - for incoming, outgoing and history file transfers.
 * <p>
 * The smack reply timer is extended to 10 sec in file sharing info exchanges e.g. IBB takes > 5sec
 *
 * @author Eng Chong Meng
 */
public abstract class FileTransferConversation extends BaseSlice
        implements Component.ClickedListener, Component.LongClickedListener, FileTransferProgressListener,
        UploadProgressListener, Slider.ValueChangedListener {
    private static final String TAG = FileTransferConversation.class.getSimpleName();
    /**
     * Image default width / height.
     */
    private static final int IMAGE_WIDTH = 64;
    private static final int IMAGE_HEIGHT = 64;

    /**
     * The xfer file full path for saving the received file.
     */
    protected File mXferFile;

    protected Uri mUri;

    protected String mDate;

    /**
     * The file transfer.
     */
    protected FileTransfer mFileTransfer = null;

    /**
     * The size of the file to be transferred.
     */
    protected long mTransferFileSize = 0;

    /**
     * The time of the last fileTransfer update.
     */
    private long mLastTimestamp = -1;

    /**
     * The number of bytes last transferred.
     */
    private long mLastTransferredBytes = 0;

    /**
     * The last calculated progress speed.
     */
    private long mTransferSpeedAverage = 0;

    /**
     * The last estimated time.
     */
    private long mEstimatedTimeLeft = -1;

    /**
     * The state of a player where playback is stopped
     */
    private static final int STATE_STOP = 0;
    /**
     * The state of a player when it's created
     */
    private static final int STATE_IDLE = 1;
    /**
     * The state of a player where playback is paused
     */
    private static final int STATE_PAUSE = 2;
    /**
     * The state of a player that is actively playing
     */
    private static final int STATE_PLAY = 3;

    private static final Map<Uri, CommonEventSubscriber> esRegisters = new HashMap<>();
    private AudioCommonEventSubscriber mEventSubscriber;

    private int playerState = STATE_STOP;

    private FrameAnimationElement playStateAnim;

    private boolean isMediaAudio = false;
    private String mimeType = null;

    private final String mDir;

    private boolean isSeeking = false;
    private int positionSeek;

    /**
     * The file transfer index/position of the message in chatListAdapter
     */
    protected int msgViewId = 0;

    /**
     * The message Uuid  uniquely identify the record in the message database
     */
    protected String msgUuid;

    /*
     * mEntityJid can be Contact or ChatRoom
     */
    protected Object mEntityJid;

    /*
     * Transfer file encryption type
     */
    protected int mEncryption = IMessage.ENCRYPTION_NONE;

    protected ChatSlice mChatSlice;
    protected ChatAbility mChatAbility;
    protected XMPPConnection mConnection;

    protected ChatSlice.MessageViewHolder messageViewHolder;

    protected FileTransferConversation(ChatSlice cPanel, String dir) {
        mChatSlice = cPanel;
        mChatAbility = (ChatAbility) cPanel.getAbility();
        mConnection = cPanel.getChatPanel().getProtocolProvider().getConnection();
        mDir = dir;
    }

    protected Component inflateViewForFileTransfer(LayoutScatter inflater, ChatSlice.MessageViewHolder msgViewHolder,
            ComponentContainer container, boolean init) {
        this.messageViewHolder = msgViewHolder;
        Component convertView = null;

        if (init) {
            if (FileRecord.IN.equals(mDir))
                convertView = inflater.parse(ResourceTable.Layout_chat_file_transfer_in_row, container, false);
            else
                convertView = inflater.parse(ResourceTable.Layout_chat_file_transfer_out_row, container, false);

            messageViewHolder.fileIcon = convertView.findComponentById(ResourceTable.Id_button_file);
            messageViewHolder.stickerView = convertView.findComponentById(ResourceTable.Id_sticker);

            messageViewHolder.playerView = convertView.findComponentById(ResourceTable.Id_playerView);
            messageViewHolder.fileAudio = convertView.findComponentById(ResourceTable.Id_filename_audio);
            messageViewHolder.playbackPlay = convertView.findComponentById(ResourceTable.Id_playback_play);
            messageViewHolder.playbackPosition = convertView.findComponentById(ResourceTable.Id_playback_position);
            messageViewHolder.playbackDuration = convertView.findComponentById(ResourceTable.Id_playback_duration);
            messageViewHolder.playbackSeekBar = convertView.findComponentById(ResourceTable.Id_playback_seekbar);

            messageViewHolder.fileLabel = convertView.findComponentById(ResourceTable.Id_filexferFileNameView);
            messageViewHolder.fileStatus = convertView.findComponentById(ResourceTable.Id_filexferStatusView);
            messageViewHolder.fileXferError = convertView.findComponentById(ResourceTable.Id_errorView);
            messageViewHolder.encStateView = convertView.findComponentById(ResourceTable.Id_encFileStateView);

            messageViewHolder.timeView = convertView.findComponentById(ResourceTable.Id_xferTimeView);
            messageViewHolder.fileXferSpeed = convertView.findComponentById(ResourceTable.Id_file_progressSpeed);
            messageViewHolder.estTimeRemain = convertView.findComponentById(ResourceTable.Id_file_estTime);
            messageViewHolder.progressBar = convertView.findComponentById(ResourceTable.Id_file_progressbar);

            messageViewHolder.cancelButton = convertView.findComponentById(ResourceTable.Id_buttonCancel);
            messageViewHolder.retryButton = convertView.findComponentById(ResourceTable.Id_button_retry);
            messageViewHolder.acceptButton = convertView.findComponentById(ResourceTable.Id_button_accept);
            messageViewHolder.declineButton = convertView.findComponentById(ResourceTable.Id_button_decline);
        }

        hideProgressRelatedComponents();

        // set to viewHolder default state
        messageViewHolder.playerView.setVisibility(Component.HIDE);
        messageViewHolder.stickerView.setVisibility(Component.HIDE);
        messageViewHolder.fileLabel.setVisibility(Component.VISIBLE);

        messageViewHolder.playbackSeekBar.setValueChangedListener(this);
        messageViewHolder.cancelButton.setClickedListener(this);
        messageViewHolder.stickerView.setClickedListener(this);

        messageViewHolder.playbackPlay.setClickedListener(this);
        messageViewHolder.playbackPlay.setLongClickedListener(this);

        playStateAnim = new FrameAnimationElement(getContext(), ResourceTable.Graphic_ic_player_playing);
        playStateAnim.setOneShot(false);
        messageViewHolder.playbackPlay.setBackground(playStateAnim);

        messageViewHolder.fileStatus.setTextColor(Color.BLACK);
        return convertView;
    }

    /**
     * A common routine to update the file transfer view component states
     *
     * @param status FileTransferStatusChangeEvent status
     * @param statusText the status text for update
     */
    protected void updateXferFileViewState(int status, final String statusText) {
        messageViewHolder.acceptButton.setVisibility(Component.HIDE);
        messageViewHolder.declineButton.setVisibility(Component.HIDE);
        messageViewHolder.cancelButton.setVisibility(Component.HIDE);
        messageViewHolder.retryButton.setVisibility(Component.HIDE);
        messageViewHolder.fileStatus.setTextColor(Color.BLACK);

        switch (status) {
            // Only allow user to cancel while in active data stream transferring; both legacy si and JFT cannot
            // support transfer cancel during protocol negotiation.
            case FileTransferStatusChangeEvent.PREPARING:
                // Preserve the cancel button view height, avoid being partially hidden by android when it is enabled
                messageViewHolder.cancelButton.setVisibility(Component.HIDE);
                break;

            case FileTransferStatusChangeEvent.WAITING:
                if (this instanceof FileSendConversation) {
                    messageViewHolder.cancelButton.setVisibility(Component.VISIBLE);
                }
                else {
                    messageViewHolder.acceptButton.setVisibility(Component.VISIBLE);
                    messageViewHolder.declineButton.setVisibility(Component.VISIBLE);
                }
                break;

            case FileTransferStatusChangeEvent.IN_PROGRESS:
                messageViewHolder.cancelButton.setVisibility(Component.VISIBLE);
                break;

            case FileTransferStatusChangeEvent.COMPLETED:
                if (mXferFile == null)
                    break;

                // Update file label and image for incoming file
                if (FileRecord.IN.equals(mDir)) {
                    updateFileViewInfo(mXferFile, false);
                }

                // Fix unknown HttpFileDownload file size or final last transferredBytes update not receive in case.
                // Show full for progressBar on local received mXferFile completed.
                long fileSize = mXferFile.length();
                onUploadProgress(fileSize, fileSize);
                break;

            case FileTransferStatusChangeEvent.FAILED:
            case FileTransferStatusChangeEvent.CANCELED:
                // Allow user retries only if sender cancels the file transfer
                if (FileRecord.OUT.equals(mDir)) {
                    messageViewHolder.retryButton.setVisibility(Component.VISIBLE);
                    messageViewHolder.cancelButton.setVisibility(Component.VISIBLE);
                } // fall through

            case FileTransferStatusChangeEvent.DECLINED: // user reject the incoming file xfer
                messageViewHolder.fileStatus.setTextColor(Color.RED);
                break;
        }

        if (!TextUtils.isEmpty(statusText)) {
            messageViewHolder.fileStatus.setText(statusText);
        }
        messageViewHolder.timeView.setText(mDate);
    }

    /**
     * Check for auto-download only if the file size is not zero
     *
     * @param fileSize transfer file size
     */
    protected boolean checkAutoAccept(long fileSize) {
        if (fileSize > 0 && ConfigurationUtils.isAutoAcceptFile(fileSize)) {
            uiHandler.postTask(() -> {
                messageViewHolder.acceptButton.simulateClick();
            }, 500);
            return true;
        }
        return false;
    }

    /**
     * Shows the given error message in the error area of this component.
     *
     * @param resId the message to show
     */
    protected void showErrorMessage(int resId) {
        String message = aTalkApp.getResString(resId);
        messageViewHolder.fileXferError.setText(message);
        messageViewHolder.fileXferError.setVisibility(Component.VISIBLE);
    }

    /**
     * Shows file thumbnail.
     *
     * @param thumbnail the thumbnail to show
     */
    public void showThumbnail(byte[] thumbnail) {
        runOnUiThread(() -> {
            if (thumbnail != null && thumbnail.length > 0) {
                PixelMap thumbnailIcon = AppImageUtil.pixelMapFromBytes(thumbnail);
                Size size = thumbnailIcon.getImageInfo().size;
                int mWidth = size.width;
                int mHeight = size.height;

                if (mWidth > IMAGE_WIDTH || mHeight > IMAGE_HEIGHT) {
                    messageViewHolder.fileIcon.setScaleMode(ScaleMode.INSIDE);
                }
                else {
                    messageViewHolder.fileIcon.setScaleMode(ScaleMode.CENTER);
                }
                messageViewHolder.fileIcon.setPixelMap(thumbnailIcon);

                // Update stickerView drawable only if is null
                if (messageViewHolder.stickerView.getPixelMap() == null) {
                    messageViewHolder.stickerView.setVisibility(Component.VISIBLE);
                    PixelMap scaledThumbnail = AppImageUtil.scaledPixelMapFromBytes(thumbnail, mWidth * 2, mHeight * 2);
                    messageViewHolder.stickerView.setPixelMap(scaledThumbnail);
                }
            }
        });
    }

    /**
     * Initialize all the local parameters i.e. mXferFile, mUri, mimeType and isMediaAudio
     * Update the file transfer view display info in thumbnail or audio player UI accordingly.
     *
     * @param file the file that has been downloaded/received or sent
     * @param isHistory true if the view file is history, so show small image size
     */
    protected void updateFileViewInfo(File file, boolean isHistory) {
        if (file != null)
            messageViewHolder.fileLabel.setText(getFileLabel(file));

        // File length = 0 will cause Glade to throw errors
        if ((file == null) || !file.exists() || file.length() == 0)
            return;

        mXferFile = file;
        mUri = FileBackend.getUriForFile(mChatAbility, file);
        mimeType = checkMimeType(file);
        isMediaAudio = ((mimeType != null) && (mimeType.contains("audio") || mimeType.contains("3gp")));

        if (isMediaAudio && playerInit()) {
            messageViewHolder.playerView.setVisibility(Component.VISIBLE);
            messageViewHolder.stickerView.setVisibility(Component.HIDE);
            messageViewHolder.fileLabel.setVisibility(Component.HIDE);
            messageViewHolder.fileAudio.setText(getFileLabel(file));
        }
        else {
            messageViewHolder.playerView.setVisibility(Component.HIDE);
            messageViewHolder.stickerView.setVisibility(Component.VISIBLE);
            messageViewHolder.fileLabel.setVisibility(Component.VISIBLE);
            updateImageView(isHistory);
        }

        final String toolTip = aTalkApp.getResString(ResourceTable.String_open_file_vai_image);
        // messageViewHolder.fileIcon.setContentDescription(toolTip);
        messageViewHolder.fileIcon.setClickedListener(this);

        messageViewHolder.fileIcon.setLongClickedListener(v -> {
            aTalkApp.showToastMessage(toolTip);
        });
    }

    /**
     * Load the received media file image into the stickerView.
     * Ensure the loaded image view is fully visible after resource is ready.
     */
    protected void updateImageView(boolean isHistory) {
        if (isHistory || FileRecord.OUT.equals(mDir)) {
            MyGlideApp.loadImage(messageViewHolder.stickerView, mXferFile, isHistory);
            return;
        }

        Glide.with(aTalkApp.getInstance())
                .asDrawable()
                .load(Uri.getUriFromFile(mXferFile))
                .override(1280, 608)
                .into(new CustomViewTarget<Image, Drawable>(messageViewHolder.stickerView) {
                          @Override
                          public void onResourceReady(Drawable resource, @Nullable Transition<? super Drawable> transition) {
                              messageViewHolder.stickerView.setImageDrawable(resource);
                              if (resource instanceof GifDrawable) {
                                  ((GifDrawable) resource).start();
                              }
                              mChatSlice.scrollToBottom();
                          }

                          @Override
                          protected void onResourceCleared(@Nullable Drawable placeholder) {
                              Timber.d("Glide onResourceCleared received!!!");
                          }

                          @Override
                          public void onLoadFailed(@Nullable Drawable errorDrawable) {
                              messageViewHolder.stickerView.setImageResource(ResourceTable.Media_ic_file_open);
                              mChatSlice.scrollToBottom();
                          }
                      }
                );
    }

    /**
     * Sets the file transfer and addProgressListener().
     * Note: HttpFileUpload adds ProgressListener in httpFileUploadManager.uploadFile()
     *
     * @param fileTransfer the file transfer
     * @param transferFileSize the size of the transferred file Running in thread, not UI here
     */
    protected void setFileTransfer(FileTransfer fileTransfer, long transferFileSize) {
        mFileTransfer = fileTransfer;
        mTransferFileSize = transferFileSize;
        fileTransfer.addProgressListener(this);
    }

    /**
     * Hides all progress related components.
     */
    protected void hideProgressRelatedComponents() {
        messageViewHolder.progressBar.setVisibility(Component.HIDE);
        messageViewHolder.fileXferSpeed.setVisibility(Component.HIDE);
        messageViewHolder.estTimeRemain.setVisibility(Component.HIDE);
    }

    /**
     * Remove file transfer progress listener
     */
    protected void removeProgressListener() {
        mFileTransfer.removeProgressListener(this);
    }

    /**
     * Updates progress bar progress line every time a progress event has been received file transport
     * Note: total size of event.getProgress() is always lag behind event.getFileTransfer().getTransferredBytes();
     *
     * @param event the <code>FileTransferProgressEvent</code> that notifies us
     */
    @Override
    public void progressChanged(final FileTransferProgressEvent event) {
        long transferredBytes = event.getFileTransfer().getTransferredBytes();
        long progressTimestamp = event.getTimestamp();

        updateProgress(transferredBytes, progressTimestamp);
    }

    /**
     * Callback for displaying http file upload, and file transfer progress status.
     *
     * @param uploadedBytes the number of bytes uploaded at the moment
     * @param totalBytes the total number of bytes to be uploaded
     */
    @Override
    public void onUploadProgress(long uploadedBytes, long totalBytes) {
        updateProgress(uploadedBytes, System.currentTimeMillis());
    }

    protected void updateProgress(long transferredBytes, long progressTimestamp) {
        long SMOOTHING_FACTOR = 100;

        // before file transfer start is -1
        if (transferredBytes < 0)
            return;

        final String bytesString = ByteFormat.format(transferredBytes);
        long byteTransferDelta = (transferredBytes == 0) ? 0 : (transferredBytes - mLastTransferredBytes);

        // Calculate running average transfer speed in bytes/sec and time left, with the given SMOOTHING_FACTOR
        if (mLastTimestamp > 0) {
            long timeElapsed = progressTimestamp - mLastTimestamp;
            long transferSpeedCurrent = (timeElapsed > 0) ? (byteTransferDelta * 1000) / timeElapsed : 0;
            if (mTransferSpeedAverage != 0) {
                mTransferSpeedAverage = (transferSpeedCurrent + (SMOOTHING_FACTOR - 1) * mTransferSpeedAverage) / SMOOTHING_FACTOR;
            }
            else {
                mTransferSpeedAverage = transferSpeedCurrent;
            }
        }
        else {
            mEstimatedTimeLeft = -1;
        }

        // Calculate  running average time left in sec
        if (mTransferSpeedAverage > 0)
            mEstimatedTimeLeft = (mTransferFileSize - transferredBytes) / mTransferSpeedAverage;

        mLastTimestamp = progressTimestamp;
        mLastTransferredBytes = transferredBytes;

        runOnUiThread(() -> {
            // Need to do it here as it was found that Http File Upload completed before the progress Bar is even visible
            if (!(Component.VISIBLE == messageViewHolder.progressBar.getVisibility())) {
                messageViewHolder.progressBar.setVisibility(Component.VISIBLE);
                mChatSlice.scrollToBottom();
            }
            // In case transfer file size is unknown in HttpFileDownload
            if (mTransferFileSize <= 0)
                messageViewHolder.progressBar.setMaxValue((int) mTransferFileSize);

            // Note: progress bar can only handle int size (4-bytes: 2,147,483, 647);
            messageViewHolder.progressBar.setProgressValue((int) transferredBytes);

            if (mTransferSpeedAverage > 0) {
                messageViewHolder.fileXferSpeed.setVisibility(Component.VISIBLE);
                messageViewHolder.fileXferSpeed.setText(
                        aTalkApp.getResString(ResourceTable.String_speed_info, ByteFormat.format(mTransferSpeedAverage), bytesString));
            }

            if (transferredBytes >= mTransferFileSize) {
                messageViewHolder.estTimeRemain.setVisibility(Component.HIDE);
            }
            else if (mEstimatedTimeLeft > 0) {
                messageViewHolder.estTimeRemain.setVisibility(Component.VISIBLE);
                messageViewHolder.estTimeRemain.setText(aTalkApp.getResString(ResourceTable.String_estimated_time_,
                        GuiUtils.formatSeconds(mEstimatedTimeLeft * 1000)));
            }
        });
    }

    /**
     * Returns a string showing information for the given file.
     *
     * @param file the file
     *
     * @return the name and size of the given file
     */
    protected String getFileLabel(File file) {
        if ((file != null) && file.exists()) {
            String fileName = file.getName();
            long fileSize = file.length();
            return getFileLabel(fileName, fileSize);
        }
        return (file == null) ? "" : file.getName();
    }

    /**
     * Returns the string, showing information for the given file.
     *
     * @param fileName the name of the file
     * @param fileSize the size of the file
     *
     * @return the name of the given file
     */
    protected String getFileLabel(String fileName, long fileSize) {
        String text = ByteFormat.format(fileSize);
        return fileName + " (" + text + ")";
    }

    /**
     * Returns the label to show on the progress bar.
     *
     * @param bytesString the bytes that have been transferred
     *
     * @return the label to show on the progress bar
     */
    protected abstract String getProgressLabel(long bytesString);

    /**
     * updateStatus includes UI view and DB status update for class extension implementation.
     *
     * @param status current file transfer status.
     * @param reason may be null or internal generated based on status.
     */
    protected abstract void updateStatus(final int status, final String reason);

    /**
     * Init some of the file transfer parameters. Mainly call by sendFile and File History.
     *
     * @param status File transfer send status
     * @param jid Contact or ChatRoom for Http file upload service
     * @param encryption File encryption type
     * @param reason Contact or ChatRoom for Http file upload service
     */
    public void setStatus(final int status, Object jid, int encryption, String reason) {
        mEntityJid = jid;
        mEncryption = encryption;
        // Must execute in UiThread to Update UI information
        runOnUiThread(() -> {
            setEncryptionState(mEncryption);
            updateStatus(status, reason);
        });
    }

    /**
     * Set the file encryption status icon.
     * Access directly by file receive constructor; sendFile via setStatus().
     *
     * @param encryption the encryption
     */
    protected void setEncryptionState(int encryption) {
        if (IMessage.ENCRYPTION_OMEMO == encryption)
            messageViewHolder.encStateView.setPixelMap(ResourceTable.Media_encryption_omemo);
        else
            messageViewHolder.encStateView.setPixelMap(ResourceTable.Media_encryption_none);
    }

    /**
     * Get the current status fo the file transfer
     *
     * @return the current status of the file transfer
     */
    protected int getXferStatus() {
        return mChatSlice.getChatListAdapter().getXferStatus(msgViewId);
    }

    /**
     * @return the fileTransfer file
     */
    public File getXferFile() {
        return mXferFile;
    }

    /**
     * The message Uuid uniquely identify the record in the message database
     *
     * @return the uid for the requested message to send file
     */
    public String getMessageUuid() {
        return msgUuid;
    }

    /**
     * Check if File Transferred has endded.
     *
     * @param status current file transfer status
     *
     * @return true is file transfer process has already completed.
     */
    protected boolean isFileTransferEnd(int status) {
        return (status == FileTransferStatusChangeEvent.COMPLETED
                || status == FileTransferStatusChangeEvent.CANCELED
                || status == FileTransferStatusChangeEvent.FAILED
                || status == FileTransferStatusChangeEvent.DECLINED);
    }

    /**
     * Handles buttons click action events.
     */
    @Override
    public void onClick(Component view) {
        switch (view.getId()) {
            case ResourceTable.Id_button_file:
            case ResourceTable.Id_sticker:
                if (mChatAbility != null)
                    mChatAbility.openDownloadable(mXferFile, view);
                break;

            case ResourceTable.Id_playback_play:
                playStart();
                break;

            case ResourceTable.Id_buttonCancel:
                messageViewHolder.retryButton.setVisibility(Component.HIDE);
                messageViewHolder.cancelButton.setVisibility(Component.HIDE);
                // Let file transport event call back to handle updateStatus() if mFileTransfer not null.
                if (mFileTransfer != null) {
                    mFileTransfer.cancel();
                }
                else {
                    updateStatus(FileTransferStatusChangeEvent.CANCELED, null);
                }
                break;
        }
    }

    /**
     * Handles buttons long press action events
     * mainly use to stop and release player
     */
    @Override
    public void onLongClicked(Component v) {
        if (v.getId() == ResourceTable.Id_playback_play) {
            playerStop();
        }
    }

    /**
     * Initialize the broadcast receiver for the media player (uri).
     * Keep the active bc receiver instance in bcRegisters list to ensure only one bc is registered
     *
     * @param file the media file
     *
     * @return true if init is successful
     */
    private boolean eventReceiverInit(File file) {
        String mimeType = checkMimeType(file);
        if ((mimeType != null) && (mimeType.contains("audio") || mimeType.contains("3gp"))) {
            if (playerState == STATE_STOP) {
                try {
                    CommonEventSubscriber subscriber;
                    if ((subscriber = esRegisters.get(mUri)) != null) {
                        CommonEventManager.unsubscribeCommonEvent(subscriber);
                    }
                    subscribe();
                    esRegisters.put(mUri, mEventSubscriber);
                } catch (RemoteException e) {
                    Timber.w("%s", e.getMessage());
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Get the active media player status or just media info for the view display;
     * update the view holder content via Broadcast receiver
     */
    private boolean playerInit() {
        if (isMediaAudio) {
            if (playerState == STATE_STOP) {
                if (!eventReceiverInit(mXferFile))
                    return false;

                Operation operation = new Intent.OperationBuilder()
                        .withDeviceId("")
                        .withBundleName(getBundleName())
                        .withAbilityName(AudioBgService.class)
                        .build();

                Intent playerIntent = new Intent();
                playerIntent.setAction(AudioBgService.ACTION_PLAYER_INIT)
                        .setUriAndType(mUri, mimeType)
                        .setOperation(operation);
                mChatAbility.startAbility(playerIntent);
                // connectAbility(playerIntent, new ServiceConnectCallback())
            }
            return true;
        }
        return false;
    }

    private void startService(String action, int... extra) {
        Operation operation = new Intent.OperationBuilder()
                .withDeviceId("")
                .withBundleName(getBundleName())
                .withAbilityName(AudioBgService.class)
                .build();

        Intent playerIntent = new Intent();
        playerIntent.setAction(action)
                .setUriAndType(mUri, mimeType)
                .setOperation(operation);
        if (extra.length != 0)
            playerIntent.setParam(AudioBgService.PLAYBACK_POSITION, extra[0]);

        mChatAbility.startAbility(playerIntent);
    }

    /**
     * Stop the current active media player playback
     */
    private void playerStop() {
        if (isMediaAudio) {
            if ((playerState == STATE_PAUSE) || (playerState == STATE_PLAY)) {
                startService(AudioBgService.ACTION_PLAYER_STOP);
            }
        }
    }

    /**
     * Toggle audio file playback states:
     * STOP -> PLAY -> PAUSE -> PLAY;
     * long press play button to STOP
     * <p>
     * Proceed to open the file for VIEW if this is not an audio file
     */
    private void playStart() {
        if (isMediaAudio) {
            if (playerState == STATE_PLAY) {
                startService(AudioBgService.ACTION_PLAYER_PAUSE);
            }
            else if (playerState == STATE_STOP) {
                if (!eventReceiverInit(mXferFile)) {
                    return;
                }
            }
            else {
                startService(AudioBgService.ACTION_PLAYER_START);
            }
        }
        else {
//            Intent intent = new Intent(mChatAbility, AudioBgService.class);
//            intent = new Intent(Intent.ACTION_VIEW);
//            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            intent.setDataAndType(mUri, mimeType);
//
//            PackageManager manager = mChatAbility.getPackageManager();
//            List<ResolveInfo> info = manager.queryIntentActivities(intent, 0);
//            if (info.isEmpty() == 0) {
//                intent.setDataAndType(mUri, "*/*");
//            }
//            try {
//                mChatAbility.startAbility(intent);
//            } catch (AbilityNotFoundException e) {
//                aTalkApp.showToastMessage(ResourceTable.String_file_open_no_application);
//            }
            Intent intent = new Intent();
            Operation operation = new Intent.OperationBuilder()
                    .withBundleName(mChatAbility.getBundleName())
                    .withAbilityName(AudioBgService.class)
                    .withAction(Intent.ACTION_PLAY)
                    .withUri(mUri)
                    .build();
            intent.setOperation(operation);
            intent.setUriAndType(mUri, "*/*");
            mChatAbility.startAbility(intent, 0);
        }
    }

    /**
     * SeekTo player new start play position
     *
     * @param position seek time position
     */
    private void playerSeek(int position) {
        if (isMediaAudio) {
            if (eventReceiverInit(mXferFile))
                startService(AudioBgService.ACTION_PLAYER_SEEK, position);
        }
    }

    private CommonEventSubscribeInfo getSubscribeInfo() {
        MatchingSkills filter = new MatchingSkills();
        filter.addEvent(AudioBgService.PLAYBACK_STATE);
        filter.addEvent(AudioBgService.PLAYBACK_STATUS);
        return new CommonEventSubscribeInfo(filter);
    }

    private void subscribe() {
        MatchingSkills filter = new MatchingSkills();
        filter.addEvent(AudioBgService.PLAYBACK_STATE);
        filter.addEvent(AudioBgService.PLAYBACK_STATUS);

        CommonEventSubscribeInfo subscribeInfo = new CommonEventSubscribeInfo(filter);
        mEventSubscriber = new AudioCommonEventSubscriber(subscribeInfo);
        try {
            CommonEventManager.subscribeCommonEvent(mEventSubscriber);
        } catch (RemoteException e) {
            LogUtil.error(TAG, "subscribeCommonEvent occur exception. " + e.getMessage());
        }
    }

    public void unSubscribe() {
        try {
            CommonEventManager.unsubscribeCommonEvent(mEventSubscriber);
        } catch (RemoteException e) {
            LogUtil.error(TAG, "Unsubscribe exception: " + e.getMessage());
        }
    }

    /**
     * Media player EventSubscriber to animate and update player view holder info
     */
    private class AudioCommonEventSubscriber extends CommonEventSubscriber {
        AudioCommonEventSubscriber(CommonEventSubscribeInfo info) {
            super(info);
        }

        @Override
        public void onReceiveEvent(CommonEventData eventData) {
            Intent intent = eventData.getIntent();

            // proceed only if it is the playback of the current mUri
            if (!mUri.equals(intent.getSerializableParam(AudioBgService.PLAYBACK_URI)))
                return;

            int position = intent.getIntParam(AudioBgService.PLAYBACK_POSITION, 0);
            int audioDuration = intent.getIntParam(AudioBgService.PLAYBACK_DURATION, 0);

            if ((playerState == STATE_PLAY) && AudioBgService.PLAYBACK_STATUS.equals(intent.getAction())) {
                if (!isSeeking)
                    messageViewHolder.playbackPosition.setText(formatTime(position));
                messageViewHolder.playbackDuration.setText(formatTime(audioDuration - position));
                messageViewHolder.playbackSeekBar.setMaxValue(audioDuration);
                messageViewHolder.playbackSeekBar.setProgressValue(position);

            }
            else if (AudioBgService.PLAYBACK_STATE.equals(intent.getAction())) {
                AudioBgService.PlaybackState playbackState = intent.getSerializableParam(AudioBgService.PLAYBACK_STATE);

                Timber.d("Audio playback state: %s (%s/%s): %s", playbackState, position, audioDuration, mUri.getEncodedPath());
                switch (playbackState) {
                    case init:
                        playerState = STATE_IDLE;
                        messageViewHolder.playbackDuration.setText(formatTime(audioDuration));
                        messageViewHolder.playbackPosition.setText(formatTime(0));
                        messageViewHolder.playbackSeekBar.setMaxValue(audioDuration);
                        messageViewHolder.playbackSeekBar.setProgressValue(0);

                        messageViewHolder.playbackPlay.setPixelMap(ResourceTable.Media_ic_player_stop);
                        playStateAnim.stop();
                        break;

                    case play:
                        playerState = STATE_PLAY;
                        messageViewHolder.playbackSeekBar.setMaxValue(audioDuration);
                        messageViewHolder.playerView.release();

                        messageViewHolder.playbackPlay.setPixelMap(null);
                        playStateAnim.start();
                        break;

                    case stop:
                        playerState = STATE_STOP;
                        esRegisters.remove(mUri);
                        unSubscribe();
                    case pause:
                        if (playerState != STATE_STOP) {
                            playerState = STATE_PAUSE;
                        }
                        messageViewHolder.playbackPosition.setText(formatTime(position));
                        messageViewHolder.playbackDuration.setText(formatTime(audioDuration - position));
                        messageViewHolder.playbackSeekBar.setMaxValue(audioDuration);
                        messageViewHolder.playbackSeekBar.setProgressValue(position);

                        playStateAnim.stop();
                        messageViewHolder.playbackPlay.setPixelMap((playerState == STATE_PAUSE)
                                ? ResourceTable.Media_ic_player_pause : ResourceTable.Media_ic_player_stop);
                        break;
                }
            }
        }
    }

    /**
     * Slider.ValueChangedListener callback interface during multimedia playback:
     * A slider callback that notifies clients when the progress level has been
     * changed. This includes changes that were initiated by the user through a
     * touch gesture or arrow key/trackball as well as changes that were initiated
     * programmatically.
     */
    @Override
    public void onProgressUpdated(Slider seekBar, int progress, boolean fromUser) {
        if (fromUser && (messageViewHolder.playbackSeekBar == seekBar)) {
            positionSeek = progress;
            messageViewHolder.playbackPosition.setText(formatTime(progress));
        }
    }

    @Override
    public void onTouchStart(Slider seekBar) {
        if (messageViewHolder.playbackSeekBar == seekBar) {
            isSeeking = true;
        }
    }

    @Override
    public void onTouchEnd(Slider seekBar) {
        if (messageViewHolder.playbackSeekBar == seekBar) {
            playerSeek(positionSeek);
            isSeeking = false;
        }
    }

    /**
     * Format the given time to mm:ss
     *
     * @param time time is ms
     *
     * @return the formatted time string in mm:ss
     */
    private String formatTime(int time) {
        // int ms = (time % 1000) / 10;
        int seconds = time / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    /**
     * Determine the mimeType of the given file
     *
     * @param file the media file to check
     *
     * @return mimeType or null if undetermined
     */
    private String checkMimeType(File file) {
        if (!file.exists()) {
            // aTalkApp.showToastMessage(ResourceTable.String_service_gui_FILE_DOES_NOT_EXIST);
            return null;
        }

        try {
            Uri uri = FileBackend.getUriForFile(mChatAbility, file);
            String mimeType = getMimeType(mChatAbility, uri);
            if ((mimeType == null) || mimeType.contains("application")) {
                mimeType = "*/*";
            }
            return mimeType;

        } catch (SecurityException e) {
            Timber.i("No permission to access %s: %s", file.getAbsolutePath(), e.getMessage());
            aTalkApp.showToastMessage(ResourceTable.String_file_open_no_permission);
            return null;
        }
    }

    /**
     * Generate the mXferFile full filePath based on the given fileName and mimeType
     *
     * @param fileName the incoming xfer fileName
     * @param mimeType the incoming file mimeType
     */
    protected void setTransferFilePath(String fileName, String mimeType) {
        String downloadPath = FileBackend.MEDIA_DOCUMENT;
        if (fileName.contains("voice-"))
            downloadPath = FileBackend.MEDIA_VOICE_RECEIVE;
        else if (StringUtils.isNotEmpty(mimeType) && !mimeType.startsWith("*")) {
            downloadPath = FileBackend.MEDIA + File.separator + mimeType.split("/")[0];
        }

        File downloadDir = FileBackend.getaTalkStore(downloadPath, true);
        mXferFile = new File(downloadDir, fileName);

        // If a file with the given name already exists, add an index to the file name.
        int index = 0;
        int filenameLength = fileName.lastIndexOf(".");
        if (filenameLength == -1) {
            filenameLength = fileName.length();
        }
        while (mXferFile.exists()) {
            String newFileName = fileName.substring(0, filenameLength) + "-"
                    + ++index + fileName.substring(filenameLength);
            mXferFile = new File(downloadDir, newFileName);
        }
    }
}
