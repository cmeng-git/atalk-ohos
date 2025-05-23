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
package org.atalk.ohos.gui.account.settings;

import ohos.aafwk.content.Intent;
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.utils.PacMap;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.settings.BasePreferenceSlice;
import org.atalk.ohos.gui.widgets.TouchInterceptor;

import java.io.Serializable;
import java.util.List;

/**
 * The fragment allows user to edit encodings and their priorities.
 *
 * @author Eng Chong Meng
 */
public class MediaEncodingsSlice extends BasePreferenceSlice implements TouchInterceptor.DropListener {
    /**
     * Argument key for list of encodings as strings (see {@link MediaEncodingAbility} for utility methods.)
     */
    public static final String ARG_ENCODINGS = "arg.encodings";

    /**
     * Argument key for encodings priorities.
     */
    public static final String ARG_PRIORITIES = "arg.priorities";

    /**
     * Adapter encapsulating manipulation of encodings list and their priorities
     */
    private OrderListProvider orderListProvider;

    /**
     * List of encodings
     */
    private List<String> encodings;

    /**
     * List of priorities
     */
    private List<Integer> priorities;

    /**
     * Flag holding enabled status for the fragment. All views will be grayed out if the fragment is not enabled.
     */
    private boolean isEnabled = true;

    /**
     * Flag tells us if there were any changes made.
     */
    private boolean hasChanges = false;

    /**
     * Sets enabled status for this fragment.
     *
     * @param isEnabled <code>true</code> to enable the fragment.
     */
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
        orderListProvider.invalidate();
    }

    /**
     * Returns <code>true</code> if this fragment is holding any uncommitted changes
     *
     * @return <code>true</code> if this fragment is holding any uncommitted changes
     */
    public boolean hasChanges() {
        return hasChanges;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart(Intent intent) {
        LayoutScatter inflater = LayoutScatter.getInstance(getContext());

        encodings = intent.getStringArrayListParam(ARG_ENCODINGS);
        priorities = intent.getIntegerArrayListParam(ARG_PRIORITIES);
        if (encodings.contains("VP8/90000"))
            setPrefTitle(ResourceTable.String_settings_video_codec);
        else
            setPrefTitle(ResourceTable.String_settings_audio_codecs);

        Component content = inflater.parse(ResourceTable.Layout_encoding, null, false);

        /*
         * The {@link TouchInterceptor} widget that allows user to drag items to set their order
         */
        TouchInterceptor listWidget = (TouchInterceptor) content.findComponentById(ResourceTable.Id_encodingList);
        this.orderListProvider = new OrderListProvider(ResourceTable.Layout_encoding_item);

        listWidget.setItemProvider(orderListProvider);
        listWidget.setDropListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveAbilityState(PacMap outState) {
        super.onSaveAbilityState(outState);

        outState.putSerializableObject(ARG_ENCODINGS, (Serializable) encodings);
        outState.putSerializableObject(ARG_PRIORITIES, (Serializable) priorities);
    }

    /**
     * Implements {@link TouchInterceptor.DropListener}
     *
     * @param from index indicating source position
     * @param to index indicating destination position
     */
    public void drop(int from, int to) {
        orderListProvider.swapItems(from, to);
        hasChanges = true;
    }

    /**
     * Function used to calculate priority based on item index
     *
     * @param idx the index of encoding on the list
     *
     * @return encoding priority value for given <code>idx</code>
     */
    static public int calcPriority(List<?> encodings, int idx) {
        return encodings.size() - idx;
    }

    /**
     * Utility method for calculating encodings priorities.
     *
     * @param idx encoding index in the list
     *
     * @return the priority value for given encoding index.
     */
    private int calcPriority(int idx) {
        return calcPriority(encodings, idx);
    }

    /**
     * Creates new <code>EncodingsFragment</code> for given list of encodings and priorities.
     *
     * @param encodings list of encodings as strings.
     * @param priorities list of encodings priorities.
     *
     * @return parametrized instance of <code>EncodingsFragment</code>.
     */
    static public MediaEncodingsSlice newInstance(List<String> encodings, List<Integer> priorities) {
        MediaEncodingsSlice fragment = new MediaEncodingsSlice();

        PacMap args = new PacMap();
        args.putSerializableObject(ARG_ENCODINGS, (Serializable) encodings);
        args.putSerializableObject(ARG_PRIORITIES, (Serializable) priorities);

        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Returns encodings strings list.
     *
     * @return encodings strings list.
     */
    public List<String> getEncodings() {
        return encodings;
    }

    /**
     * Returns encodings priorities list.
     *
     * @return encodings priorities list.
     */
    public List<Integer> getPriorities() {
        return priorities;
    }

    /**
     * Class implements encodings model for the list widget. Enables/disables each encoding and sets its priority.
     * It is also responsible for creating Views for list rows.
     */
    class OrderListProvider extends BaseItemProvider {
        /**
         * ID of the list row layout
         */
        private final int viewResId;

        /**
         * Creates a new instance of {@link OrderListProvider}.
         *
         * @param viewResId ID of the list row layout
         */
        public OrderListProvider(int viewResId) {
            this.viewResId = viewResId;
        }

        /**
         * Swaps encodings on the list and changes their priorities
         *
         * @param from source item position
         * @param to destination items position
         */
        void swapItems(int from, int to) {
            // Swap positions
            String swap = encodings.get(from);
            int swapPrior = priorities.get(from);
            encodings.remove(from);
            priorities.remove(from);

            // Swap priorities
            encodings.add(to, swap);
            priorities.add(to, swapPrior);

            for (int i = 0; i < encodings.size(); i++) {
                priorities.set(i, priorities.get(i) > 0 ? calcPriority(i) : 0);
            }

            // Update the UI
            invalidate();
        }

        /**
         * Refresh the list on UI thread
         */
        public void invalidate() {
            BaseAbility.runOnUiThread(this::notifyAll);
        }

        public int getCount() {
            return encodings.size();
        }

        public Object getItem(int i) {
            return encodings.get(i);
        }

        public long getItemId(int i) {
            return i;
        }

        public Component getComponent(final int i, Component view, ComponentContainer viewGroup) {
            // Creates the list row view
            ComponentContainer gv = (ComponentContainer) LayoutScatter.getInstance(getContext())
                    .parse(this.viewResId, viewGroup, false);
            // Creates the enable/disable button
            Checkbox cb = gv.findComponentById(ResourceTable.Id_checkbox);
            cb.setChecked(priorities.get(i) > 0);
            cb.setCheckedStateChangedListener((cButton, isChecked) -> {
                priorities.set(i, isChecked ? calcPriority(i) : 0);
                hasChanges = true;
            });

            // Create string for given format entry
            String mf = encodings.get(i);
            Text tv = gv.findComponentById(ResourceTable.Id_text1);
            tv.setText(mf);
            // Creates the drag handle view(used to grab list entries)
            Image iv = gv.findComponentById(ResourceTable.Id_dragHandle);
            if (!isEnabled)
                gv.removeComponent(iv);
            cb.setEnabled(isEnabled);
            tv.setEnabled(isEnabled);

            return gv;
        }
    }
}
