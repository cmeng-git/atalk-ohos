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
package org.atalk.ohos.gui.chat.conference;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.agp.components.TextField;
import ohos.agp.window.service.Display;
import ohos.app.Context;
import ohos.media.image.ImageSource;
import ohos.media.image.PixelMap;
import ohos.media.image.common.Size;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.chat.ChatMessage;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.util.AppImageUtil;
import org.atalk.ohos.util.ComponentUtil;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaIdFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Body;
import org.jivesoftware.smackx.bob.element.BoBDataExtension;
import org.jivesoftware.smackx.captcha.packet.CaptchaExtension;
import org.jivesoftware.smackx.captcha.packet.CaptchaIQ;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.TextSingleFormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import timber.log.Timber;

/**
 * The dialog pops up when the user joining chat room receive a normal message containing
 * captcha challenge for spam protection
 *
 * @author Eng Chong Meng
 */
public class CaptchaDialog {
    /* Captcha response state */
    public static final int unknown = -1;
    public static final int validated = 0;
    public static final int awaiting = 1;
    public static final int failed = 2;
    public static final int cancel = 3;

    private TextField mCaptchaText;
    private Text mReason;

    private Image mImageView;
    private Button mAcceptButton;
    private Button mCancelButton;
    private Button mOKButton;

    private PixelMap mCaptcha;
    private DataForm mDataForm;
    private DataForm.Builder formBuilder;
    private String mReasonText;

    private static XMPPConnection mConnection;
    private static Message mMessage;
    private final Context mContext;
    private final CaptchaDialogListener callBack;

    private DialogA sDialog;

    public interface CaptchaDialogListener {
        void onResult(int state);

        void addMessage(String msg, int msgType);
    }

    public CaptchaDialog(Context context, MultiUserChat multiUserChat, Message message, CaptchaDialogListener listener) {
        mContext = context;
        mConnection = multiUserChat.getXmppConnection();
        mMessage = message;
        callBack = listener;
    }

    public DialogA create() {
        LayoutScatter scatter = LayoutScatter.getInstance(mContext);
        Component component = scatter.parse(ResourceTable.Layout_captcha_challenge, null, false);

        mImageView = component.findComponentById(ResourceTable.Id_captcha);
        mCaptchaText = component.findComponentById(ResourceTable.Id_input);
        mReason = component.findComponentById(ResourceTable.Id_reason_field);

        DialogA.Builder builder = new DialogA.Builder(mContext);
        builder.setTitle(ResourceTable.Layout_captcha_challenge)
                .setComponent(component)
                .setNegativeButton(ResourceTable.String_cancel, dialog -> {
                    if (onAcceptClicked(true))
                        showResult();
                    else
                        dialog.remove();
                })
                .setNeutralButton(ResourceTable.String_submit, dialog -> {
                    if (TextUtils.isEmpty(ComponentUtil.toString(mCaptchaText))) {
                        aTalkApp.showToastMessage(ResourceTable.String_captcha_text_empty);
                    }
                    else {
                        if (onAcceptClicked(false))
                            showResult();
                        else
                            dialog.remove();
                    }
                })
                .setPositiveButton(ResourceTable.String_ok, DialogA::remove);

        sDialog = builder.create();
        // initial buttons visibility states
        mCancelButton = sDialog.getButton(DialogA.BUTTON_NEGATIVE);
        mAcceptButton = sDialog.getButton(DialogA.BUTTON_NEUTRAL);
        mOKButton = sDialog.getButton(DialogA.BUTTON_POSITIVE);

        if (initCaptchaData()) {
            showCaptchaContent();
            mImageView.setClickedListener(v -> mCaptchaText.requestFocus());
        }

        sDialog.setSwipeToDismiss(true);
        sDialog.setAutoClosable(false);
        return sDialog;
    }

    /*
     * Update dialog content with the received captcha information for form presentation.
     */
    private void showCaptchaContent() {
        // Scale the captcha to the display resolution
        int density = (int) new Display().getRealAttributes().scalDensity;
        Size size = mCaptcha.getFitDensitySize(density);
        PixelMap captcha = AppImageUtil.scaledPixelMap(mCaptcha, size.width, size.height);
        mImageView.setPixelMap(captcha);

        Body bodyExt = mMessage.getExtension(Body.class);
        if (bodyExt != null)
            mReasonText = bodyExt.getMessage();
        else
            mReasonText = mMessage.getBody();

        mReason.setText(mReasonText);
        mCaptchaText.requestFocus();
    }

