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
package org.atalk.ohos.gui.dialogs;

import ohos.agp.components.Component;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.agp.window.dialog.BaseDialog;
import ohos.app.Context;

import org.atalk.ohos.aTalkApp;

import timber.log.Timber;

public class PopupMenu extends BaseDialog {
    private final Context mContext;
    private final Component mAnchor;
    private DirectionalLayout mMenu = null;

    MenuItemClickedListener mMenuItemClickListener;

    public PopupMenu(Context context) {
        this(context, null);
    }

    public PopupMenu(Context context, Component anchor) {
        super(context);
        mContext = context;
        mAnchor = anchor;
    }

    public void setMenuItemClickedListener(MenuItemClickedListener listener) {
        mMenuItemClickListener = listener;
    }

    public DirectionalLayout setupMenu(int ResId) {
        LayoutScatter scatter = LayoutScatter.getInstance(mContext);
        mMenu = (DirectionalLayout) scatter.parse(ResId, null, false);

        for (int i = 0; i < mMenu.getChildCount(); i++) {
            Component menuItem = mMenu.getComponentAt(i);
            if (menuItem instanceof Text) {
                menuItem.setClickedListener(new ItemClickedListener());
            }
        }
        setAnchorPosition(mAnchor);
        return mMenu;
    }

    public void dismiss() {
        if (mMenu != null) {
            mMenu.invalidate();
            mMenu.release();
        }
    }

    public DirectionalLayout getMenu() {
        if (mMenu == null) {
            mMenu = new DirectionalLayout(aTalkApp.getInstance());
        }
        return mMenu;
    }

    private void setAnchorPosition(Component anchor) {
        int x, y;
        if (anchor != null) {
            x = anchor.getLeft();
            y = anchor.getBottom();
        }
        else {
            x = 60;
            y = mMenu.getRight();
        }
        setOffset(x, y);
    }

    public Component setVisible(int resId, boolean visible) {
        Component menuItem = mMenu.findComponentById(resId);
        if (menuItem != null) {
            int visibility = visible ? Component.VISIBLE : Component.HIDE;
            menuItem.setVisibility(visibility);
        }
        return menuItem;
    }

    public void setHeaderTitle(String title) {
        Text header = new Text(mContext);
        header.setText(title);
        mMenu.addComponent(header, 0);
    }

    public Text addMenuItem(String strItem) {
        Text menuItem = new Text(mContext);
        menuItem.setText(strItem);
        mMenu.addComponent(menuItem);
        return menuItem;
    }

    public Text findMenuItem(int resId) {
        return mMenu.findComponentById(resId);
    }

    public class ItemClickedListener implements Component.ClickedListener {
        @Override
        public void onClick(Component item) {
            if (mMenuItemClickListener != null) {
                boolean handled = mMenuItemClickListener.onMenuItemClick(item);
                if (!handled) {
                    Timber.w("Menu item not handled: %s", item.toString());
                }
            }
        }
    }

    public interface MenuItemClickedListener {
        boolean onMenuItemClick(Component menuItem);
    }
}