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
package org.atalk.ohos.gui.dialogs;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ohos.agp.colors.RgbColor;
import ohos.agp.components.AttrHelper;
import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.text.Font;
import ohos.agp.utils.Color;
import ohos.agp.utils.LayoutAlignment;
import ohos.agp.window.dialog.CommonDialog;
import ohos.agp.window.dialog.IDialog;
import ohos.agp.window.dialog.ListDialog;
import ohos.agp.window.service.WindowManager;
import ohos.app.Context;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.util.ButtonElement;
import org.atalk.ohos.util.ResourceTool;

/**
 * <code>DialogH</code> can be used to display alerts without having parent <code>Ability</code>
 * (from services). <br/> Simple alerts can be displayed using static method <code>showDialog(...)
 * </code>.<br/> Optionally confirm button's text and the listener can be supplied. It allows to
 * react to users actions. For this purpose use method <code>showConfirmDialog(...)</code>.<br/>
 * For more sophisticated use cases content fragment class with it's arguments can be specified
 * in method <code>showCustomDialog()</code>. When they're present the alert message will be replaced
 * by the {@link ohos.aafwk.ability.AbilitySlice}'s <code>Component</code>.
 *
 * @author Eng Chong Meng
 */
public class DialogA extends CommonDialog {
    /* Button position is from left to right */
    public static final int BUTTON_NEGATIVE = BUTTON1;
    public static final int BUTTON_NEUTRAL = BUTTON2;
    public static final int BUTTON_POSITIVE = BUTTON3;

    private static final int DIALOG_WIDTH_VP = (int) (aTalkApp.mDisplaySize.width * 0.9);
    private static final int MAX_BUTTON_NUMBER = 3;
    private final String[] mButtonTexts = new String[MAX_BUTTON_NUMBER];
    private final int[] mButtonTypes = new int[MAX_BUTTON_NUMBER];
    private final ClickedListener[] mButtonListeners = new ClickedListener[MAX_BUTTON_NUMBER];
    private int buttonNum = 0;

    // Contain a reference of the added Buttons, retrieve in the order when the buttons is added.
    private final Map<Integer, Button> mButtons = new HashMap<>();

    private boolean mOutsideTouchClosable = false;
    private boolean mBackKeyToDismiss = false;
    private boolean mSwipeToDismiss = true;
    private Component mComponent;
    private String mTitle;
    private String mContent;
    private int mIconId;
    private float dim = -1f;

    private String[] mItems;
    private boolean[] mCheckedItems;
    private boolean mIsMultiChoice = false;
    private IDialog.CheckBoxClickedListener mCheckBoxClickedListener;

    private CommonDialog mCommonDialog;

    public DialogA(Context context) {
        super(context);
    }

    public void show() {
        if (mCommonDialog != null) {
            mCommonDialog.show();
            if (dim >= 0) {
                changeDialogDim(mCommonDialog, dim);
            }
        }
    }

    public void remove() {
        if (mCommonDialog != null) {
            mCommonDialog.destroy();
        }
    }

    @Override
    protected void onShow() {
        super.onShow();
    }

    public boolean isShowing() {
        return (mCommonDialog != null) && mCommonDialog.isShowing();
    }

    public Component getComponent() {
        return mComponent;
    }

    public CommonDialog getDialog() {
        return mCommonDialog;
    }

    public Button getButton(int index) {
        return mButtons.get(index);
    }

    private void changeDialogDim(CommonDialog dialog, float dim) {
        Optional<WindowManager.LayoutConfig> configOpt = dialog.getWindow().getLayoutConfig();
        configOpt.ifPresent(config -> {
            config.dim = dim;
            dialog.getWindow().setLayoutConfig(config);
        });
    }

    public interface ClickedListener {
        void onClick(DialogA dialog);
    }

    public static class Builder {
        private final DialogA mDialogA;

        private final Context mContext;

        public Builder(Context context) {
            mContext = context;
            mDialogA = new DialogA(context);
        }

        public Builder setTitle(int resId) {
            mDialogA.mTitle = mContext.getString(resId);
            return this;
        }

        public Builder setTitle(String title) {
            mDialogA.mTitle = title;
            return this;
        }

        public Builder setComponent(Component component) {
            mDialogA.mComponent = component;
            return this;
        }

        public Builder setContent(int resId, Object... arg) {
            return setContent(mContext.getString(resId, arg));
        }

