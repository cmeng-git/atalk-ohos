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

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.Component;
import ohos.agp.components.Text;
import ohos.app.Context;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.dialogs.DialogA;

import timber.log.Timber;

/**
 * Ability displays the certificate to the user and asks him whether to trust the certificate.
 * It also uses <code>CertificateInfoDialog</code> to display detailed information about the certificate.
 *
 * @author Eng Chong Meng
 */
public class VerifyCertificateAbility extends BaseAbility implements CertificateDialog.CertInfoDialogListener {
    /**
     * Request identifier extra key.
     */
    private static final String REQ_ID = "request_id";

    /**
     * Request identifier used to retrieve dialog model.
     */
    private long requestId;

    /**
     * Dialog model.
     */
    private VerifyCertDialog certDialog;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        this.requestId = intent.getLongParam(REQ_ID, -1);
        if (requestId == -1) {
            return;  // not serious enough to throw exception
            // throw new RuntimeException("No request id supplied");
        }

        certDialog = CertificateDialogActivator.getDialog(requestId);
        if (certDialog == null) {
            Timber.e("No dialog instance found for %s", requestId);
            terminateAbility();
            return;
        }

        // Prevents from closing the dialog on outside window touch
        // setFinishOnTouchOutside(false);
        // setTitle(certDialog.getTitle());
        Text msgView = findComponentById(ResourceTable.Id_message);
        msgView.setText(certDialog.getMsg());

        setUIContent(ResourceTable.Layout_verify_certificate);
    }

    /**
     * Method fired when "show certificate info" button is clicked.
     *
     * @param v button's <code>Component</code>
     */
    public void onShowCertClicked(Component v) {
        DialogA certDialog = new CertificateDialog(this, requestId).createDialog();
        certDialog.show();
    }

    /**
     * Method fired when continue button is clicked.
     *
     * @param v button's <code>Component</code>
     */
    public void onContinueClicked(Component v) {
        certDialog.setTrusted(true);
        terminateAbility();
    }

    /**
     * Method fired when cancel button is clicked.
     *
     * @param v button's <code>Component</code>
     */
    public void onCancelClicked(Component v) {
        certDialog.setTrusted(false);
        terminateAbility();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (certDialog != null)
            certDialog.notifyFinished();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDialogResult(boolean continueAnyway) {
        if (continueAnyway) {
            onContinueClicked(null);
        }
        else {
            onCancelClicked(null);
        }
    }

    /**
     * Creates new parametrized <code>Intent</code> for <code>VerifyCertificateAbility</code>.
     *
     * @param ctx Android context.
     * @param requestId request identifier of dialog model.
     *
     * @return new parametrized <code>Intent</code> for <code>VerifyCertificateAbility</code>.
     */
    public static Intent createIntent(Context ctx, Long requestId) {
        Intent intent = new Intent();
        Operation operation =
                new Intent.OperationBuilder()
                        .withBundleName(ctx.getBundleName())
                        .withAbilityName(VerifyCertificateAbility.class)
                        .build();
        intent.setOperation(operation);
        intent.setParam(REQ_ID, requestId);
        return intent;
    }
}
