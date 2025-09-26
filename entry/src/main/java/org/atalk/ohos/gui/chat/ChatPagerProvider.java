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
package org.atalk.ohos.gui.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.PageSliderProvider;

import net.java.sip.communicator.service.gui.Chat;
import net.java.sip.communicator.service.gui.event.ChatListener;

import org.atalk.ohos.BaseAbility;

/**
 * A pager provider used to display active chats.
 *
 * @author Eng Chong Meng
 */
public class ChatPagerProvider extends PageSliderProvider implements ChatListener {
    /**
     * The list of contained chat session ids.
     */
    private final List<String> mChats;
    private final Map<String, ChatSlice> mChatSlices = new HashMap<>();
    /**
     * Parent <code>ChatAbility</code>.
     */
    private final ChatAbility parent;

    /**
     * Remembers currently displayed <code>ChatSlice</code>.
     */
    private ChatSlice mPrimaryItem;

    /**
     * Creates an instance of <code>ChatPagerAdapter</code> by specifying the parent
     * <code>ChatAbility</code> and its <code>FragmentManager</code>.
     *
     * @param parent parent <code>FragmentManager</code>
     */
    public ChatPagerProvider(ChatAbility parent) {
        mChats = ChatSessionManager.getActiveChatIds();
        this.parent = parent;
        ChatSessionManager.addChatListener(this);
    }

    /**
     * Releases resources used by this instance. Once called this instance is considered invalid.
     */
    public void dispose() {
        ChatSessionManager.removeChatListener(this);
    }

    public ChatSlice getCurrentChatSlice() {
        return mPrimaryItem;
    }

    /**
     * Returns chat id corresponding to the given position.
     *
     * @param pos the position of the chat we're looking for
     *
     * @return chat id corresponding to the given position
     */
    public String getChatId(int pos) {
        synchronized (mChats) {
            if (mChats.size() <= pos)
                return null;
            return mChats.get(pos);
        }
    }

    /**
     * Returns index of the <code>ChatPanel</code> in this adapter identified by given <code>sessionId</code>.
     *
     * @param sessionId chat session identifier.
     *
     * @return index of the <code>ChatPanel</code> in this adapter identified by given <code>sessionId</code>.
     */
    public int getChatIdx(String sessionId) {
        if (sessionId == null)
            return POSITION_INVALID;

        for (int i = 0; i < mChats.size(); i++) {
            if (getChatId(i).equals(sessionId))
                return i;
        }
        return POSITION_INVALID;
    }

    /**
     * Removes the given chat session id from this pager if exist.
     *
     * @param chatId the chat id to remove from this pager
     */
    public void removeChatSession(String chatId) {
        synchronized (mChats) {
            if (mChats.remove(chatId)) {
                notifyDataChanged();
            }
        }
    }

    /**
     * Removes all <code>ChatSlice</code>s from this pager.
     */
    public void removeAllChatSessions() {
        synchronized (mChats) {
            mChats.clear();
        }
        notifyDataChanged();
    }

    /**
     * Returns the position of the given <code>object</code> in this pager.
     * cmeng - Seem this is not call by PagerAdapter at all
     *
     * @return the position of the given <code>object</code> in this pager
     */
    @Override
    public int getPageIndex(Object object) {
        String id = ((ChatSlice) object).getChatPanel().getChatSession().getChatId();
        synchronized (mChats) {
            if (mChats.contains(id))
                return mChats.indexOf(id);
        }
        return POSITION_INVALID;
    }

    /**
     * Returns the count of contained <code>ChatSlice</code>s.
     *
     * @return the count of contained <code>ChatSlice</code>s
     */
    @Override
    public int getCount() {
        synchronized (mChats) {
            return mChats.size();
        }
    }

    /**
     * Returns the <code>AbilitySlice</code> at the given position in this pager.
     *
     * @return the <code>AbilitySlice</code> at the given position in this pager
     */
    @Override
    public ChatSlice createPageInContainer(ComponentContainer componentContainer, int pos) {
        String chatId = mChats.get(pos);
        ChatSlice chatSlice = ChatSlice.newInstance(chatId);
        mChatSlices.put(chatId, chatSlice);
        return chatSlice;
    }

    @Override
    public void destroyPageFromContainer(ComponentContainer componentContainer, int pos, Object obj) {
        synchronized (mChats) {
            if (mChats.remove(pos) != null) {
                notifyDataChanged();
            }
        }
    }

    @Override
    public boolean isPageMatchToObject(Component component, Object obj) {
        return obj instanceof ChatSlice;
    }

    public void setPrimaryItem(String chatId) {
        /*
         * Notifies ChatFragments about their visibility state changes. This method is invoked
         * many times with the same parameter, so we keep track of last item and notify only on changes.
         *
         * This is required, because normal onActive/onInactive fragment cycle doesn't work
         * as expected with pager adapter.
         */
        ChatSlice newPrimary = mChatSlices.get(chatId);
        if (newPrimary != mPrimaryItem) {
            if (mPrimaryItem != null)
                mPrimaryItem.setPrimarySelected(false);
            if (newPrimary != null)
                newPrimary.setPrimarySelected(true);
        }
        mPrimaryItem = newPrimary;
    }

    @Override
    public void chatClosed(final Chat chat) {
        BaseAbility.runOnUiThread(() -> {
            removeChatSession(((ChatPanel) chat).getChatSession().getChatId());
        });
    }

    @Override
    public void chatCreated(final Chat chat) {
        BaseAbility.runOnUiThread(() -> {
            synchronized (mChats) {
                mChats.add(((ChatPanel) chat).getChatSession().getChatId());
                notifyDataChanged();
            }
        });
    }
}