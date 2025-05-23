/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.contactlist;


import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.Text;
import ohos.app.Context;

import net.java.sip.communicator.service.contactlist.MetaContactGroup;
import net.java.sip.communicator.service.contactlist.MetaContactListService;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.util.CollectionProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This adapter displays all <code>MetaContactGroup</code> items. If in the constructor <code>ListContainer</code> id
 * will be passed it will include "create new group" functionality. That means extra item "create group.."
 * will be appended on the last position and when selected create group dialog will popup automatically.
 * When a new group is created, it is implicitly included into this adapter.
 * Use setItemLayout and setDropDownLayout to change the spinner style if need to.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class MetaContactGroupProvider extends CollectionProvider<Object> {
    /**
     * Object instance used to identify "Create group..." item.
     */
    private static final Object ADD_NEW_OBJECT = new Object();

    /**
     * Item layout
     */
    private int itemLayout;

    /**
     * Drop down item layout
     */
    private int dropDownLayout;

    /**
     * Instance of used <code>ListContainer</code>.
     */
    private ListContainer adapterView;

    /**
     * Creates a new instance of <code>MetaContactGroupProvider</code>. It will be filled with all
     * currently available <code>MetaContactGroup</code>.
     *
     * @param context the parent <code>Ability</code>.
     * @param adapterViewId id of the <code>ListContainer</code>.
     * @param includeRoot <code>true</code> if "No group" item should be included
     * @param includeCreate <code>true</code> if "Create group" item should be included
     */
    public MetaContactGroupProvider(Context context, int adapterViewId, boolean includeRoot, boolean includeCreate) {
        super(context, getAllContactGroups(includeRoot, includeCreate).iterator());

        if (adapterViewId != -1)
            init(adapterViewId);
    }

    /**
     * Creates a new instance of <code>MetaContactGroupProvider</code>. It will be filled with all
     * currently available <code>MetaContactGroup</code>.
     *
     * @param context the parent <code>Context</code>.
     * @param adapterView the <code>ListContainer</code> that will be used.
     * @param includeRoot <code>true</code> if "No group" item should be included
     * @param includeCreate <code>true</code> if "Create group" item should be included
     */
    public MetaContactGroupProvider(Context context, ListContainer adapterView, boolean includeRoot, boolean includeCreate) {
        super(context, getAllContactGroups(includeRoot, includeCreate).iterator());
        init(adapterView);
    }

    private void init(int adapterViewId) {
        ListContainer aView = mContext.getCallingAbility().findComponentById(adapterViewId);
        init(aView);
    }

    private void init(ListContainer adapterView) {
        this.adapterView = adapterView;
        this.itemLayout = ResourceTable.Layout_simple_spinner_item;
        this.dropDownLayout = ResourceTable.Layout_simple_spinner_dropdown_item;

        // Handle add new group action
        adapterView.setItemSelectedListener((parent, view, position, id) -> {
            Object item = parent.getItemProvider().getItem(position);
            if (item == MetaContactGroupProvider.ADD_NEW_OBJECT) {
                new AddGroupDialog(getContext()).show(this::onNewGroupCreated);
            }
        });
    }

    /**
     * Returns the list of all currently available <code>MetaContactGroup</code>.
     *
     * @param includeRoot indicates whether "No group" item should be included in the list.
     * @param includeCreateNew indicates whether "create new group" item should be included in the list.
     *
     * @return the list of all currently available <code>MetaContactGroup</code>.
     */
    private static List<Object> getAllContactGroups(boolean includeRoot, boolean includeCreateNew) {
        MetaContactListService contactListService = AppGUIActivator.getContactListService();

        MetaContactGroup root = contactListService.getRoot();
        ArrayList<Object> groupList = new ArrayList<>();
        if (includeRoot) {
            groupList.add(root);
        }

        Iterator<MetaContactGroup> mcGroups = root.getSubgroups();
        while (mcGroups.hasNext()) {
            groupList.add(mcGroups.next());
        }

        // Add new group item
        if (includeCreateNew)
            groupList.add(ADD_NEW_OBJECT);

        return groupList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Component getComponent(boolean isDropDown, Object item, ComponentContainer parent, LayoutScatter inflater) {
        int rowResId = isDropDown ? dropDownLayout : itemLayout;
        Component rowView = inflater.parse(rowResId, parent, false);
        Text tv = rowView.findComponentById(ResourceTable.Id_text1);

        if (item.equals(ADD_NEW_OBJECT)) {
            tv.setText(ResourceTable.String_create_group);
        }
        else if (item.equals(AppGUIActivator.getContactListService().getRoot())) {
            // Root - Contacts
            tv.setText(ResourceTable.String_no_group);
        }
        else {
            tv.setText(((MetaContactGroup) item).getGroupName());
        }
        return rowView;
    }

    /**
     * Handles on new group created event by append item into the list and notifying about data set change.
     *
     * @param newGroup new contact group if was created or <code>null</code> if user cancelled the dialog.
     */
    private void onNewGroupCreated(MetaContactGroup newGroup) {
        if (newGroup == null)
            return;

        int pos = getCount() - 1;
        insert(pos, newGroup);

        adapterView.setSelectedItemIndex(pos);
        notifyDataChanged();
    }

    /**
     * Sets to caller defined item layout resource id.
     *
     * @param itemLayout the item layout resource id to set.
     */
    public void setItemLayout(int itemLayout) {
        this.itemLayout = itemLayout;
    }

    /**
     * Set to caller defined drop down item layout resource id.
     *
     * @param dropDownLayout the drop down item layout resource id to set.
     */
    public void setDropDownLayout(int dropDownLayout) {
        this.dropDownLayout = dropDownLayout;
    }
}
