/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.settings.widget;

import org.atalk.impl.neomedia.codec.video.MediaDecoder;
import org.atalk.impl.neomedia.codec.video.MediaEncoder;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.service.configuration.ConfigurationService;

/**
 * Class that handles common attributes and operations for all configuration widgets.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class ConfigWidgetUtil implements OnBindEditTextListener {
    /**
     * The parent <code>Preference</code> handled by this instance.
     */
    private final Preference parent;

    /**
     * Flag indicates whether configuration property should be stored in separate thread to prevent network on main
     * thread exceptions.
     */
    private boolean useNewThread;

    /**
     * Flag indicates whether value should be mapped to the summary.
     */
    private boolean mapSummary;
    private int mInputType = EditorInfo.TYPE_NULL;

    /**
     * Creates new instance of <code>ConfigWidgetUtil</code> for given <code>parent</code> <code>Preference</code>.
     *
     * @param parent the <code>Preference</code> that will be handled by this instance.
     */
    public ConfigWidgetUtil(Preference parent) {
        this.parent = parent;
    }

    /**
     * Creates new instance of <code>ConfigWidgetUtil</code> for given <code>parent</code> <code>Preference</code>.
     *
     * @param parent the <code>Preference</code> that will be handled by this instance.
     * @param mapSummary indicates whether value should be displayed as a summary
     */
    public ConfigWidgetUtil(Preference parent, boolean mapSummary) {
        this.parent = parent;
        this.mapSummary = true;
    }

    /**
     * PArses the attributes. Should be called by parent <code>Preference</code>.
     *
     * @param context the Android context
     * @param attrs the attribute set
     */
    void parseAttributes(Context context, AttributeSet attrs) {
        TypedArray attArray = context.obtainStyledAttributes(attrs, R.styleable.ConfigWidget);
        useNewThread = attArray.getBoolean(R.styleable.ConfigWidget_storeInNewThread, false);
        mapSummary = attArray.getBoolean(R.styleable.ConfigWidget_mapSummary, mapSummary);
    }

    /**
     * Updates the summary if necessary. Should be called by parent <code>Preference</code> on value initialization.
     *
     * @param value the current value
     */
    void updateSummary(Object value) {
        if (mapSummary) {
            String text = (value != null) ? value.toString() : "";
            if (parent instanceof EditTextPreference) {
                if ((mInputType != EditorInfo.TYPE_NULL)
                        && ((mInputType & InputType.TYPE_MASK_VARIATION) == InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                    text = text.replaceAll("(?s).", "*");
                }
            }
            parent.setSummary(text);
        }
    }

    @Override
    public void onBindEditText(TextField editText) {
        mInputType = editText.getInputType();
    }

    /**
     * Persists new value through the <code>getConfigurationService</code>.
     *
     * @param value the new value to persist.
     */
    public void handlePersistValue(final Object value) {
        updateSummary(value);
        Thread store = new Thread() {
            @Override
            public void run() {
                ConfigurationService confService = AppGUIActivator.getConfigurationService();
                if (confService != null) {
                    confService.setProperty(parent.getKey(), value);
                    if (parent.getKey().equals(MediaDecoder.HW_DECODING_ENABLE_PROPERTY)) {
                        setSurfaceOption(MediaDecoder.DIRECT_SURFACE_DECODE_PROPERTY, value);
                    }
                    else if (parent.getKey().equals(MediaEncoder.HW_ENCODING_ENABLE_PROPERTY)) {
                        setSurfaceOption(MediaEncoder.DIRECT_SURFACE_ENCODE_PROPERTY, value);
                    }
                }
            }
        };

        if (useNewThread)
            store.start();
        else
            store.run();
    }

    /**
     * Couple the codec surface enable option to the codec option state;
     * Current aTalk implementation requires surface option for android codec to be selected by fmj
     *
     * @param key surface preference key
     * @param value the value to persist.
     */
    private void setSurfaceOption(String key, Object value) {
        AppGUIActivator.getConfigurationService().setProperty(key, value);
        ConfigCheckBox surfaceEnable = parent.getPreferenceManager().findPreference(key);
        if (surfaceEnable != null) {
            surfaceEnable.setChecked((Boolean) value);
        }
    }
}
