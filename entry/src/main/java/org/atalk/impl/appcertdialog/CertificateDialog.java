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

import java.security.cert.Certificate;

import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.webengine.WebConfig;
import ohos.agp.components.webengine.WebView;
import ohos.app.Context;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.dialogs.DialogA;

/**
 * The dialog that displays certificate details. It allows user to mark the certificate as "always trusted".
 * The dialog details are created dynamically in html format. That's because it's certificate implementation
 * dependent. Parent <code>Ability</code> must implement <code>CertInfoDialogListener</code>.
 *
 * @author Eng Chong Meng
 */
public class CertificateDialog {
    private final CertInfoDialogListener mListener;
    private final Context mContext;
    private final long mRequestId;

    /**
     * @param requestId identifier of dialog model managed by <code>CertificateDialogServiceImpl</code>
     *
     */
    public CertificateDialog(Context context, long requestId) {
        mListener = (CertInfoDialogListener) context;
        mContext = context;
        mRequestId = requestId;
    }

    public DialogA createDialog() {
        VerifyCertDialog certDialog = CertificateDialogActivator.impl.retrieveDialog(mRequestId);
        if (certDialog == null)
            throw new RuntimeException("No dialog model found for: " + mRequestId);

        // Alert view and its title
        DialogA.Builder builder = new DialogA.Builder(mContext);
        builder.setTitle(certDialog.getTitle());

        // Prevents from closing the dialog on outside window touch
        builder.setOutsideTouchClosable(false);

        // Certificate content in html format
        LayoutScatter scatter = LayoutScatter.getInstance(mContext);
        Component component = scatter.parse(ResourceTable.Layout_cert_info, null, false);
        component.findComponentById(ResourceTable.Id_continueBtn).setClickedListener(clickedListener);
        component.findComponentById(ResourceTable.Id_cancel).setClickedListener(clickedListener);

        WebView certView = component.findComponentById(ResourceTable.Id_certificateInfo);
        WebConfig settings = certView.getWebConfig();
        settings.setTextAutoSizing(true);
        // settings.setDefaultFixedFontSize(10);
        // settings.setBuiltInZoomControls(true);

        Certificate cert = certDialog.getCertificate();
        X509CertificateView certInfo = new X509CertificateView(mContext);
        String certHtml = certInfo.toString(cert);
        certView.load(certHtml, "text/html", true);

        // Always trust checkbox
        // Updates always trust property of dialog model
        Checkbox alwaysTrustBtn = component.findComponentById(ResourceTable.Id_alwaysTrust);
        alwaysTrustBtn.setCheckedStateChangedListener((buttonView, isChecked) -> {
            CertificateDialogActivator.getDialog(mRequestId).setAlwaysTrust(isChecked);
        });

        // contentView.findComponentById(ResourceTable.Id_dummyView).setVisibility(Component.HIDE);
        return builder.setComponent(component)
                .create();
    }

    private final Component.ClickedListener clickedListener = new Component.ClickedListener() {
        @Override
        public void onClick(Component component) {
            switch (component.getId()) {
                case ResourceTable.Id_continueBtn:
                    mListener.onDialogResult(true);
                    break;

                case ResourceTable.Id_cancel:
                    mListener.onDialogResult(false);
                    break;
            }
        }
    };

    /**
     * Interface used to pass dialog result to parent <code>Ability</code>.
     */
    public interface CertInfoDialogListener {
        /**
         * Fired when dialog is dismissed. Passes the result as an argument.
         *
         * @param continueAnyway <code>true</code> if continue anyway button was pressed, <code>false</code>
         * means that the dialog was discarded or cancel button was pressed.
         */
        void onDialogResult(boolean continueAnyway);
    }
}
