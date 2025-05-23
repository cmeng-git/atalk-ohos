/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.account;

import net.java.sip.communicator.impl.configuration.ConfigurationActivator;
import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.util.CollectionProvider;
import org.atalk.ohos.gui.util.event.EventListener;
import org.atalk.service.osgi.OSGiAbility;
import org.jivesoftware.smackx.pubsub.provider.ItemProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

import java.util.ArrayList;
import java.util.Collection;

import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.app.Context;
import ohos.media.image.PixelMap;

import timber.log.Timber;

/**
 * This is a convenience class which implements an {@link ItemProvider} interface to put the list of
 * {@link Account}s into Android widgets.
 * <p>
 * The {@link Component}s for each row are created from the layout resource id given in constructor.
 * This view should contain: <br/>
 * - <code>ResourceTable.Id_accountName</code> for the account name text ({@link Text}) <br/>
 * - <code>ResourceTable.Id_accountProtoIcon</code> for the protocol icon of type ({@link PixelMap}) <br/>
 * - <code>ResourceTable.Id_accountStatusIcon</code> for the presence status icon ({@link PixelMap}) <br/>
 * - <code>ResourceTable.Id_accountStatus</code> for the presence status name ({@link Text}) <br/>
 * It implements {@link EventListener} to refresh the list on any changes to the {@link Account}.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class AccountsListProvider extends CollectionProvider<Account>
        implements EventListener<AccountEvent>, ServiceListener
{
    /**
     * The {@link Component} resources ID describing list's row
     */
    private final int listRowResourceID;

    /**
     * The {@link ohos.agp.components.Component} resources ID describing list's row
     */
    private final int dropDownRowResourceID;

    /**
     * The {@link BundleContext} of parent {@link OSGiAbility}
     */
    private final BundleContext bundleContext;
    /**
     * The flag indicates whether disabled accounts should be filtered out from the list
     */
    private final boolean filterDisabledAccounts;

    /**
     * Creates new instance of {@link AccountsListProvider}
     *
     * @param context the {@link ohos.aafwk.ability.Ability} running this adapter
     * @param accounts collection of accounts that will be displayed
     * @param listRowResourceID the layout resource ID see {@link AccountsListProvider} for detailed description
     * @param filterDisabledAccounts flag indicates if disabled accounts should be filtered out from the list
     */
    public AccountsListProvider(Context context, int listRowResourceID, int dropDownRowResourceID,
            Collection<AccountID> accounts, boolean filterDisabledAccounts)
    {
        super(context);
        this.filterDisabledAccounts = filterDisabledAccounts;

        this.listRowResourceID = listRowResourceID;
        this.dropDownRowResourceID = dropDownRowResourceID;

        this.bundleContext = ConfigurationActivator.bundleContext;
        bundleContext.addServiceListener(this);
        initAccounts(accounts);
    }

    /**
     * Initialize the list and filters out disabled accounts if necessary.
     *
     * @param collection set of {@link AccountID} that will be displayed
     */
    private void initAccounts(Collection<AccountID> collection)
    {
        ArrayList<Account> accounts = new ArrayList<>();
        for (AccountID acc : collection) {
            Account account = new Account(acc, bundleContext, getContext());
            if (filterDisabledAccounts && !account.isEnabled())
                continue;

            // Skip hidden accounts
            if (acc.isHidden())
                continue;

            account.addAccountEventListener(this);
            accounts.add(account);
        }
        setList(accounts);
    }

    public void serviceChanged(ServiceEvent event)
    {
        // if the event is caused by a bundle being stopped, we don't want to know
        if (event.getServiceReference().getBundle().getState() == Bundle.STOPPING) {
            return;
        }

        // we don't care if the source service is not a protocol provider
        Object sourceService = bundleContext.getService(event.getServiceReference());
        if (!(sourceService instanceof ProtocolProviderService)) {
            return;
        }

        ProtocolProviderService protocolProvider = (ProtocolProviderService) sourceService;

        // Add or remove the protocol provider from our accounts list.
        if (event.getType() == ServiceEvent.REGISTERED) {
            Account acc = findAccountID(protocolProvider.getAccountID());
            if (acc == null) {
                addAccount(new Account(protocolProvider.getAccountID(), bundleContext, getContext().getContext()));
            }
            // Register for account events listener if account exists on this list
            else {
                acc.addAccountEventListener(this);
            }
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING) {
            Account account = findAccountID(protocolProvider.getAccountID());
            // Remove enabled account if exist
            if (account != null && account.isEnabled()) {
                removeAccount(account);
            }
        }
    }

    /**
     * Unregisters status update listeners for accounts
     */
    void deinitStatusListeners()
    {
        for (int accIdx = 0; accIdx < getCount(); accIdx++) {
            Account account = getObject(accIdx);
            account.destroy();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Component getComponent(boolean isDropDown, Account account, ComponentContainer parent, LayoutScatter inflater)
    {
        int rowResID = listRowResourceID;

        if (isDropDown && dropDownRowResourceID != -1) {
            rowResID = dropDownRowResourceID;
        }

        Component statusItem = inflater.parse(rowResID, parent, false);
        Text accountName = statusItem.findComponentById(ResourceTable.Id_protocolProvider);
        Image accountProtocol = statusItem.findComponentById(ResourceTable.Id_accountProtoIcon);
        Image statusIconView = statusItem.findComponentById(ResourceTable.Id_accountStatusIcon);
        Text accountStatus = statusItem.findComponentById(ResourceTable.Id_accountStatus);

        // Sets account's properties
        if (accountName != null)
            accountName.setText(account.getAccountName());

        if (accountProtocol != null) {
            PixelMap protoIcon = account.getProtocolIcon();
            if (protoIcon != null) {
                accountProtocol.setPixelMap(protoIcon);
            }
        }

        if (accountStatus != null)
            accountStatus.setText(account.getStatusName());

        if (statusIconView != null) {
            PixelMap statusIcon = account.getStatusIcon();
            if (statusIcon != null) {
                statusIconView.setPixelMap(statusIcon);
            }
        }
        return statusItem;
    }

    /**
     * Check if given <code>account</code> exists on the list
     *
     * @param account {@link AccountID} that has to be found on the list
     * @return <code>true</code> if account is on the list
     */
    private Account findAccountID(AccountID account)
    {
        for (int i = 0; i < getCount(); i++) {
            Account acc = getObject(i);
            if (acc.getAccountID().equals(account))
                return acc;
        }
        return null;
    }

    /**
     * Adds new account to the list
     *
     * @param account {@link Account} that will be added to the list
     */
    private void addAccount(Account account)
    {
        if (filterDisabledAccounts && !account.isEnabled())
            return;

        if (account.getAccountID().isHidden())
            return;

        Timber.d("Account added: %s", account.getUserID());
        add(account);
        account.addAccountEventListener(this);
    }

    /**
     * Removes the account from the list
     *
     * @param account the {@link Account} that will be removed from the list
     */
    private void removeAccount(Account account)
    {
        if (account != null) {
            Timber.d("Account removed: %s", account.getUserID());
            account.removeAccountEventListener(this);
            remove(account);
        }
    }

    /**
     * Does refresh the list
     *
     * @param accountEvent the {@link AccountEvent} that caused the change event
     */
    public void onChangeEvent(AccountEvent accountEvent)
    {
        // Timber.log(TimberLog.FINE, "Not an Error! Received accountEvent update for: "
        //		+ accountEvent.getSource().getAccountName() + " " + accountEvent.toString(), new Throwable());
        doRefreshList();
    }
}
