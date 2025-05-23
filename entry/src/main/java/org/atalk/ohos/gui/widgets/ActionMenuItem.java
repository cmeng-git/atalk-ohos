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
package org.atalk.ohos.gui.widgets;

import ohos.media.image.PixelMap;

/**
 * Action item, displayed as menu with icon and text.
 *
 * @author Eng Chong Meng
 */
public class ActionMenuItem {
    private PixelMap thumb;
    private PixelMap icon;
    private String title;
    private int actionId = -1;
    private boolean selected;
    private boolean sticky;

    /**
     * Constructor
     *
     * @param actionId Action id for case statements
     * @param title Title
     * @param icon Icon to use
     */
    public ActionMenuItem(int actionId, String title, PixelMap icon) {
        this.title = title;
        this.icon = icon;
        this.actionId = actionId;
    }

    /**
     * Set action title
     *
     * @param title action title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get action title
     *
     * @return action title
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Set action icon
     *
     * @param icon {@link PixelMap} action icon
     */
    public void setIcon(PixelMap icon) {
        this.icon = icon;
    }

    /**
     * Get action icon
     *
     * @return {@link PixelMap} action icon
     */
    public PixelMap getIcon() {
        return this.icon;
    }

    /**
     * Set action id
     *
     * @param actionId Action id for this action
     */
    public void setActionId(int actionId) {
        this.actionId = actionId;
    }

    /**
     * @return Our action id
     */
    public int getActionId() {
        return actionId;
    }

    /**
     * Set sticky status of button
     *
     * @param sticky true for sticky, pop up sends event but does not disappear
     */
    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    /**
     * @return true if button is sticky, menu stays visible after press
     */
    public boolean isSticky() {
        return sticky;
    }

    /**
     * Set selected flag;
     *
     * @param selected Flag to indicate the item is selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /**
     * Check if item is selected
     *
     * @return true or false
     */
    public boolean isSelected() {
        return this.selected;
    }

    /**
     * Set thumb
     *
     * @param thumb Thumb image
     */
    public void setThumb(PixelMap thumb) {
        this.thumb = thumb;
    }

    /**
     * Get thumb image
     *
     * @return Thumb image
     */
    public PixelMap getThumb() {
        return this.thumb;
    }
}