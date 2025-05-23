package com.pierfrancescosoffritti.youtubeplayer.ui.menu.defaultMenu;

import java.util.ArrayList;
import java.util.List;

import ohos.agp.components.Component;
import ohos.agp.components.DirectionalLayoutManager;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.app.Context;

import com.pierfrancescosoffritti.youtubeplayer.ui.menu.MenuItem;
import com.pierfrancescosoffritti.youtubeplayer.ui.menu.YouTubePlayerMenu;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.dialogs.PopupMenu;

import timber.log.Timber;

public class DefaultYouTubePlayerMenu implements YouTubePlayerMenu {

    private final Context context;
    private final List<MenuItem> menuItems;

    private PopupMenu popupWindow;

    public DefaultYouTubePlayerMenu(Context context) {
        this.context = context;

        this.menuItems = new ArrayList<>();
    }

    @Override
    public void show(Component anchorView) {
        popupWindow = createPopupWindow();
        // popupWindow.showAsDropDown(anchorView, 0, - context.getResources().getDimensionPixelSize(ResourceTable.Float_ayp_8dp) * 4);

        if(menuItems.isEmpty())
            Timber.e(YouTubePlayerMenu.class.getName(), "The menu is empty");
    }

    @Override
    public void dismiss() {
        if (popupWindow != null)
            popupWindow.dismiss();
    }

    @Override
    public YouTubePlayerMenu addItem(MenuItem menuItem) {
        menuItems.add(menuItem);
        return this;
    }

    @Override
    public YouTubePlayerMenu removeItem(int itemIndex) {
        menuItems.remove(itemIndex);
        return this;
    }

    @Override
    public YouTubePlayerMenu removeItem(MenuItem menuItem) {
        menuItems.remove(menuItem);
        return this;
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    private PopupMenu createPopupWindow() {
        LayoutScatter layoutScatter = LayoutScatter.getInstance(context);
        Component view = layoutScatter.parse(ResourceTable.Layout_ayp_player_menu, null, false);

        ListContainer recyclerView = view.findComponentById(ResourceTable.Id_recycler_view);
        recyclerView.setLayoutManager(new DirectionalLayoutManager());

        recyclerView.setItemProvider(new MenuAdapter(context, menuItems));
        // recyclerView.setHasFixedSize(true);

        PopupMenu popupWindow = new PopupMenu(context);
//        popupWindow.setupMenu()
//        , view, ComponentContainer.LayoutConfig.MATCH_CONTENT, ComponentContainer.LayoutConfig.MATCH_CONTENT);
//        popupWindow.setContentView(view);
//        popupWindow.setFocusable(true);

        return popupWindow;
    }
}
