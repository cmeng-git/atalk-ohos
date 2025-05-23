/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui;

import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.ProgressBar;
import ohos.agp.components.Text;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.actionbar.ActionBarUtil;
import org.atalk.service.osgi.OSGiService;

/**
 * Ability displayed when shutdown procedure is in progress.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class ShutdownAbility extends BaseAbility {
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        if (!OSGiService.hasStarted()) {
            Operation operation = new Intent.OperationBuilder()
                    .withBundleName(getBundleName())
                    .withAbilityName(LauncherAbility.class)
                    .build();

            intent.setOperation(operation);
            startAbility(intent);
            terminateAbility();
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Disable up arrow
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            ActionBarUtil.setTitle(this, getTitle());
        }

        setUIContent(ResourceTable.Layout_splash);
        Text shutDown = findComponentById(ResourceTable.Id_stateInfo);
        shutDown.setText(ResourceTable.String_shutting_down);

        ProgressBar mActionBarProgress = findComponentById(ResourceTable.Id_actionbar_progress);
        mActionBarProgress.setVisibility(ProgressBar.VISIBLE);
    }
}
