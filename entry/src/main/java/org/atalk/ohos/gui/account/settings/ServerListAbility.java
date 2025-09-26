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
package org.atalk.ohos.gui.account.settings;

import ohos.aafwk.content.Intent;
import ohos.agp.components.Component;
import ohos.agp.components.ListComponent;
import ohos.agp.components.ListContainer;

import net.java.sip.communicator.service.protocol.jabber.JabberAccountRegistration;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.agp.components.Menu;
import org.atalk.ohos.agp.components.MenuInflater;
import org.atalk.ohos.agp.components.MenuItem;
import org.atalk.ohos.gui.dialogs.DialogA;

/**
 * The activity allows user to edit STUN or Jingle Nodes list of the Jabber account.
 *
 * @author Eng Chong Meng
 */
public class ServerListAbility extends BaseAbility {
    /**
     * Request code when launched for STUN servers list edit
     */
    public static int RCODE_STUN_TURN = 1;

    /**
     * Request code used when launched for Jingle Nodes edit
     */
    public static int RCODE_JINGLE_NODES = 2;

    /**
     * Request code intent's extra key
     */
    public static String REQUEST_CODE_KEY = "requestCode";

    /**
     * Jabber account registration intent's extra key
     */
    public static String JABBER_REGISTRATION_KEY = "JabberReg";

    /**
     * The registration object storing edited properties
     */
    private JabberAccountRegistration registration;

    /**
     * The list model for currently edited items
     */
    private ServerItemProvider mAdapter;

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        this.registration = intent.getSerializableParam(JABBER_REGISTRATION_KEY);
        int listType = intent.getIntParam(REQUEST_CODE_KEY, -1);

        if (listType == RCODE_STUN_TURN) {
            mAdapter = new StunServerProvider(this, registration);
            setMainTitle(ResourceTable.String_stun_turn_server);
        }
        else if (listType == RCODE_JINGLE_NODES) {
            mAdapter = new JingleNodeProvider(this, registration);
            setMainTitle(ResourceTable.String_jbr_jingle_nodes);
        }
        else {
            throw new IllegalArgumentException();
        }

        ListFragment listFragment = new ServerListSlice();
        listFragment.setListAdapter(mAdapter);
        // Display the fragment as the main content.
        getSupportFragmentManager().beginTransaction()
                .replace(android.ResourceTable.Id_content, listFragment)
                .commit();

        findComponentById(android.ResourceTable.Id_content).setClickedListener(view -> showServerEditDialog(-1));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.parse(ResourceTable.Layout_menu_server_list, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getId() == ResourceTable.Id_addItem) {
            showServerEditDialog(-1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Shows the item edit dialog, created with factory method of list model
     *
     * @param listPosition the position of selected item, -1 means "create new item"
     */
    void showServerEditDialog(int listPosition) {
        DialogA securityDialog = mAdapter.createItemEditDialog(listPosition);
        securityDialog.show();
    }

    @Override
    protected void onBackPressed() {
        Intent result = new Intent();
        result.setParam(JABBER_REGISTRATION_KEY, registration);
        setResult(BaseAbility.RESULT_OK, result);
        terminateAbility();
    }

    /**
     * The server list fragment. Required to catch events.
     */
    static public class ServerListSlice extends ListComponent {
        @Override
        public void onViewCreated(Component view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            setEmptyText(getString(ResourceTable.String_service_gui_SERVERS_LIST_EMPTY));
        }

        @Override
        public void onListItemClick(ListContainer l, Component v, int position, long id) {
            super.onListItemClick(l, v, position, id);
            ((ServerListActivity) getActivity()).showServerEditDialog(position);
        }
    }
}
