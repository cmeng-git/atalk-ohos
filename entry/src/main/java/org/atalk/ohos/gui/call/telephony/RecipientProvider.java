package org.atalk.ohos.gui.call.telephony;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.call.telephony.RecipientSelectView.Recipient;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.app.Context;
import ohos.media.image.PixelMap;

public class RecipientProvider extends BaseItemProvider implements Filterable {
    private final Context context;
    private List<Recipient> recipients;
    private String highlight;
    private boolean showAdvancedInfo;


    public RecipientProvider(Context context) {
        super();
        this.context = context;
    }

    public void setRecipients(List<Recipient> recipients) {
        this.recipients = recipients;
        notifyDataChanged();
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    @Override
    public int getCount() {
        return recipients == null ? 0 : recipients.size();
    }

    @Override
    public Recipient getItem(int position) {
        return recipients == null ? null : recipients.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Component getComponent(int position, Component view, ComponentContainer parent) {
        if (view == null) {
            view = newView(parent);
        }

        Recipient recipient = getItem(position);
        bindView(view, recipient);

        return view;
    }

    private Component newView(ComponentContainer parent) {
        Component view = LayoutScatter.getInstance(context).parse(ResourceTable.Layout_recipient_dropdown_item, parent, false);

        RecipientTokenHolder holder = new RecipientTokenHolder(view);
        view.setTag(holder);
        return view;
    }

    private void bindView(Component view, Recipient recipient) {
        RecipientTokenHolder holder = (RecipientTokenHolder) view.getTag();
        holder.name.setText(highlightText(recipient.getDisplayNameOrUnknown(context)));

        String address = recipient.address.getAddress();
        holder.phone.setText(highlightText(address));

        setContactPhotoOrPlaceholder(context, holder.photo, recipient);
    }

    public static void setContactPhotoOrPlaceholder(Context context, PixelMap imageView, Recipient recipient) {
//        ContactPicture.getContactPictureLoader(context).loadContactPicture(address, imageView);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                if (recipients == null) {
                    return null;
                }

                FilterResults result = new FilterResults();
                result.values = recipients;
                result.count = recipients.size();

                return result;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataChanged();
            }
        };
    }

    public void setShowAdvancedInfo(boolean showAdvancedInfo) {
        this.showAdvancedInfo = showAdvancedInfo;
    }

    private static class RecipientTokenHolder {
        public final Text name;
        public final Text phone;
        final PixelMap photo;

        RecipientTokenHolder(Component view) {
            name = view.findComponentById(ResourceTable.Id_text1);
            phone = view.findComponentById(ResourceTable.Id_text2);
            photo = view.findComponentById(ResourceTable.Id_contact_photo);
        }
    }

    private Spannable highlightText(String text) {
        Spannable highlightedSpannable = Spannable.Factory.getInstance().newSpannable(text);

        if (highlight == null) {
            return highlightedSpannable;
        }

        Pattern pattern = Pattern.compile(highlight, Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            highlightedSpannable.setSpan(
                    new ForegroundColorSpan(context.getResources().getColor(android.ResourceTable.Color_holo_blue_light)),
                    matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return highlightedSpannable;
    }
}
