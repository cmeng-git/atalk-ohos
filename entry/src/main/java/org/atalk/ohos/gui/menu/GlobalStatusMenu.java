package org.atalk.ohos.gui.menu;

import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.DependentLayout.LayoutConfig;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.ScrollView;
import ohos.agp.components.Text;
import ohos.agp.utils.Color;
import ohos.agp.utils.LayoutAlignment;
import ohos.agp.utils.Rect;
import ohos.agp.window.dialog.BaseDialog;
import ohos.agp.window.dialog.IDialog;
import ohos.agp.window.dialog.PopupDialog;
import ohos.app.Context;
import ohos.media.image.PixelMap;

import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.event.ProviderPresenceStatusChangeEvent;
import net.java.sip.communicator.service.protocol.event.ProviderPresenceStatusListener;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService;
import net.java.sip.communicator.service.protocol.jabberconstants.JabberStatusEnum;
import net.java.sip.communicator.util.ServiceUtils;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.account.StatusListProvider;
import org.atalk.ohos.gui.widgets.ActionMenuItem;
import org.atalk.ohos.util.AppImageUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import timber.log.Timber;

public class GlobalStatusMenu implements ServiceListener, ProviderPresenceStatusListener {
    private final Context mContext;
    private final LayoutScatter mInflater;
    private final PopupDialog mWindow;
    private final Component mRootView;
    private boolean mDidAction;
    private int mInsertPos;
    private int mChildPos;
    private final ComponentContainer mTrack;
    private final ScrollView mScroller;
    private OnActionItemClickListener mItemClickListener;
    private OnDismissListener mDismissListener;
    private int rootWidth = 0;
    private final GlobalStatusService globalStatus;

    private final List<ActionMenuItem> actionItems = new ArrayList<>();
    private static final Map<ProtocolProviderService, Component> accountSpinner = new HashMap<>();

    public GlobalStatusMenu(Context context, Component component) {
        mContext = context;
        mInflater = LayoutScatter.getInstance(mContext);

        mRootView = mInflater.parse(ResourceTable.Layout_status_menu, null, false);
        mRootView.setLayoutConfig(new LayoutConfig(LayoutConfig.MATCH_CONTENT, LayoutConfig.MATCH_CONTENT));

        mTrack = mRootView.findComponentById(ResourceTable.Id_tracks);
        mScroller = mRootView.findComponentById(ResourceTable.Id_scroller);

        mWindow = new PopupDialog(context, component);
        mWindow.setSize(LayoutConfig.MATCH_CONTENT, LayoutConfig.MATCH_CONTENT);
        mWindow.setBackColor(Color.TRANSPARENT);
        mWindow.setAutoClosable(true);
        mWindow.setHasArrow(true);
        mWindow.setCustomComponent(mRootView);
        mWindow.registerRemoveCallback(removeCallback);

        mChildPos = 0;
        globalStatus = ServiceUtils.getService(AppGUIActivator.bundleContext, GlobalStatusService.class);

        // start listening for newly register or removed protocol providers
        // cmeng: bundleContext can be null from field ??? can have problem in status update when blocked
        // This happens when Ability is recreated by the system after OSGi service has been
        // killed (and the whole process)
        if (AppGUIActivator.bundleContext == null) {
            Timber.e("OSGi service probably not initialized");
            return;
        }
        AppGUIActivator.bundleContext.addServiceListener(this);
    }

    BaseDialog.RemoveCallback removeCallback = new BaseDialog.RemoveCallback() {
        @Override
        public void onRemove(IDialog iDialog) {
            if (!mDidAction && mDismissListener != null) {
                mDismissListener.onDismiss();
            }
        }
    };

    /**
     * Set listener for action item clicked.
     *
     * @param listener Listener
     */
    public void setOnActionItemClickListener(OnActionItemClickListener listener) {
        mItemClickListener = listener;
    }

    /**
     * Set listener for window dismissed. This listener will only be fired if the quick action
     * dialog is dismissed by clicking outside the dialog or clicking on sticky item.
     */
    public void setOnDismissListener(GlobalStatusMenu.OnDismissListener listener) {
        mDismissListener = listener;
    }

    /**
     * Listener for item click
     */
    public interface OnActionItemClickListener {
        void onItemClick(GlobalStatusMenu source, int pos, int actionId);
    }

    /**
     * Listener for window dismiss
     */
    public interface OnDismissListener {
        void onDismiss();
    }

