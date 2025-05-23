/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.authorization;

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Component;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.util.ComponentUtil;

/**
 * This dialog is displayed in order to prepare the authorization request that has to be sent to
 * the user we want to include in our contact list.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class RequestAuthorizationDialog extends BaseAbility {
    /**
     * Request identifier extra key.
     */
    private static final String EXTRA_REQUEST_ID = "request_id";

    /**
     * The request holder.
     */
    private AuthorizationHandlerImpl.AuthorizationRequestedHolder request;

    /**
     * Flag stores the discard state.
     */
    private boolean discard;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_request_authorization);
        long requestId = intent.getLongParam(EXTRA_REQUEST_ID, -1);
        if (requestId == -1)
            throw new IllegalArgumentException();

        this.request = AuthorizationHandlerImpl.getRequest(requestId);
        String userID = request.contact.getProtocolProvider().getAccountID().getUserID();
        String contactId = request.contact.getAddress();

        ComponentUtil.setTextViewValue(getContentView(), ResourceTable.Id_requestInfo,
                getString(ResourceTable.String_request_authorization_prompt, userID, contactId));

        // Prevents from closing the dialog on outside touch
        setFinishOnTouchOutside(false);
    }

    @Override
    protected void onBackPressed() {
        // Prevent Back Key from closing the dialog
        return;
    }

    /**
     * Method fired when the request button is clicked.
     *
     * @param v the button's <code>Component.</code>
     */
    public void onRequestClicked(Component v) {
        String requestText = ComponentUtil.getTextViewValue(getContentView(), ResourceTable.Id_requestText);
        request.submit(requestText);
        discard = false;
        terminateAbility();
    }

    /**
     * Method fired when the cancel button is clicked.
     *
     * @param v the button's <code>Component</code>
     */
    public void onCancelClicked(Component v) {
        discard = true;
        terminateAbility();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStop() {
        if (discard)
            request.discard();
        super.onStop();
    }

    /**
     * Creates the <code>Intent</code> to start <code>RequestAuthorizationDialog</code> parametrized with
     * given <code>requestId</code>.
     *
     * @param requestId the id of authentication request.
     *
     * @return <code>Intent</code> that start <code>RequestAuthorizationDialog</code> parametrized with given request
     * id.
     */
    public static Intent getRequestAuthDialogIntent(long requestId) {
        Intent intent = new Intent();
        Operation operation = new Intent.OperationBuilder()
                .withDeviceId("")
                .withBundleName(aTalkApp.getInstance().getBundleName())
                .withAbilityName(RequestAuthorizationDialog.class)
                .build();
        intent.setOperation(operation);
        intent.setParam(EXTRA_REQUEST_ID, requestId);
        return intent;
    }
}
