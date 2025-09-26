/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.settings.notification;

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.Text;
import ohos.app.Context;
import ohos.utils.net.Uri;

import net.java.sip.communicator.plugin.notificationwiring.SoundProperties;
import net.java.sip.communicator.service.notification.NotificationAction;
import net.java.sip.communicator.service.notification.NotificationChangeListener;
import net.java.sip.communicator.service.notification.NotificationService;
import net.java.sip.communicator.service.notification.SoundNotificationAction;
import net.java.sip.communicator.service.notification.event.NotificationActionTypeEvent;
import net.java.sip.communicator.service.notification.event.NotificationEventTypeEvent;
import net.java.sip.communicator.util.ServiceUtils;

import org.atalk.impl.androidresources.AppResourceServiceImpl;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.actionbar.ActionBarToggleSlice;
import org.atalk.ohos.gui.actionbar.ActionBarUtil;
import org.atalk.service.resources.ResourceManagementService;

/**
 * The screen that displays notification event details. It allows user to enable/disable the whole
 * event as well as adjust particular notification handlers like popups, sound or vibration.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class NotificationDetails extends BaseAbility
        implements NotificationChangeListener, ActionBarToggleSlice.ActionBarToggleModel {
    /**
     * Event type extra key
     */
    private final static String EVENT_TYPE_EXTRA = "event_type";

    /**
     * The event type string that identifies the event
     */
    private String eventType;

    /**
     * Notification service instance
     */
    private NotificationService notificationService;

    /**
     * Resource service instance
     */
    private ResourceManagementService rms;

    /**
     * The description <code>Component.</code>
     */
    private Text description;

    /**
     * Popup handler checkbox <code>Component</code>
     */
    private Button popup;

    /**
     * Sound notification handler checkbox <code>Component.</code>
     */
    private Button soundNotification;

    /**
     * Sound playback handler checkbox <code>Component.</code>
     */
    private Button soundPlayback;

    /**
     * Vibrate handler checkbox <code>Component.</code>
     */
    private Button vibrate;

    // Sound Descriptor variable
    private Button mSoundDescriptor;

    private String eventTitle;
    private String ringToneTitle;

    private Uri soundDefaultUri;
    private Uri soundDescriptorUri;
    Ringtone ringTone = null;
    SoundNotificationAction soundHandler;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        this.eventType = getIntent().getStringExtra(EVENT_TYPE_EXTRA);
        if (eventType == null)
            throw new IllegalArgumentException();

        this.notificationService = ServiceUtils.getService(AppGUIActivator.bundleContext, NotificationService.class);
        this.rms = ServiceUtils.getService(AppGUIActivator.bundleContext, ResourceManagementService.class);

        setUIContent(ResourceTable.Layout_notification_details);
        this.description = findComponentById(ResourceTable.Id_description);
        this.popup = findComponentById(ResourceTable.Id_popup);
        this.soundNotification = findComponentById(ResourceTable.Id_soundNotification);
        this.soundPlayback = findComponentById(ResourceTable.Id_soundPlayback);
        this.vibrate = findComponentById(ResourceTable.Id_vibrate);

        AbilityResultLauncher<Integer> mPickRingTone = pickRingTone();
        mSoundDescriptor = findComponentById(ResourceTable.Id_sound_descriptor);
        mSoundDescriptor.setClickedListener(view -> {
            // set RingTone picker to show only the relevant notification or ringtone
            if (soundHandler.getLoopInterval() < 0)
                mPickRingTone.launch(RingtoneManager.TYPE_NOTIFICATION);
            else
                mPickRingTone.launch(RingtoneManager.TYPE_RINGTONE);
        });

        // ActionBarUtil.setTitle(this, aTalkApp.getStringResourceByName(NotificationSettings.N_PREFIX + eventType));
        eventTitle = rms.getI18NString(NotificationSettings.NOTICE_PREFIX + eventType);
        ActionBarUtil.setTitle(this, eventTitle);

        // The SoundNotification init
        initSoundNotification();

        if (mInState == null) {
            getSupportFragmentManager().beginTransaction().add(ActionBarToggleSlice.newInstance(""),
                    "action_bar_toggle").commit();
        }
    }

    /**
     * Initialize all the sound Notification parameters on entry
     */
    private void initSoundNotification() {
        soundHandler = (SoundNotificationAction)
                notificationService.getEventNotificationAction(eventType, NotificationAction.ACTION_SOUND);

        if (soundHandler != null) {
            String soundFile = "android.resource://" + getPackageName() + "/" + SoundProperties.getSoundDescriptor(eventType);
            soundDefaultUri = Uri.parse(soundFile);

            String descriptor = soundHandler.getDescriptor();
            if (descriptor.startsWith(AppResourceServiceImpl.PROTOCOL)) {
                soundDescriptorUri = soundDefaultUri;
                ringToneTitle = eventTitle;
            }
            else {
                soundDescriptorUri = Uri.parse(descriptor);
                ringToneTitle = RingtoneManager.getRingtone(this, soundDescriptorUri).getTitle(this);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActive() {
        super.onActive();
        updateDisplay();
        notificationService.addNotificationChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onInactive() {
        super.onInactive();
        notificationService.removeNotificationChangeListener(this);
        if (ringTone != null) {
            ringTone.stop();
            ringTone = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    private void updateDisplay() {
        boolean enable = notificationService.isActive(eventType);

        // Description
        // description.setText(aTalkApp.getStringResourceByName(NotificationSettings.N_PREFIX + eventType + "_description"));
        description.setText(rms.getI18NString(NotificationSettings.NOTICE_PREFIX + eventType + ".description"));
        description.setEnabled(enable);

        // The popup
        NotificationAction popupHandler
                = notificationService.getEventNotificationAction(eventType, NotificationAction.ACTION_POPUP_MESSAGE);
        popup.setEnabled(enable && popupHandler != null);
        if (popupHandler != null)
            popup.setChecked(popupHandler.isEnabled());

        soundNotification.setEnabled(enable && soundHandler != null);
        soundPlayback.setEnabled(enable && soundHandler != null);

        // if soundHandler is null then hide the sound file selection else init its attributes
        if (soundHandler != null) {
            soundNotification.setChecked(soundHandler.isSoundNotificationEnabled());
            soundPlayback.setChecked(soundHandler.isSoundPlaybackEnabled());
            mSoundDescriptor.setText(ringToneTitle);
        }
        else {
            findComponentById(ResourceTable.Id_soundAttributes).setVisibility(Component.HIDE);
        }

        // Vibrate action
        NotificationAction vibrateHandler
                = notificationService.getEventNotificationAction(eventType, NotificationAction.ACTION_VIBRATE);
        vibrate.setEnabled(enable && vibrateHandler != null);
        if (vibrateHandler != null)
            vibrate.setChecked(vibrateHandler.isEnabled());
    }

    /**
     * Fired when popup checkbox is clicked.
     *
     * @param v popup checkbox <code>Component</code>
     */
    public void onPopupClicked(Component v) {
        boolean enabled = ((CompoundButton) v).isChecked();

        NotificationAction action
                = notificationService.getEventNotificationAction(eventType, NotificationAction.ACTION_POPUP_MESSAGE);
        action.setEnabled(enabled);
        notificationService.registerNotificationForEvent(eventType, action);
    }

    /**
     * Fired when sound notification checkbox is clicked.
     *
     * @param v sound notification checkbox <code>Component</code>
     */
    public void onSoundNotificationClicked(Component v) {
        boolean enabled = ((CompoundButton) v).isChecked();
        soundHandler.setSoundNotificationEnabled(enabled);
        notificationService.registerNotificationForEvent(eventType, soundHandler);
    }

    /**
     * Fired when sound playback checkbox is clicked.
     *
     * @param v sound playback checkbox <code>Component</code>
     */
    public void onSoundPlaybackClicked(Component v) {
        boolean enabled = ((CompoundButton) v).isChecked();
        soundHandler.setSoundPlaybackEnabled(enabled);
        notificationService.registerNotificationForEvent(eventType, soundHandler);
    }

    /**
     * Fired when vibrate notification checkbox is clicked.
     *
     * @param v vibrate notification checkbox <code>Component</code>
     */
    public void onVibrateClicked(Component v) {
        boolean enabled = ((CompoundButton) v).isChecked();

        NotificationAction action
                = notificationService.getEventNotificationAction(eventType, NotificationAction.ACTION_VIBRATE);
        action.setEnabled(enabled);
        notificationService.registerNotificationForEvent(eventType, action);
    }

    /**
     * Toggle play mode for the ringtone when user clicks the play/pause button;
     *
     * @param v playback view
     */
    public void onPlayBackClicked(Component v) {
        if (ringTone == null) {
            ringTone = RingtoneManager.getRingtone(this, soundDescriptorUri);
        }

        if (ringTone.isPlaying()) {
            ringTone.stop();
            ringTone = null;
        }
        else
            ringTone.play();
    }

    /**
     * PIckRingtone class AbilityResultContract implementation, with ringtoneType of either:
     * 1. RingtoneManager.TYPE_NOTIFICATION
     * 2. RingtoneManager.TYPE_RINGTONE
     */
    public class PickRingtone extends AbilityResultContract<Integer, Uri> {
        @NonNull
        @Override
        public Intent createIntent(Context context, Integer ringtoneType) {
            final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.setParam(RingtoneManager.EXTRA_RINGTONE_TITLE, eventTitle);
            intent.setParam(RingtoneManager.EXTRA_RINGTONE_TYPE, ringtoneType);
            intent.setParam(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, soundDefaultUri);
            intent.setParam(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, soundDescriptorUri);
            return intent;
        }

        @Override
        public Uri parseResult(int resultCode, @Nullable Intent result) {
            if (BaseAbility.RESULT_OK !=  resultCode || result == null) {
                return null;
            }
            return result.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        }
    }

    /**
     * Opens a FileChooserDialog to let the user pick attachments
     */
    private AbilityResultLauncher<Integer> pickRingTone() {
        return registerForAbilityResult(new PickRingtone(), ringToneUri -> {
            if (ringToneUri == null) {
                ringToneUri = soundDefaultUri;
            }
            updateSoundNotification(ringToneUri);
        });
    }

    /**
     * Update the display and setup the SoundNotification with the newly user selected ringTone
     *
     * @param ringToneUri user selected ringtone Uri
     */
    private void updateSoundNotification(Uri ringToneUri) {
        String soundDescriptor;
        if (soundDefaultUri.equals(ringToneUri)) {
            ringToneTitle = eventTitle;
            soundDescriptor = SoundProperties.getSoundDescriptor(eventType);
        }
        else {
            ringTone = RingtoneManager.getRingtone(this, ringToneUri);
            ringToneTitle = ringTone.getTitle(this);
            soundDescriptor = ringToneUri.toString();
        }
        soundDescriptorUri = ringToneUri;

        soundHandler.setDescriptor(soundDescriptor);
        notificationService.registerNotificationForEvent(eventType, soundHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionAdded(NotificationActionTypeEvent event) {
        handleActionEvent(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionRemoved(NotificationActionTypeEvent event) {
        handleActionEvent(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionChanged(NotificationActionTypeEvent event) {
        handleActionEvent(event);
    }

    /**
     * Handles add/changed/removed notification action events by refreshing the display if the event
     * is related with the one currently displayed.
     *
     * @param event the event object
     */
    private void handleActionEvent(NotificationActionTypeEvent event) {
        if (event.getEventType().equals(eventType)) {
            runOnUiThread(this::updateDisplay);
        }
    }

    /**
     * {@inheritDoc} Not interested in type added event.
     */
    @Override
    public void eventTypeAdded(NotificationEventTypeEvent event) {
    }

    /**
     * {@inheritDoc}
     * <p>
     * If removed event is the one currently displayed, closes the <code>Ability</code>.
     */
    @Override
    public void eventTypeRemoved(NotificationEventTypeEvent event) {
        if (!event.getEventType().equals(eventType))
            return;

        // Event no longer exists
        runOnUiThread(this::terminateAbility);
    }

    /**
     * Gets the <code>Intent</code> for starting <code>NotificationDetails</code> <code>Ability</code>.
     *
     * @param ctx the context
     * @param eventType name of the event that will be displayed by <code>NotificationDetails</code>.
     *
     * @return the <code>Intent</code> for starting <code>NotificationDetails</code> <code>Ability</code>.
     */
    public static Intent getIntent(Context ctx, String eventType) {
        Operation operation = new Intent.OperationBuilder()
                .withBundleName(ctx.getBundleName())
                .withAbilityName(NotificationDetails.class)
                .build();

        Intent intent = new Intent();
        intent.setParam(EVENT_TYPE_EXTRA, eventType);
        intent.setOperation(operation);
        return intent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isChecked() {
        return notificationService.isActive(eventType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setChecked(boolean isChecked) {
        notificationService.setActive(eventType, isChecked);
        updateDisplay();
    }
}
