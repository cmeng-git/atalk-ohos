/*
 * Copyright (C) 2012-2015 cketti and contributors
 * https://github.com/cketti/ckChangeLog/graphs/contributors
 *
 * Portions Copyright (C) 2012 Martin van Zuilekom (http://martin.cubeactive.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Based on android-change-log:
 *
 * Copyright (C) 2011, Karsten Priegnitz
 *
 * Permission to use, copy, modify, and distribute this piece of software
 * for any purpose with or without fee is hereby granted, provided that
 * the above copyright notice and this permission notice appear in the
 * source code of all copies.
 *
 * It would be appreciated if you mention the author in your change log,
 * contributors list or the like.
 *
 * http://code.google.com/p/android-change-log/
 */
package org.atalk.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ohos.agp.components.webengine.WebView;
import ohos.app.Context;
import ohos.data.preferences.Preferences;
import ohos.utils.PlainArray;

import org.atalk.impl.appupdate.VersionServiceImpl;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.whispersystems.libsignal.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * Display a dialog showing a full or partial (What's New) change log.
 * ChangeLog ported from de.cketti.library.changelog.
 *
 * @author Karsten Priegnitz
 * @author Eng Chong Meng
 */
public class ChangeLog {
    /**
     * Tag that is used when sending error/debug messages to the log.
     */
    protected static final String LOG_TAG = "ckChangeLog";

    /**
     * This is the key used when storing the version code in SharedPreferences.
     */
    protected static final String VERSION_KEY = "ckChangeLog_last_version_code";

    /**
     * Constant that used when no version code is available.
     */
    protected static final int NO_VERSION = -1;

    /**
     * Default CSS styles used to format the change log.
     */
    public static final String DEFAULT_CSS =
            "h1 { margin-left: 0px; font-size: 1.2em; }" + "\n" +
                    "li { margin-left: 0px; }" + "\n" +
                    "ul { padding-left: 2em; }";

    private static final String CL_FILE_PATH = "resources/rawfile/xml/changelog_master.xml";

    /**
     * Context that is used to access the resources and to create the ChangeLog dialogs.
     */
    protected final Context mContext;

    private final Preferences mPrefs;

    /**
     * Last version code read from {@code SharedPreferences} or {@link #NO_VERSION}.
     */
    private final long mLastVersionCode;

    /**
     * Version code of the current installation.
     */
    private final long mCurrentVersionCode;

    /**
     * Version name of the current installation.
     */
    private final String mCurrentVersionName;

    /**
     * Contains constants for the release element of {@code changelog.xml}.
     */
    protected interface ReleaseTag {
        String NAME = "release";
        String ATTRIBUTE_VERSION = "version";
        String ATTRIBUTE_VERSION_CODE = "versioncode";
    }

    /**
     * Contains constants for the change element of {@code changelog.xml}.
     */
    protected interface ChangeTag {
        String NAME = "change";
    }

    /**
     * Create a {@code ChangeLog} instance using the default {@link Preferences} file.
     *
     * @param context Context that is used to access the resources and to create the ChangeLog dialogs.
     */
    public ChangeLog(Context context) {
        mContext = context;
        mPrefs = BaseAbility.getPreferenceStore();

        // Get last version code
        mLastVersionCode = mPrefs.getInt(VERSION_KEY, NO_VERSION);

        // Get current version code and version name
        VersionServiceImpl versionService = VersionServiceImpl.getInstance();
        mCurrentVersionName = versionService.getCurrentVersionName();
        mCurrentVersionCode = versionService.getCurrentVersionCode();
    }

    /**
     * Get version code of last installation.
     *
     * @return The version code of the last installation of this app (as described in the former
     * manifest). This will be the same as returned by {@link #getCurrentVersionCode()} the
     * second time this version of the app is launched (more precisely: the second time
     * {@code ChangeLog} is instantiated).
     *
     * @see <a href="http://developer.android.com/guide/topics/manifest/manifest-element.html#vcode">android:versionCode</a>
     */
    public long getLastVersionCode() {
        return mLastVersionCode;
    }

    /**
     * Get version code of current installation.
     *
     * @return The version code of this app as described in the manifest.
     *
     * @see <a href="http://developer.android.com/guide/topics/manifest/manifest-element.html#vcode">android:versionCode</a>
     */
    public long getCurrentVersionCode() {
        return mCurrentVersionCode;
    }

    /**
     * Get version name of current installation.
     *
     * @return The version name of this app as described in the manifest.
     *
     * @see <a href="http://developer.android.com/guide/topics/manifest/manifest-element.html#vname">android:versionName</a>
     */
    public String getCurrentVersionName() {
        return mCurrentVersionName;
    }

    /**
     * Check if this is the first execution of this app version.
     *
     * @return {@code true} if this version of your app is started the first time.
     */
    public boolean isFirstRun() {
        return mLastVersionCode < mCurrentVersionCode;
    }

    /**
     * Check if this is a new installation.
     *
     * @return {@code true} if your app including {@code ChangeLog} is started the first time ever.
     * Also {@code true} if your app was uninstalled and installed again.
     */
    public boolean isFirstRunEver() {
        return mLastVersionCode == NO_VERSION;
    }

    /**
     * Skip the "What's new" dialog for this app version.
     *
     * <p>
     * Future calls to {@link #isFirstRun()} and {@link #isFirstRunEver()} will return {@code false}
     * for the current app version.
     * </p>
     */
    public void skipLogDialog() {
        updateVersionInPreferences();
    }

    /**
     * Get the "What's New" dialog.
     *
     * @return An AlertDialog displaying the changes since the previous installed version of your
     * app (What's New). But when this is the first run of your app including
     * {@code ChangeLog} then the full log dialog is show.
     */
    public DialogA getLogDialog() {
        return getDialog(isFirstRunEver());
    }

    /**
     * Get a dialog with the full change log.
     *
     * @return An AlertDialog with a full change log displayed.
     */
    public DialogA getFullLogDialog() {
        return getDialog(true);
    }

    /**
     * Create a dialog containing (parts of the) change log.
     *
     * @param full If this is {@code true} the full change log is displayed. Otherwise only changes for
     * versions newer than the last version are displayed.
     *
     * @return A dialog containing the (partial) change log.
     */
    protected DialogA getDialog(boolean full) {
        WebView wv = new WebView(mContext);
        //wv.setBackgroundColor(0); // transparent
        wv.load(null, getLog(full), "text/html", "UTF-8", null);

        DialogA.Builder builder = new DialogA.Builder(mContext)
                .setTitle(mContext.getString(
                        full ? ResourceTable.String_changelog_full_title : ResourceTable.String_changelog_title))
                .setComponent(wv)
                // OK - Save the current version code as "last version code".
                .setPositiveButton(ResourceTable.String_ok, dialog -> {
                    updateVersionInPreferences();
                });
        if (!full) {
            // Show "More…" button if we're only displaying a partial change log.
            builder.setPositiveButton(ResourceTable.String_changelog_show_full, dialog -> {
                getFullLogDialog().show();
            });
        }

        DialogA sDialog = builder.create();
        sDialog.setSwipeToDismiss(true);
        sDialog.setAutoClosable(false);
        sDialog.siteRemovable(false);
        return sDialog;
    }

    /**
     * Write current version code to the preferences.
     */
    protected void updateVersionInPreferences() {
        mPrefs.putLong(VERSION_KEY, mCurrentVersionCode);
        mPrefs.flush();
    }

    /**
     * Get changes since last version as HTML string.
     *
     * @return HTML string containing the changes since the previous installed version of your app
     * (What's New).
     */
    public String getLog() {
        return getLog(false);
    }

    /**
     * Get full change log as HTML string.
     *
     * @return HTML string containing the full change log.
     */
    public String getFullLog() {
        return getLog(true);
    }

    /**
     * Get (partial) change log as HTML string.
     *
     * @param full If this is {@code true} the full change log is returned. Otherwise only changes for
     * versions newer than the last version are returned.
     *
     * @return The (partial) change log.
     */
    protected String getLog(boolean full) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><head><style type=\"text/css\">");
        sb.append(DEFAULT_CSS);
        sb.append("</style></head><body>");

        String versionFormat = mContext.getString(ResourceTable.String_changelog_version_format);
        List<ReleaseItem> changelog = getChangeLog(full);

        for (ReleaseItem release : changelog) {
            sb.append("<h1>");
            sb.append(String.format(versionFormat, release.versionName));
            sb.append("</h1><ul>");
            for (String change : release.changes) {
                sb.append("<li>");
                sb.append(change);
                sb.append("</li>");
            }
            sb.append("</ul>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Returns the merged change log.
     *
     * @param full If this is {@code true} the full change log is returned. Otherwise only changes for
     * versions newer than the last version are returned.
     *
     * @return A sorted {@code List} containing {@link ReleaseItem}s representing the (partial)
     * change log.
     *
     * @see #getChangeLogComparator()
     */
    public List<ReleaseItem> getChangeLog(boolean full) {
        PlainArray<ReleaseItem> masterChangelog = getMasterChangeLog(full);
        PlainArray<ReleaseItem> changelog = getLocalizedChangeLog(full);

        List<ReleaseItem> mergedChangeLog = new ArrayList<>(masterChangelog.size());

        for (int i = 0, len = masterChangelog.size(); i < len; i++) {
            int key = masterChangelog.keyAt(i);

            // Use release information from localized change log and fall back to the master file if necessary.
            ReleaseItem release = changelog.get(key, masterChangelog.get(key).get());
            mergedChangeLog.add(release);
        }

        mergedChangeLog.sort(getChangeLogComparator());
        return mergedChangeLog;
    }

    /**
     * Read master change log from {@code changelog_master.xml}
     */
    protected PlainArray<ReleaseItem> getMasterChangeLog(boolean full) {
        return readChangeLogFromResource(CL_FILE_PATH, full);
    }

    /**
     * Read localized change log from {@code xml[-lang]/changelog.xml}
     */
    protected PlainArray<ReleaseItem> getLocalizedChangeLog(boolean full) {
        return readChangeLogFromResource("resources/rawfile/xml/changelog.xml", full);
    }

    /**
     * Read change log from XML resource file.
     *
     * @param xmlFP Resource filePath of the XML file to read the change log from.
     * @param full If this is {@code true} the full change log is returned. Otherwise only changes for
     * versions newer than the last version are returned.
     *
     * @return A {@code SparseArray} containing {@link ReleaseItem}s representing the (partial)
     * change log.
     */
    protected final PlainArray<ReleaseItem> readChangeLogFromResource(String xmlFP, boolean full) {
        String line = "";
        StringBuilder fileData = new StringBuilder();

        try {
            InputStream is = mContext.getResourceManager().getRawFileEntry(xmlFP).openRawFile();
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                fileData.append(line);
            }
            is.close();
            parser.setInput(new StringReader(fileData.toString()));
            return readChangeLog(parser, full);

        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read the change log from an XML file.
     *
     * @param xml The {@code XmlPullParser} instance used to read the change log.
     * @param full If {@code true} the full change log is read. Otherwise only the changes since the
     * last (saved) version are read.
     *
     * @return A {@code SparseArray} mapping the version codes to release information.
     */
    protected PlainArray<ReleaseItem> readChangeLog(XmlPullParser xml, boolean full) {
        PlainArray<ReleaseItem> result = new PlainArray<>();

        try {
            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && xml.getName().equals(ReleaseTag.NAME)) {
                    if (parseReleaseTag(xml, full, result)) {
                        // Stop reading more elements if this entry is not newer than the last version.
                        break;
                    }
                }
                eventType = xml.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return result;
    }

    /**
     * Parse the {@code release} tag of a change log XML file.
     *
     * @param xml The {@code XmlPullParser} instance used to read the change log.
     * @param full If {@code true} the contents of the {@code release} tag are always added to
     * {@code changelog}. Otherwise only if the item's {@code versioncode} attribute is
     * higher than the last version code.
     * @param changelog The {@code SparseArray} to add a new {@link ReleaseItem} instance to.
     *
     * @return {@code true} if the {@code release} element is describing changes of a version older
     * or equal to the last version. In that case {@code changelog} won't be modified and
     * {@link #readChangeLog(XmlPullParser, boolean)} will stop reading more elements from
     * the change log file.
     *
     * @throws XmlPullParserException
     * @throws IOException
     */
    private boolean parseReleaseTag(XmlPullParser xml, boolean full,
            PlainArray<ReleaseItem> changelog) throws XmlPullParserException, IOException {

        String version = xml.getAttributeValue(null, ReleaseTag.ATTRIBUTE_VERSION);

        int versionCode;
        try {
            String versionCodeStr = xml.getAttributeValue(null, ReleaseTag.ATTRIBUTE_VERSION_CODE);
            versionCode = Integer.parseInt(versionCodeStr);
        } catch (NumberFormatException e) {
            versionCode = NO_VERSION;
        }

        if (!full && versionCode <= mLastVersionCode) {
            return true;
        }

        int eventType = xml.getEventType();
        List<String> changes = new ArrayList<String>();
        while (eventType != XmlPullParser.END_TAG || xml.getName().equals(ChangeTag.NAME)) {
            if (eventType == XmlPullParser.START_TAG && xml.getName().equals(ChangeTag.NAME)) {
                eventType = xml.next();

                changes.add(xml.getText());
            }
            eventType = xml.next();
        }

        ReleaseItem release = new ReleaseItem(versionCode, version, changes);
        changelog.put(versionCode, release);

        return false;
    }

    /**
     * Returns a {@link Comparator} that specifies the sort order of the {@link ReleaseItem}s.
     * The default implementation returns the items in reverse order (latest version first).
     */
    protected Comparator<ReleaseItem> getChangeLogComparator() {
        return (lhs, rhs) -> Integer.compare(rhs.versionCode, lhs.versionCode);
    }

    /**
     * Container used to store information about a release/version.
     */
    public static class ReleaseItem {
        /**
         * Version code of the release.
         */
        public final int versionCode;

        /**
         * Version name of the release.
         */
        public final String versionName;

        /**
         * List of changes introduced with that release.
         */
        public final List<String> changes;

        ReleaseItem(int versionCode, String versionName, List<String> changes) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.changes = changes;
        }
    }
}
