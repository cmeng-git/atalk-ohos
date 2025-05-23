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
package org.atalk.util.event;

import org.atalk.ohos.agp.components.JComponent;

/**
 * Represents a <code>VideoEvent</code> which notifies about an update to the size
 * of a specific visual <code>JComponent</code> depicting video.
 *
 * @author Lyubomir Marinov
 * @author Eng Chong Meng
 */
public class SizeChangeVideoEvent extends VideoEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The new height of the associated visual <code>JComponent</code>.
     */
    private final int height;

    /**
     * The new width of the associated visual <code>JComponent</code>.
     */
    private final int width;

    /**
     * Initializes a new <code>SizeChangeVideoEvent</code> which is to notify about
     * an update to the size of a specific visual <code>JComponent</code> depicting video.
     *
     * @param source the source of the new <code>SizeChangeVideoEvent</code>
     * @param visualComponent the visual <code>JComponent</code> depicting video with the updated size
     * @param origin the origin of the video the new <code>SizeChangeVideoEvent</code> is to notify about
     * @param width the new width of <code>visualComponent</code>
     * @param height the new height of <code>visualComponent</code>
     */
    public SizeChangeVideoEvent(Object source, JComponent visualComponent, int origin, int width, int height)
    {
        super(source, VIDEO_SIZE_CHANGE, visualComponent, origin);
        this.width = width;
        this.height = height;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Makes sure that the cloning of this instance initializes a new
     * <code>SizeChangeVideoEvent</code> instance.
     */
    @Override
    public VideoEvent clone(Object source)
    {
        return new SizeChangeVideoEvent(source, getVisualComponent(), getOrigin(), getWidth(), getHeight());
    }

    /**
     * Gets the new height of the associated visual <code>JComponent</code>.
     *
     * @return the new height of the associated visual <code>JComponent</code>
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * Gets the new width of the associated visual <code>JComponent</code>.
     *
     * @return the new width of the associated visual <code>JComponent</code>
     */
    public int getWidth()
    {
        return width;
    }
}
