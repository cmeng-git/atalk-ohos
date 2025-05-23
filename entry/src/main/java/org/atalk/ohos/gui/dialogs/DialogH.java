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
package org.atalk.ohos.gui.dialogs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Component;
import ohos.agp.components.DirectionalLayout.LayoutConfig;
import ohos.agp.window.dialog.CommonDialog;
import ohos.agp.window.dialog.IDialog;
import ohos.app.Context;
import ohos.utils.PacMap;

import org.atalk.ohos.ResourceTable;

/**
 * <code>DialogH</code> can be used to display alerts without having parent <code>Ability</code>
 * (from services). <br/> Simple alerts can be displayed using static method <code>showDialog(...)
 * </code>.<br/> Optionally confirm button's text and the listener can be supplied. It allows to
 * react to users actions. For this purpose use method <code>showConfirmDialog(...)</code>.<br/>
 * For more sophisticated use cases content fragment class with it's arguments can be specified
 * in method <code>showCustomDialog()</code>. When they're present the alert message will be replaced
 * by the {@link ohos.aafwk.ability.AbilitySlice}'s <code>Component</code>.
 *
 * @author Eng Chong Meng
 */
public class DialogH extends CommonDialog {
    public static final int BUTTON_NEGATIVE = BUTTON1;
    public static final int BUTTON_NEUTRAL = BUTTON2;
    public static final int BUTTON_POSITIVE = BUTTON3;

    /**
     * Dialog title extra.
     */
    public static final String EXTRA_TITLE = "title";

    /**
     * Dialog message extra.
     */
    public static final String EXTRA_MESSAGE = "message";

    /**
     * Optional confirm button label extra.
     */
    public static final String EXTRA_CONFIRM_TXT = "confirm_txt";

    /**
     * Dialog id extra used to listen for close dialog broadcast intents.
     */
    private static final String EXTRA_DIALOG_ID = "dialog_id";

    /**
     * Optional listener ID extra(can be supplied only using method static <code>showConfirmDialog</code>.
     */
    public static final String EXTRA_LISTENER_ID = "listener_id";

    /**
     * Optional content fragment's class name that will be used instead of text message.
     */
    public static final String EXTRA_CONTENT_FRAGMENT = "fragment_class";

    /**
     * Optional content fragment's argument <code>Bundle</code>.
     */
    public static final String EXTRA_CONTENT_ARGS = "fragment_args";

    /**
     * Prevents from closing this Ability on outside touch events and blocks the back key if set to <code>true</code>.
     */
    public static final String EXTRA_CANCELABLE = "cancelable";

    /**
     * Hide all buttons.
     */
    public static final String EXTRA_REMOVE_BUTTONS = "remove_buttons";

    /**
     * Static map holds listeners for currently displayed dialogs.
     */
    private static final Map<Long, DialogListener> listenersMap = new HashMap<>();

    /**
     * Static list holds existing dialog instances (since onStart() until onStop()). Only
     * dialogs with valid id are listed here.
     */
    private final static List<Long> displayedDialogs = new ArrayList<>();

    /**
     * The dialog listener.
     */
    private DialogListener mListener;

    /**
     * Dialog listener's id used to identify listener in {@link #listenersMap}.
     * The value get be retrieved for use in caller reference when user click the button.
     */
    private long mListenerId;

    /**
     * Name of the action which can be used to close dialog with given id supplied in
     * {@link #EXTRA_DIALOG_ID}.
     */
    public static final String ACTION_CLOSE_DIALOG = "org.atalk.gui.close_dialog";

    /**
     * Name of the action which can be used to focus dialog with given id supplied in
     * {@link #EXTRA_DIALOG_ID}.
     */
    public static final String ACTION_FOCUS_DIALOG = "org.atalk.gui.focus_dialog";

    private static DialogH mInstance;

    private static boolean cancelable = false;
    private Component mContent;

    private Context mContext;

    public DialogH(Context context) {
        super(context);
        mInstance = this;
    }

    public static DialogH getInstance(Context context) {
        if (mInstance != null)
            return mInstance;

        return new DialogH(context);
    }

