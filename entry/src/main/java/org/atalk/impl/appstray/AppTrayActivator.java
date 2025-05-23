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
package org.atalk.impl.appstray;

import net.java.sip.communicator.service.systray.SystrayService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import timber.log.Timber;

/**
 * Android tray service activator.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class AppTrayActivator implements BundleActivator {
    /**
     * OSGI bundle context
     */
    public static BundleContext bundleContext;

    /**
     * <code>SystrayServiceImpl</code> instance.
     */
    private SystrayServiceImpl systrayService;

    /**
     * {@inheritDoc}
     */
    public void start(BundleContext bundleContext)
            throws Exception {
        AppTrayActivator.bundleContext = bundleContext;

        // Create the notification service implementation
        this.systrayService = new SystrayServiceImpl();

        bundleContext.registerService(SystrayService.class.getName(), systrayService, null);
        systrayService.start();

        Timber.i("Systray Service ...[REGISTERED]");
    }

    /**
     * {@inheritDoc}
     */
    public void stop(BundleContext bundleContext)
            throws Exception {
        systrayService.stop();
    }
}
