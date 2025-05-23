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
package org.atalk.impl.appnotification;

import net.java.sip.communicator.service.notification.NotificationService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import timber.log.Timber;

/**
 * Bundle adds Android specific notification handlers.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class NotificationActivator implements BundleActivator {
    /**
     * OSGI bundle context.
     */
    protected static BundleContext bundleContext;

    /**
     * Notification service instance.
     */
    private static NotificationService notificationService;

    /**
     * Vibrate handler instance.
     */
    private VibrateHandlerImpl vibrateHandler;

    /**
     * {@inheritDoc}
     */
    public void start(BundleContext bc)
            throws Exception {
        bundleContext = bc;
        // Get the notification service implementation
        ServiceReference<?> notifyReference = bundleContext.getServiceReference(NotificationService.class.getName());

        notificationService = (NotificationService) bundleContext.getService(notifyReference);
        vibrateHandler = new VibrateHandlerImpl();
        notificationService.addActionHandler(vibrateHandler);
        Timber.i("Android notification handler Service...[REGISTERED]");
    }

    /**
     * {@inheritDoc}
     */
    public void stop(BundleContext bc)
            throws Exception {
        notificationService.removeActionHandler(vibrateHandler.getActionType());
        Timber.d("Android notification handler Service ...[STOPPED]");
    }
}
