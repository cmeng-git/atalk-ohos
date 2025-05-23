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
package org.atalk.ohos;

import ohos.agp.components.Component;
import ohos.agp.components.ListContainer;
import ohos.app.Context;
import ohos.utils.PlainBooleanArray;

/**
 * BaseAbility implements the support of user set Theme and locale.
 * All app activities must extend BaseAbility inorder to support Theme and locale.
 */
public class MListContainer extends ListContainer implements ListContainer.ItemClickedListener, ListContainer.ItemSelectedListener {

    MultiChoiceListener mMultiChoiceListener = null;
    PlainBooleanArray mCheckedList = new PlainBooleanArray();

    public MListContainer(Context context) {
        super(context);
    }

    public void setMultiChoiceListener(MultiChoiceListener listener) {
        mMultiChoiceListener = listener;
    }

    @Override
    public void onItemSelected(ListContainer listContainer, Component component, int pos, long id) {
        int key = (int) id;
         boolean isChecked = mCheckedList.get(key, false);

         if (isChecked) {
             mCheckedList.remove(key);
             isChecked = false;
         } else {
             mCheckedList.put(key, true);
             isChecked = true;
         }

        if (mMultiChoiceListener != null)
            mMultiChoiceListener.onItemCheckStateChanged(this, component, pos, id, isChecked);
    }

    @Override
    public void onItemClicked(ListContainer listContainer, Component component, int pos, long id) {
        int key = (int) id;
        if (component.isSelected()) {
            component.setSelected(false);
            mCheckedList.remove(key);

            if (mMultiChoiceListener != null)
                mMultiChoiceListener.onItemCheckStateChanged(this, component, pos, id, false);
        }
    }

    public void setSelection(int pos) {
        long id= getItemId(pos);
        Component item = getComponentAt(pos);
        if (!item.isSelected()) {
            item.setSelected(true);
            mCheckedList.put((int) id, true);

            if (mMultiChoiceListener != null)
                mMultiChoiceListener.onItemCheckStateChanged(this, item, pos, id, false);
        }
    }

    public long getItemId(int pos) {
        return pos;
    }

    public PlainBooleanArray getCheckedItemPositions() {
        return mCheckedList;
    }
    public int getCheckedItemCount() {
        return mCheckedList.size();
    }

    public interface MultiChoiceListener {
        void onItemCheckStateChanged(ListContainer listContainer, Component component, int pos, long id, boolean checked);
    }
}
