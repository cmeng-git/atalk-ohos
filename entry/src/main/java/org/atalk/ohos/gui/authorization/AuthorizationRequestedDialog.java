/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.authorization;

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Button;
import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.Picker;
import ohos.app.Context;

import net.java.sip.communicator.service.protocol.AuthorizationResponse;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.contactlist.MetaContactGroupProvider;
import org.atalk.ohos.util.ComponentUtil;

/**
 * The dialog is displayed when someone wants to add us to his contact list and the authorization
 * is required.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class AuthorizationRequestedDialog extends BaseAbility {
    /**
     * Request id managed by <code>AuthorizationHandlerImpl</code>.
     */
    private static final String EXTRA_REQUEST_ID = "request_id";

    /**
     * Request holder object.
     */
    AuthorizationHandlerImpl.AuthorizationRequestedHolder request;

    /**
     * Ignore request by default
     */
    AuthorizationResponse.AuthorizationResponseCode responseCode = AuthorizationResponse.IGNORE;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_authorization_requested);

        long requestId = intent.getLongParam(EXTRA_REQUEST_ID, -1);
        if (requestId == -1)
            throw new IllegalArgumentException();

        this.request = AuthorizationHandlerImpl.getRequest(requestId);
        String contactId = request.contact.getAddress();
        Component content = findComponentById(ResourceTable.Id_content);
        ComponentUtil.setTextViewValue(content, ResourceTable.Id_requestInfo,
                getString(ResourceTable.String_authorization_request_info, contactId));

        ComponentUtil.setTextViewValue(content, ResourceTable.Id_addToContacts,
                getString(ResourceTable.String_add_contact_to_list, contactId));

        Picker contactGroupSpinner = findComponentById(ResourceTable.Id_selectGroupSpinner);
        contactGroupSpinner.setItemProvider(new MetaContactGroupProvider(this, ResourceTable.Id_selectGroupSpinner,
                true, true));

        Checkbox addToContactsCb = findComponentById(ResourceTable.Id_addToContacts);
        addToContactsCb.setCheckedStateChangedListener((buttonView, isChecked)
                -> updateAddToContactsStatus(isChecked));

        // Prevents from closing the dialog on outside touch
        setFinishOnTouchOutside(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActive() {
        super.onActive();

        // Update add to contacts status
        updateAddToContactsStatus(ComponentUtil.isCompoundChecked(getContentView(), ResourceTable.Id_addToContacts));
    }

    @Override
    protected void onBackPressed() {
        // Prevent Back Key from closing the dialog
        return;
    }

    /**
     * Updates select group spinner status based on add to contact list checkbox state.
     *
     * @param isChecked <code>true</code> if "add to contacts" checkbox is checked.
     */
    private void updateAddToContactsStatus(boolean isChecked) {
        ComponentUtil.ensureEnabled(getContentView(), ResourceTable.Id_selectGroupSpinner, isChecked);
    }

    /**
     * Method fired when user accept the request.
     *
     * @param v the button's <code>Component.</code>
     */
    @SuppressWarnings("unused")
    public void onAcceptClicked(Component v) {
        responseCode = AuthorizationResponse.ACCEPT;
        terminateAbility();
    }

    /**
     * Method fired when reject button is clicked.
     *
     * @param v the button's <code>Component</code>
     */
    @SuppressWarnings("unused")
    public void onRejectClicked(Component v) {
        responseCode = AuthorizationResponse.REJECT;
        terminateAbility();
    }

    /**
     * Method fired when ignore button is clicked.
     *
     * @param v the button's <code>Component.</code>
     */
    @SuppressWarnings("unused")
    public void onIgnoreClicked(Component v) {
        terminateAbility();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStop() {
        super.onStop();

        // cmeng - Handle in OperationSetPersistentPresenceJabberImpl#handleSubscribeReceived
//		if (ComponentUtil.isCompoundChecked(getContentView(), ResourceTable.Id_addToContacts)
//				&& responseCode.equals(AuthorizationResponse.ACCEPT)) {
//			// Add to contacts
//			Picker groupSpinner = findComponentById(ResourceTable.Id_selectGroupSpinner);
//			ContactListUtils.addContact(request.contact.getProtocolProvider(),
//					(MetaContactGroup) groupSpinner.getSelectedItem(), request.contact.getAddress());
//		}
        request.notifyResponseReceived(responseCode);
    }

    /**
     * Shows <code>AuthorizationRequestedDialog</code> for the request with given <code>id</code>.
     *
     * @param id request identifier for which new dialog will be displayed.
     */
    public static void showDialog(Long id) {
        Context ctx = aTalkApp.getInstance();
        Intent showIntent = new Intent();
        Operation operation =
                new Intent.OperationBuilder()
                        .withDeviceId("")
                        .withBundleName(ctx.getBundleName())
                        .withAbilityName(AuthorizationRequestedDialog.class.getName())
                        .build();
        showIntent.setOperation(operation);
        showIntent.setParam(EXTRA_REQUEST_ID, id);
        ctx.startAbility(showIntent, 0);
    }
}
