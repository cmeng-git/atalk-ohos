package com.pierfrancescosoffritti.youtubeplayer.ui.menu;

import ohos.agp.components.Component.ClickedListener;

public class MenuItem {
    private final String mText;
    private final int mIcon;

    private final ClickedListener mClickListener;

    public MenuItem(String text, final int icon, ClickedListener onClickListener) {
        mText = text;
        mIcon = icon;
        mClickListener = onClickListener;
    }

    public String getText() {
        return mText;
    }

    public int getIcon() {
        return mIcon;
    }

    public ClickedListener getOnClickListener() {
        return mClickListener;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MenuItem menuItem = (MenuItem) o;
        return mIcon == menuItem.mIcon && mText.equals(menuItem.mText);
    }

    @Override
    public int hashCode() {
        int result = mText.hashCode();
        result = 31 * result + mIcon;
        return result;
    }
}