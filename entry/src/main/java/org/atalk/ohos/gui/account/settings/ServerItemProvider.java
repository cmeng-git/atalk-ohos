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

import ohos.aafwk.ability.Ability;
import ohos.agp.components.BaseItemProvider;
import ohos.app.Context;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.gui.dialogs.DialogA;

/**
 * Class is used in {@link ServerListAbility} to handle list model. It also provides the edit dialog fragment for it's
 * items.
 *
 * @author Eng Chong Meng
 */
abstract class ServerItemProvider extends BaseItemProvider {
    /**
     * Parent {@link Ability} used as a context
     */
    protected final Context mContext;

    /**
     * Creates new instance of {@link ServerItemProvider}
     *
     * @param parent the parent {@link Ability} used as a context
     */
    public ServerItemProvider(Context parent) {
        mContext = parent;
    }

    public long getItemId(int i) {
        return i;
    }

    /**
     * Request list repaint
     */
    protected void refresh() {
        BaseAbility.runOnUiThread(this::notifyDataChanged);
    }

    /**
     * Factory method should return a {@link DialogA} that will allow user to
     * edit list item at specified <code>position</code>.
     *
     * @param position the position of item to edit
     *
     * @return the {@link DialogA} that should wil be displayed when item is clicked
     */
    abstract DialogA createItemEditDialog(int position);

}
