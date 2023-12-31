/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.phonenumbers;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import net.java.sip.communicator.service.protocol.PhoneNumberI18nService;
import net.java.sip.communicator.service.protocol.ProtocolProviderActivator;

import org.atalk.service.configuration.ConfigurationService;

import java.util.regex.Pattern;

/**
 * Implements <code>PhoneNumberI18nService</code> which aids the parsing, formatting and validating of international phone
 * numbers.
 *
 * @author Lyubomir Marinov
 * @author Vincent Lucas
 * @author Damian Minkov
 */
public class PhoneNumberI18nServiceImpl implements PhoneNumberI18nService {
	/**
	 * The configuration service.
	 */
	private static ConfigurationService configService = ProtocolProviderActivator.getConfigurationService();

	/**
	 * Characters which have to be removed from a phone number in order to normalized it.
	 */
	private static final Pattern removedCharactersToNormalizedPhoneNumber = Pattern.compile("[-\\(\\)\\.\\\\\\/ ]");

	/**
	 * Characters which have to be removed from a number (which is not a phone number, such as a sip id, a jabber id,
	 * etc.) in order to normalized it.
	 */
	private static final Pattern removedCharactersToNormalizedIdentifier = Pattern.compile("[\\(\\) ]");

	/**
	 * The list of characters corresponding to the number 2 in a phone dial pad.
	 */
	private static final Pattern charactersFordialPadNumber2 = Pattern.compile("[abc]", Pattern.CASE_INSENSITIVE);
	/**
	 * The list of characters corresponding to the number 3 in a phone dial pad.
	 */
	private static final Pattern charactersFordialPadNumber3 = Pattern.compile("[def]", Pattern.CASE_INSENSITIVE);

	/**
	 * The list of characters corresponding to the number 4 in a phone dial pad.
	 */
	private static final Pattern charactersFordialPadNumber4 = Pattern.compile("[ghi]", Pattern.CASE_INSENSITIVE);

	/**
	 * The list of characters corresponding to the number 5 in a phone dial pad.
	 */
	private static final Pattern charactersFordialPadNumber5 = Pattern.compile("[jkl]", Pattern.CASE_INSENSITIVE);

	/**
	 * The list of characters corresponding to the number 6 in a phone dial pad.
	 */
	private static final Pattern charactersFordialPadNumber6 = Pattern.compile("[mno]", Pattern.CASE_INSENSITIVE);

	/**
	 * The list of characters corresponding to the number 7 in a phone dial pad.
	 */
	private static final Pattern charactersFordialPadNumber7 = Pattern.compile("[pqrs]", Pattern.CASE_INSENSITIVE);

	/**
	 * The list of characters corresponding to the number 8 in a phone dial pad.
	 */
	private static final Pattern charactersFordialPadNumber8 = Pattern.compile("[tuv]", Pattern.CASE_INSENSITIVE);

	/**
	 * The list of characters corresponding to the number 9 in a phone dial pad.
	 */
	private static final Pattern charactersFordialPadNumber9 = Pattern.compile("[wxyz]", Pattern.CASE_INSENSITIVE);

	/**
	 * Normalizes a <code>String</code> which may be a phone number or a identifier by removing useless characters and, if
	 * necessary, replacing the alpahe characters in corresponding dial pad numbers.
	 *
	 * @param possibleNumber a <code>String</code> which may represents a phone number or an identifier to normalize.
	 * @return a <code>String</code> which is a normalized form of the specified <code>possibleNumber</code>.
	 */
	public String normalize(String possibleNumber) {
		String normalizedNumber;
		if (isPhoneNumber(possibleNumber)) {
			normalizedNumber = normalizePhoneNumber(possibleNumber);
		}
		else {
			normalizedNumber = normalizeIdentifier(possibleNumber);
		}
		return normalizedNumber;
	}

	/**
	 * Normalizes a <code>String</code> phone number by converting alpha characters to their respective digits on a keypad
	 * and then stripping non-digit characters.
	 *
	 * @param phoneNumber a <code>String</code> which represents a phone number to normalize
	 * @return a <code>String</code> which is a normalized form of the specified <code>phoneNumber</code>
	 * @see net.java.sip.communicator.impl.phonenumbers.PhoneNumberI18nServiceImpl#normalize(String)
	 */
	private static String normalizePhoneNumber(String phoneNumber) {
		phoneNumber = convertAlphaCharactersInNumber(phoneNumber);
		return removedCharactersToNormalizedPhoneNumber.matcher(phoneNumber).replaceAll("");
	}