    /**
     * Get the listener Id for the dialog
     *
     * @return the current mListenerId
     */
    public long getListenerID() {
        return mListenerId;
    }

    /**
     * Creates an <code>Intent</code> that will display a dialog with given <code>title</code> and content <code>message</code>.
     *
     * @param ctx Android context.
     * @param title dialog title that will be used
     * @param message dialog message that wil be used.
     *
     * @return an <code>Intent</code> that will display a dialog.
     */
    public static Intent getDialogIntent(Context ctx, String title, String message) {
        Intent alert = new Intent();
        Operation operation = new Intent.OperationBuilder()
                .withBundleName(ctx.getBundleName())
                .withAbilityName(DialogH.class)
                .build();
        alert.setOperation(operation);
        alert.setParam(EXTRA_TITLE, title);
        alert.setParam(EXTRA_MESSAGE, message);
        return alert;
    }

    /**
     * Shows a dialog for the given context and a title given by <code>titleId</code> and
     * message given by <code>messageId</code> with its optional arg.
     *
     * @param context the android <code>Context</code>
     * @param titleId the title identifier in the resources
     * @param messageId the message identifier in the resources
     * @param arg optional arg for the message expansion.
     */
    public void showDialog(Context context, int titleId, int messageId, Object... arg) {
        showDialog(context, context.getString(titleId), context.getString(messageId, arg));
    }

    /**
     * Show simple alert that will be disposed when user presses OK button.
     *
     * @param context Android context.
     * @param title the dialog title that will be used.
     * @param message the dialog message that will be used.
     */
    public void showDialog(Context context, String title, String message) {
        CommonDialog commonDialog = new CommonDialog(context);
        commonDialog.setSize(LayoutConfig.MATCH_PARENT, LayoutConfig.MATCH_CONTENT);
        commonDialog.setTitleText(title)
                .setContentText(message)
                .setButton(BUTTON_POSITIVE, context.getString(ResourceTable.Id_button_OK), null);

        commonDialog.setAutoClosable(true);
        commonDialog.siteRemovable(cancelable);
        commonDialog.show();
    }

    /**
     * Shows confirm dialog allowing to handle confirm action using supplied <code>listener</code>.
     *
     * @param context the android context.
     * @param title dialog title Res that will be used
     * @param message the message identifier in the resources
     * @param confirmTxt confirm button label Res.
     * @param listener the <code>DialogInterface.DialogListener</code> to attach to the confirm button
     * @param arg optional arg for the message resource arg.
     */
    public void showConfirmDialog(Context context, int title, int message,
            int confirmTxt, DialogListener listener, Object... arg) {
        showConfirmDialog(context, context.getString(title), context.getString(message, arg),
                context.getString(confirmTxt), listener);
    }

    /**
     * Shows confirm dialog allowing to handle confirm action using supplied <code>listener</code>.
     *
     * @param context Android context.
     * @param title dialog title that will be used
     * @param message dialog message that wil be used.
     * @param confirmTxt confirm button label.
     * @param listener the confirm action listener.
     */
    public long showConfirmDialog(Context context, String title, String message, String confirmTxt, DialogListener listener) {
        CommonDialog commonDialog = new CommonDialog(context);
        commonDialog.setSize(LayoutConfig.MATCH_PARENT, LayoutConfig.MATCH_CONTENT);
        commonDialog.setTitleText(title)
                .setContentText(message)
                .setButton(BUTTON_NEGATIVE, context.getString(ResourceTable.String_cancel), mDialogListener)
                .setButton(BUTTON_POSITIVE, confirmTxt, mDialogListener);

        commonDialog.setAutoClosable(true);
        commonDialog.siteRemovable(cancelable);
        commonDialog.show();

        long listenerId = System.currentTimeMillis();
        if (listener != null) {
            listenersMap.put(listenerId, listener);
        }
        return listenerId;
    }

//    /*
//     * Show custom dialog. Alert text will be replaced by the {@link Fragment} created from
//     * <code>fragmentClass</code> name. Optional <code>fragmentArguments</code> <code>Bundle</code> will be
//     * supplied to created instance.
//     *
//     * @param context Android context.
//     * @param title the title that will be used.
//     * @param fragmentClass <code>Fragment</code>'s class name that will be used instead of text message.
//     * @param fragmentArguments optional <code>Fragment</code> arguments <code>Bundle</code>.
//     * @param confirmTxt the confirm button's label.
//     * @param listener listener that will be notified on user actions.
//     * @param extraArguments additional arguments with keys defined in {@link DialogActivity}.
//     */
//    public static long showCustomDialog(Context context, String title, String fragmentClass,
//            Component component, String confirmTxt,
//            DialogListener listener, Map<String, Serializable> extraArguments) {
//
//    }

