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
package org.atalk.ohos.gui.actionbar;

import ohos.agp.components.Text;
import ohos.agp.components.ToggleButton;
import ohos.app.Context;

import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.agp.components.Menu;
import org.atalk.ohos.agp.components.MenuInflater;
import org.atalk.ohos.agp.components.MenuItem;

/**
 * AbilitySlice adds a toggle button to the action bar with text description to the right of it.
 * Button is handled through the <code>ActionBarToggleModel</code> which must be implemented by
 * parent <code>Ability</code>.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class ActionBarToggleSlice extends BaseSlice {
    /**
     * Text description's argument key
     */
    private static final String ARG_LABEL_TEXT = "text";

    /**
     * Button model
     */
    private ActionBarToggleModel model;

    /**
     * Menu instance used to update the button
     */
    private ToggleButton mToggleCB;

    /**
     * Creates new instance of <code>ActionBarToggleFragment</code> with given description(can be
     * empty but not <code>null</code>).
     *
     * @param labelText toggle button's description(can be empty, but not <code>null</code>).
     *
     * @return new instance of <code>ActionBarToggleFragment</code> parametrized with description argument.
     */
    static public ActionBarToggleSlice newInstance(String labelText) {
        ActionBarToggleSlice fragment = new ActionBarToggleSlice();
        Bundle args = new Bundle();
        args.putString(ARG_LABEL_TEXT, labelText);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        requireActivity().addMenuProvider(this);
        this.model = (ActionBarToggleModel) context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateMenu(Menu menu, MenuInflater inflater) {
        inflater.parse(ResourceTable.Layout_menu_actionbar_toggle, menu);

        // Binds the button
        mToggleCB = ((MenuItem) menu.findComponentById(ResourceTable.Id_toggleView)).getActionView().findComponentById(ResourceTable.Id_toggle);
        mToggleCB.setCheckedStateChangedListener((cb, checked) -> model.setChecked(checked));

        // Set label text
        ((Text) menu.findComponentById(ResourceTable.Id_toggleView).getActionView().findComponentById(ResourceTable.Id_text1))
                .setText(getArguments().getString(ARG_LABEL_TEXT));

        updateChecked();
    }

    @Override
    public boolean onMenuItemSelected(MenuItem menuItem) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActive() {
        super.onActive();
        updateChecked();
    }

    /**
     * {@inheritDoc}
     */
    private void updateChecked() {
        if (mToggleCB != null) {
            mToggleCB.setChecked(model.isChecked());
        }
    }

    /**
     * Toggle button's model that has to be implemented by parent <code>Ability</code>.
     */
    public interface ActionBarToggleModel {
        /**
         * Return <code>true</code> if button's model is currently in checked state.
         *
         * @return <code>true</code> if button's model is currently in checked state.
         */
        boolean isChecked();

        /**
         * Method fired when the button is clicked.
         *
         * @param isChecked <code>true</code> if new button's state is checked.
         */
        void setChecked(boolean isChecked);
    }
}
