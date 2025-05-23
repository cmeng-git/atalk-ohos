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
package org.atalk.ohos.gui.menu;

import ohos.aafwk.content.Intent;

import org.atalk.ohos.BuildConfig;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.agp.components.Menu;
import org.atalk.ohos.agp.components.MenuInflater;
import org.atalk.ohos.agp.components.MenuItem;
import org.atalk.ohos.gui.About;
import org.atalk.persistance.ServerPersistentStoresRefreshDialog;
import org.atalk.service.osgi.OSGiAbility;
import org.atalk.service.osgi.OSGiService;

/**
 * Extends this activity to handle exit options menu item.
 *
 * @author Eng Chong Meng
 */
public abstract class ExitMenuAbility extends OSGiAbility {
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_menu_exit);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getContext());
        inflater.parse(ResourceTable.Layout_menu_exit, menu);

        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            ((Menu) menu.findComponentById(ResourceTable.Id_menu_exit)).setVisible(true);
            ((Menu) menu.findComponentById(ResourceTable.Id_del_database)).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getId()) {
            // Shutdown application
            case ResourceTable.Id_menu_exit:
                shutdownApplication();
                break;
            case ResourceTable.Id_online_help:
                About.atalkUrlAccess(this, getString(ResourceTable.String_FAQ_Link));
                break;
            case ResourceTable.Id_about:
                startAbility(About.class);
                break;
            // delete database
            case ResourceTable.Id_del_database:
                ServerPersistentStoresRefreshDialog.deleteDB();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * Shutdowns the app by stopping <code>OSGiService</code> and broadcasting action {@link #ACTION_EXIT}.
     */
    public void shutdownApplication() {
        // Shutdown the OSGi service
        Intent exitIntent = new Intent()
                .setAction(ACTION_EXIT)
                .setElementName(getBundleName(), OSGiService.class);
        stopAbility(exitIntent);

        // Broadcast the exit action
        // sendBroadcast(exitIntent);
    }
}
