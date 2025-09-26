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
package org.atalk.ohos.gui.widgets;

import ohos.agp.components.Component;
import ohos.agp.components.Text;
import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;
import ohos.utils.PacMap;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.ResourceTable;

/**
 * The controller used for displaying a custom toast that can be clicked.
 *
 * @author Eng Chong Meng
 */
public class LegacyClickableToastCtrl {
    /**
     * How long the toast will be displayed.
     */
    private static final long DISPLAY_DURATION = 10000;

    private static final String TOAST_MSG = "toast_message";

    /**
     * The toast <code>Component.</code> container.
     */
    protected Component toastView;

    /**
     * The <code>Text</code> displaying message text.
     */
    private final Text messageView;

    /**
     * Handler object used for hiding the toast if it's not clicked.
     */
    private final EventHandler hideHandler = new EventHandler(EventRunner.create());

    /**
     * The listener that will be notified when the toast is clicked.
     */
    private final Component.ClickedListener clickListener;

    /**
     * State object for message text.
     */
    protected String toastMessage;

    /**
     * Creates new instance of <code>ClickableToastController</code>.
     *
     * @param toastView the <code>Component.</code> that will be animated. Must contain <code>ResourceTable.Id_toast_msg</code> <code>Text</code>.
     * @param clickListener the click listener that will be notified when the toast is clicked.
     */
    public LegacyClickableToastCtrl(Component toastView, Component.ClickedListener clickListener) {
        this(toastView, clickListener, ResourceTable.Id_toast_msg);
    }

    /**
     * Creates new instance of <code>ClickableToastController</code>.
     *
     * @param toastView the <code>Component.</code> that will be animated. Must contain <code>ResourceTable.Id_toast_msg</code> <code>Text</code>.
     * @param clickListener the click listener that will be notified when the toast is clicked.
     * @param toastButtonId the id of <code>Component.</code> contained in <code>toastView
     * </code> that will be used as a button.
     */
    public LegacyClickableToastCtrl(Component toastView, Component.ClickedListener clickListener, int toastButtonId) {
        this.toastView = toastView;
        this.clickListener = clickListener;
        messageView = toastView.findComponentById(ResourceTable.Id_toast_msg);

        toastView.findComponentById(toastButtonId).setClickedListener(view -> {
            hideToast(false);
            LegacyClickableToastCtrl.this.clickListener.onClick(view);
        });

        hideToast(true);
    }

    /**
     * Shows the toast.
     *
     * @param immediate if <code>true</code> there wil be no animation.
     * @param message the toast text to use.
     */
    public void showToast(boolean immediate, String message) {
        toastMessage = message;
        messageView.setText(toastMessage);

        hideHandler.removeTask(hideRunnable);
        hideHandler.postTask(hideRunnable, DISPLAY_DURATION);
        toastView.setVisibility(Component.VISIBLE);
    }

    /**
     * Hides the toast.
     *
     * @param immediate if <code>true</code> no animation will be used.
     */
    public void hideToast(boolean immediate) {
        hideHandler.removeTask(hideRunnable);
        if (immediate) {
            onHide();
        }
    }

    /**
     * Performed to hide the toast view.
     */
    protected void onHide() {
        toastView.setVisibility(Component.HIDE);
        toastMessage = null;
    }

    /**
     * {@inheritDoc}
     */
    public void onSaveAbilityState(PacMap outState) {
        outState.putString(TOAST_MSG, toastMessage);
    }

    /**
     * {@inheritDoc}
     */
    public void onRestoreAbilityState(PacMap inState) {
        if (inState != null) {
            toastMessage = inState.getString(TOAST_MSG);

            if (!TextUtils.isEmpty(toastMessage)) {
                showToast(true, toastMessage);
            }
        }
    }

    /**
     * Hides the toast after delay.
     */
    private final Runnable hideRunnable = () -> hideToast(false);
}
