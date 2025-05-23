/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.impl.androidauthwindow;

import ohos.aafwk.content.Intent;
import ohos.agp.components.Component;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.util.ComponentUtil;

/**
 * Ability controls authentication dialog for <code>AuthenticationWindowService</code>.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class AuthWindowAbility extends BaseAbility
{
    /**
     * Request id key.
     */
    static final String REQUEST_ID_EXTRA = "request_id";

    /**
     * Authentication window instance
     */
    private AuthWindowImpl mAuthWindow;
    private Component contentView;

    /**
     * Changes will be stored only if flag is set to <code>false</code>.
     */
    private boolean cancelled = true;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent)
    {
        super.onStart(intent);
        long requestId = intent.getLongParam(REQUEST_ID_EXTRA, -1);
        if (requestId == -1)
            throw new IllegalArgumentException();

        // Content view
        setUIContent(ResourceTable.Layout_auth_window);
        contentView = findComponentById(ResourceTable.Id_content);

        // Server name
        mAuthWindow = AuthWindowServiceImpl.getAuthWindow(requestId);
        // NPE return from field
        if (mAuthWindow == null)
            return;

        String server = mAuthWindow.getServer();

        // Title
        String title = mAuthWindow.getWindowTitle();
        if (title == null) {
            title = getString(ResourceTable.String_authentication_title, server);
        }
        setMainTitle(title);

        // Message
        String message = mAuthWindow.getWindowText();
        if (message == null) {
            message = getString(ResourceTable.String_authentication_requested_by_server, server);
        }
        ComponentUtil.setTextViewValue(contentView, ResourceTable.Id_text, message);

        // Username label and field
        if (mAuthWindow.getUsernameLabel() != null)
            ComponentUtil.setTextViewValue(contentView, ResourceTable.Id_username_label, mAuthWindow.getUsernameLabel());

        if (mAuthWindow.getUserName() != null)
            ComponentUtil.setTextViewValue(contentView, ResourceTable.Id_username, mAuthWindow.getUserName());

        ComponentUtil.ensureEnabled(contentView, ResourceTable.Id_username, mAuthWindow.isUserNameEditable());

        // Password filed and label
        if (mAuthWindow.getPasswordLabel() != null)
            ComponentUtil.setTextViewValue(contentView, ResourceTable.Id_password_label, mAuthWindow.getPasswordLabel());

        ComponentUtil.setCompoundChecked(contentView, ResourceTable.Id_store_password, mAuthWindow.isRememberPassword());
        ComponentUtil.ensureVisible(contentView, ResourceTable.Id_store_password, mAuthWindow.isAllowSavePassword());
    }

    /**
     * Fired when the ok button is clicked.
     *
     * @param v ok button's <code>Component</code>
     */
    public void onOkClicked(Component v)
    {
        String userName = ComponentUtil.getTextViewValue(contentView, ResourceTable.Id_username);
        String password = ComponentUtil.getTextViewValue(contentView, ResourceTable.Id_password);
        if ((userName == null) || (password == null)) {
            aTalkApp.showToastMessage(ResourceTable.String_certconfig_incomplete);
        }
        else {
            cancelled = false;
            mAuthWindow.setUsername(userName);
            mAuthWindow.setPassword(password);
            mAuthWindow.setRememberPassword(ComponentUtil.isCompoundChecked(contentView, ResourceTable.Id_store_password));
            terminateAbility();
        }
    }

    /**
     * Fired when the cancel button is clicked.
     *
     * @param v cancel button's <code>Component</code>
     */
    public void onCancelClicked(Component v)
    {
        cancelled = true;
        terminateAbility();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStop()
    {
        mAuthWindow.setCanceled(cancelled);
        mAuthWindow.windowClosed();
        super.onStop();
    }
}
