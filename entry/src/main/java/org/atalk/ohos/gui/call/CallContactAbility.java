/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.call;

import ohos.aafwk.content.Intent;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.aTalk;

/**
 * Tha <code>CallContactAbility</code> can be used to call contact. The phone number can be filled
 * from <code>Intent</code> data.
 *
 * @author Yana Stamcheva
 * @author Pawel Domas
 */
public class CallContactAbility extends BaseAbility {
    protected CallContactSlice ccFragment;

    /**
     * Called when the activity is starting. Initializes the corresponding call interface.
     *
     * @param intent transfers information between objects; consists of the operation and parameters attributes
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);

        // There's no need to create fragment if the Ability is being restored.
        if (mInState == null) {
            // Create new call contact fragment
            String phoneNumber = null;
            Intent intent = getIntent();
            if (intent.getDataString() != null)
                phoneNumber = PhoneNumberUtils.getNumberFromIntent(intent, this);
            ccFragment = CallContactSlice.newInstance(phoneNumber);
            getSupportFragmentManager().beginTransaction().add(ResourceTable.Id_content, ccFragment).commit();
        }
    }

    @Override
    public void onRequestPermissionsFromUserResult(int requestCode, String[] permissions, int[] grantResults) {
        // If request is canceled, the result arrays are empty.
        super.onRequestPermissionsFromUserResult(requestCode, permissions, grantResults);
        if (requestCode == aTalk.PRC_GET_CONTACTS) {
            if ((grantResults.length > 0)
                    && (grantResults[0] == IBundleManager.PERMISSION_GRANTED)) {
                // permission granted, so proceed
                ccFragment.initAndroidAccounts();
            }
        }
    }
}