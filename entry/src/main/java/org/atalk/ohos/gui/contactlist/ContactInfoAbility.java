package org.atalk.ohos.gui.contactlist;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.Button;
import ohos.agp.components.Image;
import ohos.agp.components.Text;
import ohos.agp.components.element.ShapeElement;
import ohos.media.image.PixelMap;

import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredContactInfo;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.AboutMeDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.AddressDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.BinaryDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.BirthDateDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.CityDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.CountryDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.DisplayNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.EmailAddressDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.FirstNameDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenderDetail;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
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

import org.apache.commons.lang3.StringUtils;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.actionbar.ActionBarUtil;
import org.atalk.ohos.util.AppImageUtil;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import timber.log.Timber;

/**
 * Ability allows user to view presence status, status message, the avatar and the full
 * vCard-temp information for the {@link #mContact}.
 * <p>
 * The main panel that allows users to view their account information. Different instances of
 * this class are created for every registered <code>ProtocolProviderService</code>.
 * Currently, supported account details are first/middle/last names, nickname,
 * street/city/region/country address, postal code, birth date, gender, organization name, job
 * title, about me, home/work email, home/work phone.
 * <p>
 * The {@link #mContact} is retrieved from the {@link Intent} by direct access to
 *
 * @author Eng Chong Meng
 * @link ContactListSlice#getClickedContact()
 */
