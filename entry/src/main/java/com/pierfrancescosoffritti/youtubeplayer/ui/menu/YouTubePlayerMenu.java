package com.pierfrancescosoffritti.youtubeplayer.ui.menu;

import ohos.agp.components.Component;
public interface YouTubePlayerMenu {
    int getItemCount();
    void show(Component anchorView);
    void dismiss();

    YouTubePlayerMenu addItem(MenuItem menuItem);
    YouTubePlayerMenu removeItem(int itemIndex);
    YouTubePlayerMenu removeItem(MenuItem menuItem);
}
