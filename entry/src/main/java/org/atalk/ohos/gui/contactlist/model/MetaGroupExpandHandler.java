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
package org.atalk.ohos.gui.contactlist.model;

import ohos.agp.components.ListContainer;

import net.java.sip.communicator.service.contactlist.MetaContactGroup;

/**
 * Implements contact groups expand memory.
 *
 * @author Eng Chong Meng
 */
public class MetaGroupExpandHandler implements ListContainer.OnGroupExpandListener,
        ListContainer.OnGroupCollapseListener {
    /**
     * Data key used to remember group state.
     */
    private static final String KEY_EXPAND_MEMORY = "key.expand.memory";

    /**
     * Meta contact list adapter used by this instance.
     */
    private final MetaContactListProvider contactList;

    /**
     * The contact list view.
     */
    private final ListContainer contactListContainer;

    /**
     * Creates new instance of <code>MetaGroupExpandHandler</code>.
     *
     * @param contactList contact list data model.
     * @param contactListContainer contact list view.
     */
    public MetaGroupExpandHandler(MetaContactListProvider contactList, ListContainer contactListContainer) {
        this.contactList = contactList;
        this.contactListContainer = contactListContainer;
    }

    /**
     * Binds the listener and restores previous groups expanded/collapsed state.
     */
    public void bindAndRestore() {
        for (int gIdx = 0; gIdx < contactList.getGroupCount(); gIdx++) {
            MetaContactGroup metaGroup = (MetaContactGroup) contactList.getGroup(gIdx);

            if (Boolean.FALSE.equals(metaGroup.getData(KEY_EXPAND_MEMORY))) {
                contactListContainer.collapseGroup(gIdx);
            }
            else {
                // Will expand by default
                contactListContainer.expandGroup(gIdx);
            }
        }
        contactListContainer.setOnGroupExpandListener(this);
        contactListContainer.setOnGroupCollapseListener(this);
    }

    /**
     * Unbinds the listener.
     */
    public void unbind() {
        contactListContainer.setOnGroupExpandListener(null);
        contactListContainer.setOnGroupCollapseListener(null);
    }

    @Override
    public void onGroupCollapse(int groupPosition) {
        ((MetaContactGroup) contactList.getGroup(groupPosition)).setData(KEY_EXPAND_MEMORY, false);
    }

    @Override
    public void onGroupExpand(int groupPosition) {
        ((MetaContactGroup) contactList.getGroup(groupPosition)).setData(KEY_EXPAND_MEMORY, true);
    }
}
