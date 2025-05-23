/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atalk.util.swing;

import java.awt.Dimension;

import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutManager;
import ohos.agp.utils.LayoutAlignment;
import ohos.agp.utils.Rect;

import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.agp.components.JComponent;

/**
 * Represents a <code>LayoutManager</code> which centers the first
 * <code>JComponent</code> within its <code>Container</code> and, if the preferred size
 * of the <code>JComponent</code> is larger than the size of the <code>Container</code>,
 * scales the former within the bounds of the latter while preserving the aspect
 * ratio. <code>FitLayout</code> is appropriate for <code>Container</code>s which
 * display a single image or video <code>JComponent</code> in its entirety for which
 * preserving the aspect ratio is important.
 *
 * @author Lyubomir Marinov
 */
public class FitLayout extends LayoutManager {
    /**
     * The default height and width to be used by <code>FitLayout</code> and its
     * extenders in order to avoid falling back to zero height and/or width.
     * Introduced to mitigate issues arising from the fact that a
     * <code>JComponent</code> zero height and/or width.
     */
    protected static final int DEFAULT_HEIGHT_OR_WIDTH = 16;

    /**
     * {@inheritDoc}
     * <p>
     * Does nothing because this <code>LayoutManager</code> lays out only the first
     * <code>JComponent</code> of the parent <code>Container</code> and thus doesn't need
     * any <code>String</code> associations.
     */
    public void addLayoutComponent(String name, JComponent comp) {
    }

    /**
     * Gets the first <code>JComponent</code> of a specific <code>Container</code> if
     * there is such a <code>JComponent</code>.
     *
     * @param parent the <code>Container</code> to retrieve the first <code>JComponent</code> of
     *
     * @return the first <code>JComponent</code> of a specific <code>Container</code> if
     * there is such a <code>JComponent</code>; otherwise, <code>null</code>
     */
    protected JComponent getComponent(ComponentContainer parent) {
        int childCount = parent.getChildCount();
        return (childCount > 0) ? (JComponent) parent.getComponentAt(0) : null;
    }

    protected void layoutComponent(JComponent component, Rect bounds, float alignmentX, float alignmentY) {
        Dimension size = new Dimension(component.getEstimatedWidth(), component.getEstimatedHeight());
        /*
         * XXX The following (mostly) represents a quick and dirty hack for the
         * purposes of video conferencing which adds transparent JPanels to
         * VideoContainer and does not want them fitted because they contain
         * VideoContainers themselves and the videos get fitted in them.
         */
        if ((component instanceof ComponentContainer
                && (component.getAlpha() == 0)
                && ((ComponentContainer) component).getChildCount() > 1)
                || (component instanceof VideoContainer)
                /*
                 * If the specified component does not have a preferredSize, we
                 * cannot know its aspect ratio and we are left with no choice
                 * but to stretch it within the complete bounds.
                 */
                || ((size = component.getPreferredSize()) == null)) {
            size = new Dimension(bounds.getWidth(), bounds.getHeight());
        }
        else {
            boolean scale = false;
            double widthRatio;
            double heightRatio;

            if ((size.width != bounds.getWidth()) && (size.width > 0)) {
                scale = true;
                widthRatio = bounds.getWidth() / (double) size.width;
            }
            else
                widthRatio = 1;
            if ((size.height != bounds.getHeight()) && (size.height > 0)) {
                scale = true;
                heightRatio = bounds.getHeight() / (double) size.height;
            }
            else
                heightRatio = 1;
            if (scale) {
                double ratio = Math.min(widthRatio, heightRatio);

                size.width = (int) (size.width * ratio);
                size.height = (int) (size.height * ratio);
            }
        }

        // Respect the maximumSize of the component.
        if (component.isBoundToWindow()) {
            Dimension maxSize = aTalkApp.mDisplaySize; // component.getgetMaximumSize();

            if (size.width > maxSize.width)
                size.width = maxSize.width;
            if (size.height > maxSize.height)
                size.height = maxSize.height;
        }

        /*
         * Why would one fit a JComponent into a rectangle with zero width and
         * height?
         */
        if (size.height < 1)
            size.height = 1;
        if (size.width < 1)
            size.width = 1;

        component.arrange(
                bounds.left + Math.round((bounds.getWidth() - size.width) * alignmentX),
                bounds.top + Math.round((bounds.getHeight() - size.height) * alignmentY),
                size.width,
                size.height);
    }

    /*
     * Scales the first JComponent if its preferred size is larger than the size
     * of its parent Container in order to display the JComponent in its entirety
     * and then centers it within the display area of the parent.
     */
    public void layoutContainer(ComponentContainer parent) {
        layoutContainer(parent, LayoutAlignment.CENTER);
    }

    protected void layoutContainer(ComponentContainer parent, float componentAlignmentX) {
        JComponent component = getComponent(parent);

        if (component != null) {
            layoutComponent(component,
                    new Rect(0, 0, parent.getRight(), parent.getBottom()),
                    componentAlignmentX, LayoutAlignment.CENTER);
        }
    }

    /*
     * Since this LayoutManager lays out only the first JComponent of the
     * specified parent Container, the minimum size of the Container is the
     * minimum size of the mentioned JComponent.
     */
    public Dimension minimumLayoutSize(ComponentContainer parent) {
        JComponent component = getComponent(parent);
        return (component != null)
                ? component.getPreferredSize()
                : new Dimension(DEFAULT_HEIGHT_OR_WIDTH, DEFAULT_HEIGHT_OR_WIDTH);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Since this <code>LayoutManager</code> lays out only the first
     * <code>JComponent</code> of the specified parent <code>Container</code>, the
     * preferred size of the <code>Container</code> is the preferred size of the
     * mentioned <code>JComponent</code>.
     */
    public Dimension preferredLayoutSize(ComponentContainer parent) {
        JComponent component = getComponent(parent);
        return (component != null)
                ? component.getPreferredSize()
                : new Dimension(DEFAULT_HEIGHT_OR_WIDTH, DEFAULT_HEIGHT_OR_WIDTH);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Does nothing because this <code>LayoutManager</code> lays out only the first
     * <code>JComponent</code> of the parent <code>Container</code> and thus doesn't need
     * any <code>String</code> associations.
     */
    public void removeLayoutComponent(JComponent comp) {
    }
}
