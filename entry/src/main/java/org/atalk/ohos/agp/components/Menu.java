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
package org.atalk.ohos.agp.components;

import ohos.agp.components.Component;
import ohos.agp.components.RadioContainer;
import ohos.app.Context;
import ohos.media.image.PixelMap;

import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.util.AppImageUtil;

/**
 * ItemMenu, displayed as menu with icon and text.
 *
 * @author Eng Chong Meng
 */
public class Menu extends RadioContainer {
    public static final int NONE = 0;
    public static final int FIRST = 1;
    private Menu mMenu;
    private PixelMap mIcon;
    private String mTitle;
    private int mActionId = -1;
    private final Context mContext;

    public Menu(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * Constructor
     *
     * @param actionId Action id for case statements
     * @param title Title
     * @param icon Icon to use
     */
    public Menu(int actionId, String title, PixelMap icon) {
        super(aTalk.getInstance());
        mContext = aTalk.getInstance();
        mTitle = title;
        mIcon = icon;
        mActionId = actionId;
    }

    /**
     * Set action title
     *
     * @param title action title
     */
    public void setTitle(String title) {
        mTitle = title;
    }

    public void setTitle(int resId) {
        mTitle = mContext.getString(resId);
    }

    /**
     * Get action title
     *
     * @return action title
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Set action icon
     *
     * @param icon {@link PixelMap} action icon
     */
    public void setIcon(PixelMap icon) {
        mIcon = icon;
    }

    public void setIcon(int resId) {
        mIcon = AppImageUtil.getPixelMap(mContext, resId);
    }

    /**
     * Get action icon
     *
     * @return {@link PixelMap} action icon
     */
    public PixelMap getIcon() {
        return mIcon;
    }

    /**
     * Set action id
     *
     * @param actionId Action id for this action
     */
    public void setActionId(int actionId) {
        mActionId = actionId;
    }

    /**
     * @return Our action id
     */
    public int getActionId() {
        return mActionId;
    }

    public void setVisible(boolean visible) {
        int visibility = visible ? Component.VISIBLE : Component.HIDE;
        setVisibility(visibility);
    }

    public MenuItem add(int group, int id, int categoryOrder, String title) {
        return new MenuItem(id, title, null);
    }

    public SubMenu getSubMenu() {
        return new SubMenu(mContext) {
            @Override
            public SubMenu setHeaderTitle(int titleRes) {
                return null;
            }

            @Override
            public SubMenu setHeaderTitle(CharSequence title) {
                return null;
            }

            @Override
            public SubMenu setHeaderIcon(int iconRes) {
                return null;
            }

            @Override
            public SubMenu setHeaderIcon(ohos.media.codec.PixelMap icon) {
                return null;
            }

            @Override
            public SubMenu setHeaderView(Component view) {
                return null;
            }

            @Override
            public void clearHeader() {

            }

            @Override
            public MenuItem getItem() {
                return null;
            }
        };
    }

    public SubMenu addSubMenu(int group, int id, int categoryOrder, String title) {
        return new SubMenu(mContext) {
            @Override
            public SubMenu setHeaderTitle(int titleRes) {
                return null;
            }

            @Override
            public SubMenu setHeaderTitle(CharSequence title) {
                return null;
            }

            @Override
            public SubMenu setHeaderIcon(int iconRes) {
                return null;
            }

            @Override
            public SubMenu setHeaderIcon(ohos.media.codec.PixelMap icon) {
                return null;
            }

            @Override
            public SubMenu setHeaderView(Component view) {
                return null;
            }

            @Override
            public void clearHeader() {

            }

            @Override
            public MenuItem getItem() {
                return null;
            }
        };
    }
}