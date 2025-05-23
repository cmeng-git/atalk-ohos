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
package org.atalk.ohos.gui.chatroomslist.model;

import net.java.sip.communicator.service.muc.ChatRoomProviderWrapper;

import android.widget.ExpandableListContainer;

/**
 * Implements contact groups expand memory.
 *
 * @author Eng Chong Meng
 */
public class ChatRoomGroupExpandHandler implements ExpandableListContainer.OnGroupExpandListener,
        ExpandableListContainer.OnGroupCollapseListener {
    /**
     * Data key used to remember group state.
     */
    private static final String KEY_EXPAND_MEMORY = "key.expand.memory";

    /**
     * Meta contact list adapter used by this instance.
     */
    private final ChatRoomListProvider chatRoomList;

    /**
     * The contact list view.
     */
    private final ExpandableListContainer chatRoomListContainer;

    /**
     * Creates new instance of <code>MetaGroupExpandHandler</code>.
     *
     * @param chatRoomList contact list data model.
     * @param chatRoomListContainer contact list view.
     */
    public ChatRoomGroupExpandHandler(ChatRoomListProvider chatRoomList, ExpandableListContainer chatRoomListContainer) {
        this.chatRoomList = chatRoomList;
        this.chatRoomListContainer = chatRoomListContainer;
    }

    /**
     * Binds the listener and restores previous groups expanded/collapsed state.
     */
    public void bindAndRestore() {
        for (int gIdx = 0; gIdx < chatRoomList.getGroupCount(); gIdx++) {
            ChatRoomProviderWrapper chatRoomProviderWrapperGroup
                    = (ChatRoomProviderWrapper) chatRoomList.getGroup(gIdx);
            if (Boolean.FALSE.equals(chatRoomProviderWrapperGroup.getData(KEY_EXPAND_MEMORY))) {
                chatRoomListContainer.collapseGroup(gIdx);
            }
            else {
                // Will expand by default
                chatRoomListContainer.expandGroup(gIdx);
            }
        }
        chatRoomListContainer.setOnGroupExpandListener(this);
        chatRoomListContainer.setOnGroupCollapseListener(this);
    }

    /**
     * Unbinds the listener.
     */
    public void unbind() {
        chatRoomListContainer.setOnGroupExpandListener(null);
        chatRoomListContainer.setOnGroupCollapseListener(null);
    }

    @Override
    public void onGroupCollapse(int groupPosition) {
        ((ChatRoomProviderWrapper) chatRoomList.getGroup(groupPosition)).setData(KEY_EXPAND_MEMORY, false);
    }

    @Override
    public void onGroupExpand(int groupPosition) {
        ((ChatRoomProviderWrapper) chatRoomList.getGroup(groupPosition))
                .setData(KEY_EXPAND_MEMORY, true);
    }
}