        public Builder setContent(String content) {
            mDialogA.mContent = content;
            return this;
        }

        public Builder setIcon(int id) {
            mDialogA.mIconId = id;
            return this;
        }

        public Builder setNegativeButton(int resId, ClickedListener listener) {
            String btnText = mContext.getString(resId);
            setNegativeButton(btnText, listener);
            return this;
        }

        public Builder setNegativeButton(String text, ClickedListener listener) {
            addButton(BUTTON_NEGATIVE, text, listener);
            return this;
        }

        public Builder setNeutralButton(int resId, ClickedListener listener) {
            String btnText = mContext.getString(resId);
            return setNeutralButton(btnText, listener);
        }

        public Builder setNeutralButton(String text, ClickedListener listener) {
            addButton(BUTTON_NEUTRAL, text, listener);
            return this;
        }

        public Builder setPositiveButton(int resId, ClickedListener listener) {
            String btnText = mContext.getString(resId);
            setPositiveButton(btnText, listener);
            return this;
        }

        public Builder setPositiveButton(String text, ClickedListener listener) {
            return addButton(BUTTON_POSITIVE, text, listener);
        }

        public Builder addButton(int buttonId, String text, ClickedListener listener) {
            mDialogA.mButtonTexts[buttonId] = text;
            mDialogA.mButtonTypes[buttonId] = ButtonElement.TYPE_NORMAL;
            mDialogA.mButtonListeners[buttonId] = listener;
            mDialogA.buttonNum++;
            return this;
        }

        public Builder addButton(int resId, ClickedListener listener) {
            String btnText = mContext.getString(resId);
            return addButton(btnText, ButtonElement.TYPE_NORMAL, listener);
        }

        public Builder addButton(String text, int type, ClickedListener listener) {
            if (mDialogA.buttonNum >= MAX_BUTTON_NUMBER) {
                return this;
            }
            mDialogA.mButtonTexts[mDialogA.buttonNum] = text;
            mDialogA.mButtonTypes[mDialogA.buttonNum] = type;
            mDialogA.mButtonListeners[mDialogA.buttonNum] = listener;
            mDialogA.buttonNum++;
            return this;
        }

        public Builder setOutsideTouchClosable(boolean closable) {
            mDialogA.mOutsideTouchClosable = closable;
            return this;
        }

        public Builder setBackKeyToDismiss(boolean enable) {
            mDialogA.mBackKeyToDismiss = enable;
            return this;
        }

        public Builder setSwipeToDismiss(boolean enable) {
            mDialogA.mSwipeToDismiss = enable;
            return this;
        }

        public Builder setDim(float dim) {
            if (dim > 1) {
                mDialogA.dim = 1;
            }
            else if (dim < 0) {
                mDialogA.dim = 0;
            }
            else {
                mDialogA.dim = dim;
            }
            return this;
        }

        public Builder setMultiChoiceItems(String[] items, boolean[] checkedItems, final IDialog.CheckBoxClickedListener listener){
            mDialogA.mItems = items;
            mDialogA.mCheckedItems = checkedItems;
            mDialogA.mCheckBoxClickedListener = listener;
            mDialogA.mIsMultiChoice = true;
            return this;
        }

        public DialogA create() {
            CommonDialog sDialog;
            if (mDialogA.mIsMultiChoice) {
                sDialog = new ListDialog(mContext, ListDialog.MULTI);
                ((ListDialog) sDialog).setMultiSelectItems(mDialogA.mItems, mDialogA.mCheckedItems);
                ((ListDialog) sDialog).setOnMultiSelectListener(mDialogA.mCheckBoxClickedListener);
                sDialog.setDialogListener(() -> false);
            } else {
                sDialog = new CommonDialog(mContext);
            }

            sDialog.setSize(AttrHelper.fp2px(DIALOG_WIDTH_VP, mContext), ComponentContainer.LayoutConfig.MATCH_CONTENT);
            sDialog.setAlignment(LayoutAlignment.CENTER);
            sDialog.setOffset(0, 0);
            sDialog.setTransparent(true);
            sDialog.setContentCustomComponent(initDialogComponent());
            sDialog.setAutoClosable(mDialogA.mOutsideTouchClosable);
            sDialog.siteRemovable(mDialogA.mBackKeyToDismiss);
            sDialog.setSwipeToDismiss(mDialogA.mSwipeToDismiss);
            mDialogA.mCommonDialog = sDialog;
            return mDialogA;
        }

