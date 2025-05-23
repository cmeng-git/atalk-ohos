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
package org.atalk.impl.neomedia.device.util;

import ohos.agp.components.AttrSet;
import ohos.agp.components.Component.EstimateSizeListener;
import ohos.agp.components.textureprovider.TextureProvider;
import ohos.app.Context;

import timber.log.Timber;

/**
 * A {@link TextureProvider} that can be adjusted to a specified aspect ratio.
 */
public class AutoFitTextureView extends TextureProvider implements EstimateSizeListener {
    protected int mRatioWidth = 0;
    protected int mRatioHeight = 0;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttrSet attrs) {
        this(context, attrs, "");
    }

    public AutoFitTextureView(Context context, AttrSet attrs, String defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        if (mRatioWidth == width && mRatioHeight == height) {
            return;
        }
        mRatioWidth = width;
        mRatioHeight = height;
        setComponentSize(width, height);
    }

    /**
     * onMeasure will return the container dimension and not the device display size
     *
     * @param widthMeasureSpec EstimateSpec width
     * @param heightMeasureSpec EstimateSpec height
     */
    @Override
    public boolean onEstimateSize(int widthMeasureSpec, int heightMeasureSpec) {
        int width = EstimateSpec.getSize(widthMeasureSpec);
        int height = EstimateSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setEstimatedSize(width, height);
        }
        else {
            setEstimatedSize(mRatioWidth, mRatioHeight);
//            if (width < height * mRatioWidth / mRatioHeight) {
//                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
//                Timber.d("AutoFit TextureView onMeasureW: [%s x %s] => [%s x %s]", width, height, width, width * mRatioHeight / mRatioWidth);
//            }
//            else {
//                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
//                Timber.d("AutoFit TextureView onMeasureH: [%s x %s] => [%s x %s]", width, height, height * mRatioHeight / mRatioWidth, height);
//            }
        }
        Timber.d("AutoFit TextureView onMeasureWH: [%s x %s] => [%s x %s]", width, height, mRatioWidth, mRatioHeight);
        return true;
    }
}
