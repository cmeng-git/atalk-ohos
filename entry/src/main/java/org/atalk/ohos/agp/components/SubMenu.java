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
import ohos.app.Context;
import ohos.media.codec.PixelMap;

/**
 * Subclass of {@link Menu} for sub menus.
 * Sub menus do not support item icons, or nested sub menus.
 * <p>
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about creating menus, read the
 * <a href="{@docRoot}guide/topics/ui/menus.html">Menus</a> developer guide.</p>
 * </div>
 */

public abstract class SubMenu extends Menu {
    /**
     * Constructor
     */
    public SubMenu(Context context) {
        super(context);
    }

    /**
     * Sets the submenu header's title to the title given in <var>titleRes</var>
     * resource identifier.
     * 
     * @param titleRes The string resource identifier used for the title.
     * @return This SubMenu so additional setters can be called.
     */
    public abstract SubMenu setHeaderTitle(int titleRes);

    /**
     * Sets the submenu header's title to the title given in <var>title</var>.
     * 
     * @param title The character sequence used for the title.
     * @return This SubMenu so additional setters can be called.
     */
    public abstract SubMenu setHeaderTitle(CharSequence title);
    
    /**
     * Sets the submenu header's icon to the icon given in <var>iconRes</var>
     * resource id.
     * 
     * @param iconRes The resource identifier used for the icon.
     * @return This SubMenu so additional setters can be called.
     */
    public abstract SubMenu setHeaderIcon(int iconRes);

    /**
     * Sets the submenu header's icon to the icon given in <var>icon</var>
     * {@link PixelMap}.
     * 
     * @param icon The {@link PixelMap} used for the icon.
     * @return This SubMenu so additional setters can be called.
     */
    public abstract SubMenu setHeaderIcon(PixelMap icon);
    
    /**
     * Sets the header of the submenu to the {@link Component} given in
     * <var>view</var>. This replaces the header title and icon (and those
     * replace this).
     * 
     * @param view The {@link Component} used for the header.
     * @return This SubMenu so additional setters can be called.
     */
    public abstract SubMenu setHeaderView(Component view);
    
    /**
     * Clears the header of the submenu.
     */
    public abstract void clearHeader();
    
//    /**
//     * Change the icon associated with this submenu's item in its parent menu.
//     *
//     * @see MenuItem#setIcon(int)
//     * @param iconRes The new icon (as a resource ID) to be displayed.
//     * @return This SubMenu so additional setters can be called.
//     */
//    public abstract SubMenu setIcon(int iconRes);
//
//    /**
//     * Change the icon associated with this submenu's item in its parent menu.
//     *
//     * @see MenuItem#setIcon(PixelMap)
//     * @param icon The new icon (as a Drawable) to be displayed.
//     * @return This SubMenu so additional setters can be called.
//     */
//    public abstract SubMenu setIcon(PixelMap icon);
    
    /**
     * Gets the {@link MenuItem} that represents this submenu in the parent
     * menu.  Use this for setting additional item attributes.
     * 
     * @return The {@link MenuItem} that launches the submenu when invoked.
     */
    public abstract MenuItem getItem();
}
