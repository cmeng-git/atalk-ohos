package com.pierfrancescosoffritti.youtubeplayer.ui.menu.defaultMenu;

import com.pierfrancescosoffritti.youtubeplayer.ui.menu.MenuItem;

import java.util.List;

import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.app.Context;

import org.atalk.ohos.ResourceTable;

class MenuAdapter extends BaseItemProvider {
    public final Context mContext;
    private final List<MenuItem> menuItems;

    MenuAdapter(Context context, List<MenuItem> menuItems) {
        mContext = context;
        this.menuItems = menuItems;
    }

    public MenuAdapter.ViewHolder onCreateViewHolder(ComponentContainer parent, int viewType) {
        Component view = LayoutScatter.getInstance(parent.getContext()).parse(ResourceTable.Layout_ayp_menu_item, parent, false);
        return new ViewHolder(view);
    }

    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.root.setClickedListener(menuItems.get(position).getOnClickListener());
        holder.textView.setText(menuItems.get(position).getText());
        holder.textView.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getPixelMap(mContext, menuItems.get(position).getIcon()), null, null, null);
    }

    @Override
    public int getCount() {
        return menuItems.size();
    }

    @Override
    public Object getItem(int idx) {
        return menuItems.get(idx);
    }

    @Override
    public long getItemId(int idx) {
        return idx;
    }

    @Override
    public Component getComponent(int i, Component component, ComponentContainer componentContainer) {
        return null;
    }

    static class ViewHolder extends ComponentContainer {
        final Component root;
        final Text textView;

        ViewHolder(Component menuItemView) {
            super(null);
            root = menuItemView;
            textView = menuItemView.findComponentById(ResourceTable.Id_text);
        }
    }
}