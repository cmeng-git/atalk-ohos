package org.atalk.ohos.gui.chat;

import ohos.aafwk.content.Intent;

import org.atalk.ohos.BaseAbility;

import java.net.URI;
import java.net.URISyntaxException;
import timber.log.Timber;

/**
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class aTalkProtocolReceiver extends BaseAbility
{
    @Override
    protected void onStart(Intent intent)
    {
        super.onStart(intent);
        Timber.i("aTalk protocol intent received %s", intent);

        String urlStr = intent.getDataString();
        if (urlStr != null) {
            try {
                URI url = new URI(urlStr);
                ChatSessionManager.notifyChatLinkClicked(url);
            } catch (URISyntaxException e) {
                Timber.e(e, "Error parsing clicked URL");
            }
        }
        else {
            Timber.w("No URL supplied in aTalk link");
        }
        terminateAbility();
    }
}
