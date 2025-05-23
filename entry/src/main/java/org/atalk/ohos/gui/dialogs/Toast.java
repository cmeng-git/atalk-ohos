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

import ohos.agp.colors.RgbColor;
import ohos.agp.components.AttrHelper;
import ohos.agp.components.DependentLayout;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.utils.Color;
import ohos.agp.utils.LayoutAlignment;
import ohos.agp.utils.TextAlignment;
import ohos.agp.window.dialog.ToastDialog;
import ohos.app.Context;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;

import static ohos.agp.components.ComponentContainer.LayoutConfig.MATCH_CONTENT;
import static ohos.agp.components.ComponentContainer.LayoutConfig.MATCH_PARENT;

/**
 * Widget helper: Toast
 *
 * @since 2021-09-04
 */
public final class Toast {
    private static final int TEXT_SIZE = 48;
    private static final int LEFT_PADDING = 30;
    private static final int TOP_PADDING = 20;
    private static final int RIGHT_PADDING = 30;
    private static final int BOTTOM_PADDING = 20;
    private static final int RGB_COLOR = 0x666666FF;
    private static final int RGB_RED_COLOR = 0x8B3D48FF;
    private static final int CORNER_RADIUS = 15;
    private static final int DURATION = 2000;

    private static final int TEXT_PADDING = 20;
    private static final int TEXT_HEIGHT = 100;
    private static final int TEXT_OFFSETY = 200;
    private static final int TEXT_ALPHA = 120;

    // exclusively used to guarantee window timeouts
//    public static final int TIMEOUT_SHORT = 4000;
//    public static final int TIMEOUT_LONG = 7000;
    public static final int LENGTH_LONG = 3500;
    public static final int LENGTH_SHORT = 2000;

    /**
     * offset toast
     */
    public static final int OFFSET_TOAST = 64;
    /**
     * toast show duration
     */
    private Toast() {
    }

    /**
     * Show tips
     *
     * @param context ability slice
     * @param msg show msg
     * @param flag error color flag
     */
    public static void showTips(Context context, String msg, int flag) {
        Text textComponent = new Text(context);
        textComponent.setPadding(LEFT_PADDING, TOP_PADDING, RIGHT_PADDING, BOTTOM_PADDING);
        textComponent.setWidth(MATCH_CONTENT);
        textComponent.setHeight(MATCH_CONTENT);
        textComponent.setTextSize(TEXT_SIZE);
        textComponent.setMultipleLine(true);
        textComponent.setTextColor(Color.WHITE);
        textComponent.setTextAlignment(TextAlignment.CENTER);
        textComponent.setText(msg);

        DirectionalLayout.LayoutConfig config = new DirectionalLayout.LayoutConfig();
        config.alignment = LayoutAlignment.CENTER;
        textComponent.setLayoutConfig(config);

        ShapeElement style = new ShapeElement();
        style.setShape(ShapeElement.RECTANGLE);
        style.setCornerRadius(CORNER_RADIUS);
        style.setRgbColor(new RgbColor(flag == 0 ? RGB_COLOR : RGB_RED_COLOR));
        textComponent.setBackground(style);

        new ToastDialog(context)
                .setAlignment(LayoutAlignment.CENTER)
                .setComponent(textComponent)
                .setTransparent(true)
                .setSize(MATCH_PARENT, MATCH_CONTENT)
                .setDuration(DURATION)
                .setAutoClosable(true)
                .show();
    }

    /**
     * toast
     *
     * @param context the context
     * @param msg the toast content
     * @param duration the toast time in ms
     */
    public static void showToast(Context context, String msg, int duration) {
        Text textComponent = new Text(context);
        textComponent.setPadding(TEXT_PADDING, TEXT_PADDING, TEXT_PADDING, TEXT_PADDING);
        textComponent.setMaxTextWidth(aTalkApp.mDisplaySize.width);
        textComponent.setTextSize(TEXT_SIZE);
        textComponent.setMultipleLine(true);
        textComponent.setTextColor(Color.WHITE);
        textComponent.setText(msg);

        DependentLayout layout = new DependentLayout(context);
        layout.setWidth(aTalkApp.mDisplaySize.width);
        layout.setHeight(TEXT_HEIGHT);

        DependentLayout.LayoutConfig config = new DependentLayout.LayoutConfig(MATCH_CONTENT, MATCH_CONTENT);
        config.addRule(DependentLayout.LayoutConfig.HORIZONTAL_CENTER);
        textComponent.setLayoutConfig(config);
        layout.addComponent(textComponent);

        ShapeElement style = new ShapeElement();
        style.setCornerRadius(CORNER_RADIUS);
        style.setRgbColor(new RgbColor(0, 0, 0, TEXT_ALPHA));
        textComponent.setBackground(style);

        new ToastDialog(context)
                .setContentCustomComponent(layout)
                .setOffset(0, TEXT_OFFSETY)
                .setTransparent(true)
                .setSize(aTalkApp.mDisplaySize.width, TEXT_HEIGHT)
                .setDuration(duration)
                .show();
    }

    public static void showToast(Context context, int resId, int duration) {
        String msg = context.getString(resId);
        showToast(context, msg, duration);
    }

    /**
     * show toast
     *
     * @param context context
     * @param message message
     */
    public static void showToast(Context context, String message) {
        DirectionalLayout toastLayout = (DirectionalLayout) LayoutScatter.getInstance(context)
                .parse(ResourceTable.Layout_toast_dialog, null, false);
        Text toastText = toastLayout.findComponentById(ResourceTable.Id_msg_toast);
        toastText.setText(message);

        new ToastDialog(context)
                .setTitleCustomComponent(toastLayout)
                .setAlignment(LayoutAlignment.CENTER)
                .setOffset(0, AttrHelper.vp2px(OFFSET_TOAST, context))
                .setTransparent(true)
                .setSize(MATCH_CONTENT, MATCH_CONTENT)
                .setDuration(LENGTH_LONG)
                .show();
    }
}
