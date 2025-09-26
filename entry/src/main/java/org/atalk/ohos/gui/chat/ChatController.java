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
package org.atalk.ohos.gui.chat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.accessibility.ability.AccessibleAbility;
import ohos.accessibility.ability.SoftKeyBoardController;
import ohos.agp.animation.AnimatorProperty;
import ohos.agp.animation.AnimatorScatter;
import ohos.agp.colors.RgbColor;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.DragEvent;
import ohos.agp.components.Image;
import ohos.agp.components.ListContainer;
import ohos.agp.components.Text;
import ohos.agp.components.element.FrameAnimationElement;
import ohos.agp.components.element.ShapeElement;
import ohos.ai.asr.AsrClient;
import ohos.ai.asr.AsrIntent;
import ohos.ai.asr.AsrListener;
import ohos.app.Context;
import ohos.event.commonevent.CommonEventData;
import ohos.event.commonevent.CommonEventManager;
import ohos.event.commonevent.CommonEventSubscribeInfo;
import ohos.event.commonevent.CommonEventSubscriber;
import ohos.event.commonevent.MatchingSkills;
import ohos.miscservices.inputmethodability.InputMethodAbility;
import ohos.multimodalinput.event.KeyEvent;
import ohos.multimodalinput.event.TouchEvent;
import ohos.rpc.RemoteException;
import ohos.security.SystemPermission;
import ohos.utils.PacMap;
import ohos.utils.net.Uri;
import ohos.utils.system.SystemCapability.Accessibility;

import net.java.sip.communicator.impl.protocol.jabber.CallJabberImpl;
import net.java.sip.communicator.impl.protocol.jabber.CallPeerJabberImpl;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.IMessage;
import net.java.sip.communicator.util.ConfigurationUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.TextUtils;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.call.CallManager;
import org.atalk.ohos.gui.call.JingleMessageCallAbility;
import org.atalk.ohos.gui.call.notification.CallNotificationManager;
import org.atalk.ohos.gui.share.Attachment;
import org.atalk.ohos.gui.share.MediaPreviewProvider;
import org.atalk.ohos.gui.util.ContentEditText;
import org.atalk.ohos.gui.util.HtmlImageGetter;
import org.atalk.ohos.plugin.audioservice.AudioBgService;
import org.atalk.ohos.plugin.audioservice.SoundMeter;
import org.atalk.ohos.util.ComponentUtil;
import org.atalk.ohos.util.LogUtil;
import org.atalk.persistance.FilePathHelper;
import org.jivesoftware.smackx.chatstates.ChatState;

import timber.log.Timber;

import static ohos.ai.asr.util.AsrResultKey.RESULTS_RECOGNITION;

