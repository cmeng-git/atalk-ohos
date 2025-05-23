/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ohos.aafwk.ability.Ability;
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.app.Context;

import org.atalk.ohos.BaseAbility;

/**
 * Convenience class wrapping set of elements into {@link BaseItemProvider}
 *
 * @param <T> class of the elements contained in this adapter
 *
 * @author Eng Chong Meng
 */
public abstract class CollectionProvider<T> extends BaseItemProvider {
    /**
     * List of elements handled by this adapter
     */
    private List<T> items;

    /**
     * The parent {@link ohos.app.Context}
     */
    protected final Context mContext;

    /**
     * Creates a new instance of {@link CollectionProvider}
     *
     * @param parent the parent {@link Ability}
     */
    public CollectionProvider(Context parent) {
        mContext = parent;
    }

    /**
     * Creates new instance of {@link CollectionProvider}
     *
     * @param context the context
     * @param items iterator of {@link T} items
     */
    public CollectionProvider(Context context, Iterator<T> items) {
        mContext = context;
        setIterator(items);
    }

    /**
     * The method that accepts {@link Iterator} as a source set of objects
     *
     * @param iterator source of {@link T} instances that will be contained in this {@link CollectionProvider}
     */
    protected void setIterator(Iterator<T> iterator) {
        items = new ArrayList<T>();
        while (iterator.hasNext())
            items.add(iterator.next());
    }

    /**
     * Accepts {@link List} as a source set of {@link T}
     *
     * @param collection the {@link List} that will be included in this {@link CollectionProvider}
     */
    protected void setList(List<T> collection) {
        items = new ArrayList<T>();
        items.addAll(collection);
    }

    /**
     * Returns total count of items contained in this adapter
     *
     * @return the count of {@link T} stored in this {@link CollectionProvider}
     */
    public int getCount() {
        return items.size();
    }

    public Object getItem(int i) {
        return items.get(i);
    }

    public long getItemId(int i) {
        return i;
    }

    /**
     * Convenience method for retrieving {@link T} instances
     *
     * @param i the index of {@link T} that will be retrieved
     *
     * @return the {@link T} object located at <code>i</code> position
     */
    protected T getObject(int i) {
        return (T) items.get(i);
    }

    /**
     * Adds <code>object</code> to the adapter
     *
     * @param object instance of {@link T} that will be added to this adapter
     */
    public void add(T object) {
        if (!items.contains(object)) {
            items.add(object);
            doRefreshList();
        }
    }

    /**
     * Insert given object at specified position without notifying about adapter data change.
     *
     * @param pos the position at which given object will be inserted.
     * @param object the object to insert into adapter's list.
     */
    protected void insert(int pos, T object) {
        items.add(pos, object);
    }

    /**
     * Removes the <code>object</code> from this adapter
     *
     * @param object instance of {@link T} that will be removed from the adapter
     */
    public void remove(final T object) {
        // Remove item on UI thread to make sure it's not being painted at the same time
        BaseAbility.runOnUiThread(() -> {
            if (items.remove(object)) {
                doRefreshList();
            }
        });
    }

    /**
     * Runs list change notification on the UI thread
     */
    protected void doRefreshList() {
        BaseAbility.runOnUiThread(this::notifyDataChanged);
    }

    /**
     * {@inheritDoc}
     */
    public Component getComponent(int i, Component view, ComponentContainer viewGroup) {
        return getComponent(false, items.get(i), viewGroup, LayoutScatter.getInstance(mContext));
    }

    /**
     * {@inheritDoc}
     */
    // @Override
    public Component getDropDownView(int position, Component convertView, ComponentContainer parent) {
        return getComponent(true, items.get(position), parent, LayoutScatter.getInstance(mContext));
    }

    /**
     * Convenience method for creating new {@link Component}s for each adapter's object
     *
     * @param isDropDown <code>true</code> if the <code>Component.</code> should be created for drop down spinner item
     * @param item the item for which a new Component shall be created
     * @param parent {@link ComponentContainer} parent Component.
     * @param inflater the {@link LayoutScatter} for creating new Views
     *
     * @return a {@link Component} for given <code>item</code>
     */
    protected abstract Component getComponent(boolean isDropDown, T item, ComponentContainer parent, LayoutScatter inflater);

    protected Context getContext() {
        return mContext;
    }
}