    /**
     * Add action item
     *
     * @param action {@link ActionMenuItem}
     */
    public void addActionItem(ActionMenuItem action) {
        actionItems.add(action);
        String title = action.getTitle();
        PixelMap icon = action.getIcon();
        Component container = mInflater.parse(ResourceTable.Layout_status_menu_item, null, false);

        Image img = container.findComponentById(ResourceTable.Id_iv_icon);
        final Text text = container.findComponentById(ResourceTable.Id_tv_title);

        if (icon != null)
            img.setPixelMap(icon);
        else
            img.setVisibility(Component.HIDE);

        if (title != null)
            text.setText(title);
        else
            text.setVisibility(Component.HIDE);

        final int pos = mChildPos;
        final int actionId = action.getActionId();

        container.setClickedListener(v -> {
            if (mItemClickListener != null)
                mItemClickListener.onItemClick(GlobalStatusMenu.this, pos, actionId);

            if (!actionItems.get(pos).isSticky()) {
                mDidAction = true;
                mWindow.destroy();
            }
        });

        container.setFocusable(Component.FOCUS_ENABLE);
        container.setClickable(true);
        mTrack.addComponent(container, mInsertPos);
        mChildPos++;
        mInsertPos++;
    }

    /**
     * Add action item for protocolProvider
     *
     * @param action {@link ActionMenuItem}
     */
    public void addActionItem(ActionMenuItem action, final ProtocolProviderService pps) {
        actionItems.add(action);
        String title = action.getTitle();
        PixelMap icon = action.getIcon();
        Component statusComponent = mInflater.parse(ResourceTable.Layout_status_menu_item_spinner, null, false);

        Image img = statusComponent.findComponentById(ResourceTable.Id_iv_icon);
        Text text = statusComponent.findComponentById(ResourceTable.Id_tv_title);
        accountSpinner.put(pps, statusComponent);

        if (icon != null)
            img.setPixelMap(icon);
        else
            img.setVisibility(Component.HIDE);

        if (title != null)
            text.setText(title);
        else
            text.setVisibility(Component.HIDE);

        // WindowManager$BadTokenException
        final OperationSetPresence accountPresence = pps.getOperationSet(OperationSetPresence.class);
        List<PresenceStatus> presenceStatuses = accountPresence.getSupportedStatusSet();

        // Create spinner with presence status list for the given pps
        // Note: xml layout has forced to use Picker.MODE_DIALOG, other Note-5 crashes when use MODE_DROPDOWN
        final ListContainer statusSpinner = statusComponent.findComponentById(ResourceTable.Id_presenceSpinner);
        StatusListProvider statusAdapter
                = new StatusListProvider(mContext, ResourceTable.Layout_account_presence_status_row, presenceStatuses);
        // statusAdapter.setDropDownViewResource(ResourceTable.Layout_simple_spinner_dropdown_item);
        statusSpinner.setItemProvider(statusAdapter);

        // Default state to offline
        PresenceStatus offline = accountPresence.getPresenceStatus(JabberStatusEnum.OFFLINE);
        statusSpinner.setSelectedItemIndex(presenceStatuses.indexOf(offline));

        // Setup adapter listener for onItemSelected
        statusSpinner.setItemSelectedListener(new ListContainer.ItemSelectedListener() {
            @Override
            public void onItemSelected(ListContainer listContainer, Component component, int pos, long id) {


                final PresenceStatus selectedStatus = (PresenceStatus) statusSpinner.getComponentAt(pos);
                final String statusMessage = selectedStatus.getStatusName();

                new Thread(() -> {
                    // Try to publish selected status
                    try {
                        // cmeng: set state to false to force it to execute offline->online
                        if (globalStatus != null) {
                            globalStatus.publishStatus(pps, selectedStatus, false);
                        }
                        if (pps.isRegistered()) {
                            accountPresence.publishPresenceStatus(selectedStatus, statusMessage);
                        }
                    } catch (Exception e) {
                        Timber.e(e, "Account presence publish error.");
                    }
                }).start();
            }
        });

        statusComponent.setFocusable(Component.FOCUS_ENABLE);
        statusComponent.setClickable(true);

        OperationSetPresence presenceOpSet = pps.getOperationSet(OperationSetPresence.class);
        presenceOpSet.addProviderPresenceStatusListener(this);

        mTrack.addComponent(statusComponent, mInsertPos);
        mChildPos++;
        mInsertPos++;
    }

