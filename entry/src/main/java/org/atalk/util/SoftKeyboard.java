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
package org.atalk.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ohos.accessibility.ability.SoftKeyBoardController;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.TextField;
import ohos.eventhandler.EventHandler;

import timber.log.Timber;

import static ohos.accessibility.ability.AccessibleAbility.SHOW_MODE_AUTO;
import static ohos.accessibility.ability.AccessibleAbility.SHOW_MODE_HIDE;

public class SoftKeyboard implements Component.FocusChangedListener {
    private static final int CLEAR_FOCUS = 0;

    private final ComponentContainer mLayout;
    private int layoutBottom;
    private final SoftKeyBoardController sKBController;
    private final int[] coords;
    private boolean isKeyboardShow;
    private final SoftKeyboardChangesThread softKeyboardThread;
    private List<TextField> editTextList;

    // reference to a focused TextField
    private Component focusComponent;

    public SoftKeyboard(ComponentContainer layout, SoftKeyBoardController skbController) {
        mLayout = layout;
        keyboardHideByDefault();
        initEditTexts(layout);
        this.sKBController = skbController;
        this.coords = new int[2];
        this.isKeyboardShow = false;
        this.softKeyboardThread = new SoftKeyboardChangesThread();
        this.softKeyboardThread.start();
    }

    public void openSoftKeyboard() {
        if (!isKeyboardShow) {
            layoutBottom = getLayoutCoordinates();
            sKBController.setShowMode(SHOW_MODE_AUTO);
            softKeyboardThread.keyboardOpened();
            isKeyboardShow = true;
        }
    }

    public void closeSoftKeyboard() {
        if (isKeyboardShow) {
            sKBController.setShowMode(SHOW_MODE_HIDE);
            isKeyboardShow = false;
        }
    }

    public void setSoftKeyboardCallback(SoftKeyboardChanged mCallback) {
        softKeyboardThread.setCallback(mCallback);
    }

    public void unRegisterSoftKeyboardCallback() {
        softKeyboardThread.stopThread();
    }

    public interface SoftKeyboardChanged {
        void onSoftKeyboardHide();

        void onSoftKeyboardShow();
    }

    private int getLayoutCoordinates() {
        mLayout.getLocationOnScreen();
        return coords[1] + mLayout.getHeight();
    }

    private void keyboardHideByDefault() {
        mLayout.setFocusable(Component.FOCUS_ENABLE);
        // mLayout.setFocusableInTouchMode(true);
    }

    /*
     * InitEditTexts now handles EditTexts in nested views
     * Thanks to Francesco Verheye (verheye.francesco@gmail.com)
     */
    private void initEditTexts(ComponentContainer viewgroup) {
        if (editTextList == null)
            editTextList = new ArrayList<>();

        int childCount = viewgroup.getChildCount();
        for (int i = 0; i <= childCount - 1; i++) {
            Component v = viewgroup.getComponentAt(i);

            if (v instanceof ComponentContainer) {
                initEditTexts((ComponentContainer) v);
            }

            if (v instanceof TextField) {
                TextField editText = (TextField) v;
                editText.setFocusChangedListener(this);
                editText.setTextCursorVisible(true);
                editTextList.add(editText);
            }
        }
    }

    /*
     * OnFocusChange does update tempView correctly now when keyboard is still shown
     * Thanks to Israel Dominguez (dominguez.israel@gmail.com)
     */
    @Override
    public void onFocusChange(Component v, boolean hasFocus) {
        if (hasFocus) {
            focusComponent = v;
            if (!isKeyboardShow) {
                layoutBottom = getLayoutCoordinates();
                softKeyboardThread.keyboardOpened();
                isKeyboardShow = true;
            }
        }
    }

    // This handler will clear focus of selected TextField
    private final EventHandler mHandler = new EventHandler() {
        @Override
        public void handleMessage(Message m) {
            switch (m.what) {
                case CLEAR_FOCUS:
                    if (focusComponent != null) {
                        focusComponent.clearFocus();
                        focusComponent = null;
                    }
                    break;
            }
        }
    };

    private class SoftKeyboardChangesThread extends Thread {
        private final AtomicBoolean started;
        private SoftKeyboardChanged mCallback;

        public SoftKeyboardChangesThread() {
            started = new AtomicBoolean(true);
        }

        public void setCallback(SoftKeyboardChanged mCallback) {
            this.mCallback = mCallback;
        }

        @Override
        public void run() {
            while (started.get()) {
                // Wait until keyboard is requested to open
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Timber.w("Exception in starting keyboard thread: %s", e.getMessage());
                    }
                }

                int currentBottomLocation = getLayoutCoordinates();
                // Timber.d("SoftKeyboard Current Bottom Location #1: %s (%s)", currentBottomLocation, layoutBottom);

                // There is some lag between open soft-keyboard function and when it really appears.
                while (currentBottomLocation == layoutBottom && started.get()) {
                    currentBottomLocation = getLayoutCoordinates();
                }
                // Timber.d("SoftKeyboard Current Bottom Location #2: %s (%s)", currentBottomLocation, layoutBottom);

                if (started.get())
                    mCallback.onSoftKeyboardShow();

                // When keyboard is opened from TextField, initial bottom location is greater than
                // layoutBottom and at some moment later <= layoutBottom.
                // That broke the previous logic, so I added this new loop to handle this.
                while (currentBottomLocation >= layoutBottom && started.get()) {
                    currentBottomLocation = getLayoutCoordinates();
                }

                // Now Keyboard is shown, keep checking layout dimensions until keyboard is gone
                while (currentBottomLocation != layoutBottom && started.get()) {
                    // Timber.d("SoftKeyboard Current Bottom Location #3x %s (%s)", currentBottomLocation, layoutBottom);
                    synchronized (this) {
                        try {
                            wait(500);
                        } catch (InterruptedException e) {
                            Timber.w("Exception in waiting for keyboard hide: %s", e.getMessage());
                        }
                    }
                    currentBottomLocation = getLayoutCoordinates();
                }

                if (started.get())
                    mCallback.onSoftKeyboardHide();

                // if keyboard has been opened clicking and TextField.
                if (isKeyboardShow && started.get())
                    isKeyboardShow = false;

                // if an TextField is focused, remove its focus (on UI thread)
                if (started.get())
                    mHandler.obtainMessage(CLEAR_FOCUS).sendToTarget();
            }
        }

        public void keyboardOpened() {
            synchronized (this) {
                notify();
            }
        }

        public void stopThread() {
            synchronized (this) {
                started.set(false);
                notify();
            }
        }
    }
}