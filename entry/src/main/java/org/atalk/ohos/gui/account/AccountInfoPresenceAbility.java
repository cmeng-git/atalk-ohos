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
package org.atalk.ohos.gui.account;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;
import ohos.accessibility.ability.SoftKeyBoardController;
import ohos.agp.colors.RgbColor;
import ohos.agp.components.Button;
import ohos.agp.components.Component;
import ohos.agp.components.DatePicker;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.Image;
import ohos.agp.components.ListContainer;
import ohos.agp.components.ProgressBar;
import ohos.agp.components.Text;
import ohos.agp.components.TextField;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.text.InputFilter;
import ohos.agp.text.LengthFilter;
import ohos.media.image.PixelMap;
import ohos.utils.net.Uri;

import com.yalantis.ucrop.UCrop;

import net.java.sip.communicator.service.protocol.AccountID;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredAccountInfo;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.AboutMeDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.AddressDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.BirthDateDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.CityDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.CountryDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.DisplayNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.EmailAddressDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.FirstNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenderDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.ImageDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.JobTitleDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.LastNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.MiddleNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.MobilePhoneDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.NicknameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.PhoneNumberDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.PostalCodeDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.ProvinceDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.URLDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.WorkEmailAddressDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.WorkOrganizationNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.WorkPhoneDetail;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService;
import net.java.sip.communicator.util.ServiceUtils;
import net.java.sip.communicator.util.account.AccountUtils;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.agp.components.Menu;
import org.atalk.ohos.agp.components.MenuItem;
import org.atalk.ohos.gui.AppGUIActivator;
import org.atalk.ohos.gui.account.settings.AccountPreferenceAbility;
import org.atalk.ohos.gui.actionbar.ActionBarUtil;
import org.atalk.ohos.gui.contactlist.ContactInfoAbility;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.ohos.gui.util.event.EventListener;
import org.atalk.ohos.util.AppImageUtil;
import org.atalk.ohos.util.ComponentUtil;
import org.atalk.util.SoftKeyboard;
import org.jivesoftware.smackx.avatar.AvatarManager;

import timber.log.Timber;

/**
 * Ability allows user to set presence status, status message, change the user avatar
 * and all the vCard-temp information for the {@link # Account}.
 * <p>
 * The main panel that allows users to view and edit their account information.
 * Different instances of this class are created for every registered
 * <code>ProtocolProviderService</code>.
 * Currently, supported account details are first/middle/last names, nickname,
 * street/city/region/country address, postal code, birth date, gender,
 * organization name, job title, about me, home/work email, home/work phone.
 * <p>
 * The {@link #mAccount} is retrieved from the {@link Intent} extra by it's
 * {@link AccountID#getAccountUid()}
 *
 * @author Eng Chong Meng
 */
