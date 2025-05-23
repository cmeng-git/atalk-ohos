package org.atalk.ohos.gui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.Text;
import ohos.agp.window.dialog.CommonDialog;
import ohos.app.Context;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.chat.ChatAbility;

/**
 * The <code>AttachOptionDialog</code> provides user with optional attachments.
 *
 * @author Eng Chong Meng
 */
public class AttachOptionDialog extends CommonDialog {
    private AttachOptionModeAdapter mAttachOptionAdapter = null;
    private AttachOptionItem mSelectedItem = null;
    private final ChatAbility mParent;

    public AttachOptionDialog(Context context) {
        super(context);
        mParent = (ChatAbility) context;
        setTitleText(context.getString(ResourceTable.String_file_attachment));
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        LayoutScatter scatter = LayoutScatter.getInstance(mParent);
        Component component = scatter.parse(ResourceTable.Layout_activity_attach_option_dialog, null, false);
        setContentCustomComponent(component);

        ListContainer mListContainer = mParent.findComponentById(ResourceTable.Id_attach_optionlist);
        List<AttachOptionItem> items = new ArrayList<>(Arrays.asList(AttachOptionItem.values()));
        mAttachOptionAdapter = new AttachOptionModeAdapter(mParent.getContext(), ResourceTable.Layout_attach_option_child_row, items);
        mListContainer.setItemProvider(mAttachOptionAdapter);
        mListContainer.setItemClickedListener((parent, view, position, id) -> {
            mSelectedItem = (AttachOptionItem) mAttachOptionAdapter.getItem((int) id);
            mParent.sendAttachment(mSelectedItem);
            closeDialog();
        });
    }

    public void closeDialog() {
        destroy();
    }

    public class AttachOptionModeAdapter extends BaseItemProvider {
        int layoutResourceId;
        List<AttachOptionItem> mData;
        Context mContext;

        public AttachOptionModeAdapter(Context context, int textViewResourceId, List<AttachOptionItem> modes) {
            super();
            mContext = context;
            layoutResourceId = textViewResourceId;
            mData = modes;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int i) {
            return mData.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public Component getComponent(int position, Component convertView, ComponentContainer parent) {
            Component row = convertView;
            AttachOptionHolder holder;
            if (row == null) {
                LayoutScatter inflater = LayoutScatter.getInstance(mParent);
                row = inflater.parse(layoutResourceId, parent, false);

                holder = new AttachOptionHolder();
                holder.imgIcon = row.findComponentById(ResourceTable.Id_attachOption_icon);
                holder.txtTitle = row.findComponentById(ResourceTable.Id_attachOption_screenname);

                row.setTag(holder);
            }
            else {
                holder = (AttachOptionHolder) row.getTag();
            }

            // AttachOptionItem item = data.get(position);
            holder.txtTitle.setText(((Text) getItem(position)).getId());
            holder.imgIcon.setPixelMap(((Image) getItem(position)).getPixelMap());
            return row;
        }
    }

    static class AttachOptionHolder {
        Image imgIcon;
        Text txtTitle;
    }
}
