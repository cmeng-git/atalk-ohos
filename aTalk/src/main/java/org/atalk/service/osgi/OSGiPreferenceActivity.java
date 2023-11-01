/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.service.osgi;

import androidx.appcompat.app.ActionBar;

import org.atalk.hmos.aTalkApp;
import org.atalk.hmos.gui.actionbar.ActionBarUtil;

/**
 * Copy of <code>OSGiActivity</code> that extends <code>PreferenceActivity</code>.
 *
 * @author Eng Chong Meng
 */
public class OSGiPreferenceActivity extends OSGiActivity {
    @Override
    protected void configureToolBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Disable up arrow on home activity
            Class<?> homeActivity = aTalkApp.getHomeScreenActivityClass();
            if (this.getClass().equals(homeActivity)) {
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setHomeButtonEnabled(false);
            }
            ActionBarUtil.setTitle(this, getTitle());
        }
    }
}
