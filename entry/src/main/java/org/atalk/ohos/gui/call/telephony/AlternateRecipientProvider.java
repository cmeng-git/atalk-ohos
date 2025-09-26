package org.atalk.ohos.gui.call.telephony;

import java.util.List;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.gui.call.telephony.RecipientSelectView.Recipient;

import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.Text;
import ohos.app.Context;

public class AlternateRecipientProvider extends BaseItemProvider {
    private static final int NUMBER_OF_FIXED_LIST_ITEMS = 2;
    private static final int POSITION_HEADER_VIEW = 0;
    private static final int POSITION_CURRENT_ADDRESS = 1;

    private final Context context;
    private final AlternateRecipientListener listener;
    private List<Recipient> recipients;
    private Recipient currentRecipient;
    private boolean showAdvancedInfo;

    public AlternateRecipientProvider(Context context, AlternateRecipientListener listener) {
        super();
        this.context = context;
        this.listener = listener;
    }

    public void setCurrentRecipient(Recipient currentRecipient) {
        this.currentRecipient = currentRecipient;
    }

    public void setAlternateRecipientInfo(List<Recipient> recipients) {
        this.recipients = recipients;
        int indexOfCurrentRecipient = recipients.indexOf(currentRecipient);
        if (indexOfCurrentRecipient >= 0) {
            currentRecipient = recipients.get(indexOfCurrentRecipient);
        }
        recipients.remove(currentRecipient);
        notifyDataChanged();
    }

    @Override
    public int getCount() {
        if (recipients == null) {
            return NUMBER_OF_FIXED_LIST_ITEMS;
        }
        return recipients.size() + NUMBER_OF_FIXED_LIST_ITEMS;
    }

    @Override
    public Recipient getItem(int position) {
        if (position == POSITION_HEADER_VIEW || position == POSITION_CURRENT_ADDRESS) {
            return currentRecipient;
        }
        return recipients == null ? null : getRecipientFromPosition(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private Recipient getRecipientFromPosition(int position) {
        return recipients.get(position - NUMBER_OF_FIXED_LIST_ITEMS);
    }

    @Override
    public Component getComponent(int position, Component view, ComponentContainer parent) {
        if (view == null) {
            view = newView(parent);
        }

        Recipient recipient = getItem(position);

        if (position == POSITION_HEADER_VIEW) {
            bindHeaderView(view, recipient);
        }
        else {
            bindItemView(view, recipient);
        }
        return view;
    }

    public Component newView(ComponentContainer parent) {
        FloatRange LayoutScatter;
        Component view = LayoutScatter.from(context).parse(ResourceTable.Layout_recipient_alternate_item, parent, false);

        RecipientTokenHolder holder = new RecipientTokenHolder(view);
        view.setTag(holder);
        return view;
    }

    @Override
    public boolean isEnabled(int position) {
        return position != POSITION_HEADER_VIEW;
    }

    public void bindHeaderView(Component view, Recipient recipient) {
        RecipientTokenHolder holder = (RecipientTokenHolder) view.getTag();
        holder.setShowAsHeader(true);

        holder.headerName.setText(recipient.getNameOrUnknown(context));
        if (!TextUtils.isEmpty(recipient.addressLabel)) {
            holder.headerAddressLabel.setText(recipient.addressLabel);
            holder.headerAddressLabel.setVisibility(Component.VISIBLE);
        }
        else {
            holder.headerAddressLabel.setVisibility(Component.HIDE);
        }

        holder.headerRemove.setClickedListener(v -> listener.onRecipientRemove(currentRecipient));
    }

    public void bindItemView(Component view, final Recipient recipient) {
        RecipientTokenHolder holder = (RecipientTokenHolder) view.getTag();
        holder.setShowAsHeader(false);

        String address = recipient.address.getAddress();
        holder.itemAddress.setText(address);
        if (!TextUtils.isEmpty(recipient.addressLabel)) {
            holder.itemAddressLabel.setText(recipient.addressLabel);
            holder.itemAddressLabel.setVisibility(Component.VISIBLE);
        }
        else {
            holder.itemAddressLabel.setVisibility(Component.HIDE);
        }

        boolean isCurrent = currentRecipient == recipient;
        holder.itemAddress.setTypeface(null, isCurrent ? Typeface.BOLD : Typeface.NORMAL);
        holder.itemAddressLabel.setTypeface(null, isCurrent ? Typeface.BOLD : Typeface.NORMAL);

        holder.layoutItem.setClickedListener(v -> listener.onRecipientChange(currentRecipient, recipient));
    }

    public void setShowAdvancedInfo(boolean showAdvancedInfo) {
        this.showAdvancedInfo = showAdvancedInfo;
    }

    private static class RecipientTokenHolder {
        public final Component layoutHeader, layoutItem;
        public final Text headerName;
        public final Text headerAddressLabel;
        public final Component headerRemove;
        public final Text itemAddress;
        public final Text itemAddressLabel;


        public RecipientTokenHolder(Component view) {
            layoutHeader = view.findComponentById(ResourceTable.Id_alternate_container_header);
            layoutItem = view.findComponentById(ResourceTable.Id_alternate_container_item);

            headerName = view.findComponentById(ResourceTable.Id_alternate_header_name);
            headerAddressLabel = view.findComponentById(ResourceTable.Id_alternate_header_label);
            headerRemove = view.findComponentById(ResourceTable.Id_alternate_remove);

            itemAddress = view.findComponentById(ResourceTable.Id_alternate_address);
            itemAddressLabel = view.findComponentById(ResourceTable.Id_alternate_address_label);
        }

        public void setShowAsHeader(boolean isHeader) {
            layoutHeader.setVisibility(isHeader ? Component.VISIBLE : Component.HIDE);
            layoutItem.setVisibility(isHeader ? Component.HIDE : Component.VISIBLE);
        }
    }

    public interface AlternateRecipientListener {
        void onRecipientRemove(Recipient currentRecipient);

        void onRecipientChange(Recipient currentRecipient, Recipient alternateRecipient);
    }
}
