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
package org.atalk.ohos.gui.widgets;

import ohos.agp.animation.Animator;
import ohos.agp.animation.AnimatorProperty;
import ohos.agp.components.Component;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;

/**
 * Animated version of {@link LegacyClickableToastCtrl}
 *
 * @author Eng Chong Meng
 */
public class ClickableToastController extends LegacyClickableToastCtrl {
    /**
     * animation length
     */
    private static final long ANIM_DURATION = 2000;

    /**
     * The animator object used to animate toast <code>Component.</code> alpha property.
     */
    private final AnimatorProperty mToastAnimator;

    /**
     * Creates new instance of <code>ClickableToastController</code>.
     *
     * @param toastView the <code>Component.</code> that will be animated. Must contain <code>ResourceTable.Id_toast_msg</code> <code>Text</code>.
     * @param clickListener the click listener that will be notified when the toast is clicked.
     */
    public ClickableToastController(Component toastView, Component.ClickedListener clickListener) {
        this(toastView, clickListener, ResourceTable.Id_toast_msg);
    }

    /**
     * Creates new instance of <code>ClickableToastController</code>.
     *
     * @param toastView the <code>Component.</code> that will be animated. Must contain <code>ResourceTable.Id_toast_msg</code> <code>Text</code>.
     * @param clickListener the click listener that will be notified when the toast is clicked.
     * @param toastButtonId the id of <code>Component.</code> contained in <code>toastView
     * </code> that will be used as a button.
     */
    public ClickableToastController(Component toastView, Component.ClickedListener clickListener, int toastButtonId) {
        super(toastView, clickListener, toastButtonId);

        // Initialize animator
        mToastAnimator = new AnimatorProperty(toastView);
        mToastAnimator.setCurveType(Animator.CurveType.CYCLE); // ("alpha");
    }

    /**
     * Shows the toast.
     *
     * @param immediate if <code>true</code> there wil be no animation.
     * @param message the toast text to use.
     */
    @Override
    public void showToast(boolean immediate, String message) {
        // Must process in UI thread as caller can be from background
        BaseAbility.runOnUiThread(() -> {
            super.showToast(immediate, message);
            if (!immediate) {
                mToastAnimator.cancel();
                mToastAnimator.alphaFrom(0).alpha(1);
                mToastAnimator.setDuration(ANIM_DURATION);
                mToastAnimator.start();
            }
        });
    }

    /**
     * Hides the toast.
     *
     * @param immediate if <code>true</code> no animation will be used.
     */
    @Override
    public void hideToast(boolean immediate) {
        super.hideToast(immediate);
        if (!immediate) {
            mToastAnimator.cancel();
            mToastAnimator.alphaFrom(1).alpha(0);
            mToastAnimator.setDuration(ANIM_DURATION);
            mToastAnimator.start();
            mToastAnimator.setStateChangedListener(new Animator.StateChangedListener() {
                @Override
                public void onStart(Animator animator) {

                }

                @Override
                public void onStop(Animator animator) {

                }

                @Override
                public void onCancel(Animator animator) {

                }

                @Override
                public void onEnd(Animator animator) {
                    onHide();
                }

                @Override
                public void onPause(Animator animator) {

                }

                @Override
                public void onResume(Animator animator) {

                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onHide() {
        super.onHide();
        toastView.setAlpha(0);
    }
}
