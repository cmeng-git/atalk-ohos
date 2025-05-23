/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.account;

import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.RegistrationState;
import net.java.sip.communicator.service.protocol.SecurityAuthority;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeEvent;
import net.java.sip.communicator.service.protocol.event.RegistrationStateChangeListener;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusEnum;
import net.java.sip.communicator.util.UtilActivator;

import org.atalk.impl.timberlog.TimberLog;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import timber.log.Timber;

/**
 * The <code>LoginManager</code> manages the login operation. Here we obtain the
 * <code>ProtocolProviderFactory</code>, we make the account installation and we handle all events
 * related to the registration state.
 * <p/>
 * The <code>LoginManager</code> is the one that opens one or more <code>LoginWindow</code>s for each
 * <code>ProtocolProviderFactory</code>. The <code>LoginWindow</code> is where user could enter an
 * identifier and password.
 * <p/>
 * Note that the behavior of this class will be changed when the Configuration Service is ready.
 *
 * @author Yana Stamcheva
 * @author Eng Chong Meng
 */
public class LoginManager implements ServiceListener, RegistrationStateChangeListener //,AccountManagerListener
{
    private boolean manuallyDisconnected = false;
    private final LoginRenderer loginRenderer;

    /**
     * Creates an instance of the <code>LoginManager</code>, by specifying the main application window.
     *
     * @param loginRenderer the main application window
     */
    public LoginManager(LoginRenderer loginRenderer) {
        this.loginRenderer = loginRenderer;
        UtilActivator.bundleContext.addServiceListener(this);
    }

    /**
     * Unregisters the given protocol provider.
     *
     * @param protocolProvider the ProtocolProviderService to unregister
     */
    public static void logoff(ProtocolProviderService protocolProvider) {
        new UnregisterProvider(protocolProvider).start();
    }

    /**
     * Registers the given protocol provider.
     *
     * @param protocolProvider the ProtocolProviderService to register.
     */
    public void login(ProtocolProviderService protocolProvider) {
        // Timber.log(TimberLog.FINER, "SMACK stack access: %s", Log.getStackTraceString(new Exception()));
        loginRenderer.startConnectingUI(protocolProvider);
        new RegisterProvider(protocolProvider, loginRenderer.getSecurityAuthorityImpl(protocolProvider)).start();
    }

    /**
     * Shows login window for each registered account.
     */
    public void runLogin() {
        for (ProtocolProviderFactory providerFactory : UtilActivator.getProtocolProviderFactories().values()) {
            addAccountsForProtocolProviderFactory(providerFactory);
        }
    }

    /**
     * Handles stored accounts for a protocol provider factory and add them to the UI and register
     * them if needed.
     *
     * @param providerFactory the factory to handle.
     */
    private void addAccountsForProtocolProviderFactory(ProtocolProviderFactory providerFactory) {
        for (AccountID accountID : providerFactory.getRegisteredAccounts()) {
            ServiceReference<ProtocolProviderService> serRef = providerFactory.getProviderForAccount(accountID);
            ProtocolProviderService protocolProvider = UtilActivator.bundleContext.getService(serRef);
            handleProviderAdded(protocolProvider);
        }
    }

    /**
     * The method is called by a ProtocolProvider implementation whenever a change in the
     * registration state of the corresponding provider has occurred.
     *
     * @param evt ProviderStatusChangeEvent the event describing the status change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt) {
        RegistrationState newState = evt.getNewState();
        ProtocolProviderService protocolProvider = evt.getProvider();

        if (TimberLog.isTraceEnable)
            Timber.log(TimberLog.FINER, "Protocol provider: %s changes state to: %s Reason: %s",
                    protocolProvider, evt.getNewState().getStateName(), evt.getReason());

        if (newState.equals(RegistrationState.REGISTERED)
                || newState.equals(RegistrationState.UNREGISTERED)
                || newState.equals(RegistrationState.EXPIRED)
                || newState.equals(RegistrationState.AUTHENTICATION_FAILED)
                || newState.equals(RegistrationState.CONNECTION_FAILED)
                || newState.equals(RegistrationState.CHALLENGED_FOR_AUTHENTICATION)) {
            loginRenderer.stopConnectingUI(protocolProvider);
        }

        if (newState.equals(RegistrationState.REGISTERED)) {
            loginRenderer.protocolProviderConnected(protocolProvider, System.currentTimeMillis());
        }
    }

    /**
     * Implements the <code>ServiceListener</code> method. Verifies whether the passed event
     * concerns a <code>ProtocolProviderService</code> and adds the corresponding UI controls.
     *
     * @param event The <code>ServiceEvent</code> object.
     */

