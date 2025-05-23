/*
 * aTalk, android VoIP and Instant Messaging client
 * Copyright 2014~2024 Eng Chong Meng
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

import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.app.Context;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.util.ComponentUtil;
import ohos.utils.PacMap;
public class DialogComponent extends Component {
    public static final String ARG_MESSAGE = "dialog_message";
    public static final String ARG_CB_MESSAGE = "cb_message";
    public static final String ARG_CB_CHECK = "cb_check";
    public static final String ARG_CB_ENABLE = "cb_enable";

    public DialogComponent(Context context, PacMap pacMap) {
        super(context);

        LayoutScatter inflater = LayoutScatter.getInstance(context);
        Component component = inflater.parse(ResourceTable.Layout_dialog_component, null, false);

        ComponentUtil.setTextViewValue(component, ResourceTable.Id_messageText, pacMap.getString(ARG_MESSAGE));
        ComponentUtil.setTextViewValue(component, ResourceTable.Id_cb_option, pacMap.getString(ARG_CB_MESSAGE));
        ComponentUtil.setCompoundChecked(component, ResourceTable.Id_cb_option, pacMap.getBooleanValue(ARG_CB_CHECK, false));
        ComponentUtil.ensureEnabled(component, ResourceTable.Id_cb_option, pacMap.getBooleanValue(ARG_CB_ENABLE, false));
    }
}