/**
 * Class is used to separate the logic of send message editing process from <code>ChatSlice</code>.
 * It handles last messages correction, editing, sending messages and chat state notifications.
 * It also restores edit state when the chat fragment is scrolled in view again.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class ChatController implements Component.ClickedListener, Component.LongClickedListener,
        Component.OnDragListener, Text.TextObserver, ContentEditText.CommitListener {

    private static final String TAG = ChatController.class.getSimpleName();

    /**
     * Parent activity: ChatAbility pass in from ChatSlice.
     */
    private final Ability mChatAbility;
    /**
     * The chat fragment used by this instance.
     */
    private final ChatSlice mChatSlice;
    private AudioCommonEventSubscriber mEventSubscriber;

    /**
     * Indicates that this controller is attached to the views.
     */
    private boolean isAttached = false;
    /**
     * Correction indicator / cancel button.
     */
    private Component cancelCorrectionBtn;
    /**
     * Send button's Component.
     */
    private Component sendBtn;
    /**
     * media call button's Component.
     */
    private Component callBtn;
    /**
     * Audio recording button.
     */
    private Image audioBtn;
    /**
     * Message <code>TextField</code>.
     */
    private ContentEditText msgEdit;

    /**
     * Message editing area background.
     */
    private Component msgEditBg;

    private ComponentContainer mediaPreview;
    private Image imagePreview;

    private Component chatReplyCancel;
    private Text chatMessageReply;
    private String quotedMessage;

    /**
     * Chat chatPanel used by this controller and its parent chat fragment.
     */
    private ChatPanel chatPanel;
    /**
     * Current Chat Transport associates with this Chat Controller.
     */
    private ChatTransport mChatTransport = null;
    /**
     * Typing state control thread that goes from composing to stopped state.
     */
    private ChatStateControl chatStateCtrlThread;
    /**
     * Current chat state.
     */
    private ChatState mChatState = ChatState.gone;

    /**
     * Indicate whether sending chat state notifications to the contact is allowed:
     * 1. contact must support XEP-0085: Chat State Notifications
     * 2. User enable the chat state notifications sending option
     */
    private boolean allowsChatStateNotifications = false;

    /**
     * Audio recording variables
     */
    private final boolean isAudioAllowed;
    private boolean isRecording;

    private Component msgRecordView;
    private Text mRecordTimer;
    private Text mdBTextView;
    private Image mTrash;
    private SoundMeter mSoundMeter;

    private final Intent recordIntent = new Intent();
    private FrameAnimationElement mTtsAnimate;
    private FrameAnimationElement mTrashAnimate;
    private AnimatorProperty micAnimate, smAnimate, dbTextAnimate, recordAnimate;

    // Constant to detect slide left to cancel audio recording
    private static final int min_distance = 100;
    private float downX;

    private boolean isBlocked = false;

    /**
     * Creates new instance of <code>ChatController</code>.
     *
     * @param activity the parent <code>Ability</code>.
     * @param fragment the parent <code>ChatSlice</code>.
     */
    public ChatController(Ability activity, ChatSlice fragment) {
        mChatAbility = activity;
        mChatSlice = fragment;

        // Do not use aTalk.getInstance, may not have initialized
        isAudioAllowed = aTalk.hasPermission(mChatAbility, false,
                aTalk.PRC_RECORD_AUDIO, SystemPermission.MICROPHONE);
    }

    /**
     * Method called by the <code>ChatSlice</code> when it is displayed to the user and its <code>Component.</code> is created.
     */
    public void onShow() {
        if (!isAttached) {
            isAttached = true;

            // Ensure all view are properly initialized before any action taken on views
            initViews();

            MetaContact metaContact = chatPanel.getMetaContact();
            isBlocked = false;
            if (metaContact != null) {
                isBlocked = metaContact.getDefaultContact().isContactBlock();
            }
            if (isBlocked) {
                msgEdit.setText(ResourceTable.String_contact_blocked);
            }
            else {
                // Restore edited text
                msgEdit.setText(chatPanel.getEditedText());
            }
            msgEdit.setEnabled(!isBlocked);
            sendBtn.setEnabled(!isBlocked);

            // Timber.d("ChatController attached to %s", chatFragment.hashCode());
            msgEdit.setCommitListener(this);
            msgEdit.setFocusable(Component.FOCUS_ENABLE);
            msgEdit.setOnDragListener(this);

            chatMessageReply.setVisibility(Component.HIDE);
            chatReplyCancel.setVisibility(Component.HIDE);
            chatReplyCancel.setClickedListener(this);
            cancelCorrectionBtn.setClickedListener(this);

            sendBtn.setClickedListener(this);
            if (isAudioAllowed) {
                audioBtn.setClickedListener(this);
                audioBtn.setLongClickedListener(this);
                audioBtn.setOnDragListener(this);
            }
            else {
                Timber.w("Audio recording is not allowed - permission denied!");
            }

            callBtn.setClickedListener(this);
            mTrashAnimate = new FrameAnimationElement(mChatAbility, ResourceTable.Graphic_trash_animate);
            mTrash.setBackground(mTrashAnimate);

            AnimatorScatter scatter = AnimatorScatter.getInstance(mChatAbility);
            micAnimate = (AnimatorProperty) scatter.parse(ResourceTable.Animation_blink);

            smAnimate = (AnimatorProperty) scatter.parse(ResourceTable.Animation_zoom_out);
            smAnimate.setDuration(1000);
            smAnimate.setTarget(mSoundMeter);

            dbTextAnimate = (AnimatorProperty) scatter.parse(ResourceTable.Animation_slide_up);
            dbTextAnimate.setDuration(1000);
            dbTextAnimate.setTarget(mdBTextView);

            recordAnimate = (AnimatorProperty) scatter.parse(ResourceTable.Animation_slide_up);
            recordAnimate.setDuration(1000);
            recordAnimate.setTarget(mRecordTimer);

            updateCorrectionState();
            initChatController();
            updateSendModeState();
        }
    }

    /**
     * Initialize all the view
     */
    private void initViews() {
        chatPanel = mChatSlice.getChatPanel();

        // Gets message edit view
        msgEdit = mChatAbility.findComponentById(ResourceTable.Id_chatWriteText);

        // Message typing area background
        msgEditBg = mChatAbility.findComponentById(ResourceTable.Id_chatTypingArea);

        // Gets the cancel correction button and hooks on click action
        cancelCorrectionBtn = mChatAbility.findComponentById(ResourceTable.Id_cancelCorrectionBtn);

        // Quoted reply message view
        chatMessageReply = mChatAbility.findComponentById(ResourceTable.Id_chatMsgReply);
        chatReplyCancel = mChatAbility.findComponentById(ResourceTable.Id_chatReplyCancel);

        // Gets the send message button and hooks on click action
        sendBtn = mChatAbility.findComponentById(ResourceTable.Id_sendMessageButton);
        // Gets the send audio button and hooks on click action if permission allowed
        audioBtn = mChatAbility.findComponentById(ResourceTable.Id_audioMicButton);
        // Gets the call switch button
        callBtn = mChatAbility.findComponentById(ResourceTable.Id_chatBackToCallButton);
        mTrash = mChatAbility.findComponentById(ResourceTable.Id_ic_mic_trash);

        // Bind all image previews
        msgRecordView = mChatAbility.findComponentById(ResourceTable.Id_recordView);
        imagePreview = mChatAbility.findComponentById(ResourceTable.Id_imagePreview);
        mediaPreview = mChatAbility.findComponentById(ResourceTable.Id_media_preview);

        // Bind all sound record views
        mSoundMeter = mChatAbility.findComponentById(ResourceTable.Id_sound_meter);
        mRecordTimer = mChatAbility.findComponentById(ResourceTable.Id_recordTimer);
        mdBTextView = mChatAbility.findComponentById(ResourceTable.Id_dBTextView);
    }

    /**
     * Init to correct mChatTransport; if chatTransPort allows, then enable chatState
     * notifications thread. Perform only if the chatFragment is really visible to user
     * Otherwise the non-focus chatFragment will cause out-of-sync between chatFragment and
     * chatController i.e. entered msg display in wrong chatFragment
     */
    private void initChatController() {
        if (!mChatSlice.getAbilityInfo().isVisible()) {
            Timber.w("Skip init current Chat Transport to: %s; with visible State: %s",
                    mChatTransport, false);
            return;
        }

        mChatTransport = chatPanel.getChatSession().getCurrentChatTransport();
        allowsChatStateNotifications = (mChatTransport.allowsChatStateNotifications()
                && ConfigurationUtils.isSendChatStateNotifications());

        if (allowsChatStateNotifications) {
            msgEdit.setTouchEventListener((v, event) -> {
                if (event.getAction() == TouchEvent.PRIMARY_POINT_DOWN) {
                    onTouchAction();
                }
                return false;
            });

            // Start chat state control thread and give 500mS before sending ChatState.active
            // to take care the fast scrolling of fragment by user.
            if (chatStateCtrlThread == null) {
                mChatState = ChatState.gone;
                chatStateCtrlThread = new ChatStateControl();
                chatStateCtrlThread.start();
            }
        }
    }

    /**
     * Method called by <code>ChatSlice</code> when it's no longer displayed to the user.
     * This happens when user scroll pagerAdapter, and the chat window is out of view
     */
    public void onHide() {
        if (isAttached) {
            isAttached = false;

            // Remove text listener
            msgEdit.removeTextObserver(this);
            // Store edited text in chatPanel
            if ((chatPanel != null) && (msgEdit.getText() != null))
                chatPanel.setEditedText(msgEdit.getText());

            mediaPreview.setVisibility(Component.HIDE);
        }
    }

    /**
     * Sends the chat message or corrects the last message if the chatPanel has correction UID set.
     *
     * @param message the text string to be sent
     * @param encType The encType of the message: RemoteOnly | FLAG_MSG_OOB | 1=text/html or 0=text/plain.
     * @param msgUuId The message Id when provided is used in sending the message.
     */
    public void sendMessage(String message, int encType, String msgUuId) {
        // Sometimes it seems the chatPanel is not inSync with the chatSession or initialized,
        // i.e Conference instead of MetaContact; and may also be null, so check to ensure
        if (chatPanel == null)
            chatPanel = mChatSlice.getChatPanel();

        String correctionUID = chatPanel.getCorrectionUID();

        int encryption = IMessage.ENCRYPTION_NONE;
        if (chatPanel.isOmemoChat())
            encryption = IMessage.ENCRYPTION_OMEMO;

        if (correctionUID == null) {
            try {
                mChatTransport.sendInstantFTMessage(message, encryption | encType, msgUuId);
            } catch (Exception ex) {
                Timber.e("Send instant message exception: %s", ex.getMessage());
                aTalkApp.showToastMessage(ex.getMessage());
            }
        }
        // Last message correction
        else {
            mChatTransport.sendInstantMessage(message, encryption | encType, correctionUID);
            // Clears correction UI state
            chatPanel.setCorrectionUID(null);
            updateCorrectionState();
        }

        // must run on UiThread when access view
        BaseAbility.runOnUiThread(() -> {
            // Clears edit text field
            if (msgEdit != null)
                msgEdit.setText("");

            // just update chat state to active but not sending notifications
            mChatState = ChatState.active;
            if (chatStateCtrlThread == null) {
                chatStateCtrlThread = new ChatStateControl();
                chatStateCtrlThread.start();
            }
            chatStateCtrlThread.initChatState();
        });
    }

    /**
     * Method fired when the chat message is clicked. {@inheritDoc}.
     * Trigger from @see ChatSlice#
     */
    public void onItemClick(ListContainer adapter, Component view, int position, long id) {
        // Detect outgoing message area
        if ((view.getId() != ResourceTable.Id_outgoingMessageView) && (view.getId() != ResourceTable.Id_outgoingMessageHolder)) {
            cancelCorrection();
            return;
        }
        ChatSlice.ChatItemProvider chatItemProvider = mChatSlice.getChatListAdapter();

        // Position must be aligned to the number of header views included
        int headersCount = adapter.getHeaderViewsCount();
        int cPos = position - headersCount;
        ChatMessage chatMessage = chatItemProvider.getMessage(cPos);

        // Ensure the selected message is really the last outgoing message
        if (cPos != chatItemProvider.getCount() - 1) {
            for (int i = cPos + 1; i < chatItemProvider.getCount(); i++) {
                if (chatItemProvider.getItemViewType(i) == ChatSlice.ChatItemProvider.OUTGOING_MESSAGE_VIEW) {
                    cancelCorrection();
                    return;
                }
            }
        }

        if (mChatTransport instanceof MetaContactChatTransport) {
            if (!chatMessage.getMessage().matches(ChatMessage.HTML_MARKUP))
                editText(adapter, chatMessage, position);
        }
        // Just put the last message in edit box for Omemo send error
        else {
            msgEdit.setText(chatMessage.getContentForCorrection());
        }
    }

    public void editText(ListContainer adapter, ChatMessage chatMessage, int position) {
        // ListContainer cListContainer = chatFragment.getChatListContainer();
        String uidToCorrect = chatMessage.getUidForCorrection();
        String content = chatMessage.getContentForCorrection();

        if (!TextUtils.isEmpty(content)) {
            // Sets corrected message content and show the keyboard
            msgEdit.setText(content);
            msgEdit.requestFocus();

            // Not send message - uidToCorrect is null
            if (!TextUtils.isEmpty(uidToCorrect)) {
                // Change edit text bg colors and show cancel button
                chatPanel.setCorrectionUID(uidToCorrect);
                updateCorrectionState();

                // InputMethodManager inputMethodManager = (InputMethodManager) mChatAbility.getSystemService(Context.INPUT_METHOD_SERVICE);
                // InputMethodAbility inputMethodAbility = new InputMethodAbility();

                // if (inputMethodManager != null)
                //    inputMethodManager.showSoftInput(msgEdit, InputMethodManager.SHOW_IMPLICIT);

                SoftKeyBoardController kbController = new SoftKeyBoardController(1, new Object());
                if (kbController != null) {
                    kbController.setShowMode(AccessibleAbility.SHOW_MODE_AUTO);
                }
                // Select corrected message
                adapter.setSelectedItemIndex(position);
            }
        }
    }

    public void setQuoteMessage(ChatMessage replyMessage) {
        if (replyMessage != null) {
            chatMessageReply.setVisibility(Component.VISIBLE);
            chatReplyCancel.setVisibility(Component.VISIBLE);

            Html.ImageGetter imageGetter = new HtmlImageGetter();
            String body = replyMessage.getMessage();
            if (!body.matches(ChatMessage.HTML_MARKUP)) {
                body = body.replace("\n", "<br/>");
            }
            quotedMessage = aTalkApp.getResString(ResourceTable.String_chat_reply_quote,
                    replyMessage.getSender(), body);
            chatMessageReply.setText(Html.fromHtml(quotedMessage, Html.FROM_HTML_MODE_LEGACY, imageGetter, null));
        }
        else {
            quotedMessage = null;
            chatMessageReply.setVisibility(Component.HIDE);
            chatReplyCancel.setVisibility(Component.HIDE);
        }
    }

    /**
     * Method fired when send a message or cancel correction button is clicked.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void onClick(Component v) {
        switch (v.getId()) {
            case ResourceTable.Id_sendMessageButton:
                if (chatPanel.getProtocolProvider().isRegistered()) {
                    if (mediaPreview.getVisibility() == Component.VISIBLE) {
                        // Disable to prevent user multiple clicks.
                        sendBtn.setVisibility(Component.INVISIBLE);
                        MediaPreviewProvider mpAdapter = (MediaPreviewProvider) mediaPreview.getAdapter();
                        if (mpAdapter != null) {
                            List<Attachment> mediaPreviews = mpAdapter.getAttachments();
                            if (!mediaPreviews.isEmpty()) {
                                for (Attachment attachment : mediaPreviews) {
                                    String filePath = FilePathHelper.getFilePath(mChatAbility, attachment);
                                    if (StringUtils.isNotEmpty(filePath)) {
                                        if (new File(filePath).exists()) {
                                            chatPanel.addFTSendRequest(filePath, ChatMessage.MESSAGE_FILE_TRANSFER_SEND);
                                        }
                                        else {
                                            aTalkApp.showToastMessage(ResourceTable.String_file_does_not_exist);
                                        }
                                    }
                                }
                                mpAdapter.clearPreviews();
                            }
                        }
                    }
                    else {
                        // allow last message correction to send empty string to clear last sent text
                        String correctionUID = chatPanel.getCorrectionUID();
                        String textEdit = ComponentUtil.toString(msgEdit);
                        if ((textEdit == null) && (correctionUID != null)) {
                            textEdit = " ";
                        }
                        if ((textEdit == null) && (quotedMessage == null)) {
                            return;
                        }

                        if (quotedMessage != null) {
                            textEdit = quotedMessage + textEdit;
                        }
                        // Send http link as xhtml to avoid being interpreted by the receiver as http file download link
                        else if (textEdit.matches("(?s)^https:.*") && !textEdit.contains("\\s")) {
                            textEdit = aTalkApp.getResString(ResourceTable.String_chat_url_link, textEdit, textEdit);
                        }

                        // if text contains markup tag then send message as ENCODE_HTML mode
                        if (textEdit.matches(ChatMessage.HTML_MARKUP)) {
                            Timber.d("HTML text entry detected: %s", textEdit);
                            msgEdit.setText(textEdit);
                            sendMessage(textEdit, IMessage.ENCODE_HTML, null);
                        }
                        else
                            sendMessage(textEdit, IMessage.ENCODE_PLAIN, null);
                    }
                    updateSendModeState();
                }
                else {
                    aTalkApp.showToastMessage(ResourceTable.String_message_delivery_not_registered);
                }
                if (quotedMessage == null)
                    break;
                // else continue to cleanup quotedMessage after sending

            case ResourceTable.Id_chatReplyCancel:
                quotedMessage = null;
                chatMessageReply.setVisibility(Component.HIDE);
                chatReplyCancel.setVisibility(Component.HIDE);
                break;

            case ResourceTable.Id_cancelCorrectionBtn:
                cancelCorrection();
                // Clear last message text
                msgEdit.setText("");
                break;

            case ResourceTable.Id_chatBackToCallButton:
                if (CallManager.getActiveCallsCount() > 0) {
                    String callId = null;
                    for (Call call : CallManager.getActiveCalls()) {
                        callId = call.getCallId();
                        CallPeerJabberImpl callPeer = ((CallJabberImpl) call).getPeerBySid(callId);
                        MetaContact metaContact = chatPanel.getMetaContact();
                        if ((metaContact != null) && metaContact.getDefaultContact().equals(callPeer.getContact())) {
                            break;
                        }
                    }
                    if (callId != null)
                        CallNotificationManager.getInstanceFor(callId).backToCall();
                }
                else
                    updateSendModeState();
                break;

            case ResourceTable.Id_audioMicButton:
                if (chatPanel.isChatTtsEnable()) {
                    speechToText();
                }
                break;
        }
    }

    /**
     * Audio sending is disabled if SystemPermission.MICROPHONE is denied.
     * Audio chat message is allowed even for offline contact and in conference
     */
    @Override
    public void onLongClicked(Component v) {
        if (v.getId() == ResourceTable.Id_audioMicButton) {
            Timber.d("Current Chat Transport for audio: %s", mChatTransport.toString());
            Timber.d("Audio recording started!!!");
            isRecording = true;
            // Hide normal edit text view
            msgEdit.setVisibility(Component.HIDE);

            // Show audio record information
            msgRecordView.setVisibility(Component.VISIBLE);
            mTrash.setPixelMap(ResourceTable.Media_ic_record);
            micAnimate.setTarget(mTrash);
            micAnimate.start();
            subscribe();
            startAudioService(AudioBgService.ACTION_RECORDING);
        }
    }

    /**
     * onDrag is disabled if permission.RECORD_AUDIO is denied
     */
    @Override
    public boolean onDrag(Component component, DragEvent dragEvent) {
        boolean done = false;
        switch (dragEvent.getAction()) {
            case TouchEvent.PRIMARY_POINT_DOWN: {
                downX = dragEvent.getX();
                return false;  // to allow long press detection
            }

            case TouchEvent.PRIMARY_POINT_UP: {
                float upX = dragEvent.getX();
                float deltaX = downX - upX;

                //Swipe horizontal detected
                if (Math.abs(deltaX) > min_distance) {
                    if (isRecording && (deltaX > 0)) { // right to left
                        Timber.d("Audio recording cancelled!!!");
                        isRecording = false;
                        audioBtn.setEnabled(false); // disable while in animation
                        unSubscribe();
                        startAudioService(AudioBgService.ACTION_CANCEL);

                        // Start audio sending cancel animation
                        smAnimate.start();
                        dbTextAnimate.start();
                        recordAnimate.start();

                        micAnimate.stop();
                        mTrash.setPixelMap(null);
                        mTrashAnimate.start();
                        onAnimationEnd(1200);
                        done = true;
                    }
                }
                else {
                    if (isRecording) {
                        Timber.d("Audio recording sending!!!");
                        isRecording = false;
                        startAudioService(AudioBgService.ACTION_SEND);
                        onAnimationEnd(10);
                        done = true;
                    }
                }
            }
        }
        return done;
    }

    /**
     * Handling of KeyCode in ChatController, called from ChatAbility
     * Note: KeyEvent.Callback is only available in Ability
     */
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEY_ENTER) {
            if (mChatSlice != null) {
                sendBtn.callOnClick();
            }
            return true;
        }
        return false;
    }

    // Need to wait on a new thread for animation to end
    private void onAnimationEnd(final int wait) {
        new Thread(() -> {
            try {
                Thread.sleep(wait);
                BaseAbility.runOnUiThread(() -> {
                    mTrashAnimate.stop();
                    mTrashAnimate.setPreDecodeFrames(0);
                    msgEdit.setVisibility(Component.VISIBLE);

                    msgRecordView.setVisibility(Component.HIDE);
                    smAnimate.cancel();
                    dbTextAnimate.cancel();
                    recordAnimate.cancel();
                    audioBtn.setEnabled(true);
                });
            } catch (Exception ex) {
                Timber.e("Exception: %s", ex.getMessage());
            }
        }).start();
    }

    private void startAudioService(String mAction) {

        Operation operation = new Intent.OperationBuilder()
                .withBundleName(mChatAbility.getBundleName())
                .withAbilityName(JingleMessageCallAbility.class)
                .build();

        recordIntent.setAction(mAction)
                .setOperation(operation);
        mChatAbility.startAbility(recordIntent);
    }

    private CommonEventSubscribeInfo getSubscribeInfo() {
        MatchingSkills filter = new MatchingSkills();
        // Set up audio background service and receiver

        filter.addEvent(AudioBgService.ACTION_AUDIO_RECORD);
        filter.addEvent(AudioBgService.ACTION_SMI);
        return new CommonEventSubscribeInfo(filter);
    }

    private void subscribe() {
        MatchingSkills filter = new MatchingSkills();
        filter.addEvent(AudioBgService.ACTION_AUDIO_RECORD);
        filter.addEvent(AudioBgService.ACTION_SMI);

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

            if (isRecording && AudioBgService.ACTION_SMI.equals(intent.getAction())) {
                String mDuration = intent.getStringParam(AudioBgService.RECORD_TIMER);
                double mdBSpl = intent.getDoubleParam(AudioBgService.SPL_LEVEL, 1.0);
                double dBspl = (mdBSpl * AudioBgService.mDBRange);
                String sdBSpl = String.format(Locale.US, "%.02f", dBspl) + "dB";

                mSoundMeter.setLevel(mdBSpl);
                mdBTextView.setText(sdBSpl);
                mRecordTimer.setText(mDuration);
            }
            else if (AudioBgService.ACTION_AUDIO_RECORD.equals(intent.getAction())) {
                Timber.i("Sending audio recorded file!!!");
                unSubscribe();
                String filePath = intent.getStringParam(AudioBgService.URI);
                if (StringUtils.isNotEmpty(filePath)) {
                    chatPanel.addFTSendRequest(filePath, ChatMessage.MESSAGE_FILE_TRANSFER_SEND);
                }
                mChatAbility.stopAbility(recordIntent);
            }
        }
    }

    /**
     * Built-in speech to text recognition without a soft keyboard popup.
     * To use the soft keyboard mic, click on text entry and then click on mic.
     */
    private void speechToText() {
        AsrIntent asrIntent = new AsrIntent();
        asrIntent.setEngineType(AsrIntent.AsrEngineType.ASR_ENGINE_TYPE_LOCAL);
        asrIntent.setAudioSourceType(AsrIntent.AsrAudioSrcType.ASR_SRC_TYPE_PCM);

        AsrListener listener = new AsrListener() {
            @Override
            public void onInit(PacMap pacMap) {
                Timber.d("Ready for speech");
                mTtsAnimate = new FrameAnimationElement(mChatAbility, ResourceTable.Graphic_ic_tts_mic_play);
                audioBtn.setBackground(mTtsAnimate);
                mTtsAnimate.start();
            }

            @Override
            public void onBeginningOfSpeech() {
                Timber.d("Speech starting");
            }

            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onResults(PacMap pacMap) {
                ArrayList<String> voiceResults = pacMap.getStringList(RESULTS_RECOGNITION);
                if (voiceResults == null) {
                    Timber.w("No voice results");
                    updateSendModeState();
                }
                else {
                    // Contains multiple text strings for selection
                    // StringBuffer spkText = new StringBuffer();
                    // for (String match : voiceResults) {
                    //    spkText.append(match).append("\n");
                    // }
                    msgEdit.setText(voiceResults.get(0));
                }
            }

            /**
             * ERROR_NETWORK_TIMEOUT = 1;
             * ERROR_NETWORK = 2;
             * ERROR_AUDIO = 3;
             * ERROR_SERVER = 4;
             * ERROR_CLIENT = 5;
             * ERROR_SPEECH_TIMEOUT = 6;
             * ERROR_NO_MATCH = 7;
             * ERROR_RECOGNIZER_BUSY = 8;
             * ERROR_INSUFFICIENT_PERMISSIONS = 9;
             *
             * @param error code is defined in
             * @see SpeechRecognizer()
             */
            @Override
            public void onError(int error) {
                Timber.e("Error listening for speech: %s ", error);
                updateSendModeState();
            }

            @Override
            public void onIntermediateResults(PacMap pacMap) {
            }

            @Override
            public void onEnd() {
            }

            @Override
            public void onEvent(int i, PacMap pacMap) {
            }

            @Override
            public void onAudioStart() {
            }

            @Override
            public void onAudioEnd() {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
                mTtsAnimate.stop();
                audioBtn.setPixelMap(ResourceTable.Media_ic_tts_mic_on);
            }
        };

        AsrClient asrClient = AsrClient.createAsrClient(mChatAbility).get();
        asrClient.init(asrIntent, listener);
        asrClient.startListening(asrIntent);
    }

    /**
     * Cancels last message correction mode.
     */
    private void cancelCorrection() {
        // Reset correction status
        if (chatPanel.getCorrectionUID() != null) {
            chatPanel.setCorrectionUID(null);
            updateCorrectionState();
            msgEdit.setText("");
        }
    }

    /**
     * Insert/remove the buddy nickname into the sending text.
     *
     * @param buddy occupant jid
     */
    public void insertTo(String buddy) {
        if (buddy != null) {
            String nickName = buddy.replaceAll("(\\w+)[:|@].*", "$1");

            String editText = ComponentUtil.toString(msgEdit);
            if (editText == null) {
                nickName += ": ";
            }
            else if (editText.contains(nickName)) {
                nickName = editText.replace(nickName, "")
                        .replace(", ", "");
                if (nickName.length() == 1)
                    nickName = "";
            }
            else if (editText.contains(":")) {
                nickName = editText.replace(":", ", " + nickName + ": ");
            }
            else {
                nickName = editText + " " + nickName;
            }

            msgEdit.setText(nickName);
        }
    }

    /**
     * Updates visibility state of cancel correction button and toggles bg color of the message edit field.
     */
    private void updateCorrectionState() {
        boolean correctionMode = (chatPanel.getCorrectionUID() != null);
        int bgColorId = correctionMode ? ResourceTable.Color_msg_input_correction_bg : ResourceTable.Color_msg_input_bar_bg;
        msgEditBg.setBackground(new ShapeElement(new RgbColor(bgColorId)));
        cancelCorrectionBtn.setVisibility(correctionMode ? Component.VISIBLE : Component.HIDE);
        mChatSlice.getChatListContainer().invalidate();
    }

    /**
     * Updates chat state.
     * {@inheritDoc}
     */
    @Override
    public void onTextUpdated(String s, int start, int before, int count) {
        if (allowsChatStateNotifications) {
            if (s.length() > 0) {
                // Start or refreshComposing chat state control thread
                if (chatStateCtrlThread == null) {
                    setNewChatState(ChatState.active);
                    setNewChatState(ChatState.composing);
                    chatStateCtrlThread = new ChatStateControl();
                    chatStateCtrlThread.start();
                }
                else
                    chatStateCtrlThread.refreshChatState();
            }
        }
        updateSendModeState();
    }

    /**
     * Update the view states of all send buttons based on the current available send contents.
     * Send text button has higher priority over attachment if msgEdit is not empty
     */
    public void updateSendModeState() {
        boolean hasAttachments = (mediaPreview.getAdapter() != null)
                && ((MediaPreviewProvider) mediaPreview.getAdapter()).hasAttachments();
        if (mediaPreview != null)
            mediaPreview.setVisibility(Component.HIDE);

        imagePreview.setVisibility(Component.HIDE);
        imagePreview.setPixelMap(null);

        callBtn.setVisibility(Component.INVISIBLE);
        audioBtn.setVisibility(Component.INVISIBLE);
        msgEdit.setVisibility(Component.VISIBLE);

        // Enabled send text button if text entry box contains text or in correction mode
        // Sending Text before attachment
        if (!TextUtils.isEmpty(msgEdit.getText()) || (chatPanel.getCorrectionUID() != null)) {
            sendBtn.setVisibility(Component.VISIBLE);
        }
        else if (hasAttachments) {
            msgEdit.setVisibility(Component.HIDE);
            mediaPreview.setVisibility(Component.VISIBLE);
            imagePreview.setVisibility(Component.VISIBLE);
            sendBtn.setVisibility(Component.VISIBLE);
        }
        else {
            sendBtn.setVisibility(Component.INVISIBLE);

            if (CallManager.getActiveCallsCount() > 0) {
                callBtn.setVisibility(Component.VISIBLE);
            }
            else if (isAudioAllowed) {
                audioBtn.setPixelMap(ResourceTable.Media_ic_voice_mic);
                audioBtn.setVisibility(Component.VISIBLE);
            }
            else {
                sendBtn.setVisibility(Component.VISIBLE);
            }
        }
    }

    /**
     * Method called by <code>ChatSlice</code> and <code>ChatController</code>. when user touches the
     * display. Re-init chat state to active when user return to chat session
     */
    public void onTouchAction() {
        if (mChatState == ChatState.inactive) {
            setNewChatState(ChatState.active);
            if (chatStateCtrlThread == null) {
                chatStateCtrlThread = new ChatStateControl();
                chatStateCtrlThread.start();
            }
        }
    }

    /**
     * Method called by <code>ChatSlice</code> when user closes the chat window.
     * Update that user is no longer in this chat session and end state ctrl thread
     */
    public void onChatCloseAction() {
        setNewChatState(ChatState.gone);
    }

    /**
     * Sets new chat state and send notification is enabled.
     *
     * @param newState new chat state to set.
     */
    private void setNewChatState(ChatState newState) {
        // Timber.w("Chat state changes from: " + mChatState + " => " + newState);
        if (mChatState != newState) {
            mChatState = newState;

            if (allowsChatStateNotifications)
                mChatTransport.sendChatStateNotification(newState);
        }
    }

    @Override
    public void onCommitContent(InputContentInfoCompat info) {
        if (chatPanel.getProtocolProvider().isRegistered()) {
            Uri contentUri = info.getContentUri();
            String filePath = FilePathHelper.getFilePath(mChatAbility, contentUri);
            if (StringUtils.isNotEmpty(filePath)) {
                sendSticker(filePath);
            }
            else
                aTalkApp.showToastMessage(ResourceTable.String_file_does_not_exist);
        }
        else {
            aTalkApp.showToastMessage(ResourceTable.String_message_delivery_not_registered);
        }
    }

    private void sendSticker(String filePath) {
        UIService uiService = AppGUIActivator.getUIService();
        if (uiService != null) {
            chatPanel.addFTSendRequest(filePath, ChatMessage.MESSAGE_STICKER_SEND);
        }
    }

    /**
     * The thread lowers chat state from composing to inactive state. When
     * <code>refreshChatState</code> is called checks for eventual chat state refreshComposing.
     */
    class ChatStateControl extends Thread {
        boolean refreshComposing;
        boolean initActive;
        boolean cancel = false;

        @Override
        public void run() {
            while (mChatState != ChatState.inactive) {
                refreshComposing = false;
                initActive = false;
                ChatState newState;
                long delay;

                switch (mChatState) {
                    case gone:
                        delay = 500;
                        newState = ChatState.active;
                        break;
                    case composing:
                        delay = 10000;
                        newState = ChatState.paused;
                        break;
                    case paused:
                        delay = 15000;
                        newState = ChatState.inactive;
                        break;
                    default: // active
                        delay = 30000;
                        newState = ChatState.inactive;
                }

                synchronized (this) {
                    try {
                        // Waits the delay to enter newState
                        this.wait(delay);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (refreshComposing) {
                    newState = ChatState.composing;
                }
                else if (initActive) {
                    newState = ChatState.active;
                }
                else if (cancel) {
                    newState = ChatState.gone;
                }

                // Timber.d("Chat State changes %s (%s)", newState, mChatState);
                // Post new chat state
                setNewChatState(newState);
            }
            chatStateCtrlThread = null;
        }

        /**
         * Refresh the thread's control loop to ChatState.composing.
         */
        void refreshChatState() {
            synchronized (this) {
                refreshComposing = true;
                this.notify();
            }
        }

        /**
         * Initialize the thread' control loop to ChatState.active
         */
        void initChatState() {
            synchronized (this) {
                initActive = true;
                this.notify();
            }
        }

        /**
         * Cancels (ChatState.gone) and joins the thread.
         */
        void cancel() {
            synchronized (this) {
                cancel = true;
                this.notify();
            }
            try {
                this.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public Ability getChatAbility() {
        return mChatAbility;
    }
}
