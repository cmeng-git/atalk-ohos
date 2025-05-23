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
package org.atalk.ohos.gui.account;

import ohos.aafwk.content.Intent;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;

import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.util.ComponentUtil;

/**
 * AbilitySlice for history message delete with media delete option
 *
 * @author Eng Chong Meng
 */
public class AccountDeleteSlice extends BaseSlice
{
    public static final String ARG_MESSAGE = "dialog_message";

    @Override
    public void onStart(Intent intent) {
        LayoutScatter inflater = LayoutScatter.getInstance(getContext());

        Component viewAccountDelete = inflater.parse(ResourceTable.Layout_account_delete, null, false);
        ComponentUtil.setTextViewValue(viewAccountDelete, ResourceTable.Id_textView, getArguments().getString(ARG_MESSAGE));
    }
}
