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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.Optional;

import ohos.agp.components.Attr;
import ohos.agp.components.AttrSet;
import ohos.agp.components.Component;
import ohos.app.Context;
import ohos.global.resource.NotExistException;
import ohos.global.resource.Resource;

import org.whispersystems.libsignal.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/*
 * The XML parsing interface returned for an XML resource.  This is a standard
 * {@link XmlPullParser} interface but also extends {@link AttributeSet} and
 * adds an additional {@link #close()} method for the client to indicate when
 * it is done reading the resource.
 *
 * @author Eng Chong Meng
 */
public class XmlResourceParser implements XmlPullParser, AutoCloseable {
    private static final String LOG_TAG = "MenuInflater";
    private static final String XML_MENU = "menu";

    /**
     * Group tag name in XML.
     */
    private static final String XML_GROUP = "group";

    /**
     * Item tag name in XML.
     */
    private static final String XML_ITEM = "item";

    private static final int NO_ID = 0;

    private final Context mContext;
    private final Resource mResource;

    public XmlResourceParser(Context context, int xmlId, Menu menu) {
        mContext = context;
        String line = "";
        StringBuilder fileData = new StringBuilder();

        try {
            mResource = context.getResourceManager().getResource(xmlId);

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();

            BufferedReader br = new BufferedReader(new InputStreamReader(mResource));
            while ((line = br.readLine()) != null) {
                fileData.append(line);
            }
            mResource.close();
            parser.setInput(new StringReader(fileData.toString()));
            AttrSet attrs = new AttrSet() {
                @Override
                public Optional<String> getStyle() {
                    return Optional.empty();
                }

                @Override
                public int getLength() {
                    return 0;
                }

                @Override
                public Optional<Attr> getAttr(int i) {
                    return Optional.empty();
                }

                @Override
                public Optional<Attr> getAttr(String s) {
                    return Optional.empty();
                }
            };
            parseMenu(parser, attrs, menu);

        } catch (IOException | XmlPullParserException | NotExistException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Called internally to fill the given menu. If a sub menu is seen, it will
     * call this recursively.
     */
    private void parseMenu(XmlPullParser parser, AttrSet attrs, Menu menu)
            throws XmlPullParserException, IOException {
        MenuState menuState = new MenuState(menu);

        int eventType = parser.getEventType();
        String tagName;
        boolean lookingForEndOfUnknownTag = false;
        String unknownTagName = null;

        // This loop will skip to the menu start tag
        do {
            if (eventType == XmlPullParser.START_TAG) {
                tagName = parser.getName();
                if (tagName.equals(XML_MENU)) {
                    // Go to next tag
                    eventType = parser.next();
                    break;
                }

                throw new RuntimeException("Expecting menu, got " + tagName);
            }
            eventType = parser.next();
        } while (eventType != XmlPullParser.END_DOCUMENT);

        boolean reachedEndOfMenu = false;
        while (!reachedEndOfMenu) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (lookingForEndOfUnknownTag) {
                        break;
                    }

                    tagName = parser.getName();
                    switch (tagName) {
                        case XML_GROUP:
                            menuState.readGroup(attrs);
                            break;

                        case XML_ITEM:
                            menuState.readItem(attrs);
                            break;

                        case XML_MENU:
                            // A menu start tag denotes a submenu for an item
                            SubMenu subMenu = menuState.addSubMenuItem();
                            registerMenu(subMenu, attrs);

                            // Parse the submenu into returned SubMenu
                            parseMenu(parser, attrs, subMenu);
                            break;

                        default:
                            lookingForEndOfUnknownTag = true;
                            unknownTagName = tagName;
                            break;
                    }
                    break;

                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (lookingForEndOfUnknownTag && tagName.equals(unknownTagName)) {
                        lookingForEndOfUnknownTag = false;
                        unknownTagName = null;
                    }
                    else if (tagName.equals(XML_GROUP)) {
                        menuState.resetGroup();
                    }
                    else if (tagName.equals(XML_ITEM)) {
                        // Add the item if it hasn't been added (if the item was
                        // a submenu, it would have been added already)
                        if (!menuState.hasAddedItem()) {
                            if (menuState.itemActionProvider != null &&
                                    menuState.itemActionProvider.hasSubMenu()) {
                                registerMenu(menuState.addSubMenuItem(), attrs);
                            }
                            else {
                                registerMenu(menuState.addItem(), attrs);
                            }
                        }
                    }
                    else if (tagName.equals(XML_MENU)) {
                        reachedEndOfMenu = true;
                    }
                    break;

                case XmlPullParser.END_DOCUMENT:
                    throw new RuntimeException("Unexpected end of document");
            }

            eventType = parser.next();
        }
    }


