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
package org.atalk.ohos.gui;

import ohos.aafwk.content.Intent;
import ohos.agp.animation.AnimatorProperty;
import ohos.agp.animation.AnimatorScatter;
import ohos.agp.components.Image;
import ohos.agp.components.ProgressBar;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;

import timber.log.Timber;

/**
 * Splash screen for aTalk start up
 *
 * @author Eng Chong Meng
 */
public class Splash extends BaseAbility {

    private static boolean mFirstRun = true;

    public void onStart(Intent intent) {
        super.onStart(intent);

        setUIContent(ResourceTable.Layout_splash);
        ProgressBar mProgressBar = findComponentById(ResourceTable.Id_actionbar_progress);
        mProgressBar.setIndeterminate(true);
        mProgressBar.setVisibility(ProgressBar.VISIBLE);


        // Starts fade in animation
        Image myImageView = findComponentById(ResourceTable.Id_loadingImage);
        AnimatorScatter scatter = AnimatorScatter.getInstance(getContext());
        AnimatorProperty myFadeInAnimation = (AnimatorProperty) scatter.parse(ResourceTable.Animation_fade_in);
        myFadeInAnimation.setTarget(myImageView);
        myFadeInAnimation.start();
        mFirstRun = false;

        uiHandler.postTask(() -> {
            Timber.d("End of Splash screen Timer");
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            myFadeInAnimation.stop();
            terminateAbility();
        }, 800);
    }

    public static boolean isFirstRun() {
        return mFirstRun;
    }
}