    /**
     * Show custom dialog. Alert text will be replaced by the {@link ohos.aafwk.ability.AbilitySlice}
     * created from <code>alert</code> will be supplied to created instance.
     *
     * @param context Android context.
     * @param title the title that will be used.
     * @param component <code>Intent</code> containing the defined parameter
     * @param confirmTxt the confirm button's label.
     * @param listener listener that will be notified on user actions.
     * @param pacMap additional arguments with keys defined in {@link DialogH}.
     */
    public void showCustomDialog(Context context, String title, Component component, String confirmTxt,
            DialogListener listener, PacMap pacMap) {

        CommonDialog commonDialog = new CommonDialog(context);
        commonDialog.setContentCustomComponent(component);
        commonDialog.setSize(component.getWidth(), component.getHeight());
        commonDialog.setTitleText(title)
                .setButton(BUTTON_NEGATIVE, context.getString(ResourceTable.String_cancel), mDialogListener)
                .setButton(BUTTON_POSITIVE, confirmTxt, mDialogListener);

        if (pacMap != null) {
            cancelable = pacMap.getBooleanValue(EXTRA_CANCELABLE, false);
            boolean remove = pacMap.getBooleanValue(EXTRA_REMOVE_BUTTONS, false);
        }

        commonDialog.setAutoClosable(true);
        commonDialog.siteRemovable(cancelable);
        commonDialog.show();

        long listenerId = System.currentTimeMillis();
        if (listener != null) {
            listenersMap.put(listenerId, listener);
        }
        // return listenerId;
    }

    /**
     * Waits until the dialog with given <code>dialogId</code> is opened.
     *
     * @param dialogId the id of the dialog we want to wait for.
     *
     * @return <code>true</code> if dialog has been opened or <code>false</code> if the dialog had not
     * been opened within 10 seconds after call to this method.
     */
    public static boolean waitForDialogOpened(long dialogId) {
        synchronized (displayedDialogs) {
            if (!displayedDialogs.contains(dialogId)) {
                try {
                    displayedDialogs.wait(10000);
                    return displayedDialogs.contains(dialogId);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                return true;
            }
        }
    }

    private final IDialog.ClickedListener mDialogListener = new ClickedListener() {
        @Override
        public void onClick(IDialog iDialog, int id) {
            switch (id) {
                case BUTTON_NEGATIVE:
                    if (mListener != null) {
                        mListener.onDialogCancelled(DialogH.this);
                    }
                    iDialog.destroy();
                    break;

                case BUTTON_NEUTRAL:
                    break;

                case BUTTON_POSITIVE:
                    if (mListener != null) {
                        if (!mListener.onConfirmClicked(DialogH.this)) {
                            return;
                        }
                    }
                    iDialog.destroy();
                    break;
            }
        }
    };

    /**
     * The listener that will be notified when user clicks the confirm button or dismisses the dialog.
     */
    public interface DialogListener {
        /**
         * Fired when user clicks the dialog's confirm button.
         *
         * @param dialog source <code>DialogH</code>.
         */
        boolean onConfirmClicked(DialogH dialog);

        /**
         * Fired when user dismisses the dialog.
         *
         * @param dialog source <code>DialogH</code>
         */
        void onDialogCancelled(DialogH dialog);
    }
}
