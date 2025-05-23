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

import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import ohos.agp.colors.Color;
import ohos.agp.components.AttrSet;
import ohos.agp.components.ComponentContainer;
import ohos.app.Context;

import org.atalk.ohos.gui.aTalk;

public class JComponent extends ComponentContainer {
    int prefWidth;
    int prefHeight;
    boolean prefSizeIsSet = true;

    public JComponent() {
        super(aTalk.getInstance());
    }

    public JComponent(Context context) {
        super(context);
    }

    public JComponent(Context context, AttrSet attrSet) {
        super(context, attrSet);
    }

    /**
     * Preferred size. (This field perhaps should have been transient).
     *
     * @serial
     */
    Dimension prefSize;

    /**
     * Whether or not setPreferredSize has been invoked with a non-null value.
     */
    boolean prefSizeSet;

    /**
     * If any {@code PropertyChangeListeners} have been registered,
     * the {@code changeSupport} field describes them.
     *
     * @serial
     * @see #addPropertyChangeListener
     * @see #removePropertyChangeListener
     * @since 1.2
     */
    private PropertyChangeSupport changeSupport;

    /*
     * In some cases using "this" as an object to synchronize by
     * can lead to a deadlock if client code also uses synchronization
     * by a component object. For every such situation revealed we should
     * consider possibility of replacing "this" with the package private
     * objectLock object introduced below. So far there're 3 issues known:
     * - CR 6708322 (the getName/setName methods);
     * - CR 6608764 (the PropertyChangeListener machinery);
     * - CR 7108598 (the Container.paint/KeyboardFocusManager.clearMostRecentFocusOwner methods).
     *
     * Note: this field is considered final, though readObject() prohibits
     * initializing final fields.
     */
    private final transient Object objectLock = new Object();

    public Color getBackground() {
        return null;
    }

    public ComponentContainer getParent() {
        return null;
    }

    public Dimension getPreferredSize() {
        return new Dimension(this.prefWidth, this.prefHeight);
    }

    public boolean isDisplayable() {
        return true;
    }

    public boolean isEnabled() {
        return false;
    }

    public boolean isPreferredSizeSet() {
        return prefSizeIsSet;
    }

    public boolean isVisible() {
        return true;
    }
    public void repaint() {
        draw(null);
    }

    public void setBackground(Color paramColor) {
    }

    public void setEnabled(boolean paramBoolean) {
    }

    public Dimension getSize() {
        return new Dimension(getWidth(), getHeight());
    }

    public void setPreferredSize(Dimension prefDimension) {
        if (prefDimension == null) {
            prefWidth = 0;
            prefHeight = 0;
            prefSizeIsSet = false;
        }
        else {
            prefWidth = prefDimension.width;
            prefHeight = prefDimension.height;
            prefSizeIsSet = true;
        }
    }

    /**
     * Tests if this component is opaque. All "heavyweight" (natively-drawn)
     * components are opaque. A component is opaque if it draws all pixels in
     * the bounds; a lightweight component is partially transparent if it lets
     * pixels underneath show through. Subclasses that guarantee that all pixels
     * will be drawn should override this.
     *
     * @return true if this is opaque
     * @see #isLightweight()
     * @since 1.2
     */
    public boolean isOpaque() {
        return !isLightweight();
    }

    /**
     * Return whether the component is lightweight. That means the component has
     * no native peer, but is displayable. This applies to subclasses of
     * JComponent not in this package, such as javax.swing.
     *
     * @return true if the component has a lightweight peer
     * @see #isDisplayable()
     * @since 1.2
     */
    public boolean isLightweight() {
        return true;
    }

    public void addNotify() {
        // TODO Auto-generated method stub
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null)
            changeSupport = new PropertyChangeSupport(this);
        changeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (changeSupport == null)
            changeSupport = new PropertyChangeSupport(this);
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (changeSupport != null)
            changeSupport.removePropertyChangeListener(propertyName, listener);
    }
}
