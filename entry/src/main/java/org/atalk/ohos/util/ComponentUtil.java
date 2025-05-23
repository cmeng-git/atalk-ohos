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
package org.atalk.ohos.util;

import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.Text;
import ohos.agp.utils.Color;
import ohos.app.Context;

import org.atalk.ohos.aTalkApp;

/**
 * Utility class that encapsulates common operations on some <code>Component.</code> types.
 *
 * @author Eng Chong Meng
 */
public class ComponentUtil
{
    /**
     * Sets given <code>text</code> on the <code>Text</code> identified by the <code>id</code>. The
     * <code>Text</code> must be inside <code>container</code> component hierarchy.
     *
     * @param container the <code>Component.</code> that contains the <code>Text</code>.
     * @param id the id of <code>Text</code> we want to edit.
     * @param text string value that will be set on the <code>Text</code>.
     */
    public static void setTextViewValue(Component container, int id, String text)
    {
        Text tv = container.findComponentById(id);
        tv.setText(text);
    }

    //	public static void setTextViewHtml(Component container, int id, String text)
    //	{
    //		Text tv = (Text) container.findComponentById(id);
    //		tv.setText(android.text.Html.fromHtml(text));
    //	}

    public static void setTextViewColor(Component container, int id, int color)
    {
        Text tv = container.findComponentById(id);
        tv.setTextColor(new Color(aTalkApp.getInstance().getColor(color)));
    }

    public static void setTextViewAlpha(Component container, int id, float alpha)
    {
        Text tv = container.findComponentById(id);
        tv.setAlpha(alpha);
    }

    public static String getTextViewValue(Component container, int id)
    {
        return toString(container.findComponentById(id));
    }

    public static boolean isCompoundChecked(Component container, int id)
    {
        return ((Checkbox) container.findComponentById(id)).isChecked();
    }

    public static void setCompoundChecked(Component container, int id, boolean isChecked)
    {
        ((Checkbox) container.findComponentById(id)).setChecked(isChecked);
    }

    /**
     * Sets image identified by <code>drawableId</code> resource id on the <code>Image</code>.
     * <code>Image</code> must exist in <code>container</code> component hierarchy.
     *
     * @param container the container <code>Component.</code>.
     * @param imageViewId id of <code>Image</code> that will be used.
     * @param drawableId the resource id of drawable that will be set.
     */
    public static void setImageViewIcon(Component container, int imageViewId, int drawableId)
    {
        Image imageView = container.findComponentById(imageViewId);
        imageView.setImageAndDecodeBounds(drawableId);
    }

    /**
     * Ensures that the <code>Component.</code> is currently in visible or hidden state which depends on
     * <code>isVisible</code> flag.
     *
     * @param container parent <code>Component.</code> that contains displayed <code>Component</code>.
     * @param componentId the id of <code>Component.</code> that will be shown/hidden.
     * @param isVisible flag telling whether the <code>Component.</code> has to be shown or hidden.
     */
    static public void ensureVisible(Component container, int componentId, boolean isVisible)
    {
        Component component = container.findComponentById(componentId);
        if (isVisible && component.getVisibility() != Component.VISIBLE) {
            component.setVisibility(Component.VISIBLE);
        }
        else if (!isVisible && component.getVisibility() != Component.HIDE) {
            component.setVisibility(Component.HIDE);
        }
    }

    /**
     * Ensures that the <code>Component.</code> is currently in enabled or disabled state.
     *
     * @param container parent <code>Component.</code> that contains displayed <code>Component.</code>.
     * @param componentId the id of <code>Component.</code> that will be enabled/disabled.
     * @param isEnabled flag telling whether the <code>Component.</code> has to be enabled or disabled.
     */
    static public void ensureEnabled(Component container, int componentId, boolean isEnabled)
    {
        Component component = container.findComponentById(componentId);
        if (isEnabled && !component.isEnabled()) {
            component.setEnabled(isEnabled);
        }
        else if (!isEnabled && component.isEnabled()) {
            component.setEnabled(isEnabled);
        }
    }

    /**
     * get the textView string value or null (length == 0)
     *
     * @param textView Text or TextField
     * @return String or null
     */
    public static String toString(final Text textView)
    {
        CharSequence editText = (textView == null) ? null : textView.getText();
        String text = (editText == null) ? null : editText.toString().trim();
        return ((text == null) || (text.length() == 0)) ? null : text;
    }

    /**
     * get the textView string value or null (length == 0)
     *
     * @param textView Text or TextField
     * @return String or null
     */
    public static char[] toCharArray(final Text textView)
    {
        String text = toString(textView);
        return (text == null) ? null : text.toCharArray();
    }

    /**
     * Show or hide password
     *
     * @param component the password TextField component
     * @param show <code>true</code> set password visible to user
     */
    public static void showPassword(final Text component, final boolean show)
    {
        int cursorPosition = component.getSelectionStart();
        if (show) {
            component.setTextInputType();
            component.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        else {
            component.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        component.setSelection(cursorPosition);
    }

    /**
     * Hide soft keyboard
     *
     * @param context context
     * @param component the reference component
     */
    public static void hideKeyboard(Context context, Component component)
    {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(component.getWindowToken(), 0);
    }
}
