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
import ohos.agp.components.RadioButton;
import ohos.app.Context;
import ohos.media.image.PixelMap;

import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.util.AppImageUtil;

/**
 * ItemMenu, displayed as menu with icon and text.
 *
 * @author Eng Chong Meng
 */
public class MenuItem extends RadioButton {
    private PixelMap mIcon;
    private String mTitle;
    private boolean mSticky;

    private final Context mContext;
    /**
     * Constructor
     *
     * @param itemId Action id for case statements
     * @param title Title
     * @param icon Icon to use
     */
    public MenuItem(int itemId, String title, PixelMap icon) {
        super(aTalk.getInstance());
        mContext = aTalk.getInstance();

        mItemId = itemId;
        mTitle = title;
        mIcon = icon;
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

    public void setVisible(boolean visible) {
        int visibility = visible ? Component.VISIBLE : Component.HIDE;
        setVisibility(visibility);
    }
}