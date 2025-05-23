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
package org.atalk.ohos.gui.call;

import ohos.aafwk.content.Intent;
import ohos.sensor.agent.CategoryLightAgent;
import ohos.sensor.bean.CategoryLight;
import ohos.sensor.data.CategoryLightData;
import ohos.sensor.listener.ICategoryLightDataCallback;

import org.atalk.ohos.BaseSlice;
import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

/**
 * This fragment when added to parent <code>VideoCallAbility</code> will listen for
 * proximity sensor updates and turn the screen on and off when NEAR/FAR distance is detected.
 *
 * @author Eng Chong Meng
 */
public class ProximitySensorSlice extends BaseSlice {
    private static final int SENSOR_DELAY_UI = 1000;

    /**
     * Proximity sensor managed used by this fragment.
     */
    CategoryLightAgent categoryLightAgent = new CategoryLightAgent();

    private CategoryLight proximitySensor;

    private ICategoryLightDataCallback lightDataCallback;

    /**
     * Unreliable sensor status flag.
     */
    private boolean sensorDisabled = true;

    /**
     * Instant of fragmentManager for screen off Dialog creation
     */
    private FragmentManager fm = null;

    /**
     * Instant of screen off Dialog - dismiss in screenOn()
     */
    private static ScreenOffDialog screenOffDialog = null;


    @Override
    protected void onStart(Intent intent) {

        lightDataCallback = new ICategoryLightDataCallback() {
            @Override
            public void onSensorDataModified(CategoryLightData categoryLightData) {
                if (sensorDisabled)
                    return;

                float proximity = categoryLightData.values[0];
                Timber.i("Proximity updated: " + proximity);

                if (proximity > 0.3) {
                    screenOn();
                }
                else {
                    screenOff();
                }
            }

            @Override
            public void onAccuracyDataModified(CategoryLight categoryLight, int accuracy) {
                if (accuracy == -1) { //SensorManager.SENSOR_STATUS_UNRELIABLE) {
                    sensorDisabled = true;
                    screenOn();
                }
                else {
                    sensorDisabled = false;
                }
            }

            @Override
            public void onCommandCompleted(CategoryLight categoryLight) {
                // The sensor executes the command callback.
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActive() {
        super.onActive();

        // Skips if the sensor has been already attached
        if (proximitySensor != null) {
            proximitySensor = categoryLightAgent.getSingleSensor(CategoryLightAgent.SENSOR_CATEGORY_LIGHT);
            categoryLightAgent.setSensorDataCallback(lightDataCallback, proximitySensor, SENSOR_DELAY_UI);
            Timber.i("Using proximity sensor: %s", proximitySensor.getName());
        }
        else {
            return;
        }
        sensorDisabled = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInactive() {
        super.onInactive();
        if (proximitySensor != null) {
            screenOn();
            categoryLightAgent.releaseSensorDataCallback(lightDataCallback, proximitySensor);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        super.onStop();
        if (proximitySensor != null) {
            screenOn();
            categoryLightAgent.releaseSensorDataCallback(lightDataCallback, proximitySensor);
            proximitySensor = null;
        }
    }

    /**
     * Turns the screen off.
     */
    private void screenOff() {
        // ScreenOff exist - proximity detection screen on is out of sync; so just reuse the existing one
//		if (screenOffDialog != null) {
//			Timber.w("screenOffDialog exist when trying to perform screenOff");
//		}
        screenOffDialog = new ScreenOffDialog();
        screenOffDialog.show(fm, "screen_off_dialog");
    }

    /**
     * Turns the screen on.
     */
    private void screenOn() {
        if (screenOffDialog != null) {
            screenOffDialog.dismiss();
            screenOffDialog = null;
        }
//        else {
//			Timber.w("screenOffDialog was null when trying to perform screenOn");
//        }
    }

    /**
     * Blank full screen dialog that captures all keys (BACK is what interest us the most).
     */
    public static class ScreenOffDialog extends DialogFragment {
        private CallVolumeCtrlFragment volControl;

        @Override
        public void onActive() {
            super.onActive();
            volControl = ((VideoCallAbility) getAbility()).getVolCtrlSlice();
        }

        @Override
        public void onInactive() {
            super.onInactive();
            volControl = null;
        }

        @NotNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

            Dialog d = super.onCreateDialog(savedInstanceState);
            d.setUIContent(ResourceTable.Layout_screen_off);

            d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            d.setOnKeyListener((dialog, keyCode, event) -> {
                // Capture all events, but dispatch volume keys to volume control fragment
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (volControl != null) {
                        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                            volControl.onKeyVolUp();
                        }
                        else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                            volControl.onKeyVolDown();
                        }
                    }
                }
                // Force to exit Screen Block in case device sensor is not responding
                else if (keyCode == KeyEvent.KEYCODE_BACK && screenOffDialog != null) {
                    screenOffDialog.dismiss();
                    screenOffDialog = null;
                }
                return true;
            });
            return d;
        }
    }
}