	/**
	 * Removes useless characters from a identifier (which is not a phone number) in order to normalized it.
	 *
	 * @param id The identifier string with some useless characters like: " ", "(", ")".
	 * @return The normalized identifier.
	 */
	private static String normalizeIdentifier(String id) {
		return removedCharactersToNormalizedIdentifier.matcher(id).replaceAll("");
	}

	/**
	 * Determines whether two <code>String</code> phone numbers match.
	 *
	 * @param aPhoneNumber a <code>String</code> which represents a phone number to match to <code>bPhoneNumber</code>
	 * @param bPhoneNumber a <code>String</code> which represents a phone number to match to <code>aPhoneNumber</code>
	 * @return <code>true</code> if the specified <code>String</code>s match as phone numbers; otherwise, <code>false</code>
	 */
	public boolean phoneNumbersMatch(String aPhoneNumber, String bPhoneNumber) {
		PhoneNumberUtil.MatchType match = PhoneNumberUtil.getInstance().isNumberMatch(aPhoneNumber, bPhoneNumber);
		return match != PhoneNumberUtil.MatchType.NOT_A_NUMBER && match != PhoneNumberUtil.MatchType.NO_MATCH;
	}

	/**
	 * Tries to format the passed phone number into the international format. If
	 * parsing fails or the string is not recognized as a valid phone number,
	 * the input is returned as is.
	 *
	 * @param phoneNumber The phone number to format.
	 * @return the formatted phone number in the international format.
	 */
	public String formatForDisplay(String phoneNumber) {
		try {
			Phonenumber.PhoneNumber pn = PhoneNumberUtil.getInstance().parse(phoneNumber,
					System.getProperty("user.country"));
			if (PhoneNumberUtil.getInstance().isPossibleNumber(pn)) {
				return PhoneNumberUtil.getInstance().format(pn,
						PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
			}
		} catch (NumberParseException e) {
		}
		return phoneNumber;
	}

	/**
	 * Indicates if the given string is possibly a phone number.
	 *
	 * @param possibleNumber the string to be verified
	 * @return <code>true</code> if the possibleNumber is a phone number, <code>false</code> - otherwise
	 */
	public boolean isPhoneNumber(String possibleNumber) {
		// If the string does not contains an "@", this may be a phone number.
		if (possibleNumber.indexOf('@') == -1) {
			// If the string does not contain any alphabetical characters, then this is a phone number.
			if (!possibleNumber.matches(".*[a-zA-Z].*")) {
				return true;
			}
			else {
				// Removes the " ", "(" and ")" in order to search the "+" character at the beginning at the string.
				String tmpPossibleNumber = possibleNumber.replaceAll(" \\(\\)", "");
				// If the property is enabled and the string starts with a "+", then we consider that this is a phone
				// number.
				if (configService.getBoolean("impl.gui.ACCEPT_PHONE_NUMBER_WITH_ALPHA_CHARS", true) && tmpPossibleNumber.startsWith("+")) {
					return true;
				}
			}
		}
		// Else the string is not a phone number.
		return false;
	}

	/**
	 * Changes all alphabetical characters into numbers, following phone dial pad disposition.
	 *
	 * @param phoneNumber The phone number string with some alphabetical characters.
	 * @return The phone number with all alphabetical caracters replaced with the corresponding dial pad number.
	 */
	private static String convertAlphaCharactersInNumber(String phoneNumber) {
		phoneNumber = charactersFordialPadNumber2.matcher(phoneNumber).replaceAll("2");
		phoneNumber = charactersFordialPadNumber3.matcher(phoneNumber).replaceAll("3");
		phoneNumber = charactersFordialPadNumber4.matcher(phoneNumber).replaceAll("4");
		phoneNumber = charactersFordialPadNumber5.matcher(phoneNumber).replaceAll("5");
		phoneNumber = charactersFordialPadNumber6.matcher(phoneNumber).replaceAll("6");
		phoneNumber = charactersFordialPadNumber7.matcher(phoneNumber).replaceAll("7");
		phoneNumber = charactersFordialPadNumber8.matcher(phoneNumber).replaceAll("8");
		return charactersFordialPadNumber9.matcher(phoneNumber).replaceAll("9");
	}
}
