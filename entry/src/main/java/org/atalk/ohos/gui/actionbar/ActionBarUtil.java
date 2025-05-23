/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.actionbar;

import ohos.aafwk.ability.Ability;
import ohos.agp.components.Image;
import ohos.agp.components.Text;
import ohos.app.Context;
import ohos.media.image.PixelMap;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.util.AppImageUtil;

import timber.log.Timber;

/**
 * The <code>ActionBarUtil</code> provides utility methods for setting action bar avatar and display name.
 *
 * @author Yana Stamcheva
 * @author Eng Chong Meng
 */
public class ActionBarUtil {
    /**
     * Sets the action bar title for the given activity.
     *
     * @param activity the <code>Ability</code>, for which we set the action bar title
     * @param title the title string to set
     */
    public static void setTitle(Ability activity, String title) {
        ActionBar actionBar = activity.getSupportActionBar();
        // Some activities don't have ActionBar
        if (actionBar != null) {
            if (actionBar.getCustomView() != null) {
                Text actionBarText = activity.findComponentById(ResourceTable.Id_actionBarTitle);
                if (actionBarText != null)
                    actionBarText.setText(title);
            }
            else
                actionBar.setTitle(title);
        }
    }

    /**
     * Sets the action bar subtitle for the given activity. The text may contain
     * a, Account user online status
     * b. The chat buddy last seen date or online status
     * c. Callee Jid during media call
     *
     * @param activity the <code>Ability</code>, for which we set the action bar subtitle
     * @param subtitle the subtitle string to set
     */
    public static void setSubtitle(Ability activity, String subtitle) {
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            Text statusText = activity.findComponentById(ResourceTable.Id_actionBarStatus);
            // statusText is null while search option is selected
            if (statusText != null) {
                statusText.setText(subtitle);
                // statusText.setMovementMethod(new ScrollingMovementMethod());
                // Must have setSelected() to get text to start scroll
                statusText.setSelected(true);
            }
        }
    }

    /**
     * Gets the action bar subTitle for the given activity.
     *
     * @param activity the <code>Ability</code>, for which we get the action bar title
     *
     * @return the title string
     */
    public static String getStatus(Ability activity) {
        if (activity != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            // Some activities don't have ActionBar
            if (actionBar == null)
                return null;

            Text actionBarText = activity.findComponentById(ResourceTable.Id_actionBarStatus);
            return (actionBarText.getText());
        }
        return null;
    }

    /**
     * Get the user offline status during the selected Locale.
     * Quiet messy to use this method as the user online status is being updated from multiple places
     * including server presence status sending etc.
     *
     * @param activity the caller context
     *
     * @return use online status
     */
    public static boolean isOffline(Ability activity) {
        String offlineLabel = activity.getString(ResourceTable.String_offline);
        return offlineLabel.equals(ActionBarUtil.getStatus(activity));
    }

    /**
     * Set the action bar status for the given activity.
     *
     * @param activity the <code>Ability</code>, for which we get the action bar title
     * @param statusIcon display Icon per the user status
     */
    public static void setStatusIcon(Ability activity, byte[] statusIcon) {
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            PixelMap avatarStatusBmp = AppImageUtil.pixelMapFromBytes(statusIcon);
            if (avatarStatusBmp != null) {
                Image actionBarStatus = activity.findComponentById(ResourceTable.Id_globalStatusIcon);
                // actionBarStatus is null while search option is selected
                if (actionBarStatus != null)
                    actionBarStatus.setPixelMap(avatarStatusBmp);
            }
        }
    }

    /**
     * Sets the avatar icon of the action bar.
     *
     * @param activity the current activity where the status should be displayed
     * @param avatar the avatar to display
     */
    public static void setAvatar(Ability activity, byte[] avatar) {
        // The default avatar pixelMap for display on ActionBar
        PixelMap avatarPixelMap = getDefaultAvatarPixelMap(activity);

        // cmeng: always clear old avatar picture when pager scroll to different chat fragment
        // and invalidate pixelMap for scrolled page to update Logo properly
        // cmeng: 20200312: seems no necessary anymore? so disable it seems ok now
        // avatarDrawable.invalidateDrawable(avatarDrawable);

        PixelMap avatarBmp = null;
        if (avatar != null) {
            if (avatar.length < 256 * 1024) {
                avatarBmp = AppImageUtil.getCircularPixelMapFromBytes(avatar);
            }
            else {
                Timber.e("Avatar image is too large: %s", avatar.length);
            }
            if (avatarBmp != null) {
                avatarPixelMap.DrawableByLayerId(ResourceTable.Id_avatarDrawable, avatarBmp);
            }
            else {
                Timber.e("Failed to get avatar pixelMap from bytes");
            }
        }
        // set Logo is only available when there is no customView attached or during search
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            if (actionBar.getCustomView() == null)
                actionBar.setLogo(avatarPixelMap);
            else {
                Image logo = activity.findComponentById(ResourceTable.Id_logo);
                if (logo != null)
                    logo.setPixelMap(avatarPixelMap);
            }
        }
    }

    public static void setAvatar(Ability activity, int resId) {
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            if (actionBar.getCustomView() == null)
                actionBar.setLogo(resId);
            else {
                Image logo = activity.findComponentById(ResourceTable.Id_logo);
                if (logo != null)
                    logo.setPixelMap(resId);
            }
        }
    }

    /**
     * Returns the default avatar {@link PixelMap}
     *
     * @return the default avatar {@link PixelMap}
     */
    private static PixelMap getDefaultAvatarPixelMap(Context context) {
        return AppImageUtil.getPixelMap(context, ResourceTable.Media_avatar);
    }
}
