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
package org.atalk.ohos.gui.share;

import java.util.ArrayList;

import ohos.aafwk.content.Intent;
import ohos.agp.components.Text;
import ohos.utils.net.Uri;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.agp.components.Menu;
import org.atalk.ohos.agp.components.MenuItem;

/**
 * ShareAbility is defined as SingleTask, to avoid multiple instances being created if user does not exit
 * this activity before start another sharing.
 * <p>
 * ShareAbility provides multiple contacts sharing. However, this requires aTalk does not have any
 * chatFragment current in active open state. Otherwise, Android OS destroys this activity on first
 * contact sharing; and multiple contacts sharing is no possible.
 *
 * @author Eng Chong Meng
 */
public class ShareAbility extends BaseAbility {
    /**
     * A reference of the share object
     */
    private static Share mShare;

    /**
     * mText2 is used in aTalk to sore msgContent if multiple type sharing is requested by user
     */
    private static class Share {
        String mText2;
        ArrayList<Uri> uris = new ArrayList<>();
        public String action;
        public String type;
        public String text;

        public void clear() {
            mText2 = null;
            uris = new ArrayList<>();
            action = null;
            type = null;
            text = null;
        }
    }

    /**
     * Called when the activity is starting. Initializes the corresponding call interface.
     *
     * @param intent transfers information between objects; consists of the operation and parameters attributes
     */
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_sharewith_view);
        // configureToolBar();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            Text tv = findComponentById(ResourceTable.Id_actionBarTitle);
            tv.setText(ResourceTable.String_app_name);

            tv = findComponentById(ResourceTable.Id_actionBarStatus);
            tv.setText(ResourceTable.String_share);
            actionBar.setsetBackgroundDrawable(new ColorDrawable(getResources().getColor(ResourceTable.Color_color_bg_share)));
        }

        ContactListFragment contactList = new ContactListAbilitySlicdeFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameContainer, contactList)
                .commit();

        mShare = new Share();
        handleIntent(getIntent());
    }

    /**
     * Called when new <code>Intent</code> is received(this <code>Ability</code> is launched in <code>singleTask</code> mode.
     *
     * @param intent new <code>Intent</code> data.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /**
     * Decides what should be displayed based on supplied <code>Intent</code> and instance state.
     *
     * @param intent <code>Ability</code> <code>Intent</code>.
     */
    private void handleIntent(Intent intent) {
        super.onStart(intent);
        if (intent == null) {
            return;
        }
        final String type = intent.getType();
        final String action = intent.getAction();

        mShare.clear();
        mShare.type = type;
        mShare.action = action;

        if (ACTION_SEND.equals(action)) {
            final String text = intent.getStringParam(EXTRA_TEXT);
            final Uri uri = intent.getSequenceableParam(EXTRA_STREAM);

            if (type != null && uri != null) {
                mShare.uris.clear();
                mShare.uris.add(uri);
            }
            else {
                mShare.text = text;
            }
        }
        else if (ACTION_SEND_MULTIPLE.equals(action)) {
            final ArrayList<Uri> uris = intent.getSequenceableArrayListParam(EXTRA_STREAM);
            mShare.uris = (uris == null) ? new ArrayList<>() : uris;

            // aTalk send extra_text in categories in this case
            mShare.mText2 = intent.getStringParam(EXTRA_TEXT2);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mShare.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().parse(ResourceTable.Layout_menu_share_with, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getId() == ResourceTable.Id_menu_done) {
            terminateAbility();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Retrieve the earlier saved Share object parameters for use with chatIntent
     *
     * @param shareIntent Sharable Intent
     *
     * @return a reference copy of the update chatIntent
     */
    public static Intent getShareIntent(Intent shareIntent) {
        if (mShare == null) {
            return null;
        }

        shareIntent.setAction(mShare.action);
        shareIntent.setType(mShare.type);

        if (ACTION_SEND.equals(mShare.action)) {
            if (!mShare.uris.isEmpty()) {
                shareIntent.setAction(ACTION_SEND);
                shareIntent.setParam(EXTRA_STREAM, mShare.uris.get(0));
            }
            else {
                shareIntent.setParam(EXTRA_TEXT, mShare.text);
            }
        }
        else if (ACTION_SEND_MULTIPLE.equals(mShare.action)) {
            shareIntent.setSequenceableArrayListParam(EXTRA_STREAM, mShare.uris);

            // aTalk has the extra_text in Intent.category in this case
            if (!TextUtils.isEmpty(mShare.mText2))
                shareIntent.setParam(EXTRA_TEXT2, mShare.mText2);
        }
        return shareIntent;
    }
}
