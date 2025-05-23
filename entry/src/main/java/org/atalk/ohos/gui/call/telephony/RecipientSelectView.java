package org.atalk.ohos.gui.call.telephony;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

import ohos.agp.components.AttrSet;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.Text;
import ohos.agp.utils.Rect;
import ohos.app.Context;
import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;
import ohos.multimodalinput.event.TouchEvent;
import ohos.utils.net.Uri;

import org.apache.http.util.TextUtils;
import org.apache.james.mime4j.util.CharsetUtil;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.aTalk;

import timber.log.Timber;

public class RecipientSelectView extends TokTokenCompleteTextView<RecipientSelectView.Recipient>
        implements LoaderCallbacks<List<RecipientSelectView.Recipient>>, AlternateRecipientProvider.AlternateRecipientListener {
    private static final int MINIMUM_LENGTH_FOR_FILTERING = 2;
    private static final String ARG_QUERY = "query";
    private static final int LOADER_ID_FILTERING = 0;
    private static final int LOADER_ID_ALTERNATES = 1;

    private RecipientProvider mRecipientProvider;
    private LoaderManager loaderManager;

    private ListPopupWindow alternatesPopup;
    private AlternateRecipientProvider alternatesProvider;
    private Recipient alternatesPopupRecipient;
    private TokenListener<Recipient> listener;
    private Context mContext;

    public RecipientSelectView(Context context) {
        super(context);
        initView(context);
    }

    public RecipientSelectView(Context context, AttrSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public RecipientSelectView(Context context, AttrSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {
        mContext = context;
        alternatesPopup = new ListPopupWindow(context);
        alternatesProvider = new AlternateRecipientProvider(context, this);
        alternatesPopup.setItemProvider(alternatesProvider);

        // allow only single entry
        setTokenLimit(1);

        // if a token is completed, pick an entry based on best guess.
        // Note that we override performCompletion, so this doesn't actually do anything
        performBestGuess(true);

        mRecipientProvider = new RecipientProvider(context);
        setAdapter(mRecipientProvider);
        setLongClickable(true);

        // cmeng - must init loaderManager in initView to take care of screen rotation
        loaderManager = LoaderManager.getInstance(aTalk.getFragment(aTalk.CL_FRAGMENT));
    }

    @Override
    protected Component getViewForObject(Recipient recipient) {
        Component view = inflateLayout();
        RecipientTokenViewHolder holder = new RecipientTokenViewHolder(view);
        view.setTag(holder);
        bindObjectView(recipient, view);
        return view;
    }

    private Component inflateLayout() {
        LayoutScatter layoutInflater = LayoutScatter.getInstance(mContext);
        return layoutInflater.parse(ResourceTable.Layout_recipient_token_item, null, false);
    }

    private void bindObjectView(Recipient recipient, Component view) {
        RecipientTokenViewHolder holder = (RecipientTokenViewHolder) view.getTag();
        holder.vName.setText(recipient.getDisplayNameOrPhone());
        holder.vPhone.setText(recipient.getPhone());
        RecipientProvider.setContactPhotoOrPlaceholder(getContext(), holder.vContactPhoto, recipient);
    }

    public boolean onTouchEvent(Component component, TouchEvent touchEvent) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        Editable text = getText();

        if (text != null && action == MotionEvent.ACTION_UP) {
            int offset = getOffsetForPosition(event.getX(), event.getY());

            if (offset != -1) {
                TokenImageSpan[] links = text.getSpans(offset, offset, RecipientTokenSpan.class);
                if (links.length > 0) {
                    showAlternates(links[0].getToken());
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected Recipient defaultObject(String completionText) {
        Address[] parsedAddresses = Address.parse(completionText);
        if (!CharsetUtil.isASCII(completionText)) {
            setError(mContext.getString(ResourceTable.String_recipient_error_non_ascii));
            return null;
        }
        if (parsedAddresses.length == 0 || parsedAddresses[0].getAddress() == null) {
            // aTalk telephony call can go with text string only i.e. not Address
            // setError(getContext().getString(ResourceTable.String_recipient_error_parse_failed));
            return null;
        }
        return new Recipient(parsedAddresses[0]);
    }

    public boolean isEmpty() {
        return getObjects().isEmpty();
    }

    public void setLoaderManager(LoaderManager loaderManager) {
        this.loaderManager = loaderManager;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (loaderManager != null) {
            loaderManager.destroyLoader(LOADER_ID_ALTERNATES);
            loaderManager.destroyLoader(LOADER_ID_FILTERING);
            loaderManager = null;
        }
    }

    @Override
    public void onFocusChanged(boolean hasFocus, int direction, Rect previous) {
        super.onFocusChanged(hasFocus, direction, previous);
        if (hasFocus) {
            displayKeyboard();
        }
    }

    /**
     * TokenCompleteTextView removes composing strings, and etc, but leaves internal composition
     * predictions partially constructed. Changing either/or the Selection or Candidate start/end
     * positions, forces the IMM to reset cleaner.
     */
    @Override
    protected void replaceText(CharSequence text) {
        super.replaceText(text);
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.updateSelection(this, getSelectionStart(), getSelectionEnd(), -1, -1);
    }

    private void displayKeyboard() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public void showDropDown() {
        boolean cursorIsValid = mRecipientProvider != null;
        if (!cursorIsValid) {
            return;
        }
        super.showDropDown();
    }

    @Override
    public void performCompletion() {
        if (getListSelection() == ListContainer.INVALID_POSITION && enoughToFilter()) {
            Object recipientText = defaultObject(currentCompletionText());
            if (recipientText != null) {
                replaceText(convertSelectionToString(recipientText));
            }
        }
        else {
            super.performCompletion();
        }
    }

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        if (loaderManager == null) {
            return;
        }

        String query = text.toString();
        if (TextUtils.isEmpty(query) || query.length() < MINIMUM_LENGTH_FOR_FILTERING) {
            loaderManager.destroyLoader(LOADER_ID_FILTERING);
            return;
        }
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        loaderManager.restartLoader(LOADER_ID_FILTERING, args, this);
    }

    private void redrawAllTokens() {
        Editable text = getText();
        if (text == null) {
            return;
        }
        RecipientTokenSpan[] recipientSpans = text.getSpans(0, text.length(), RecipientTokenSpan.class);
        for (RecipientTokenSpan recipientSpan : recipientSpans) {
            bindObjectView(recipientSpan.getToken(), recipientSpan.view);
        }
        invalidate();
    }

    public void addRecipients(Recipient... recipients) {
        for (Recipient recipient : recipients) {
            addObjectSync(recipient);
        }
    }

    public Address[] getAddresses() {
        List<Recipient> recipients = getObjects();
        Address[] address = new Address[recipients.size()];
        for (int i = 0; i < address.length; i++) {
            address[i] = recipients.get(i).address;
        }
        return address;
    }

    private void showAlternates(Recipient recipient) {
        if (loaderManager == null) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(getWindowToken(), 0);

        alternatesPopupRecipient = recipient;
        loaderManager.restartLoader(LOADER_ID_ALTERNATES, null, RecipientSelectView.this);
    }

    public void postShowAlternatesPopup(final List<Recipient> data) {
        // We delay this call so the soft keyboard is gone by the time the popup is layout
        new EventHandler(EventRunner.create()).postTask(() -> {
            showAlternatesPopup(data);
        }, 100);
    }

    public void showAlternatesPopup(List<Recipient> data) {
        if (loaderManager == null) {
            return;
        }

        // Copy anchor settings from the autocomplete dropdown
        Component anchorView = getRootView().findComponentById(getDropDownAnchor());
        alternatesPopup.setAnchorView(anchorView);
        alternatesPopup.setWidth(getDropDownWidth());

        alternatesProvider.setCurrentRecipient(alternatesPopupRecipient);
        alternatesProvider.setAlternateRecipientInfo(data);

        // Clear the checked item.
        alternatesPopup.show();
        ListContainer listView = alternatesPopup.getListContainer();
        if (listView != null)
            listView.setChoiceMode(ListContainer.CHOICE_MODE_SINGLE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        alternatesPopup.dismiss();
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public Loader<List<Recipient>> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_FILTERING: {
                String query = args != null && args.containsKey(ARG_QUERY) ? args.getString(ARG_QUERY) : "";
                mRecipientProvider.setHighlight(query);
                return new RecipientLoader(getContext(), query);
            }
            case LOADER_ID_ALTERNATES: {
                Uri contactLookupUri = alternatesPopupRecipient.getContactLookupUri();
                if (contactLookupUri != null) {
                    return new RecipientLoader(getContext(), contactLookupUri, true);
                }
                else {
                    return new RecipientLoader(getContext(), alternatesPopupRecipient.address);
                }
            }
        }
        throw new IllegalStateException("Unknown Loader ID: " + id);
    }

    @Override
    public void onLoadFinished(Loader<List<Recipient>> loader, List<Recipient> data) {
        if (loaderManager == null) {
            return;
        }

        switch (loader.getId()) {
            case LOADER_ID_FILTERING: {
                mRecipientProvider.setRecipients(data);
                break;
            }
            case LOADER_ID_ALTERNATES: {
                postShowAlternatesPopup(data);
                loaderManager.destroyLoader(LOADER_ID_ALTERNATES);
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Recipient>> loader) {
        if (loader.getId() == LOADER_ID_FILTERING) {
            mRecipientProvider.setHighlight(null);
            mRecipientProvider.setRecipients(null);
        }
    }

    public boolean tryPerformCompletion() {
        if (!hasUncompletedText()) {
            return false;
        }
        int previousNumRecipients = getTokenCount();
        performCompletion();
        int numRecipients = getTokenCount();

        return previousNumRecipients != numRecipients;
    }

    private int getTokenCount() {
        return getObjects().size();
    }

    public boolean hasUncompletedText() {
        String currentCompletionText = currentCompletionText();
        return !TextUtils.isEmpty(currentCompletionText) && !isPlaceholderText(currentCompletionText);
    }

    static private boolean isPlaceholderText(String currentCompletionText) {
        // TODO string matching here is sort of a hack, but it's somewhat reliable and the info isn't easily available
        return currentCompletionText.startsWith("+") && currentCompletionText.substring(1).matches("[0-9]+");
    }

    @Override
    public void onRecipientRemove(Recipient currentRecipient) {
        alternatesPopup.dismiss();
        removeObjectSync(currentRecipient);
    }

    @Override
    public void onRecipientChange(Recipient recipientToReplace, Recipient alternateRecipient) {
        alternatesPopup.dismiss();

        List<Recipient> currentRecipients = getObjects();
        int indexOfRecipient = currentRecipients.indexOf(recipientToReplace);
        if (indexOfRecipient == -1) {
            Timber.e("Tried to refresh invalid view token!");
            return;
        }
        Recipient currentRecipient = currentRecipients.get(indexOfRecipient);
        currentRecipient.address = alternateRecipient.address;
        currentRecipient.addressLabel = alternateRecipient.addressLabel;

        Component recipientTokenView = getTokenViewForRecipient(currentRecipient);
        if (recipientTokenView == null) {
            Timber.e("Tried to refresh invalid view token!");
            return;
        }

        bindObjectView(currentRecipient, recipientTokenView);
        if (listener != null) {
            listener.onTokenChanged(currentRecipient);
        }
        invalidate();
    }

    /**
     * This method builds the span given a address object. We override it with identical
     * functionality, but using the custom RecipientTokenSpan class which allows us to
     * retrieve the view for redrawing at a later point.
     */
    @Override
    protected TokenImageSpan buildSpanForObject(Recipient obj) {
        if (obj == null) {
            return null;
        }
        Component tokenView = getViewForObject(obj);
        return new RecipientTokenSpan(tokenView, obj);
    }

    /**
     * Find the token view tied to a given address. This method relies on spans to
     * be of the RecipientTokenSpan class, as created by the buildSpanForObject method.
     */
    private Component getTokenViewForRecipient(Recipient currentRecipient) {
        Editable text = getText();
        if (text == null) {
            return null;
        }

        RecipientTokenSpan[] recipientSpans = text.getSpans(0, text.length(), RecipientTokenSpan.class);
        for (RecipientTokenSpan recipientSpan : recipientSpans) {
            if (recipientSpan.getToken().equals(currentRecipient)) {
                return recipientSpan.view;
            }
        }
        return null;
    }

    /**
     * We use a specialized version of TokenCompleteTextView.TokenListener as well,
     * adding a callback for onTokenChanged.
     */
    public void setTokenListener(TokenListener<Recipient> listener) {
        super.setTokenListener(listener);
        this.listener = listener;
    }

    public interface TokenListener<T> extends TokenCompleteTextView.TokenListener<T> {
        void onTokenChanged(T token);
    }

    private class RecipientTokenSpan extends TokenImageSpan {
        private final Component view;

        public RecipientTokenSpan(Component view, Recipient token) {
            super(view, token);
            this.view = view;
        }
    }

    private static class RecipientTokenViewHolder {
        final Text vName;
        final Text vPhone;
        final Image vContactPhoto;

        RecipientTokenViewHolder(Component view) {
            vName = view.findComponentById(ResourceTable.Id_text1);
            vPhone = view.findComponentById(ResourceTable.Id_text2);
            vContactPhoto = view.findComponentById(ResourceTable.Id_contact_photo);
        }
    }

    public static class Recipient implements Serializable {
        // null means the address is not associated with a contact
        public final Long contactId;
        public final String contactLookupKey;

        public Address address;
        public String addressLabel;

        // null if the contact has no photo. transient because we serialize this manually, see below.
        public transient Uri photoThumbnailUri;

        public Recipient(Address address) {
            this.address = address;
            this.contactId = null;
            this.contactLookupKey = null;
        }

        public Recipient(String name, String phone, String addressLabel, long contactId, String lookupKey) {
            this.address = new Address(phone, name);
            this.contactId = contactId;
            this.addressLabel = addressLabel;
            this.contactLookupKey = lookupKey;
        }

        public String getDisplayNameOrPhone() {
            final String displayName = getDisplayName();
            if (displayName != null) {
                return displayName;
            }
            return address.getAddress();
        }

        public String getPhone() {
            return address.getAddress();
        }

        public static boolean isValidPhoneNum(CharSequence target) {
            return (target != null) && (target.length() >= 4)
                    && Patterns.PHONE.matcher(target).matches();
        }

        public String getDisplayNameOrUnknown(Context context) {
            String displayName = getDisplayName();
            if (displayName != null) {
                return displayName;
            }
            return context.getString(ResourceTable.String_unknown_recipient);
        }

        public String getNameOrUnknown(Context context) {
            String name = address.getPerson();
            if (name != null) {
                return name;
            }
            return context.getString(ResourceTable.String_unknown_recipient);
        }

        private String getDisplayName() {
            if (TextUtils.isEmpty(address.getPerson())) {
                return null;
            }

            String displayName = address.getPerson();
            if (addressLabel != null) {
                displayName += " (" + addressLabel + ")";
            }
            return displayName;
        }

        public Uri getContactLookupUri() {
            if (contactId == null) {
                return null;
            }
            return Contacts.getLookupUri(contactId, contactLookupKey);
        }

        @Override
        public boolean equals(Object o) {
            // Equality is entirely up to the address
            return o instanceof Recipient && address.equals(((Recipient) o).address);
        }

        private void writeObject(ObjectOutputStream oos)
                throws IOException {
            oos.defaultWriteObject();

            // custom serialization, Android's Uri class is not serializable
            if (photoThumbnailUri != null) {
                oos.writeInt(1);
                oos.writeUTF(photoThumbnailUri.toString());
            }
            else {
                oos.writeInt(0);
            }
        }

        private void readObject(ObjectInputStream ois)
                throws ClassNotFoundException, IOException {
            ois.defaultReadObject();
            // custom deserialization, Android's Uri class is not serializable
            if (ois.readInt() != 0) {
                String uriString = ois.readUTF();
                photoThumbnailUri = Uri.parse(uriString);
            }
        }
    }
}
