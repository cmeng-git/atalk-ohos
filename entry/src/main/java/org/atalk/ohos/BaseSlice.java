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
package org.atalk.ohos;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.ability.AbilitySlice;
import ohos.app.Context;
import ohos.eventhandler.EventRunner;

/**
 * Class extends {@link AbilitySlice} provide some basic support.
 *
 * @author Eng Chong Meng
 */
public class BaseSlice extends AbilitySlice {
    protected final Context mContext;
    protected final Ability mAbility;

    /**
     * {@inheritDoc}
     */
    public BaseSlice() {
        super();
        mContext = getContext();
        mAbility = getAbility();
    }

    /**
     * Convenience method for running code on UI thread looper.
     *
     * @param action <code>Runnable</code> action to execute on UI thread.
     */
    public static void runOnUiThread(Runnable action) {
        if (EventRunner.getMainEventRunner().isCurrentRunnerThread()) {
            action.run();
        }
        else {
            // Post action to the ui looper
            BaseAbility.uiHandler.postTask(action);
        }
    }
}
