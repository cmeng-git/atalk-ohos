package com.pierfrancescosoffritti.youtubeplayer.player.views;


import ohos.agp.components.AttrSet;
import ohos.agp.components.Component;
import ohos.agp.components.StackLayout;
import ohos.app.Context;

/**
 * A FrameLayout with an aspect ration of 16:9, when the height is set to wrap_content.
 */
public class SixteenByNineFrameLayout extends StackLayout implements Component.EstimateSizeListener {
    public SixteenByNineFrameLayout(Context context) {
        this(context, null);
    }

    public SixteenByNineFrameLayout(Context context, AttrSet attrSet) {
        this(context, attrSet, null);
    }

    public SixteenByNineFrameLayout(Context context, AttrSet attrSet, String styleName) {
        super(context, attrSet, styleName);
    }

    @Override
    public boolean onEstimateSize(int widthMeasureSpec, int heightMeasureSpec) {
        // if height == wrap content make the view 16:9
        if (getLayoutConfig().height == LayoutConfig.MATCH_CONTENT) {
            int sixteenNineHeight = EstimateSpec.getSizeWithMode(EstimateSpec.getSize(widthMeasureSpec) * 9 / 16, EstimateSpec.PRECISE);
            setComponentSize(widthMeasureSpec, sixteenNineHeight);
        }
        else
            setComponentSize(widthMeasureSpec, heightMeasureSpec);

        return true;
    }
}