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
package org.atalk.ohos.gui.call;

import java.awt.*;

import ohos.agp.components.AttrSet;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.DirectionalLayout;
import ohos.app.Context;

import org.atalk.impl.neomedia.codec.video.MediaDecoder;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.agp.components.JComponent;

import timber.log.Timber;

/**
 * Layout that aligns remote video <code>JComponent.</code> by stretching it to max screen width or height.
 * It also controls whether call control buttons group should be auto hidden or stay visible all the time.
 * This layout will work only with <code>VideoCallAbility</code>.
 * <p>
 * IMPORTANT: it can't be done from <code>Ability</code>, because just after the views are created,
 * we don't know their sizes yet(return 0 or invalid).
 *
 * @author Eng Chong Meng
 */
public class RemoteVideoLayout extends DirectionalLayout implements Component.EstimateSizeListener {
    /**
     * Last saved preferred video size used to calculate the max screen scaling.
     * Must set to null for sizeChange detection on first layout init; and when the remote view is removed
     */
    protected Dimension preferredSize = null;

    /**
     * Flag indicates any size change on new request. Always forces to requestLayout state if true
     */
    private boolean preferredSizeChanged = false;

    /**
     * Stores last child count.
     */
    private int lastChildCount = -1;

    public RemoteVideoLayout(Context context) {
        super(context);
    }

    public RemoteVideoLayout(Context context, AttrSet attrs) {
        super(context, attrs);
    }

    public RemoteVideoLayout(Context context, AttrSet attrs, String styleName) {
        super(context, attrs, styleName);
    }

    /**
     * SizeChange algorithm uses preferredSize and videoSize ratio compare algorithm for full screen video;
     * Otherwise, non-null remoteVideoView will also return false when remote video dimension changes.
     * Note: use ratio compare algorithm to avoid unnecessary doAlignRemoteVideo reDraw unless there is a ratio change
     *
     * @param videoSize received video stream size
     * @param requestLayout true to force relayout request
     *
     * @return <code>false</code> if no change is required for remoteVideoViewContainer dimension update
     * to playback the newly received video size:
     * @see MediaDecoder#configureMediaCodec(Codec, String)
     */
    public boolean setVideoPreferredSize(Dimension videoSize, boolean requestLayout) {
        preferredSizeChanged = requestLayout || (preferredSize == null)
                || Math.abs(preferredSize.width / preferredSize.height - videoSize.width / videoSize.height) > 0.01f;

        preferredSize = videoSize;
        refreshContour();
        return preferredSizeChanged;
    }

    @Override
    public boolean onEstimateSize(int widthMeasureSpec, int heightMeasureSpec) {
        int childCount = getChildCount();
        if ((childCount == lastChildCount) && !preferredSizeChanged) {
            return false;
        }

        // Store values to prevent from too many calculations
        lastChildCount = childCount;
        preferredSizeChanged = false;

        Context ctx = getContext();
        if (!(ctx instanceof VideoCallAbility)) {
            return false;
        }

        VideoCallAbility videoAbility = (VideoCallAbility) ctx;
        if (childCount > 0) {
            /*
             * MeasureSpec.getSize() is determined by previous layout dimension, any may not in full screen size;
             * So force to use the device default display full screen dimension.
             * // int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
             * // int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
             */
            int parentWidth = aTalkApp.mDisplaySize.width;
            int parentHeight = aTalkApp.mDisplaySize.height;
            if (!aTalkApp.isPortrait) {
                parentWidth = parentHeight;
                parentHeight = parentWidth;
            }

            double width;
            double height;
            if (preferredSize != null) {
                width = preferredSize.width;
                height = preferredSize.height;
            }
            else {
                // NullPointerException from the field? so give it a default
                width = VideoHandlerSlice.DEFAULT_WIDTH;
                height = VideoHandlerSlice.DEFAULT_HEIGHT;
            }

            // Stretch to match height
            if (parentHeight <= parentWidth) {
                // Timber.i("Stretch to device max height: %s", parentHeight);
                double ratio = width / height;
                height = parentHeight;
                // width = height * ratio;
                width = Math.ceil((height * ratio) / 16.0) * 16;
                videoAbility.ensureAutoHideFragmentAttached();
            }
            // Stretch to match width
            else {
                // Timber.i("Stretch to device max width: %s", parentWidth);
                double ratio = height / width;
                width = parentWidth;
                height = Math.ceil((width * ratio) / 16.0) * 16;
                videoAbility.ensureAutoHideFragmentDetached();
            }

            Timber.i("Remote video view dimension: [%s x %s]", width, height);
            this.setEstimatedSize((int) width, (int) height);

            ComponentContainer.LayoutConfig params = getLayoutConfig();
            params.width = (int) width;
            params.height = (int) height;
            setLayoutConfig(params);

            for (int i = 0; i < childCount; i++) {
                JComponent child = (JComponent) getComponentAt(i);
                ComponentContainer.LayoutConfig chP = child.getLayoutConfig();
                chP.width = params.width;
                chP.height = params.height;
                child.setLayoutConfig(chP);
            }
        }
        else {
            ComponentContainer.LayoutConfig params = getLayoutConfig();
            params.width = LayoutConfig.MATCH_CONTENT;
            params.height = LayoutConfig.MATCH_CONTENT;
            this.setLayoutConfig(params);
            videoAbility.ensureAutoHideFragmentDetached();
        }
        return true;
    }
}