public class ContactInfoAbility extends BaseAbility
        implements OperationSetServerStoredContactInfo.DetailsResponseListener {
    /**
     * Mapping between all supported by this plugin <code>ServerStoredDetails</code> and their
     * respective <code>Text</code> that are used for modifying the details.
     */
    private final Map<Class<? extends GenericDetail>, Text> detailToTextField = new HashMap<>();

    private Text urlField;
    private Text ageField;
    private Text birthDateField;

    /**
     * Intent's extra's key for account ID property of this activity
     */
    public static final String INTENT_CONTACT_ID = "contact_id";

    public static final int ABOUT_ME_MAX_CHARACTERS = 200;

    /**
     * The currently selected contact we are displaying information about.
     */
    private Contact mContact;

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_contact_info);

        // Get contact ID from intent extras - but cannot link to mContact
        String contactId = intent.getStringParam(INTENT_CONTACT_ID);

        AbilitySlice clf = aTalk.getFragment(aTalk.CL_FRAGMENT);
        if (clf instanceof ContactListSlice) {
            MetaContact metaContact = ((ContactListSlice) clf).getClickedContact();
            if (metaContact == null) {
                Timber.e("Requested contact info not found: %s", contactId);
                terminateAbility();
            }
            else {
                mContact = metaContact.getDefaultContact();
                ProtocolProviderService pps = mContact.getProtocolProvider();
                /*
                 * The operation set giving access to the server stored contact details.
                 */
                OperationSetServerStoredContactInfo contactInfoOpSet
                        = pps.getOperationSet(OperationSetServerStoredContactInfo.class);
                if (contactInfoOpSet != null) {
                    initPresenceStatus();
                    initSummaryPanel();

                    // Always retrieve new contact vCard-temp info from server. Otherwise contact
                    // info changes after account login will not be reflected in the display info.
                    contactInfoOpSet.requestAllDetailsForContact(mContact, this);
                }
            }
        }
    }

    /**
     * Create and initialize the view with actual values
     */
    private void initPresenceStatus() {
        String title = mContact.getDisplayName();
        ActionBarUtil.setTitle(this, title);

        // Setup the contact presence status
        PresenceStatus presenceStatus = mContact.getPresenceStatus();
        if (presenceStatus != null) {
            ActionBarUtil.setStatusIcon(this, presenceStatus.getStatusIcon());

            Text statusNameView = findComponentById(ResourceTable.Id_presenceStatusName);
            Image statusIconView = findComponentById(ResourceTable.Id_presenceStatusIcon);

            // Set status icon
            PixelMap presenceIcon = AppImageUtil.pixelMapFromBytes(presenceStatus.getStatusIcon());
            statusIconView.setPixelMap(presenceIcon);

            // Set status name
            String statusName = presenceStatus.getStatusName();
            statusNameView.setText(statusName);

            // Add users status message if it exists
            Text statusMessage = findComponentById(ResourceTable.Id_statusMessage);
            ProtocolProviderService pps = mContact.getProtocolProvider();
            OperationSetPresence contactPresence = pps.getOperationSet(OperationSetPresence.class);
            String statusMsg = contactPresence.getCurrentStatusMessage();
            // String statusMsg = mContact.getStatusMessage();
            if (StringUtils.isNotBlank(statusMsg)) {
                statusMessage.setText(statusMsg);
            }
        }
    }

    /**
     * Creates a panel that displays the following contact details:
     * <p>
     * Currently, supported contact details are first/middle/last names, nickname,
     * street/city/region/country address, postal code, birth date, gender,
     * organization name, job title, about me, home/work email, home/work phone.
     */
    private void initSummaryPanel() {
        // Display name details.
        Text displayNameField = findComponentById(ResourceTable.Id_ci_DisplayNameField);
        detailToTextField.put(DisplayNameDetail.class, displayNameField);

        // First name details.
        Text firstNameField = findComponentById(ResourceTable.Id_ci_FirstNameField);
        detailToTextField.put(FirstNameDetail.class, firstNameField);

        // Middle name details.
        Text middleNameField = findComponentById(ResourceTable.Id_ci_MiddleNameField);
        detailToTextField.put(MiddleNameDetail.class, middleNameField);

        // Last name details.
        Text lastNameField = findComponentById(ResourceTable.Id_ci_LastNameField);
        detailToTextField.put(LastNameDetail.class, lastNameField);

        Text nicknameField = findComponentById(ResourceTable.Id_ci_NickNameField);
        detailToTextField.put(NicknameDetail.class, nicknameField);

        urlField = findComponentById(ResourceTable.Id_ci_URLField);
        detailToTextField.put(URLDetail.class, urlField);

        // Gender details.
        Text genderField = findComponentById(ResourceTable.Id_ci_GenderField);
        detailToTextField.put(GenderDetail.class, genderField);

        // Birthday and Age details.
        ageField = findComponentById(ResourceTable.Id_ci_AgeField);
        birthDateField = findComponentById(ResourceTable.Id_ci_BirthDateField);
        detailToTextField.put(BirthDateDetail.class, birthDateField);

        Text streetAddressField = findComponentById(ResourceTable.Id_ci_StreetAddressField);
        detailToTextField.put(AddressDetail.class, streetAddressField);

        Text cityField = findComponentById(ResourceTable.Id_ci_CityField);
        detailToTextField.put(CityDetail.class, cityField);

        Text regionField = findComponentById(ResourceTable.Id_ci_RegionField);
        detailToTextField.put(ProvinceDetail.class, regionField);

        Text postalCodeField = findComponentById(ResourceTable.Id_ci_PostalCodeField);
        detailToTextField.put(PostalCodeDetail.class, postalCodeField);

        Text countryField = findComponentById(ResourceTable.Id_ci_CountryField);
        detailToTextField.put(CountryDetail.class, countryField);

        // Email details.
        Text emailField = findComponentById(ResourceTable.Id_ci_EMailField);
        detailToTextField.put(EmailAddressDetail.class, emailField);

        Text workEmailField = findComponentById(ResourceTable.Id_ci_WorkEmailField);
        detailToTextField.put(WorkEmailAddressDetail.class, workEmailField);

        // Phone number details.
        Text phoneField = findComponentById(ResourceTable.Id_ci_PhoneField);
        detailToTextField.put(PhoneNumberDetail.class, phoneField);

        Text workPhoneField = findComponentById(ResourceTable.Id_ci_WorkPhoneField);
        detailToTextField.put(WorkPhoneDetail.class, workPhoneField);

        Text mobilePhoneField = findComponentById(ResourceTable.Id_ci_MobilePhoneField);
        detailToTextField.put(MobilePhoneDetail.class, mobilePhoneField);

        Text organizationField = findComponentById(ResourceTable.Id_ci_OrganizationNameField);
        detailToTextField.put(WorkOrganizationNameDetail.class, organizationField);
        Text jobTitleField = findComponentById(ResourceTable.Id_ci_JobTitleField);
        detailToTextField.put(JobTitleDetail.class, jobTitleField);

        Text aboutMeArea = findComponentById(ResourceTable.Id_ci_AboutMeField);
        //InputFilter[] filterArray = new InputFilter[1];
        //filterArray[0] = new LengthFilter(ABOUT_ME_MAX_CHARACTERS);
        //aboutMeArea.setInputFilters(filterArray);
        aboutMeArea.setBackground(new ShapeElement(getContext(), ResourceTable.Graphic_alpha_blue_01));
        detailToTextField.put(AboutMeDetail.class, aboutMeArea);

        Button mOkButton = findComponentById(ResourceTable.Id_button_OK);
        mOkButton.setClickedListener(v -> terminateAbility());
    }

    @Override
    public void detailsRetrieved(final Iterator<GenericDetail> allDetails) {
        BaseAbility.runOnUiThread(() -> {
            if (allDetails != null) {
                while (allDetails.hasNext()) {
                    GenericDetail detail = allDetails.next();
                    loadDetail(detail);
                }
            }
        });
    }

    /**
     * Loads a single <code>GenericDetail</code> obtained from the
     * <code>OperationSetServerStoredAccountInfo</code> into this plugin.
     *
     * @param detail to be loaded.
     */
    private void loadDetail(GenericDetail detail) {
        if (detail instanceof BinaryDetail) {
            Image avatarView = findComponentById(ResourceTable.Id_contactAvatar);

            // If the user has a contact image, let's use it. If not, leave the default as it
            byte[] avatarImage = (byte[]) detail.getDetailValue();
            PixelMap bitmap = AppImageUtil.pixelMapFromBytes(avatarImage);
            avatarView.setPixelMap(bitmap);

        }
        else if (detail instanceof URLDetail) {
            // If the contact's protocol supports web info, give them a link to get it
            URLDetail urlDetail = (URLDetail) detail;
            final String urlString = urlDetail.getURL().toString();
            // urlField.setText(urlString);

            String html = "Click to see web info for: <a href='"
                    + urlString + "'>"
                    + urlString
                    + "</a>";
            urlField.setText(Html.fromHtml(html));
            urlField.setClickedListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
                startAbility(browserIntent);
            });
        }
        else if (detail instanceof BirthDateDetail) {
            // birthDateDetail = (BirthDateDetail) detail;
            Calendar calendarDetail = (Calendar) detail.getDetailValue();

            Date birthDate = calendarDetail.getTime();
            DateFormat dateFormat = DateFormat.getDateInstance();
            String birthDateDetail = dateFormat.format(birthDate);
            birthDateField.setText(birthDateDetail);

            // Calculate age based on given birthDate
            Calendar mDate = Calendar.getInstance();
            int age = mDate.get(Calendar.YEAR) - calendarDetail.get(Calendar.YEAR);

            if (mDate.get(Calendar.MONTH) < calendarDetail.get(Calendar.MONTH))
                age--;
            if ((mDate.get(Calendar.MONTH) == calendarDetail.get(Calendar.MONTH))
                    && (mDate.get(Calendar.DAY_OF_MONTH)
                    < calendarDetail.get(Calendar.DAY_OF_MONTH)))
                age--;

            String ageDetail = Integer.toString(age);
            ageField.setText(ageDetail);
        }
        else {
            Text field = detailToTextField.get(detail.getClass());
            if (field != null) {
                Object obj = detail.getDetailValue();
                if (obj instanceof String)
                    field.setText((String) obj);
                else if (obj != null)
                    field.setText(obj.toString());
            }
        }
    }
}
