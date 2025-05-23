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

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;

import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/**
 * Android implementation of <code>VerifyCertificateDialogService</code>.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
class CertificateDialogServiceImpl implements VerifyCertificateDialogService
{
    /**
     * Maps request ids to <code>VerifyCertDialog</code> so that they can be retrieved by Android
     * <code>Ability</code> or <code>AbilitySlices</code>.
     */
    private final Map<Long, VerifyCertDialog> requestMap = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public VerifyCertificateDialog createDialog(Certificate[] certs, String title, String message)
    {
        if (title == null)
            title = aTalkApp.getResString(ResourceTable.String_cert_dialog_title);

        Long requestId = System.currentTimeMillis();
        VerifyCertDialog verifyCertDialog = new VerifyCertDialog(requestId, certs[0], title, message);

        requestMap.put(requestId, verifyCertDialog);
        Timber.d("%d creating dialog: %s", hashCode(), requestId);
        // Prevents from closing the dialog on outside touch

        return verifyCertDialog;
    }

    /**
     * Retrieves the dialog for given <code>requestId</code>.
     *
     * @param requestId dialog's request identifier assigned during dialog creation.
     * @return the dialog for given <code>requestId</code>.
     */
    public VerifyCertDialog retrieveDialog(Long requestId)
    {
        Timber.d("%d getting dialog: %d", hashCode(), requestId);
        return requestMap.get(requestId);
    }
}