    /**
     * Show quick action popup. Popup is automatically positioned, on top or bottom of anchor view.
     */
    public void show(Component anchor) {
        int xPos, yPos;
        mDidAction = false;
        int[] location = anchor.getLocationOnScreen();
        Rect anchorRect = new Rect(location[0], location[1],
                location[0] + anchor.getWidth(), location[1] + anchor.getHeight());

        // mRootView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mRootView.setLayoutConfig(new LayoutConfig(LayoutConfig.MATCH_CONTENT, LayoutConfig.MATCH_CONTENT));
        int rootHeight = mRootView.getEstimatedHeight();
        if (rootWidth == 0) {
            rootWidth = mRootView.getEstimatedWidth();
        }

        Dimension screenSize = aTalkApp.mDisplaySize;
        // automatically get X coord of popup (top left)
        if ((anchorRect.getWidth() + rootWidth) > screenSize.getWidth()) {
            xPos = anchor.getMinWidth() - (rootWidth - anchor.getWidth());
            xPos = Math.max(xPos, 0);

        }
        else {
            if (anchor.getWidth() > rootWidth)
                xPos = anchorRect.getCenterX() - (rootWidth / 2);
            else
                xPos = anchorRect.left;
        }

        int dyTop = anchorRect.top;
        int dyBottom = screenSize.height - anchorRect.bottom;
        boolean onTop = dyTop > dyBottom;
        if (onTop) {
            if (rootHeight > dyTop) {
                yPos = 15;
                LayoutConfig l = (LayoutConfig) mScroller.getLayoutConfig();
                l.height = dyTop - anchor.getHeight();
            }
            else {
                yPos = anchor.getMinHeight() - rootHeight;
            }
        }
        else {
            yPos = anchorRect.bottom;
            if (rootHeight > dyBottom) {
                LayoutConfig l = (LayoutConfig) mScroller.getLayoutConfig();
                l.height = dyBottom;
            }
        }

        mWindow.showOnCertainPosition(LayoutAlignment.UNSET, xPos, yPos);
        mWindow.show();
    }

    @Override
    public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt) {
        ProtocolProviderService pps = evt.getProvider();
        // Timber.w("### PPS presence status change: " + pps + " => " + evt.getNewStatus());

        // do not proceed if spinnerContainer is null
        Component statusComponent = accountSpinner.get(pps);
        if (statusComponent == null) {
            Timber.e("No presence status spinner setup for: %s", pps);
            return;
        }

        final PresenceStatus presenceStatus = evt.getNewStatus();
        final ListContainer statusSpinner = statusComponent.findComponentById(ResourceTable.Id_presenceSpinner);
        final StatusListProvider statusAdapter = (StatusListProvider) statusSpinner.getItemProvider();

        BaseAbility.runOnUiThread(() ->
                statusSpinner.setSelectedItemIndex(statusAdapter.getItemPosition(presenceStatus)));
    }

    @Override
    public void providerStatusMessageChanged(PropertyChangeEvent evt) {
        // Timber.w("### PPS Status message change: " + evt.getSource() + " => " + evt.getNewValue());
    }

    /**
     * Implements the <code>ServiceListener</code> method. Verifies whether the received event concerning
     * a <code>ProtocolProviderService</code> and take the necessary action.
     *
     * @param event The <code>ServiceEvent</code> object.
     */
    public void serviceChanged(ServiceEvent event) {
        // if the event is caused by a bundle being stopped, we don't want to know
        ServiceReference<?> serviceRef = event.getServiceReference();
        if (serviceRef.getBundle().getState() == Bundle.STOPPING)
            return;

        // bundleContext == null on exit
        BundleContext bundleContext = AppGUIActivator.bundleContext;
        if (bundleContext == null)
            return;
        Object service = bundleContext.getService(serviceRef);

        // we don't care if the source service is not a protocol provider
        if (service instanceof ProtocolProviderService) {
            // Timber.w("## ProtocolServiceProvider Add or Remove: " + event.getType());
            ProtocolProviderService pps = (ProtocolProviderService) service;

            switch (event.getType()) {
                case ServiceEvent.REGISTERED:
                    addMenuItemPPS(pps);
                    break;
                case ServiceEvent.UNREGISTERING:
                    removeMenuItemPPS(pps);
                    break;
            }
        }
    }

    /**
     * When the PPS is being registered i.e. enabled on account list
     * 1. Create a new entry in the status menu
     *
     * @param pps new provider to be added to the status menu
     */
    private void addMenuItemPPS(ProtocolProviderService pps) {
        if ((!accountSpinner.containsKey(pps))) {
            AccountID accountId = pps.getAccountID();
            String userJid = accountId.getAccountJid();
            PixelMap icon = AppImageUtil.getPixelMap(mContext, ResourceTable.Media_jabber_status_online);
            ActionMenuItem actionItem = new ActionMenuItem(mChildPos++, userJid, icon);
            addActionItem(actionItem, pps);
        }
    }

    /**
     * When a pps is unregister i.e. disabled on account list:
     * 1. Remove ProviderPresenceStatusListener for this pps
     * 2. Remove the spinner view from the status menu
     * 3. Remove entry in the accountSpinner
     * 4. Readjust all the required pointer
     *
     * @param pps provider to be removed
     */
    private void removeMenuItemPPS(ProtocolProviderService pps) {
        if (accountSpinner.containsKey(pps)) {
            OperationSetPresence presenceOpSet = pps.getOperationSet(OperationSetPresence.class);
            presenceOpSet.removeProviderPresenceStatusListener(this);

            Component statusComponent = accountSpinner.get(pps);
            statusComponent.getComponentParent().removeComponent(statusComponent);

            accountSpinner.remove(pps);
            mChildPos--;
            mInsertPos--;
        }
    }
}