    /**
     * Handles the <code>ActionEvent</code> triggered when one user clicks on the Submit button.
     * Reply with the following Captcha IQ
     * <iq type='set' from='robot@abuser.com/zombie' to='victim.com' xml:lang='en' id='z140r0s'>
     * <captcha xmlns='urn:xmpp:captcha'>
     * <x xmlns='jabber:x:data' type='submit'>
     * <field var='FORM_TYPE'><value>urn:xmpp:captcha</value></field>
     * <field var='from'><value>innocent@victim.com</value></field>
     * * <field var='challenge'><value>F3A6292C</value></field>
     * <field var='sid'><value>spam1</value></field>
     * <field var='ocr'><value>7nHL3</value></field>
     * </x>
     * </captcha>
     * </iq>
     *
     * @param isCancel true is user cancel; send empty reply and callback with cancel
     *
     * @return the captcha reply result success or failure
     */
    private boolean onAcceptClicked(boolean isCancel) {
        formBuilder = DataForm.builder(DataForm.Type.submit)
                .addField(mDataForm.getField(FormField.FORM_TYPE))
                .addField(mDataForm.getField(CaptchaExtension.FROM))
                .addField(mDataForm.getField(CaptchaExtension.CHALLENGE))
                .addField(mDataForm.getField(CaptchaExtension.SID));

        // Only localPart is required
        String userName = mMessage.getTo().toString();
        addFormField(CaptchaExtension.USER_NAME, userName);

        String rc = mCaptchaText.getText();
        if (rc != null) {
            addFormField(CaptchaExtension.OCR, rc);
        }

        /*
         * Must immediately inform caller before sending reply; otherwise may have race condition
         * i.e. <presence/> exception get process before mCaptchaState is update
         */
        if (isCancel)
            callBack.onResult(cancel);

        CaptchaIQ iqCaptcha = new CaptchaIQ(formBuilder.build());
        iqCaptcha.setType(IQ.Type.set);
        iqCaptcha.setTo(mMessage.getFrom());
        try {
            createStanzaCollectorAndSend(iqCaptcha).nextResultOrThrow();
            callBack.onResult(validated);

            mReasonText = mContext.getString(ResourceTable.String_captcha_verification_valid);
            callBack.addMessage(mReasonText, ChatMessage.MESSAGE_SYSTEM);
            return true;
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException
                 | SmackException.NotConnectedException | InterruptedException ex) {
            mReasonText = ex.getMessage();
            if (isCancel) {
                callBack.addMessage(mReasonText, ChatMessage.MESSAGE_ERROR);
            }
            else {
                // caller will retry, so do not show error.
                callBack.onResult(failed);
            }
            Timber.e("Captcha Exception: %s => %s", isCancel, mReasonText);
        }
        return false;
    }

    /**
     * Add field / value to formBuilder for registration
     *
     * @param name the FormField variable
     * @param value the FormField value
     */
    private void addFormField(String name, String value) {
        TextSingleFormField.Builder field = FormField.builder(name);
        field.setValue(value);
        formBuilder.addField(field.build());
    }

    /*
     * set Captcha IQ and receive reply
     */
    private StanzaCollector createStanzaCollectorAndSend(IQ req)
            throws SmackException.NotConnectedException, InterruptedException {
        return mConnection.createStanzaCollectorAndSend(new StanzaIdFilter(req.getStanzaId()), req);
    }

    /**
     * Perform the InBand Registration for the accountId on the defined XMPP connection by pps.
     * Registration can either be:
     * - simple username and password or
     * - With captcha protection using form with embedded captcha image if available, else the
     * image is retrieved from the given url in the form.
     */
    private boolean initCaptchaData() {
        try {
            // do not proceed if dataForm is null
            CaptchaExtension captchaExt = mMessage.getExtension(CaptchaExtension.class);
            DataForm dataForm = captchaExt.getDataForm();
            if (dataForm == null) {
                callBack.onResult(failed);
                return false;
            }

            PixelMap bmCaptcha = null;
            BoBDataExtension bob = mMessage.getExtension(BoBDataExtension.class);
            ImageSource.SourceOptions srcOptions = new ImageSource.SourceOptions();
            srcOptions.formatHint = "image/jpeg";

            ImageSource.DecodingOptions decOptions = new ImageSource.DecodingOptions();
            ImageSource imageSource = null;
            if (bob != null) {
                byte[] bytData = bob.getBobData().getContent();
                // InputStream stream = new ByteArrayInputStream(bytData);
                imageSource = ImageSource.create(bytData, srcOptions);
            }
            else {
                /*
                 * <field var='ocr' label='Enter the text you see'>
                 *   <media xmlns='urn:xmpp:media-element' height='80' width='290'>
                 *     <uri type='image/jpeg'>http://www.victim.com/challenges/ocr.jpeg?F3A6292C</uri>
                 *     <uri type='image/jpeg'>cid:sha1+f24030b8d91d233bac14777be5ab531ca3b9f102@bob.xmpp.org</uri>
                 *   </media>
                 * </field>
                 */
                // not working - smack does not support get media element embedded in ocr field data
                // FormField ocrField = dataForm.getField("ocr");
                // String mediaElement = ocrField.getDescription();
                FormField urlField = dataForm.getField("ocr");
                if (urlField != null) {
                    String urlString = urlField.getFirstValue();
                    if (urlString.contains("https://")) {
                        URL uri = new URL(urlString);
                        InputStream inStream = uri.openConnection().getInputStream();
                        imageSource = ImageSource.create(inStream, srcOptions);
                    }
                }
            }

            if (imageSource != null)
                bmCaptcha = imageSource.createPixelmap(decOptions);

            mDataForm = dataForm;
            mCaptcha = bmCaptcha;

            // use web link for captcha challenge to user if null
            if (bmCaptcha == null)
                callBack.onResult(failed);
            else
                callBack.onResult(awaiting);
            return true;
        } catch (IOException e) {
            mReasonText = e.getMessage();
            callBack.onResult(failed);
            sDialog.remove();;
        }
        return false;
    }

    /**
     * Shows IBR registration result.
     */
    private void showResult() {
        mReason.setText(mReasonText);
        mCaptchaText.setEnabled(false);

        mAcceptButton.setVisibility(Component.HIDE);
        mCancelButton.setVisibility(Component.HIDE);
        mOKButton.setVisibility(Component.VISIBLE);
    }
}
