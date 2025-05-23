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
package org.atalk.ohos.gui.settings;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.ability.AbilitySlice;
import ohos.global.resource.NotExistException;

import java.io.IOException;

/**
 * Class can be used to build that require OSGI services access.
 *
 * @author Eng Chong Meng
 */
public class BasePreferenceSlice extends AbilitySlice {
    private final Ability mAbility;


    public BasePreferenceSlice() {
        super();
        mAbility = getAbility();
    }
    /**
     * Set preference title using android inbuilt toolbar
     *
     * @param resId preference tile resourceID
     */
    public void setPrefTitle(int resId) {
        if (getAbility() == null)
            return;

        try {
            String title = mAbility.getResourceManager().getIdentifier(resId);
        } catch (IOException | NotExistException e) {
            throw new RuntimeException(e);
        }

//        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
//                    | ActionBar.DISPLAY_USE_LOGO
//                    | ActionBar.DISPLAY_SHOW_TITLE);
//
//            actionBar.setLogo(ResourceTable.Media_ic_icon);
//            actionBar.setTitle(resId);
//        }
    }
}
