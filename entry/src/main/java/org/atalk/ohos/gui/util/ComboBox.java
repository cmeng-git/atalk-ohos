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
package org.atalk.ohos.gui.util;

import java.util.List;

import ohos.agp.components.AttrSet;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.LayoutScatter;
import ohos.app.Context;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.util.ComponentUtil;

/**
 * Custom ComboBox for ohos
 *
 * @author Eng Chong Meng
 */
public class ComboBox extends DirectionalLayout {
    protected AutoCompleteTextView _text;
    protected List<String> spinnerList;

    private final int unit = TypedValue.COMPLEX_UNIT_SP;
    private final float fontSize = 15;
    private final int fontBlack = getContext().getColor(ResourceTable.Color_textColorBlack);

    private Context mContext;
    private LayoutScatter inflater;

    public ComboBox(Context context) {
        super(context);
        this.createChildControls(context);
    }

    public ComboBox(Context context, AttrSet attrs) {
        super(context, attrs);
        this.createChildControls(context);
    }

    private void createChildControls(Context context) {
        mContext = context;
        inflater = (LayoutScatter) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        this.setOrientation(HORIZONTAL);
        this.layoutConfig(new LayoutConfig(LayoutConfig.MATCH_PARENT, LayoutConfig.MATCH_CONTENT));

        _text = new AutoCompleteTextView(context);
        _text.setDropDownWidth(-1); // set the dropdown width to match screen
        _text.setTextSize(unit, fontSize);
        _text.setTextColor(fontBlack);
        _text.setSingleLine();
        _text.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
                | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        _text.setRawInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        this.addView(_text, new LayoutConfig(LayoutConfig.MATCH_CONTENT, LayoutConfig.MATCH_CONTENT, 1));

        Button _button = new Button(context);
        _button.setImageResource(android.ResourceTable.Media_arrow_down_float);
        _button.setClickedListener(v -> {
            if (!TextUtils.isEmpty(getText()) && !spinnerList.contains(getText())) {
                ComponentUtil.hideKeyboard(mContext, _text);
                setSuggestionSource(spinnerList); // rest to user supplied list
            }
            _text.showDropDown();
        });
        this.addView(_button, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    /**
     * Sets the source for DDLB suggestions. Cursor MUST be managed by supplier!!
     *
     * @param source Source of suggestions.
     * @param column Which column from source to show.
     */
    public void setSuggestionSource(Cursor source, String column) {
        String[] from = new String[]{column};
        int[] to = new int[]{android.ResourceTable.Id_text1};
        SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(this.getContext(),
                ResourceTable.Layout_simple_spinner_dropdown_item, source, from, to);

        // this is to ensure that when suggestion is selected it provides the value to the textBox
        cursorAdapter.setStringConversionColumn(source.getColumnIndex(column));
        _text.setItemProvider(cursorAdapter);
    }

    public void setSuggestionSource(List<String> list) {
        spinnerList = list;

        // Create an ArrayAdapter using the string array and custom spinner item with radio button
        ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this.getContext(), ResourceTable.Layout_simple_spinner_item, list) {
            // Allow to change font style in dropdown vew
            public Component getComponent(int position, Component convertView, ComponentContainer parent) {
                if (convertView == null) {
                    convertView = inflater.parse(ResourceTable.Layout_adapter_radio_item, null);
                }
                Text name = convertView.findComponentById(ResourceTable.Id_item_name);
                RadioButton radio = convertView.findComponentById(ResourceTable.Id_item_radio);

                final String variation = list.get(position);
                name.setText(variation);

                int mSelected = list.indexOf(getText());
                radio.setChecked(position == mSelected);

                return convertView;
            }
        };

        // Specify the layout to use when the list of choices appears
        mAdapter.setDropDownViewResource(ResourceTable.Layout_simple_spinner_dropdown_item);

        // Apply the adapter to the ComboBox
        _text.setItemProvider(mAdapter);
    }

    /**
     * Gets the text in the combo box.
     *
     * @return Text or null if text isEmpty().
     */
    public String getText() {
        return ComponentUtil.toString(_text);
    }

    /**
     * Sets the text in combo box.
     */
    public void setText(String text) {
        _text.setText(text);
    }

    /**
     * Sets the textSize in comboBox.
     */
    public void setTextSize(float size) {
        this.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    /**
     * Sets the unit and textSize in comboBox.
     */
    public void setTextSize(int unit, float size) {
        _text.setTextSize(unit, size);
    }

    /**
     * Set the call back when an item in the combo box dropdown list item is selected
     *
     * @param l ListContainer OnItemClickListener
     */
    public void setOnItemClickListener(ListContainer.OnItemClickListener l) {
        _text.setOnItemClickListener(l);
    }
}