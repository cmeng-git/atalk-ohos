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

import ohos.agp.colors.RgbColor;
import ohos.agp.components.ComponentState;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.components.element.StateElement;
import ohos.app.Context;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.util.ResourceTool;

public class ButtonElement extends StateElement {
    private final Context mContext;

    private final int[] mStateFlagEmpty = new int[]{ComponentState.COMPONENT_STATE_EMPTY};

    private final int[] mStateFlagPressed = new int[]{ComponentState.COMPONENT_STATE_PRESSED};

    public static final int TYPE_NORMAL = 0;

    public static final int TYPE_ACCENT = 1;

    public static final int TYPE_ERROR = 2;

    public ButtonElement(Context context, int type) {
        super();
        mContext = context;
        int[][] mAllTypeColorIds = {
                {
                        ResourceTable.Color_colorNormal, ResourceTable.Color_colorNormalPressed
                },
                {
                        ResourceTable.Color_colorAccent, ResourceTable.Color_colorAccentPressed
                },
                {
                        ResourceTable.Color_colorError, ResourceTable.Color_colorErrorPressed
                }
        };
        initBackground(mAllTypeColorIds[type]);
    }

    private void initBackground(int[] colorIds) {
        this.addState(mStateFlagPressed, new ShapeElement() {
            {
                setRgbColor(RgbColor.fromArgbInt(ResourceTool.getColor(mContext, colorIds[1], 0)));
                setCornerRadius(ResourceTool.getFloat(mContext, ResourceTable.Float_dialog_corner_radius, 0));
            }
        });
        this.addState(mStateFlagEmpty, new ShapeElement() {
            {
                setRgbColor(RgbColor.fromArgbInt(ResourceTool.getColor(mContext, colorIds[0], 0)));
                setCornerRadius(ResourceTool.getFloat(mContext, ResourceTable.Float_dialog_corner_radius, 0));
            }
        });
    }
}