    /**
     * Close this parser. Calls on the interface are no longer valid after this call.
     */
    @Override
    public void close() {
        if (mResource != null) {
            try {
                mResource.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void setFeature(String s, boolean b) throws XmlPullParserException {

    }

    @Override
    public boolean getFeature(String s) {
        return false;
    }

    @Override
    public void setProperty(String s, Object o) throws XmlPullParserException {

    }

    @Override
    public Object getProperty(String s) {
        return null;
    }

    @Override
    public void setInput(Reader reader) throws XmlPullParserException {

    }

    @Override
    public void setInput(InputStream inputStream, String s) throws XmlPullParserException {

    }

    @Override
    public String getInputEncoding() {
        return null;
    }

    @Override
    public void defineEntityReplacementText(String s, String s1) throws XmlPullParserException {

    }

    @Override
    public int getNamespaceCount(int i) throws XmlPullParserException {
        return 0;
    }

    @Override
    public String getNamespacePrefix(int i) throws XmlPullParserException {
        return null;
    }

    @Override
    public String getNamespaceUri(int i) throws XmlPullParserException {
        return null;
    }

    @Override
    public String getNamespace(String s) {
        return null;
    }

    @Override
    public int getDepth() {
        return 0;
    }

    @Override
    public String getPositionDescription() {
        return null;
    }

    @Override
    public int getLineNumber() {
        return 0;
    }

    @Override
    public int getColumnNumber() {
        return 0;
    }

    @Override
    public boolean isWhitespace() throws XmlPullParserException {
        return false;
    }

    @Override
    public String getText() {
        return null;
    }

    @Override
    public char[] getTextCharacters(int[] ints) {
        return new char[0];
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getPrefix() {
        return null;
    }

    @Override
    public boolean isEmptyElementTag() throws XmlPullParserException {
        return false;
    }

    @Override
    public int getAttributeCount() {
        return 0;
    }

    @Override
    public String getAttributeNamespace(int i) {
        return null;
    }

    @Override
    public String getAttributeName(int i) {
        return null;
    }

    @Override
    public String getAttributePrefix(int i) {
        return null;
    }

    @Override
    public String getAttributeType(int i) {
        return null;
    }

    @Override
    public boolean isAttributeDefault(int i) {
        return false;
    }

    @Override
    public String getAttributeValue(int i) {
        return null;
    }

    @Override
    public String getAttributeValue(String s, String s1) {
        return null;
    }

    @Override
    public int getEventType() throws XmlPullParserException {
        return 0;
    }

    @Override
    public int next() throws XmlPullParserException, IOException {
        return 0;
    }

    @Override
    public int nextToken() throws XmlPullParserException, IOException {
        return 0;
    }

    @Override
    public void require(int i, String s, String s1) throws XmlPullParserException, IOException {

    }

    @Override
    public String nextText() throws XmlPullParserException, IOException {
        return null;
    }

    @Override
    public int nextTag() throws XmlPullParserException, IOException {
        return 0;
    }

    /**
     * State for the current menu.
     * <p>
     * Groups can not be nested unless there is another menu (which will have
     * its state class).
     */
    private class MenuState {
        private Menu menu;

        /*
         * Group state is set on items as they are added, allowing an item to
         * override its group state. (As opposed to set on items at the group end tag.)
         */
        private int groupId;
        private int groupCategory;
        private int groupOrder;
        private int groupCheckable;
        private boolean groupVisible;
        private boolean groupEnabled;

        private boolean itemAdded;
        private int itemId;
        private int itemCategoryOrder;
        private String itemTitle;
        private String itemTitleCondensed;
        private int itemIconResId;
        private ColorStateList itemIconTintList = null;
        private BlendMode mItemIconBlendMode = null;
        private char itemAlphabeticShortcut;
        private int itemAlphabeticModifiers;
        private char itemNumericShortcut;
        private int itemNumericModifiers;
        /**
         * Sync to attrs.xml enum:
         * - 0: none
         * - 1: all
         * - 2: exclusive
         */
        private int itemCheckable;
        private boolean itemChecked;
        private boolean itemVisible;
        private boolean itemEnabled;

        /**
         * Sync to attrs.xml enum, values in MenuItem:
         * - 0: never
         * - 1: ifRoom
         * - 2: always
         * - -1: Safe sentinel for "no value".
         */
        private int itemShowAsAction;

        private int itemActionViewLayout;
        private String itemActionViewClassName;
        private String itemActionProviderClassName;

        private String itemListenerMethodName;

        private ActionProvider itemActionProvider;

        private CharSequence itemContentDescription;
        private CharSequence itemTooltipText;

        private static final int defaultGroupId = NO_ID;
        private static final int defaultItemId = NO_ID;
        private static final int defaultItemCategory = 0;
        private static final int defaultItemOrder = 0;
        private static final int defaultItemCheckable = 0;
        private static final boolean defaultItemChecked = false;
        private static final boolean defaultItemVisible = true;
        private static final boolean defaultItemEnabled = true;

        public MenuState(final Menu menu) {
            this.menu = menu;

            resetGroup();
        }

        public void resetGroup() {
            groupId = defaultGroupId;
            groupCategory = defaultItemCategory;
            groupOrder = defaultItemOrder;
            groupCheckable = defaultItemCheckable;
            groupVisible = defaultItemVisible;
            groupEnabled = defaultItemEnabled;
        }

        /**
         * Called when the parser is pointing to a group tag.
         */
        public void readGroup(AttrSet attrs) {
            TypedArray a = mContext.obtainStyledAttributes(attrs,
                    R.styleable.MenuGroup);

            groupId = a.getResourceId(R.styleable.MenuGroup_id, defaultGroupId);
            groupCategory = a.getInt(R.styleable.MenuGroup_menuCategory, defaultItemCategory);
            groupOrder = a.getInt(R.styleable.MenuGroup_orderInCategory, defaultItemOrder);
            groupCheckable = a.getInt(R.styleable.MenuGroup_checkableBehavior, defaultItemCheckable);
            groupVisible = a.getBoolean(R.styleable.MenuGroup_visible, defaultItemVisible);
            groupEnabled = a.getBoolean(R.styleable.MenuGroup_enabled, defaultItemEnabled);

            a.recycle();
        }

        /**
         * Called when the parser is pointing to an item tag.
         */
        public void readItem(AttrSet attrs) {
            TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.MenuItem);

            // Inherit attributes from the group as default value
            itemId = a.getResourceId(R.styleable.MenuItem_id, defaultItemId);
            final int category = a.getInt(R.styleable.MenuItem_menuCategory, groupCategory);
            final int order = a.getInt(R.styleable.MenuItem_orderInCategory, groupOrder);
            itemCategoryOrder = (category & Menu.CATEGORY_MASK) | (order & Menu.USER_MASK);
            itemTitle = a.getText(R.styleable.MenuItem_title);
            itemTitleCondensed = a.getText(R.styleable.MenuItem_titleCondensed);
            itemIconResId = a.getResourceId(R.styleable.MenuItem_icon, 0);
            if (a.hasValue(R.styleable.MenuItem_iconTintMode)) {
                mItemIconBlendMode = Drawable.parseBlendMode(a.getInt( R.styleable.MenuItem_iconTintMode, -1),
                        mItemIconBlendMode);
            }
            else {
                // Reset to null so that it's not carried over to the next item
                mItemIconBlendMode = null;
            }
            if (a.hasValue(R.styleable.MenuItem_iconTint)) {
                itemIconTintList = a.getColorStateList(
                        R.styleable.MenuItem_iconTint);
            }
            else {
                // Reset to null so that it's not carried over to the next item
                itemIconTintList = null;
            }

            itemAlphabeticShortcut = getShortcut(a.getString(R.styleable.MenuItem_alphabeticShortcut));
            itemAlphabeticModifiers = a.getInt(R.styleable.MenuItem_alphabeticModifiers, KeyEvent.META_CTRL_ON);
            itemNumericShortcut =
                    getShortcut(a.getString(R.styleable.MenuItem_numericShortcut));
            itemNumericModifiers =
                    a.getInt(R.styleable.MenuItem_numericModifiers,
                            KeyEvent.META_CTRL_ON);
            if (a.hasValue(R.styleable.MenuItem_checkable)) {
                // Item has attribute checkable, use it
                itemCheckable = a.getBoolean(R.styleable.MenuItem_checkable, false) ? 1 : 0;
            }
            else {
                // Item does not have attribute, use the group's (group can have one more state
                // for checkable that represents the exclusive checkable)
                itemCheckable = groupCheckable;
            }
            itemChecked = a.getBoolean(R.styleable.MenuItem_checked, defaultItemChecked);
            itemVisible = a.getBoolean(R.styleable.MenuItem_visible, groupVisible);
            itemEnabled = a.getBoolean(R.styleable.MenuItem_enabled, groupEnabled);
            itemShowAsAction = a.getInt(R.styleable.MenuItem_showAsAction, -1);
            itemListenerMethodName = a.getString(R.styleable.MenuItem_onClick);
            itemActionViewLayout = a.getResourceId(R.styleable.MenuItem_actionLayout, 0);
            itemActionViewClassName = a.getString(R.styleable.MenuItem_actionViewClass);
            itemActionProviderClassName = a.getString(R.styleable.MenuItem_actionProviderClass);

            final boolean hasActionProvider = itemActionProviderClassName != null;
            if (hasActionProvider && itemActionViewLayout == 0 && itemActionViewClassName == null) {
                itemActionProvider = newInstance(itemActionProviderClassName,
                        ACTION_PROVIDER_CONSTRUCTOR_SIGNATURE,
                        mActionProviderConstructorArguments);
            }
            else {
                if (hasActionProvider) {
                    Log.w(LOG_TAG, "Ignoring attribute 'actionProviderClass'. Action view already specified.");
                }
                itemActionProvider = null;
            }

            itemContentDescription = a.getText(R.styleable.MenuItem_contentDescription);
            itemTooltipText = a.getText(R.styleable.MenuItem_tooltipText);
            a.recycle();

            itemAdded = false;
        }

        private char getShortcut(String shortcutString) {
            if (shortcutString == null) {
                return 0;
            }
            else {
                return shortcutString.charAt(0);
            }
        }

        private void setItem(MenuItem menuItem) {
            menuItem.setSelected(itemChecked);
            menuItem.setVisible(itemVisible);
            menuItem.setEnabled(itemEnabled);
            menuItem.setSelected(itemCheckable >= 1);
            menuItem.setTitle(itemTitleCondensed);
            menuItem.setIcon(itemIconResId);
            menuItem.setAlphabeticShortcut(itemAlphabeticShortcut, itemAlphabeticModifiers);
            menuItem.setNumericShortcut(itemNumericShortcut, itemNumericModifiers);;

            if (itemShowAsAction >= 0) {
                menuItem.setShowAsAction(itemShowAsAction);
            }

            if (mItemIconBlendMode != null) {
                menuItem.setIconTintBlendMode(mItemIconBlendMode);
            }

            if (itemIconTintList != null) {
                menuItem.setIconTintList(itemIconTintList);
            }

            if (itemListenerMethodName != null) {
                if (mContext.isRestricted()) {
                    throw new IllegalStateException("The android:onClick attribute cannot "
                            + "be used within a restricted context");
                }
                menuItem.setClickedListener(
                        new Component.ClickedListener() {
                            @Override
                            public void onClick(Component component) {
                                // OnMenuItemClickListener(getRealOwner(), itemListenerMethodName));
                            }
                        });
            }

            if (menuItem instanceof MenuItemImpl) {
                MenuItemImpl impl = (MenuItemImpl) menuItem;
                if (itemCheckable >= 2) {
                    impl.setExclusiveCheckable(true);
                }
            }

            boolean actionViewSpecified = false;
            if (itemActionViewClassName != null) {
                Component actionView = newInstance(itemActionViewClassName,
                        ACTION_VIEW_CONSTRUCTOR_SIGNATURE, mActionViewConstructorArguments);
                menuItem.setActionView(actionView);
                actionViewSpecified = true;
            }
            if (itemActionViewLayout > 0) {
                if (!actionViewSpecified) {
                    menuItem.setActionView(itemActionViewLayout);
                    actionViewSpecified = true;
                }
                else {
                    Log.w(LOG_TAG, "Ignoring attribute 'itemActionViewLayout'."
                            + " Action view already specified.");
                }
            }
            if (itemActionProvider != null) {
                menuItem.setActionProvider(itemActionProvider);
            }

            menuItem.setComponentDescription(itemContentDescription);
            menuItem.setTooltipText(itemTooltipText);
        }

        public MenuItem addItem() {
            itemAdded = true;
            MenuItem item = menu.add(groupId, itemId, itemCategoryOrder, itemTitle);
            setItem(item);
            return item;
        }

        public SubMenu addSubMenuItem() {
            itemAdded = true;
            SubMenu subMenu = menu.addSubMenu(groupId, itemId, itemCategoryOrder, itemTitle);
            setItem(subMenu.getItem());
            return subMenu;
        }

        public boolean hasAddedItem() {
            return itemAdded;
        }

        @SuppressWarnings("unchecked")
        private <T> T newInstance(String className, Class<?>[] constructorSignature,
                Object[] arguments) {
            try {
                Class<?> clazz = mContext.getClass().getClassLoader().loadClass(className);
                Constructor<?> constructor = clazz.getConstructor(constructorSignature);
                constructor.setAccessible(true);
                return (T) constructor.newInstance(arguments);
            } catch (Exception e) {
                Log.w(LOG_TAG, "Cannot instantiate class: " + className, e);
            }
            return null;
        }
    }
}