        private Component initDialogComponent() {
            Component dialogComponent = LayoutScatter.getInstance(mContext).parse(ResourceTable.Layout_alert_dialog, null, false);
            dialogComponent.setBackground(new ShapeElement() {{
                setRgbColor(RgbColor.fromArgbInt(ResourceTool.getColor(mContext, ResourceTable.Color_bg_dialog_light, 0xffffff)));
                setCornerRadius(ResourceTool.getFloat(mContext, ResourceTable.Float_dialog_corner_radius, 0));
            }});

            Text textTitle = dialogComponent.findComponentById(ResourceTable.Id_title);
            if (mDialogA.mTitle != null) {
                textTitle.setText(mDialogA.mTitle);
                textTitle.setVisibility(Component.VISIBLE);
            }

            Image imageIcon = dialogComponent.findComponentById(ResourceTable.Id_icon);
            if (mDialogA.mIconId != 0) {
                imageIcon.setPixelMap(mDialogA.mIconId);
                imageIcon.setVisibility(Component.VISIBLE);
                Component titleLayout = dialogComponent.findComponentById(ResourceTable.Id_title_layout);
                titleLayout.setVisibility(Component.HIDE);
            }

            Text textContent = dialogComponent.findComponentById(ResourceTable.Id_content);
            Component bottomLayout = dialogComponent.findComponentById(ResourceTable.Id_bottom_layout);
            if (mDialogA.mContent != null) {
                textContent.setText(mDialogA.mContent);
                textContent.setVisibility(Component.VISIBLE);
                bottomLayout.setVisibility(Component.VISIBLE);
            }

            DirectionalLayout componentLayout = dialogComponent.findComponentById(ResourceTable.Id_component_layout);
            if (mDialogA.mComponent != null) {
                componentLayout.setVisibility(Component.VISIBLE);
                componentLayout.addComponent(mDialogA.mComponent);
            }

            DirectionalLayout operationLayout = dialogComponent.findComponentById(ResourceTable.Id_operation_layout);
            if (mDialogA.buttonNum != 0) {
                operationLayout.setVisibility(Component.VISIBLE);
                bottomLayout.setVisibility(Component.VISIBLE);
            }

            float totalWeight = 0.0f;
            for (int i = 0; i < mDialogA.buttonNum; i++) {
                // Skip is button is not defined.
                if (TextUtils.isBlank(mDialogA.mButtonTexts[i]))
                    continue;

                Button button = new Button(mContext);
                mDialogA.mButtons.put(i, button);
                totalWeight += 1.0f;

                operationLayout.addComponent(button);
                DirectionalLayout.LayoutConfig config = new DirectionalLayout.LayoutConfig();
                config.height = (int) ResourceTool.getFloat(mContext, ResourceTable.Float_button_height, 0);
                config.width = DirectionalLayout.LayoutConfig.MATCH_PARENT;
                config.weight = 1;
                int spacingHorizontal = (int) ResourceTool.getFloat(mContext, ResourceTable.Float_spacing_mini, 0);
                config.setMargins(spacingHorizontal, 0, spacingHorizontal, 0);
                button.setLayoutConfig(config);
                button.setBackground(new ButtonElement(mContext, mDialogA.mButtonTypes[i]));
                if (mDialogA.mButtonTypes[i] == ButtonElement.TYPE_NORMAL) {
                    button.setTextColor(new Color(ResourceTool.getColor(mContext, ResourceTable.Color_text_color_light, 0xFFFFFF)));
                }
                else {
                    button.setTextColor(new Color(ResourceTool.getColor(mContext, ResourceTable.Color_colorNormal, 0xFFFFFF)));
                }

                button.setText(mDialogA.mButtonTexts[i]);
                int currentIndex = i;
                button.setClickedListener(component -> mDialogA.mButtonListeners[currentIndex].onClick(mDialogA));
                button.setFont(Font.DEFAULT_BOLD);
                button.setTextSize((int) ResourceTool.getFloat(mContext, ResourceTable.Float_button_size, 0));
                button.setMultipleLine(false);
                button.setTruncationMode(Text.TruncationMode.ELLIPSIS_AT_END);
                button.setPaddingLeft(spacingHorizontal);
                button.setPaddingRight(spacingHorizontal);
            }
            operationLayout.setTotalWeight(totalWeight);
            return dialogComponent;
        }
    }
}
