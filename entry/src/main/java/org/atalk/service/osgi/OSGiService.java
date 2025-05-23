/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.service.osgi;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;
import ohos.rpc.IRemoteObject;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.util.AppUtils;
import org.atalk.impl.appnotification.AppNotifications;
import org.atalk.impl.osgi.OSGiServiceImpl;

import java.security.Security;

/**
 * Implements an Android {@link Ability} which (automatically) starts and stops an OSGi framework (implementation).
 *
 * @author Lyubomir Marinov
 * @author Eng Chong Meng
 */
public class OSGiService extends Ability {
    /**
     * The ID of aTalk notification icon
     */
    private static final String GENERAL_NOTIFICATION_ID = "$string:app_name";

    /**
     * Indicates that aTalk icon is being displayed on android notification tray.
     */
    private static boolean appIcon_shown = false;

    /**
     * Indicates if the service has been started and general notification icon is available
     */
    private static boolean serviceStarted;

    /**
     * This field will be cleared by System.exit() called after shutdown completes.
     */
    private static boolean isShuttingDown;

    /**
     * The very implementation of this Android <code>Service</code> which is split out of the class <code>OSGiService</code> so
     * that the class <code>OSGiService</code> may remain in a <code>service</code> package and be treated as public from the
     * Android point of view and the class <code>OSGiServiceImpl</code> may reside in an <code>impl</code> package and be
     * recognized as internal from the aTalk point of view.
     */
    private final OSGiServiceImpl impl;

    /**
     * Initializes a new <code>OSGiService</code> implementation.
     */
    public OSGiService() {
        impl = new OSGiServiceImpl(this);
    }

    @Override
    public IRemoteObject onConnect(Intent intent) {
        return impl.onConnect(intent);
    }

    /**
     * Protects against starting next OSGi service while the previous one has not completed it's shutdown procedure.
     * This field will be cleared by System.exit() called after shutdown completes.
     */
    private static boolean started;

    public static boolean hasStarted() {
        return started;
    }

    /**
     * This field will be cleared by System.exit() called after shutdown completes.
     */
    private static boolean shuttingDown;

    public static boolean isShuttingDown() {
        return isShuttingDown;
    }

    @Override
    public void onStart(Intent intent) {
        // We are still running
        if (started) {
            return;
        }
        started = true;
        impl.onStart();
    }

    @Override
    public void onStop() {
        if (isShuttingDown) {
            return;
        }
        isShuttingDown = true;
        impl.onStop();
    }

    @Override
    protected void onCommand(Intent intent, boolean restart, int startId) {
        impl.onCommand(intent, restart, startId);
    }

    /**
     * Method called by OSGi impl when start command completes.
     */
    public void onOSGiStarted() {
        serviceStarted = true;

        if (aTalkApp.isIconEnabled()) {
            showIcon();
        }

        aTalkApp.getConfig().addPropertyChangeListener(aTalkApp.SHOW_ICON_PROPERTY_NAME, event -> {
            if (aTalkApp.isIconEnabled()) {
                showIcon();
            }
            else {
                hideIcon();
            }
        });
    }

    /**
     * Start the service in foreground and creates shows general notification icon.
     */
    private void showIcon() {
        String title = getString(ResourceTable.String_app_name);
        // The intent to launch when the user clicks the expanded notification
        PendingIntent pendIntent = aTalkApp.getaTalkIconIntent();

        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this, AppNotifications.DEFAULT_GROUP);
        nBuilder.setContentTitle(title)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(ResourceTable.Media_ic_notification)
                .setNumber(0)
                .setContentIntent(pendIntent);

        Notification notice = nBuilder.build();
        notice.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

        this.startForeground(GENERAL_NOTIFICATION_ID, notice);
        mNotificationManager.notify(GENERAL_NOTIFICATION_ID, notice);
        appIcon_shown = true;
    }

    /**
     * Stops the foreground service and hides general notification icon
     */
    public void stopForegroundService() {
        hideIcon();
        serviceStarted = false;
    }

    private void hideIcon() {
        if (running_foreground) {
            stopForeground(true);
            appIcon_shown = false;
            AppUtils.generalNotificationInvalidated();
        }
		mNotificationManager.cancel(GENERAL_NOTIFICATION_ID);
        appIcon_shown = false;
    }

    /**
     * Returns general notification ID that can be used to post notification bound to our global icon
     * in android notification tray
     *
     * @return the notification ID greater than 0 or -1 if service is not running
     */
    public static int getGeneralNotificationId() {
        if (serviceStarted && appIcon_shown) {
            return GENERAL_NOTIFICATION_ID;
        }
        return -1;
    }

    static {
        Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 1);
    }
}
