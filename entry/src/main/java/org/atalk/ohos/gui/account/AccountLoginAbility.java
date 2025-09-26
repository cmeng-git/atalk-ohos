/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.account;

import java.util.Map;

import ohos.aafwk.content.Intent;

import net.java.sip.communicator.service.gui.AccountRegistrationWizard;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.ohos.gui.menu.ExitMenuAbility;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import timber.log.Timber;

/**
 * The <code>AccountLoginAbility</code> is the activity responsible for creating or
 * registration a new account on the server.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class AccountLoginAbility extends ExitMenuAbility
        implements AccountLoginSlice.AccountLoginListener
{
    /**
     * The username property name.
     */
    public static final String USERNAME = "Username";

    /**
     * The password property name.
     */
    public static final String PASSWORD = "Password";

    /**
     * Called when the activity is starting. Initializes the corresponding call interface.
     *
     * @param intent transfers information between objects; consists of the operation and parameters attributes
     */
    @Override
    protected void onStart(Intent intent)
    {
        super.onStart(intent);

        // If we have instance state it means the fragment is already created
        if (mInState == null) {
            // Create AccountLoginFragment fragment
            String login = getIntent().getStringExtra(USERNAME);
            String password = getIntent().getStringExtra(PASSWORD);
            AccountLoginFragment accountLogin = AccountLoginSlice.createInstance(login, password);
            getSupportFragmentManager().beginTransaction().add(android.ResourceTable.Id_content, accountLogin).commit();
        }
    }

    /**
     * Create an new account database with the given <code>userName</code>, <code>password</code>
     * and <code>protocolName</code>.
     *
     * @param userName the username of the account
     * @param password the password of the account
     * @param protocolName the name of the protocol
     * @return the <code>ProtocolProviderService</code> corresponding to the newly signed in account
     */
    private ProtocolProviderService createAccount(String userName, String password,
            String protocolName, Map<String, String> accountProperties)
    {
        BundleContext bundleContext = getBundleContext();
        // Find all the available AccountRegistrationWizard that the system has implemented
        ServiceReference<?>[] accountWizardRefs = null;
        try {
            accountWizardRefs = bundleContext.getServiceReferences(AccountRegistrationWizard.class.getName(), null);
        } catch (InvalidSyntaxException ex) {
            // this shouldn't happen since we have provided all parameter string
            Timber.e(ex, "Error while retrieving service refs");
        }

        // in case we found none, then exit.
        if (accountWizardRefs == null) {
            Timber.e("No registered account registration wizards found");
            return null;
        }

        Timber.d("Found %s already installed providers.", accountWizardRefs.length);

        // Get the user selected AccountRegistrationWizard for account registration
        AccountRegistrationWizard selectedWizard = null;
        for (ServiceReference<?> accountWizardRef : accountWizardRefs) {
            AccountRegistrationWizard accReg = (AccountRegistrationWizard) bundleContext.getService(accountWizardRef);
            if (accReg.getProtocolName().equals(protocolName)) {
                selectedWizard = accReg;
                break;
            }
        }
        if (selectedWizard == null) {
            Timber.w("No account registration wizard found for protocol name: %s", protocolName);
            return null;
        }
        try {
            selectedWizard.setModification(false);
            return selectedWizard.signin(userName, password, accountProperties);
        } catch (OperationFailedException e) {
            Timber.e(e, "Account creation operation failed.");

            switch (e.getErrorCode()) {
                case OperationFailedException.ILLEGAL_ARGUMENT:
                    DialogH.getInstance(this).showDialog(this, ResourceTable.String_login_failed,
                            ResourceTable.String_username_password_null);
                    break;
                case OperationFailedException.IDENTIFICATION_CONFLICT:
                    DialogH.getInstance(this).showDialog(this, ResourceTable.String_login_failed,
                            ResourceTable.String_user_exist_error);
                    break;
                case OperationFailedException.SERVER_NOT_SPECIFIED:
                    DialogH.getInstance(this).showDialog(this, ResourceTable.String_login_failed,
                            ResourceTable.String_server_info_not_complete);
                    break;
                default:
                    DialogH.getInstance(this).showDialog(this, ResourceTable.String_login_failed,
                            ResourceTable.String_account_create_failed, e.getMessage());
            }
        } catch (Exception e) {
            Timber.e(e, "Exception while adding account: %s", e.getMessage());
            DialogH.getInstance(this).showDialog(this, ResourceTable.String_error,
                    ResourceTable.String_account_create_failed, e.getMessage());
        }
        return null;
    }

    /**
     * See {@link AccountLoginFragment.AccountLoginListener#onLoginPerformed}
     * Not used causing problem in API-34 release, So disable it.
     */
    @Override
    public void onLoginPerformed(String userName, String password, String network, Map<String, String> accountProperties)
    {
        ProtocolProviderService pps = createAccount(userName, password, network, accountProperties);
        if (pps != null) {
            Intent intent = new Intent();
            startAbility(new Intent(this, aTalk.class));
            terminateAbility();
        }
    }
}