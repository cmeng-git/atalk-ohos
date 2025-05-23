/*
 * aTalk, android VoIP and Instant Messaging client
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
package org.atalk.impl.appupdate;

import net.java.sip.communicator.util.ServiceUtils;
import net.java.sip.communicator.util.SimpleServiceActivator;

import org.atalk.service.configuration.ConfigurationService;
import org.osgi.framework.BundleContext;

/**
 * Android update service activator.
 *
 * @author Eng Chong Meng
 */
public class UpdateActivator extends SimpleServiceActivator<UpdateServiceImpl> {
    /**
     * <code>BundleContext</code> instance.
     */
    static BundleContext bundleContext;

    /**
     * Creates new instance of <code>UpdateActivator</code>.
     */
    public UpdateActivator() {
        super(UpdateServiceImpl.class, "Android update service");
    }

    /**
     * Gets the <code>ConfigurationService</code> using current <code>BundleContext</code>.
     *
     * @return the <code>ConfigurationService</code>
     */
    public static ConfigurationService getConfiguration() {
        return ServiceUtils.getService(bundleContext, ConfigurationService.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected UpdateServiceImpl createServiceImpl() {
        return new UpdateServiceImpl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(BundleContext bundleContext)
            throws Exception {
        UpdateActivator.bundleContext = bundleContext;
        super.start(bundleContext);
        serviceImpl.removeOldDownloads();
    }
}
