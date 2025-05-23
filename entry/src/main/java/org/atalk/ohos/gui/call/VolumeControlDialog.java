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

import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ProgressBar;
import ohos.agp.components.Slider;
import ohos.agp.window.dialog.CommonDialog;
import ohos.app.Context;

import org.atalk.impl.neomedia.MediaServiceImpl;
import org.atalk.impl.neomedia.NeomediaActivator;
import org.atalk.ohos.ResourceTable;
import org.atalk.service.neomedia.VolumeControl;
import org.atalk.service.neomedia.event.VolumeChangeEvent;
import org.atalk.service.neomedia.event.VolumeChangeListener;

/**
 * The dialog allows user to manipulate input or output volume gain level. To specify which one will be
 * manipulated by current instance the DIRECTION_XX should be specified with one of direction values.
 *
 * @author Eng Chong Meng
 */
public class VolumeControlDialog extends CommonDialog implements VolumeChangeListener, Slider.ValueChangedListener {
    /**
     * The direction argument value for output volume gain.
     */
    public static final int DIRECTION_OUTPUT = 0;

    /**
     * The direction argument value for input volume gain.
     */
    public static final int DIRECTION_INPUT = 1;

    /**
     * Abstract volume control used by this dialog.
     */
    private final VolumeControl volumeControl;

    private final Slider volumeSlider;

    public VolumeControlDialog(Context context, int direction) {
        super(context);
        LayoutScatter inflater = LayoutScatter.getInstance(context);
        Component content = inflater.parse(ResourceTable.Layout_volume_control, null, false);

        volumeSlider = content.findComponentById(ResourceTable.Id_seekBar);
        volumeSlider.setValueChangedListener(this);

        // Selects input or output volume control based on the arguments.
        int titleStrId = -1;
        MediaServiceImpl mediaService = NeomediaActivator.getMediaServiceImpl();
        if (direction == DIRECTION_OUTPUT) {
            titleStrId = ResourceTable.String_volume_control_title;
            volumeControl = mediaService.getOutputVolumeControl();
        }
        else if (direction == DIRECTION_INPUT) {
            titleStrId = ResourceTable.String_mic_control_title;
            volumeControl = mediaService.getInputVolumeControl();
        }
        else {
            throw new IllegalArgumentException();
        }

        setTitleText(context.getString(titleStrId));
        setContentCustomComponent(content);
    }

    @Override
    protected void onShow() {
        super.onShow();
        volumeControl.addVolumeChangeListener(this);

        // Initialize volume bar
        int progress = getVolumeBarProgress(volumeSlider, volumeControl.getVolume());
        volumeSlider.setProgressValue(progress);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        volumeControl.removeVolumeChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public void volumeChange(VolumeChangeEvent volumeChangeEvent) {
        int progress = getVolumeBarProgress(volumeSlider, volumeChangeEvent.getLevel());
        volumeSlider.setProgressValue(progress);
    }

    /**
     * Calculates the progress value suitable for given <code>Slider</code> from the device volume level.
     *
     * @param volumeBar the <code>Slider</code> for which the progress value will be calculated.
     * @param volLevel actual volume level from <code>VolumeControl</code>. Value <code>-1.0</code> means
	 * the level is invalid and default progress value should be provided.
     *
     * @return the progress value calculated from given volume level that will be suitable for specified <code>Slider</code>.
     */
    private int getVolumeBarProgress(ProgressBar volumeBar, float volLevel) {
        if (volLevel == -1.0) {
            // If the volume is invalid position at the middle
            volLevel = getVolumeCtrlRange() / 2;
        }

        float progress = volLevel / getVolumeCtrlRange();
        return (int) (progress * volumeBar.getMax());
    }

    /**
     * Returns abstract volume control range calculated for volume control min and max values.
     *
     * @return the volume control range calculated for current volume control min and max values.
     */
    private float getVolumeCtrlRange() {
        return volumeControl.getMaxValue() - volumeControl.getMinValue();
    }

    /**
     * {@inheritDoc}
     */
    public void onProgressUpdated(Slider seekBar, int progress, boolean fromUser) {
        if (!fromUser)
            return;

        float position = (float) progress / (float) seekBar.getMax();
        volumeControl.setVolume(getVolumeCtrlRange() * position);
    }

    /**
     * {@inheritDoc}
     */
    public void onTouchStart(Slider seekBar) {
    }

    /**
     * {@inheritDoc}
     */
    public void onTouchEnd(Slider seekBar) {
    }
}