public class AccountInfoPresenceAbility extends BaseAbility
        implements EventListener<AccountEvent>, DialogH.DialogListener,
        SoftKeyboard.SoftKeyboardChanged, DatePicker.ValueChangedListener {
    private DatePicker mDatePicker;

    private static final String AVATAR_ICON_REMOVE = "Remove Picture";

    // avatar default image size
    private static final int AVATAR_PREFERRED_SIZE = 64;
    private static final int CROP_MAX_SIZE = 108;

    /**
     * Intent's extra's key for account ID property of this activity
     */
    static public final String INTENT_ACCOUNT_ID = "account_id";

    /**
     * The account's {@link OperationSetPresence} used to perform presence operations
     */
    private OperationSetPresence accountPresence;

    /**
     * The instance of {@link Account} used for operations on the account
     */
    private Account mAccount;

    /**
     * Flag indicates if there were any uncommitted changes that shall be applied on exit
     */
    private boolean hasChanges = false;

    /**
     * Flag indicates if there were any uncommitted status changes that shall be applied on exit
     */
    private boolean hasStatusChanges = false;

    /**
     * Mapping between all supported by this plugin <code>ServerStoredDetails</code> and their
     * respective <code>Text</code> that are used for modifying the details.
     */
    private final Map<Class<? extends GenericDetail>, Text> detailToTextField = new HashMap<>();

    /**
     * The <code>ProtocolProviderService</code> that this panel is associated with.
     */
    ProtocolProviderService protocolProvider;

    /**
     * The operation set giving access to the server stored account details.
     */
    private OperationSetServerStoredAccountInfo accountInfoOpSet;

    private StatusListProvider statusProvider;
    private ListContainer mStatusPicker;

    /*
     * imageUrlField contains the link to the image or a command to remove avatar
     */
    private TextField imageUrlField;

    private TextField urlField;
    private TextField aboutMeArea;
    private TextField ageField;
    private TextField birthDateField;
    private Button mApplyButton;

    private Text.TextObserver editTextObserver;

    private DisplayNameDetail displayNameDetail;
    private FirstNameDetail firstNameDetail;
    private MiddleNameDetail middleNameDetail;
    private LastNameDetail lastNameDetail;
    private NicknameDetail nicknameDetail;
    private URLDetail urlDetail;
    private AddressDetail streetAddressDetail;
    private CityDetail cityDetail;
    private ProvinceDetail regionDetail;
    private PostalCodeDetail postalCodeDetail;
    private CountryDetail countryDetail;
    private PhoneNumberDetail phoneDetail;
    private WorkPhoneDetail workPhoneDetail;
    private MobilePhoneDetail mobilePhoneDetail;
    private EmailAddressDetail emailDetail;
    private WorkEmailAddressDetail workEmailDetail;
    private WorkOrganizationNameDetail organizationDetail;
    private JobTitleDetail jobTitleDetail;
    private AboutMeDetail aboutMeDetail;
    private GenderDetail genderDetail;
    private BirthDateDetail birthDateDetail;

    private Image avatarView;
    private ImageDetail avatarDetail;
    private DateFormat dateFormat;

    /**
     * Container for apply and cancel buttons; auto- hide when field text entry is active
     */
    private Component mButtonContainer;

    private Image mCalenderButton;
    private SoftKeyboard softKeyboard;
    private ProgressBar mProgressBar;
    private boolean isRegistered;

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);

        // Set the main layout
        setUIContent(ResourceTable.Layout_account_info_presence_status);
        mButtonContainer = findComponentById(ResourceTable.Id_button_Container);

        avatarView = findComponentById(ResourceTable.Id_accountAvatar);
        registerForContextMenu(avatarView);
        avatarView.setClickedListener(v -> openContextMenu(avatarView));

        // Get account ID from intent extras; and find account for given account ID
        String accountIDStr = intent.getStringParam(INTENT_ACCOUNT_ID);
        AccountID accountID = AccountUtils.getAccountIDForUID(accountIDStr);

        if (accountID == null) {
            Timber.e("No account found for: %s", accountIDStr);
            terminateAbility();
            return;
        }

        mAccount = new Account(accountID, AppGUIActivator.bundleContext, this);
        mAccount.addAccountEventListener(this);
        protocolProvider = mAccount.getProtocolProvider();

        editTextObserver = new EditTextWatcher();
        initPresenceStatus();
        initSoftKeyboard();

        accountInfoOpSet = protocolProvider.getOperationSet(OperationSetServerStoredAccountInfo.class);
        if (accountInfoOpSet != null) {
            initSummaryPanel();

            // May still be in logging if user enters preference edit immediately after account is enabled
            if (!protocolProvider.isRegistered()) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Timber.e("Account Registration State wait error: %s", protocolProvider.getRegistrationState());
                }
                Timber.d("Account Registration State: %s", protocolProvider.getRegistrationState());
            }

            isRegistered = protocolProvider.isRegistered();
            if (!isRegistered) {
                setTextEditState(false);
                aTalkApp.showToastMessage(ResourceTable.String_accountinfo_not_registered_message);
            }
            else {
                loadDetails();
            }
        }
    }

    @Override
    protected void onActive() {
        super.onActive();
        // setPrefTitle(ResourceTable.String_plugin_accountinfo_TITLE);
        ActionBarUtil.setTitle(this, getString(ResourceTable.String_accountinfo_title));

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mProgressBar != null)
            mProgressBar.release();

        if (softKeyboard != null) {
            softKeyboard.unRegisterSoftKeyboardCallback();
            softKeyboard = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (!hasChanges && !hasStatusChanges) {
            super.onBackPressed();
        }
        else {
            checkUnsavedChanges();
        }
    }

    /**
     * Create and initialize the view with actual values
     */
    private void initPresenceStatus() {
        this.accountPresence = mAccount.getPresenceOpSet();

        // Check for presence support
        if (accountPresence == null) {
            aTalkApp.showToastMessage(ResourceTable.String_presence_not_supported, mAccount.getAccountName());
            terminateAbility();
            return;
        }

        // Account properties
        ActionBarUtil.setSubtitle(this, mAccount.getAccountName());

        // Create Picker with status list
        mStatusPicker = findComponentById(ResourceTable.Id_presenceStatusSpinner);

        // Create list adapter
        List<PresenceStatus> presenceStatuses = accountPresence.getSupportedStatusSet();
        statusProvider = new StatusListProvider(this, ResourceTable.Layout_account_presence_status_row, presenceStatuses);
        mStatusPicker.setItemProvider(statusProvider);
        mStatusPicker.setItemSelectedListener((listContainer, selectedItem, position, id) -> hasStatusChanges = true);

        // Selects current status
        PresenceStatus presenceStatus = accountPresence.getPresenceStatus();
        ActionBarUtil.setStatusIcon(this, presenceStatus.getStatusIcon());

        // Sets current status message
        Text statusMessageEdit = findComponentById(ResourceTable.Id_statusMessage);
        statusMessageEdit.setText(accountPresence.getCurrentStatusMessage());

        // Watch the text for any changes
        statusMessageEdit.addTextObserver(editTextObserver);
    }

    /**
     * Initialized the main panel that contains all <code>ServerStoredDetails</code> and update
     * mapping between supported <code>ServerStoredDetails</code> and their respective
     * <code>Text</code> that are used for modifying the details.
     */
    private void initSummaryPanel() {
        imageUrlField = findComponentById(ResourceTable.Id_ai_ImageUrl);
        detailToTextField.put(ImageDetail.class, imageUrlField);

        Text displayNameField = findComponentById(ResourceTable.Id_ai_DisplayNameField);
        Component displayNameContainer = findComponentById(ResourceTable.Id_ai_DisplayName_Container);
        if (accountInfoOpSet.isDetailClassSupported(DisplayNameDetail.class)) {
            displayNameContainer.setVisibility(Component.VISIBLE);
            detailToTextField.put(DisplayNameDetail.class, displayNameField);
        }

        Text firstNameField = findComponentById(ResourceTable.Id_ai_FirstNameField);
        detailToTextField.put(FirstNameDetail.class, firstNameField);

        Text middleNameField = findComponentById(ResourceTable.Id_ai_MiddleNameField);
        detailToTextField.put(MiddleNameDetail.class, middleNameField);

        Text lastNameField = findComponentById(ResourceTable.Id_ai_LastNameField);
        detailToTextField.put(LastNameDetail.class, lastNameField);

        Text nicknameField = findComponentById(ResourceTable.Id_ai_NickNameField);
        Component nickNameContainer = findComponentById(ResourceTable.Id_ai_NickName_Container);
        if (accountInfoOpSet.isDetailClassSupported(NicknameDetail.class)) {
            nickNameContainer.setVisibility(Component.VISIBLE);
            detailToTextField.put(NicknameDetail.class, nicknameField);
        }

        urlField = findComponentById(ResourceTable.Id_ai_URLField);
        Component urlContainer = findComponentById(ResourceTable.Id_ai_URL_Container);
        if (accountInfoOpSet.isDetailClassSupported(URLDetail.class)) {
            urlContainer.setVisibility(Component.VISIBLE);
            detailToTextField.put(URLDetail.class, urlField);
        }

        Text genderField = findComponentById(ResourceTable.Id_ai_GenderField);
        Component genderContainer = findComponentById(ResourceTable.Id_ai_Gender_Container);
        if (accountInfoOpSet.isDetailClassSupported(GenderDetail.class)) {
            genderContainer.setVisibility(Component.VISIBLE);
            detailToTextField.put(GenderDetail.class, genderField);
        }

        birthDateField = findComponentById(ResourceTable.Id_ai_BirthDateField);
        detailToTextField.put(BirthDateDetail.class, birthDateField);
        birthDateField.setEnabled(false);

        ageField = findComponentById(ResourceTable.Id_ai_AgeField);
        ageField.setEnabled(false);

        Text streetAddressField = findComponentById(ResourceTable.Id_ai_StreetAddressField);
        Component streetAddressContainer = findComponentById(ResourceTable.Id_ai_StreetAddress_Container);
        if (accountInfoOpSet.isDetailClassSupported(AddressDetail.class)) {
            streetAddressContainer.setVisibility(Component.VISIBLE);
            detailToTextField.put(AddressDetail.class, streetAddressField);
        }

        Text cityField = findComponentById(ResourceTable.Id_ai_CityField);
        Component cityContainer = findComponentById(ResourceTable.Id_ai_City_Container);
        if (accountInfoOpSet.isDetailClassSupported(CityDetail.class)) {
            cityContainer.setVisibility(Component.VISIBLE);
            detailToTextField.put(CityDetail.class, cityField);
        }

        Text regionField = findComponentById(ResourceTable.Id_ai_RegionField);
        Component regionContainer = findComponentById(ResourceTable.Id_ai_Region_Container);
        if (accountInfoOpSet.isDetailClassSupported(ProvinceDetail.class)) {
            regionContainer.setVisibility(Component.VISIBLE);
            detailToTextField.put(ProvinceDetail.class, regionField);
        }

        Text postalCodeField = findComponentById(ResourceTable.Id_ai_PostalCodeField);
        Component postalCodeContainer = findComponentById(ResourceTable.Id_ai_PostalCode_Container);
        if (accountInfoOpSet.isDetailClassSupported(PostalCodeDetail.class)) {
            postalCodeContainer.setVisibility(Component.VISIBLE);
            detailToTextField.put(PostalCodeDetail.class, postalCodeField);
        }

        Text countryField = findComponentById(ResourceTable.Id_ai_CountryField);
        Component countryContainer = findComponentById(ResourceTable.Id_ai_Country_Container);
        if (accountInfoOpSet.isDetailClassSupported(CountryDetail.class)) {
            countryContainer.setVisibility(Component.VISIBLE);
            detailToTextField.put(CountryDetail.class, countryField);
        }

        Text emailField = findComponentById(ResourceTable.Id_ai_EMailField);
        detailToTextField.put(EmailAddressDetail.class, emailField);

        Text workEmailField = findComponentById(ResourceTable.Id_ai_WorkEmailField);
        Component workEmailContainer = findComponentById(ResourceTable.Id_ai_WorkEmail_Container);
        if (accountInfoOpSet.isDetailClassSupported(WorkEmailAddressDetail.class)) {
            workEmailContainer.setVisibility(Component.VISIBLE);
            detailToTextField.put(WorkEmailAddressDetail.class, workEmailField);
        }

        Text phoneField = findComponentById(ResourceTable.Id_ai_PhoneField);
        detailToTextField.put(PhoneNumberDetail.class, phoneField);

        Text workPhoneField = findComponentById(ResourceTable.Id_ai_WorkPhoneField);
        Component workPhoneContainer = findComponentById(ResourceTable.Id_ai_WorkPhone_Container);
        if (accountInfoOpSet.isDetailClassSupported(WorkPhoneDetail.class)) {
            workPhoneContainer.setVisibility(Component.VISIBLE);
            detailToTextField.put(WorkPhoneDetail.class, workPhoneField);
        }

        Text mobilePhoneField = findComponentById(ResourceTable.Id_ai_MobilePhoneField);
        Component mobileContainer = findComponentById(ResourceTable.Id_ai_MobilePhone_Container);
        if (accountInfoOpSet.isDetailClassSupported(MobilePhoneDetail.class)) {
            mobileContainer.setVisibility(Component.VISIBLE);
            detailToTextField.put(MobilePhoneDetail.class, mobilePhoneField);
        }

        Text organizationField = findComponentById(ResourceTable.Id_ai_OrganizationNameField);
        Component organizationNameContainer = findComponentById(ResourceTable.Id_ai_OrganizationName_Container);
        if (accountInfoOpSet.isDetailClassSupported(WorkOrganizationNameDetail.class)) {
            organizationNameContainer.setVisibility(Component.VISIBLE);
            detailToTextField.put(WorkOrganizationNameDetail.class, organizationField);
        }

        Text jobTitleField = findComponentById(ResourceTable.Id_ai_JobTitleField);
        Component jobDetailContainer = findComponentById(ResourceTable.Id_ai_JobTitle_Container);
        if (accountInfoOpSet.isDetailClassSupported(JobTitleDetail.class)) {
            jobDetailContainer.setVisibility(Component.VISIBLE);
            detailToTextField.put(JobTitleDetail.class, jobTitleField);
        }

        aboutMeArea = findComponentById(ResourceTable.Id_ai_AboutMeField);
        Component aboutMeContainer = findComponentById(ResourceTable.Id_ai_AboutMe_Container);
        if (accountInfoOpSet.isDetailClassSupported(AboutMeDetail.class)) {
            aboutMeContainer.setVisibility(Component.VISIBLE);
            detailToTextField.put(AboutMeDetail.class, aboutMeArea);

            // aboutMeArea.setEnabled(false); cause auto-launch of softKeyboard creating problem
            InputFilter[] filterArray = new InputFilter[1];
            filterArray[0] = new LengthFilter(ContactInfoAbility.ABOUT_ME_MAX_CHARACTERS);
            aboutMeArea.setInputFilters(filterArray);
            aboutMeArea.setBackground(new ShapeElement(new RgbColor(ResourceTable.Graphic_alpha_blue_01)));
        }

        // Setup and initialize birthday calendar basic parameters
        dateFormat = DateFormat.getDateInstance();
        Calendar today = Calendar.getInstance();
        int mYear = today.get(Calendar.YEAR);
        int mMonth = today.get(Calendar.MONTH);
        int mDay = today.get(Calendar.DAY_OF_MONTH);
        mDatePicker = findComponentById(ResourceTable.Id_datePicker);
        mDatePicker.init(mYear, mMonth, mDay, this);

        mCalenderButton = findComponentById(ResourceTable.Id_datePicker);
        mCalenderButton.setEnabled(false);
        mCalenderButton.setClickedListener(v -> {
            if (mDatePicker.getVisibility() == Component.HIDE)
                mDatePicker.setVisibility(Component.VISIBLE);
            else
                mDatePicker.setVisibility(Component.HIDE);
        });

        mApplyButton = findComponentById(ResourceTable.Id_button_Apply);
        mApplyButton.setClickedListener(v -> {
            if (hasChanges || hasStatusChanges)
                launchApplyProgressBar();
            else
                terminateAbility();
        });

        Button mCancelButton = findComponentById(ResourceTable.Id_button_Cancel);
        mCancelButton.setClickedListener(v -> checkUnsavedChanges());
    }

    /**
     * check for any unsaved changes and alert user
     */
    private void checkUnsavedChanges() {
        if (hasChanges) {
            DialogH.getInstance(this).showConfirmDialog(this,
                    ResourceTable.String_unsaved_changes_title,
                    ResourceTable.String_unsaved_changes,
                    ResourceTable.String_save, this);
        }
        else {
            terminateAbility();
        }
    }

    /**
     * Fired when user clicks the dialog's confirm button.
     *
     * @param dialog source <code>DialogH</code>.
     */
    public boolean onConfirmClicked(DialogH dialog) {
        return mApplyButton.callOnClick();
    }

    /**
     * Fired when user dismisses the dialog.
     *
     * @param dialog source <code>DialogH</code>
     */
    public void onDialogCancelled(DialogH dialog) {
        dialog.destroy();
    }

    @Override
    public void onValueChanged(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
        Calendar mDate = Calendar.getInstance();

        int age = mDate.get(Calendar.YEAR) - year;
        if (mDate.get(Calendar.MONTH) < monthOfYear)
            age--;
        if ((mDate.get(Calendar.MONTH) == monthOfYear)
                && (mDate.get(Calendar.DAY_OF_MONTH) < dayOfMonth))
            age--;

        String ageDetail = Integer.toString(age);
        ageField.setText(ageDetail);

        mDate.set(year, monthOfYear, dayOfMonth);
        birthDateField.setText(dateFormat.format(mDate.getTime()));

        // internal program call is with dialog == null
        hasChanges = (datePicker != null);
    }

    private void setTextEditState(boolean editState) {
        boolean isEditable;
        for (Class<? extends GenericDetail> editable : detailToTextField.keySet()) {
            Text field = detailToTextField.get(editable);

            isEditable = editState && accountInfoOpSet.isDetailClassEditable(editable);
            if (editable.equals(BirthDateDetail.class))
                mCalenderButton.setEnabled(isEditable);
            else if (editable.equals(ImageDetail.class))
                avatarView.setEnabled(isEditable);
            else if (field != null) {
                field.setEnabled(isEditable);
                if (isEditable)
                    field.addTextObserver(editTextObserver);
            }
        }
    }

    /**
     * Loads all <code>ServerStoredDetails</code> which are currently supported by
     * this plugin. Note that some <code>OperationSetServerStoredAccountInfo</code>
     * implementations may support details that are not supported by this plugin.
     * In this case they will not be loaded.
     */
    private void loadDetails() {
        if (accountInfoOpSet != null) {
            new DetailsLoadWorker().execute();
        }
    }

    /**
     * Loads details in separate thread.
     */
    private class DetailsLoadWorker {
        public void execute() {
            /*
             * Called on the event dispatching thread (not on the worker thread)
             * after the {@code construct} method has returned.
             */
            Executors.newSingleThreadExecutor().execute(() -> {
                Iterator<GenericDetail> allDetails = accountInfoOpSet.getAllAvailableDetails();

                BaseAbility.runOnUiThread(() -> {
                    if (allDetails != null) {
                        while (allDetails.hasNext()) {
                            GenericDetail detail = allDetails.next();
                            loadDetail(detail);
                        }

                        // Setup textFields' editable state and addTextChangedListener if enabled
                        boolean isEditable;
                        for (Class<? extends GenericDetail> editable : detailToTextField.keySet()) {
                            Text field = detailToTextField.get(editable);
                            isEditable = accountInfoOpSet.isDetailClassEditable(editable);

                            if (editable.equals(BirthDateDetail.class))
                                mCalenderButton.setEnabled(isEditable);
                            else if (editable.equals(ImageDetail.class))
                                avatarView.setEnabled(isEditable);
                            else {
                                if (field != null) {
                                    field.setEnabled(isEditable);
                                    if (isEditable)
                                        field.addTextObserver(editTextObserver);
                                }
                            }
                        }
                    }
                    // get user avatar via XEP-0084
                    getUserAvatarData();

                });
            });
        }
    }

    /**
     * Loads a single <code>GenericDetail</code> obtained from the
     * <code>OperationSetServerStoredAccountInfo</code> into this plugin.
     * <p>
     * If VcardTemp contains <photo/>, it will be converted to XEP-0084 avatarData &
     * avatarMetadata, and remove it from VCardTemp.
     *
     * @param detail the loaded detail for extraction.
     */
    private void loadDetail(GenericDetail detail) {
        if (detail.getClass().equals(AboutMeDetail.class)) {
            aboutMeDetail = (AboutMeDetail) detail;
            aboutMeArea.setText((String) detail.getDetailValue());
            return;
        }

        if (detail instanceof BirthDateDetail) {
            birthDateDetail = (BirthDateDetail) detail;
            Object objBirthDate = birthDateDetail.getDetailValue();

            // default to today if birthDate is null
            if (objBirthDate instanceof Calendar) {
                Calendar birthDate = (Calendar) objBirthDate;

                int bYear = birthDate.get(Calendar.YEAR);
                int bMonth = birthDate.get(Calendar.MONTH);
                int bDay = birthDate.get(Calendar.DAY_OF_MONTH);
                // Preset calendarDatePicker date
                mDatePicker.updateDate(bYear, bMonth, bDay);

                // Update BirthDate and Age
                onValueChanged(null, bYear, bMonth, bDay);
            }
            else if (objBirthDate != null) {
                birthDateField.setText((String) objBirthDate);
            }
            return;
        }

        Text field = detailToTextField.get(detail.getClass());
        if (field != null) {
            if (detail instanceof ImageDetail) {
                avatarDetail = (ImageDetail) detail;
                byte[] avatarBytes = avatarDetail.getBytes();
                PixelMap avatarImage = AppImageUtil.pixelMapFromBytes(avatarBytes);
                avatarView.setPixelMap(avatarImage);
            }
            else if (detail instanceof URLDetail) {
                urlDetail = (URLDetail) detail;
                urlField.setText(urlDetail.getURL().toString());
            }
            else {
                Object obj = detail.getDetailValue();
                if (obj instanceof String)
                    field.setText((String) obj);
                else if (obj != null)
                    field.setText(obj.toString());

                if (detail.getClass().equals(DisplayNameDetail.class))
                    displayNameDetail = (DisplayNameDetail) detail;
                else if (detail.getClass().equals(FirstNameDetail.class))
                    firstNameDetail = (FirstNameDetail) detail;
                else if (detail.getClass().equals(MiddleNameDetail.class))
                    middleNameDetail = (MiddleNameDetail) detail;
                else if (detail.getClass().equals(LastNameDetail.class))
                    lastNameDetail = (LastNameDetail) detail;
                else if (detail.getClass().equals(NicknameDetail.class))
                    nicknameDetail = (NicknameDetail) detail;
                else if (detail.getClass().equals(GenderDetail.class))
                    genderDetail = (GenderDetail) detail;
                else if (detail.getClass().equals(AddressDetail.class))
                    streetAddressDetail = (AddressDetail) detail;
                else if (detail.getClass().equals(CityDetail.class))
                    cityDetail = (CityDetail) detail;
                else if (detail.getClass().equals(ProvinceDetail.class))
                    regionDetail = (ProvinceDetail) detail;
                else if (detail.getClass().equals(PostalCodeDetail.class))
                    postalCodeDetail = (PostalCodeDetail) detail;
                else if (detail.getClass().equals(CountryDetail.class))
                    countryDetail = (CountryDetail) detail;
                else if (detail.getClass().equals(PhoneNumberDetail.class))
                    phoneDetail = (PhoneNumberDetail) detail;
                else if (detail.getClass().equals(WorkPhoneDetail.class))
                    workPhoneDetail = (WorkPhoneDetail) detail;
                else if (detail.getClass().equals(MobilePhoneDetail.class))
                    mobilePhoneDetail = (MobilePhoneDetail) detail;
                else if (detail.getClass().equals(EmailAddressDetail.class))
                    emailDetail = (EmailAddressDetail) detail;
                else if (detail.getClass().equals(WorkEmailAddressDetail.class))
                    workEmailDetail = (WorkEmailAddressDetail) detail;
                else if (detail.getClass().equals(
                        WorkOrganizationNameDetail.class))
                    organizationDetail = (WorkOrganizationNameDetail) detail;
                else if (detail.getClass().equals(JobTitleDetail.class))
                    jobTitleDetail = (JobTitleDetail) detail;
                else if (detail.getClass().equals(AboutMeDetail.class))
                    aboutMeDetail = (AboutMeDetail) detail;
            }
        }
    }

    /**
     * Retrieve avatar via XEP-0084 and override vCard <photo/> content if avatarImage not null
     */
    private void getUserAvatarData() {
        byte[] avatarImage = AvatarManager.getAvatarImageByJid(mAccount.getJid().asBareJid());
        if (avatarImage != null && avatarImage.length > 0) {
            PixelMap bitmap = AppImageUtil.pixelMapFromBytes(avatarImage);
            avatarView.setPixelMap(bitmap);
        }
        else {
            avatarView.setPixelMap(ResourceTable.Media_person_photo);
        }
    }

    /**
     * Attempts to upload all <code>ServerStoredDetails</code> on the server using
     * <code>OperationSetServerStoredAccountInfo</code>
     */
    private void SubmitChangesAction() {
        if (!isRegistered || !hasChanges)
            return;

        if (accountInfoOpSet.isDetailClassSupported(ImageDetail.class)) {
            String sCommand = ComponentUtil.toString(imageUrlField);
            if (sCommand != null) {
                ImageDetail newDetail;

                /*
                 * command to remove avatar photo from vCardTemp. XEP-0084 support will always
                 * init imageUrlField = AVATAR_ICON_REMOVE
                 */
                if (AVATAR_ICON_REMOVE.equals(sCommand)) {
                    newDetail = new ImageDetail("avatar", new byte[0]);
                    changeDetail(avatarDetail, newDetail);
                }
                else {
                    Uri imageUri = Uri.parse(sCommand);
                    PixelMap avatarImage = AppImageUtil.scaledPixelMapFromContentUri(
                            imageUri, AVATAR_PREFERRED_SIZE, AVATAR_PREFERRED_SIZE);

                    // Convert to bytes if not null
                    if (avatarImage != null) {
                        final byte[] rawImage = AppImageUtil.convertPixelMapToBytes(avatarImage);

                        newDetail = new ImageDetail("avatar", rawImage);
                        changeDetail(avatarDetail, newDetail);
                    }
                    else
                        showAvatarChangeError();
                }
            }
        }

        if (accountInfoOpSet.isDetailClassSupported(DisplayNameDetail.class)) {
            String text = getText(DisplayNameDetail.class);

            DisplayNameDetail newDetail = null;
            if (text != null)
                newDetail = new DisplayNameDetail(text);

            if (displayNameDetail != null || newDetail != null)
                changeDetail(displayNameDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(FirstNameDetail.class)) {
            String text = getText(FirstNameDetail.class);

            FirstNameDetail newDetail = null;
            if (text != null)
                newDetail = new FirstNameDetail(text);

            if (firstNameDetail != null || newDetail != null)
                changeDetail(firstNameDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(MiddleNameDetail.class)) {
            String text = getText(MiddleNameDetail.class);

            MiddleNameDetail newDetail = null;
            if (text != null)
                newDetail = new MiddleNameDetail(text);

            if (middleNameDetail != null || newDetail != null)
                changeDetail(middleNameDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(LastNameDetail.class)) {
            String text = getText(LastNameDetail.class);
            LastNameDetail newDetail = null;

            if (text != null)
                newDetail = new LastNameDetail(text);

            if (lastNameDetail != null || newDetail != null)
                changeDetail(lastNameDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(NicknameDetail.class)) {
            String text = getText(NicknameDetail.class);

            NicknameDetail newDetail = null;
            if (text != null)
                newDetail = new NicknameDetail(text);

            if (nicknameDetail != null || newDetail != null)
                changeDetail(nicknameDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(URLDetail.class)) {
            String text = getText(URLDetail.class);

            URL url;
            URLDetail newDetail = null;

            if (text != null) {
                try {
                    url = new URL(text);
                    newDetail = new URLDetail("URL", url);
                } catch (MalformedURLException e1) {
                    Timber.d("URL field has malformed URL; save as text instead.");
                    newDetail = new URLDetail("URL", text);
                }
            }
            if (urlDetail != null || newDetail != null)
                changeDetail(urlDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(GenderDetail.class)) {
            String text = getText(GenderDetail.class);
            GenderDetail newDetail = null;

            if (text != null)
                newDetail = new GenderDetail(text);

            if (genderDetail != null || newDetail != null)
                changeDetail(genderDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(BirthDateDetail.class)) {
            String text = ComponentUtil.toString(birthDateField);
            BirthDateDetail newDetail = null;

            if (text != null) {
                Calendar birthDate = Calendar.getInstance();
                try {
                    Date mDate = dateFormat.parse(text);
                    birthDate.setTime(mDate);
                    newDetail = new BirthDateDetail(birthDate);
                } catch (ParseException e) {
                    // Save as String value
                    newDetail = new BirthDateDetail(text);
                }
            }
            if (birthDateDetail != null || newDetail != null)
                changeDetail(birthDateDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(AddressDetail.class)) {
            String text = getText(AddressDetail.class);

            AddressDetail newDetail = null;
            if (text != null)
                newDetail = new AddressDetail(text);

            if (streetAddressDetail != null || newDetail != null)
                changeDetail(streetAddressDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(CityDetail.class)) {
            String text = getText(CityDetail.class);

            CityDetail newDetail = null;
            if (text != null)
                newDetail = new CityDetail(text);

            if (cityDetail != null || newDetail != null)
                changeDetail(cityDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(ProvinceDetail.class)) {
            String text = getText(ProvinceDetail.class);

            ProvinceDetail newDetail = null;
            if (text != null)
                newDetail = new ProvinceDetail(text);

            if (regionDetail != null || newDetail != null)
                changeDetail(regionDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(PostalCodeDetail.class)) {
            String text = getText(PostalCodeDetail.class);

            PostalCodeDetail newDetail = null;
            if (text != null)
                newDetail = new PostalCodeDetail(text);

            if (postalCodeDetail != null || newDetail != null)
                changeDetail(postalCodeDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(CountryDetail.class)) {
            String text = getText(CountryDetail.class);

            CountryDetail newDetail = null;
            if (text != null)
                newDetail = new CountryDetail(text);

            if (countryDetail != null || newDetail != null)
                changeDetail(countryDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(EmailAddressDetail.class)) {
            String text = getText(EmailAddressDetail.class);

            EmailAddressDetail newDetail = null;
            if (text != null)
                newDetail = new EmailAddressDetail(text);

            if (emailDetail != null || newDetail != null)
                changeDetail(emailDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(WorkEmailAddressDetail.class)) {
            String text = getText(WorkEmailAddressDetail.class);

            WorkEmailAddressDetail newDetail = null;
            if (text != null)
                newDetail = new WorkEmailAddressDetail(text);

            if (workEmailDetail != null || newDetail != null)
                changeDetail(workEmailDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(PhoneNumberDetail.class)) {
            String text = getText(PhoneNumberDetail.class);

            PhoneNumberDetail newDetail = null;
            if (text != null)
                newDetail = new PhoneNumberDetail(text);

            if (phoneDetail != null || newDetail != null)
                changeDetail(phoneDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(WorkPhoneDetail.class)) {
            String text = getText(WorkPhoneDetail.class);

            WorkPhoneDetail newDetail = null;
            if (text != null)
                newDetail = new WorkPhoneDetail(text);

            if (workPhoneDetail != null || newDetail != null)
                changeDetail(workPhoneDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(MobilePhoneDetail.class)) {
            String text = getText(MobilePhoneDetail.class);

            MobilePhoneDetail newDetail = null;
            if (text != null)
                newDetail = new MobilePhoneDetail(text);

            if (mobilePhoneDetail != null || newDetail != null)
                changeDetail(mobilePhoneDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(WorkOrganizationNameDetail.class)) {
            String text = getText(WorkOrganizationNameDetail.class);

            WorkOrganizationNameDetail newDetail = null;
            if (text != null)
                newDetail = new WorkOrganizationNameDetail(text);

            if (organizationDetail != null || newDetail != null)
                changeDetail(organizationDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(JobTitleDetail.class)) {
            String text = getText(JobTitleDetail.class);

            JobTitleDetail newDetail = null;
            if (text != null)
                newDetail = new JobTitleDetail(text);

            if (jobTitleDetail != null || newDetail != null)
                changeDetail(jobTitleDetail, newDetail);
        }

        if (accountInfoOpSet.isDetailClassSupported(AboutMeDetail.class)) {
            String text = ComponentUtil.toString(aboutMeArea);

            AboutMeDetail newDetail = null;
            if (text != null)
                newDetail = new AboutMeDetail(text);

            if (aboutMeDetail != null || newDetail != null)
                changeDetail(aboutMeDetail, newDetail);
        }

        try {
            //mainScrollPane.getVerticalScrollBar().setValue(0);
            accountInfoOpSet.save();
        } catch (OperationFailedException e1) {
            showAvatarChangeError();
        }
    }

    /**
     * get the class's Text string value or null (length == 0)
     *
     * @param className Class Name
     *
     * @return String or null if string length == 0
     */
    private String getText(Class<? extends GenericDetail> className) {
        Text Text = detailToTextField.get(className);
        return ComponentUtil.toString(Text);
    }

    /**
     * A helper method to decide whether to add new
     * <code>ServerStoredDetails</code> or to replace an old one.
     *
     * @param oldDetail the detail to be replaced.
     * @param newDetail the replacement.
     */
    private void changeDetail(GenericDetail oldDetail, GenericDetail newDetail) {
        try {
            if (newDetail == null) {
                accountInfoOpSet.removeDetail(oldDetail);
            }
            else if (oldDetail == null) {
                accountInfoOpSet.addDetail(newDetail);
            }
            else {
                accountInfoOpSet.replaceDetail(oldDetail, newDetail);
            }
        } catch (ArrayIndexOutOfBoundsException | OperationFailedException e1) {
            Timber.d("Failed to update account details.%s %s", mAccount.getAccountName(), e1.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().parse(ResourceTable.Layout_menu_presence_status, menu);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getId();
        if (id == ResourceTable.Id_remove) {
            AccountDeleteDialog.create(this, mAccount, accID -> {
                // Prevent from submitting status
                hasStatusChanges = false;
                hasChanges = false;
                terminateAbility();
            });
            return true;
        }
        else if (id == ResourceTable.Id_account_settings) {
            Intent preferences = AccountPreferenceAbility.getIntent(this, mAccount.getAccountID());
            startAbility(preferences);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onCreateContextMenu(ContextMenu menu, Component v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == ResourceTable.Id_accountAvatar) {
            getMenuInflater().parse(ResourceTable.Layout_menu_avatar, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getId()) {
            case ResourceTable.Id_avatar_ChoosePicture:
                onAvatarClicked(avatarView);
                return true;

            case ResourceTable.Id_avatar_RemovePicture:
                imageUrlField.setText(AVATAR_ICON_REMOVE);
                avatarView.setPixelMap(ResourceTable.Media_person_photo);
                hasChanges = true;
                return true;

            case ResourceTable.Id_avatar_Cancel:
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Method mapped to the avatar image clicked event. It starts the select image {@link Intent}
     *
     * @param avatarView the {@link Component.} that has been clicked
     */
    public void onAvatarClicked(Component avatarView) {
        if (mAccount.getAvatarOpSet() == null) {
            Timber.w("Avatar operation set is not supported by %s", mAccount.getAccountName());
            showAvatarChangeError();
            return;
        }
        getAvatarContent().launch("image/*");
    }

    /**
     * A contract specifying that an activity can be called with an input of type I
     * and produce an output of type O
     *
     * @return an instant of ActivityResultLauncher<String>
     *
     * @see ActivityResultCaller
     */
    private ActivityResultLauncher<String> getAvatarContent() {
        return registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) {
                Timber.e("No image data selected for avatar!");
                showAvatarChangeError();
            }
            else {
                String fileName = "cropImage";
                File tmpFile = new File(this.getCacheDir(), fileName);
                Uri destinationUri = Uri.getUriFromFile(tmpFile);

                UCrop.of(uri, destinationUri)
                        .withAspectRatio(1, 1)
                        .withMaxResultSize(CROP_MAX_SIZE, CROP_MAX_SIZE)
                        .start(this);
            }
        });
    }

    /**
     * Method handles callbacks from external {@link Intent} that retrieve avatar image
     *
     * @param requestCode the request code
     * @param resultCode the result code
     * @param data the source {@link Intent} that returns the result
     */
    protected void onAbilityResult(int requestCode, int resultCode, Intent data) {
        super.onAbilityResult(requestCode, resultCode, data);
        if (BaseAbility.RESULT_OK != resultCode)
            return;

        switch (requestCode) {
            case UCrop.REQUEST_CROP:
                final Uri resultUri = UCrop.getOutput(data);
                if (resultUri == null)
                    break;
                try {
                    PixelMap avatarImage = AppImageUtil.scaledPixelMapFromContentUri(
                            resultUri, AVATAR_PREFERRED_SIZE, AVATAR_PREFERRED_SIZE);
                    if (avatarImage == null) {
                        Timber.e("Failed to obtain bitmap from: %s", data);
                        showAvatarChangeError();
                    }
                    else {
                        avatarView.setPixelMap(avatarImage);
                        imageUrlField.setText(resultUri.toString());
                        hasChanges = true;
                    }
                } catch (IOException e) {
                    Timber.e(e, "%s", e.getMessage());
                    showAvatarChangeError();
                }
                break;

            case UCrop.RESULT_ERROR:
                final Throwable cropError = UCrop.getError(data);
                String errMsg = "Image crop error: ";
                if (cropError != null)
                    errMsg += cropError.getMessage();
                Timber.e("%s", errMsg);
                showAvatarChangeError();
                break;
        }
    }

    private void showAvatarChangeError() {
        DialogH.getInstance(this).showDialog(this,
                ResourceTable.String_error, ResourceTable.String_avatar_set_error, mAccount.getAccountName());
    }

    /**
     * Method starts a new Thread and publishes the status
     *
     * @param status {@link PresenceStatus} to be set
     * @param text the status message
     */
    private void publishStatus(final PresenceStatus status, final String text) {
        new Thread(() -> {
            try {
                // Try to publish selected status
                Timber.d("Publishing status %s msg: %s", status, text);
                GlobalStatusService globalStatus
                        = ServiceUtils.getService(AppGUIActivator.bundleContext, GlobalStatusService.class);

                ProtocolProviderService pps = mAccount.getProtocolProvider();
                // cmeng: set state to false to force it to execute offline->online
                if (globalStatus != null)
                    globalStatus.publishStatus(pps, status, false);
                if (pps.isRegistered())
                    accountPresence.publishPresenceStatus(status, text);
            } catch (Exception e) {
                Timber.e(e);
            }
        }).start();
    }

    /**
     * Fired when the {@link #mAccount} has changed and the UI need to be updated
     *
     * @param eventObject the instance that has been changed
     * cmeng: may not be required anymore with new implementation
     */
    @Override
    public void onChangeEvent(final AccountEvent eventObject) {
        if (eventObject.getEventType() != AccountEvent.AVATAR_CHANGE) {
            return;
        }

        BaseAbility.runOnUiThread(() -> {
            Account account = eventObject.getSource();
            avatarView.setPixelMap(account.getAvatarIcon());
        });
    }

    /**
     * Checks if there are any uncommitted changes and applies them eventually
     */
    private void commitStatusChanges() {
        if (hasStatusChanges) {

            PresenceStatus selectedStatus = statusProvider.getItem(mStatusPicker.getSelectedItemIndex());
            String statusMessageText = ComponentUtil.toString(findComponentById(ResourceTable.Id_statusMessage));

            if ((selectedStatus.getStatus() == PresenceStatus.OFFLINE) && (hasChanges)) {
                // abort all account info changes if user goes offline
                hasChanges = false;

                if (mProgressBar != null) {
                    mProgressBar.setProgressHintText(getString(ResourceTable.String_accountinfo_discard_change));
                }
            }
            // Publish status in new thread
            publishStatus(selectedStatus, statusMessageText);
        }
    }

    /**
     * Progressing dialog while applying changes to account info/status
     * Auto cancel the dialog at end of applying cycle
     */
    public void launchApplyProgressBar() {
        mProgressBar = new ProgressBar(getContext());
        mProgressBar.setIndeterminate(true);
        mProgressBar.setProgressHintText(getString(ResourceTable.String_please_wait)
                + "\n" + getString(ResourceTable.String_apply_changes));

        new Thread(() -> {
            try {
                commitStatusChanges();
                SubmitChangesAction();
                // too fast to be viewed user at times - so pause for 2.0 seconds
                Thread.sleep(2000);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            mProgressBar.release();
            terminateAbility();
        }).start();
    }

    /*
     * cmeng 20191118 - manipulate softKeyboard may cause problem in >= android-9 (API-28)
     * all view Dimensions are incorrectly init when softKeyboard is auto launched.
     * aboutMeArea.setEnabled(false); cause softKeyboard to auto-launch
     *
     * SoftKeyboard event handler to show/hide view buttons to give more space for fields' text entry.
     * # init to handle when softKeyboard is hided/shown
     */
    private void initSoftKeyboard() {
        DirectionalLayout mainLayout = findComponentById(ResourceTable.Id_accountInfo_layout);
        SoftKeyBoardController skbController = new SoftKeyBoardController(1000, null);

        /*  Instantiate and pass a callback */
        softKeyboard = new SoftKeyboard(mainLayout, skbController);
        softKeyboard.setSoftKeyboardCallback(this);
    }

    // Events to show or hide buttons for bigger view space for text entry
    @Override
    public void onSoftKeyboardHide() {
        BaseAbility.runOnUiThread(() -> {
            mButtonContainer.setVisibility(Component.VISIBLE);
        });
    }

    @Override
    public void onSoftKeyboardShow() {
        BaseAbility.runOnUiThread(() -> {
            mButtonContainer.setVisibility(Component.HIDE);
        });
    }

    private class EditTextWatcher implements Text.TextObserver {
        @Override
        public void onTextUpdated(String text, int start, int before, int count) {
            hasChanges = true;
        }
    }
}