    public void serviceChanged(ServiceEvent event) {
        ServiceReference<?> serviceRef = event.getServiceReference();

        // if the event is caused by a bundle being stopped, we don't want to know
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
            return;


        Object service = UtilActivator.bundleContext.getService(serviceRef);

        // we don't care if the source service is not a protocol provider
        if (!(service instanceof ProtocolProviderService))
            return;

        switch (event.getType()) {
            case ServiceEvent.REGISTERED:
                handleProviderAdded((ProtocolProviderService) service);
                break;
            case ServiceEvent.UNREGISTERING:
                handleProviderRemoved((ProtocolProviderService) service);
                break;
        }
    }

    /**
     * Adds all UI components (status selector box, etc) related to the given protocol provider.
     *
     * @param protocolProvider the <code>ProtocolProviderService</code>
     */
    private void handleProviderAdded(ProtocolProviderService protocolProvider) {
        Timber.log(TimberLog.FINER, "The following protocol provider was just added: "
                + protocolProvider.getAccountID().getAccountJid());

        synchronized (loginRenderer) {
            if (!loginRenderer.containsProtocolProviderUI(protocolProvider)) {
                protocolProvider.addRegistrationStateChangeListener(this);
                loginRenderer.addProtocolProviderUI(protocolProvider);
            }
            // we have already added this provider and scheduled a login if needed we've done our
            // work, if it fails or something else reconnect or other plugins will take care
            else
                return;
        }

        Object status = AccountStatusUtils.getProtocolProviderLastStatus(protocolProvider);
        if ((status == null)
                || status.equals(GlobalStatusEnum.ONLINE_STATUS)
                || ((status instanceof PresenceStatus)
                && (((PresenceStatus) status).getStatus() >= PresenceStatus.ONLINE_THRESHOLD))) {
            login(protocolProvider);
        }
    }

    /**
     * Removes all UI components related to the given protocol provider.
     *
     * @param protocolProvider the <code>ProtocolProviderService</code>
     */
    private void handleProviderRemoved(ProtocolProviderService protocolProvider) {
        loginRenderer.removeProtocolProviderUI(protocolProvider);
    }

    /**
     * Returns <code>true</code> to indicate the atalk has been manually disconnected, <code>false</code> - otherwise.
     *
     * @return <code>true</code> to indicate the atalk has been manually disconnected, <code>false</code> - otherwise
     */
    public boolean isManuallyDisconnected() {
        return manuallyDisconnected;
    }

    /**
     * Sets the manually disconnected property.
     *
     * @param manuallyDisconnected <code>true</code> to indicate the atalk has been manually
     * disconnected, <code>false</code> - otherwise
     */
    public void setManuallyDisconnected(boolean manuallyDisconnected) {
        this.manuallyDisconnected = manuallyDisconnected;
    }

    /**
     * Registers a protocol provider in a separate thread.
     */
    private class RegisterProvider extends Thread {
        private final ProtocolProviderService protocolProvider;
        private final SecurityAuthority secAuth;

        RegisterProvider(ProtocolProviderService protocolProvider, SecurityAuthority secAuth) {
            this.protocolProvider = protocolProvider;
            this.secAuth = secAuth;

//			Timber.log(TimberLog.FINER, new Exception("Not an error! Just tracing for provider registering."),
//	    		"Registering provider: %s", protocolProvider.getAccountID().getAccountJid());
        }

        /**
         * Registers the contained protocol provider.
         * # Process all possible errors that may occur during the registration process.
         * # This is now handled with pps registration process
         */
        @Override
        public void run() {
            try {
                protocolProvider.register(secAuth);
            } catch (OperationFailedException ex) {
                handleOperationFailedException(ex);
            } catch (Throwable ex) {
                // cmeng: all exceptions will be handled within pps
                Timber.e(ex, "Failed to register protocol provider. ");
            }
        }

        private void handleOperationFailedException(OperationFailedException ex) {
            Timber.e(ex, "Provider failed to register with: ");
            if (OperationFailedException.NETWORK_FAILURE == ex.getErrorCode()) {
                loginRenderer.protocolProviderConnectionFailed(protocolProvider, LoginManager.this);
            }
        }
    }

    /**
     * Unregisters a protocol provider in a separate thread.
     */
    private static class UnregisterProvider extends Thread {
        ProtocolProviderService protocolProvider;

        UnregisterProvider(ProtocolProviderService protocolProvider) {
            this.protocolProvider = protocolProvider;
        }

        /**
         * Unregisters the contained protocol provider and process all possible errors that may
         * occur during the un-registration process.
         */
        @Override
        public void run() {
            try {
                protocolProvider.unregister(true);
            } catch (OperationFailedException ex) {
                Timber.e("Provider failed unRegistration with error: %s", ex.getMessage());
                new DialogA.Builder(aTalkApp.getInstance())
                        .setTitle(ResourceTable.String_error)
                        .setContent(ResourceTable.String_logoff_failed,
                                protocolProvider.getAccountID().getUserID(),
                                protocolProvider.getAccountID().getService())
                        .create()
                        .show();
            }
        }
    }
}
