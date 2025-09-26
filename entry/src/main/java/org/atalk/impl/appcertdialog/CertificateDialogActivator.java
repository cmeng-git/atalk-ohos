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
package org.atalk.impl.appcertdialog;

import net.java.sip.communicator.service.certificate.VerifyCertificateDialogService;
import net.java.sip.communicator.util.SimpleServiceActivator;

import org.osgi.framework.BundleContext;

/**
 * Activator of <code>VerifyCertificateDialogService</code> Android implementation.
 *
 * @author Pawel Domas
 */
public class CertificateDialogActivator extends SimpleServiceActivator<CertificateDialogServiceImpl> {
    /**
     * Creates a new instance of CertificateDialogActivator.
     */
    public CertificateDialogActivator() {
        super(VerifyCertificateDialogService.class, "Android verify certificate service");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CertificateDialogServiceImpl createServiceImpl() {
        impl = new CertificateDialogServiceImpl();
        return impl;
    }

    /**
     * Cached instance of service impl.
     */
    public static CertificateDialogServiceImpl impl;

    /**
     * Gets the <code>VerifyCertDialog</code> for given <code>requestId</code>.
     *
     * @param requestId identifier of the request managed by <code>CertificateDialogServiceImpl</code>.
     *
     * @return <code>VerifyCertDialog</code> for given <code>requestId</code> or <code>null</code> if service has been shutdown.
     */
    public static VerifyCertDialog getDialog(Long requestId) {
        if (impl != null) {
            return impl.retrieveDialog(requestId);
        }
        else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(BundleContext bundleContext)
            throws Exception {
        super.stop(bundleContext);
        // Clears service reference
        impl = null;
    }
}
