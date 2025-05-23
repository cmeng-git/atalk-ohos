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
package org.atalk.ohos.plugin.certconfig;

import ohos.aafwk.content.Intent;
import ohos.agp.components.Button;
import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Picker;
import ohos.app.Context;

import net.java.sip.communicator.service.certificate.CertificateConfigEntry;
import net.java.sip.communicator.service.certificate.CertificateService;

import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.ResourceTable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.security.Security;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Advanced configuration form to define client TLS certificate templates.
 *
 * @author Eng Chong Meng
 */
public class TLS_Configuration extends BaseSlice
        implements Component.ClickedListener, Component.CheckedChangedListener,
        ListContainer.OnItemSelectedListener, PropertyChangeListener, CertConfigEntryDialog.OnFinishedCallback {
    private CertificateService cvs;

    /**
     * Certificate spinner list for selection
     */
    private final List<String> mCertList = new ArrayList<>();
    private ArrayAdapter<String> certAdapter;

    private Picker certSpinner;
    private CertificateConfigEntry mCertEntry = null;

    /**
     * A map of <row, CertificateConfigEntry>
     */
    private final Map<Integer, CertificateConfigEntry> mCertEntryList = new LinkedHashMap<>();

    private Checkbox chkEnableOcsp;

    private Button cmdRemove;
    private Button cmdEdit;

    private Context mContext;

    @Override
    public void onStart(Intent intent) {
        LayoutScatter inflater = LayoutScatter.getInstance(getContext());
        mContext = getContext();
        cvs = CertConfigActivator.getCertService();
        CertConfigActivator.getConfigService().addPropertyChangeListener(this);

        Component content = inflater.parse(ResourceTable.Layout_cert_tls_config, container, false);

        Checkbox chkEnableRevocationCheck = content.findComponentById(ResourceTable.Id_cb_crl);
        chkEnableRevocationCheck.setOnCheckedChangeListener(this);

        chkEnableOcsp = content.findComponentById(ResourceTable.Id_cb_ocsp);
        chkEnableOcsp.setOnCheckedChangeListener(this);

        certSpinner = content.findComponentById(ResourceTable.Id_cboCert);
        initCertSpinner();

        Button mAdd = content.findComponentById(ResourceTable.Id_cmd_add);
        mAdd.setClickedListener(this);

        cmdRemove = content.findComponentById(ResourceTable.Id_cmd_remove);
        cmdRemove.setClickedListener(this);

        cmdEdit = content.findComponentById(ResourceTable.Id_cmd_edit);
        cmdEdit.setClickedListener(this);
    }

    private void initCertSpinner() {
        initCertList();
        certAdapter = new ArrayAdapter<>(mContext, ResourceTable.Layout_simple_spinner_item, mCertList);
        certAdapter.setDropDownViewResource(ResourceTable.Layout_simple_spinner_dropdown_item);
        certSpinner.setItemProvider(certAdapter);
        certSpinner.setOnItemSelectedListener(this);
    }

    private void initCertList() {
        mCertList.clear();
        List<CertificateConfigEntry> certEntries = cvs.getClientAuthCertificateConfigs();
        for (int idx = 0; idx < certEntries.size(); idx++) {
            CertificateConfigEntry entry = certEntries.get(idx);
            mCertList.add(entry.toString());
            mCertEntryList.put(idx, entry);
        }
    }

    @Override
    public void onClick(Component v) {
        CertConfigEntryDialog dialog;
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        ft.addToBackStack(null);

        switch (v.getId()) {
            case ResourceTable.Id_cmd_add:
                dialog = CertConfigEntryDialog.getInstance(CertificateConfigEntry.CERT_NONE, this);
                dialog.show(ft, "CertConfigEntry");
                break;

            case ResourceTable.Id_cmd_remove:
                if (mCertEntry != null) {
                    Timber.d("Certificate Entry removed: %s", mCertEntry.getId());
                    CertConfigActivator.getCertService().removeClientAuthCertificateConfig(mCertEntry.getId());
                }
                break;

            case ResourceTable.Id_cmd_edit:
                if (mCertEntry != null) {
                    dialog = CertConfigEntryDialog.getInstance(mCertEntry, this);
                    dialog.show(ft, "CertConfigEntry");
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(Button buttonView, boolean isChecked) {
        String enabled = Boolean.valueOf(isChecked).toString();
        switch (buttonView.getId()) {
            case ResourceTable.Id_cb_crl:
                CertConfigActivator.getConfigService().setProperty(
                        CertificateService.PNAME_REVOCATION_CHECK_ENABLED, isChecked);

                System.setProperty(CertificateService.SECURITY_CRLDP_ENABLE, enabled);
                System.setProperty(CertificateService.SECURITY_SSL_CHECK_REVOCATION, enabled);
                chkEnableOcsp.setEnabled(isChecked);
                break;

            case ResourceTable.Id_cb_ocsp:
                CertConfigActivator.getConfigService().setProperty(
                        CertificateService.PNAME_OCSP_ENABLED, isChecked);
                Security.setProperty(CertificateService.SECURITY_OCSP_ENABLE, enabled);
                break;
        }
    }

    @Override
    public void onItemSelected(ListContainer<?> adapter, Component view, int pos, long id) {
        if (adapter.getId() == ResourceTable.Id_cboCert) {
            certSpinner.setSelection(pos);

            mCertEntry = mCertEntryList.get(pos);
            cmdRemove.setEnabled(true);
            cmdEdit.setEnabled(true);
        }
    }

    @Override
    public void onNothingSelected(ListContainer<?> parent) {
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().startsWith(CertificateService.PNAME_CLIENTAUTH_CERTCONFIG_BASE)) {
            initCertList();
            certAdapter.notifyDataChanged();
        }
    }

    @Override
    public void onCloseDialog(Boolean success, CertificateConfigEntry entry) {
        if (success) {
            CertConfigActivator.getCertService().setClientAuthCertificateConfig(entry);
        }
    }
}
