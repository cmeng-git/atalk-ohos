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
package org.atalk.impl.appupdate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import ohos.app.Context;
import ohos.bundle.BundleInfo;
import ohos.bundle.IBundleInstaller;
import ohos.bundle.InstallParam;
import ohos.bundle.InstallerCallback;
import ohos.data.preferences.Preferences;
import ohos.miscservices.download.DownloadConfig;
import ohos.miscservices.download.DownloadSession;
import ohos.miscservices.download.DownloadSession.DownloadInfo;
import ohos.miscservices.download.IDownloadListener;
import ohos.rpc.MessageParcel;
import ohos.rpc.RemoteException;
import ohos.utils.net.Uri;

import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.BuildConfig;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.gui.aTalk;
import org.atalk.ohos.gui.dialogs.DialogH;
import org.atalk.persistance.FileBackend;
import org.atalk.persistance.FilePathHelper;
import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

/**
 * aTalk update service implementation. It checks for an update and schedules .apk download using <code>DownloadSession</code>.
 * It is only activated for the debug version. Android initials the auto-update from PlayStore for release version.
 *
 * @author Eng Chong Meng
 */
public class UpdateServiceImpl {
    // Default update link; path is case-sensitive.
    private static final String[] updateLinks = {
            "https://raw.githubusercontent.com/cmeng-git/atalk-ohos/main/entry/release/version.properties",
            "https://atalk.sytes.net/releases/atalk-ohos/version.properties"
    };

    // filename is case-sensitive
    private static final String fileNameApk = String.format("aTalk-%s.hap", BuildConfig.BUILD_TYPE);

    // github apk is in the release directory; for large apk size download
    private static final String urlApk = "https://github.com/cmeng-git/atalk-ohos/releases/download/%s/";
    /**
     * Apk mime type constant.
     */
    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";

    private static final Context mContext = aTalkApp.getInstance();
    /**
     * The download link for the installed application
     */
    private String downloadLink = null;

    /**
     * Current installed version string / version Code
     */
    private String currentVersionName;
    private long currentVersionCode;

    /**
     * The latest version string / version code
     */
    private String latestVersion;
    private long latestVersionCode;
    private boolean mIsLatest = false;

    private DownloadSession downloadSession;
    private IDownloadListener dnListener = null;
    private HttpURLConnection mHttpConnection;

    /**
     * <code>Preferences</code> used to store download ids.
     */
    private Preferences mPrefs;

    /**
     * Name of <code>Preferences</code> entry used to store old download ids. Ids are stored in
     * single string separated by ",".
     */
    private static final String ENTRY_NAME = "apk_ids";

    private static UpdateServiceImpl mInstance = null;

    public static UpdateServiceImpl getInstance() {
        if (mInstance == null) {
            mInstance = new UpdateServiceImpl();
        }
        return mInstance;
    }

    protected UpdateServiceImpl() {
        if (mPrefs == null) {
            mPrefs = BaseAbility.getPreferenceStore();
        }
    }

    /**
     * Checks for updates and notify user of any new version, and take necessary action.
     */
    public void checkForUpdates() {
        // cmeng: reverse the logic to !isLatestVersion() for testing
        mIsLatest = isLatestVersion();
        Timber.i("Is latest: %s\nCurrent version: %s\nLatest version: %s\nDownload link: %s",
                mIsLatest, currentVersionName, latestVersion, downloadLink);

        if ((downloadLink != null)) {
            if (!mIsLatest) {
                if (checkLastDLFileAction() < DownloadSession.ERROR_UNKNOWN)
                    return;

                Context ctx = aTalkApp.getInstance();
                DialogH.getInstance(ctx).showConfirmDialog(ctx,
                        ResourceTable.String_update_install_update,
                        ResourceTable.String_update_update_available,
                        ResourceTable.String_update_download,
                        new DialogH.DialogListener() {
                            @Override
                            public boolean onConfirmClicked(DialogH dialog) {
                                downloadApk();
                                return true;
                            }

                            @Override
                            public void onDialogCancelled(@NotNull DialogH dialog) {
                            }
                        }, latestVersion, latestVersionCode, fileNameApk, currentVersionName, currentVersionCode
                );
            }
            else {
                // Notify that running version is up to date
                Context ctx = aTalkApp.getInstance();
                DialogH.getInstance(ctx).showConfirmDialog(ctx,
                        ResourceTable.String_update_new_version_none,
                        ResourceTable.String_update_up_to_date,
                        ResourceTable.String_update_download,
                        new DialogH.DialogListener() {
                            @Override
                            public boolean onConfirmClicked(DialogH dialog) {
                                if (aTalk.hasWriteStoragePermission(aTalk.getInstance(), true)) {
                                    if (checkLastDLFileAction() >= DownloadSession.ERROR_UNKNOWN) {
                                        downloadApk();
                                    }
                                }
                                return true;
                            }

                            @Override
                            public void onDialogCancelled(@NotNull DialogH dialog) {
                            }
                        }, currentVersionName, currentVersionCode, latestVersion, latestVersionCode
                );
            }
        }
        else {
            aTalkApp.showToastMessage(ResourceTable.String_update_new_version_none);
        }
    }

    /**
     * Check for any existing downloaded file and take appropriate action;
     *
     * @return Last DownloadSession status; default to DownloadSession.ERROR_UNKNOWN if status unknown
     */
    private int checkLastDLFileAction() {
        // Check old or scheduled downloads
        int lastJobStatus = DownloadSession.ERROR_UNKNOWN;

        List<Long> previousDownloads = getOldDownloads();
        if (previousDownloads.size() > 0) {
            long lastDownload = previousDownloads.get(previousDownloads.size() - 1);

            DownloadSession dnSession = new DownloadSession(mContext, (Uri) null);
            dnSession.attach(lastDownload);
            DownloadInfo dnInfo = dnSession.query();
            lastJobStatus = dnInfo.getStatus();
            Context ctx = aTalkApp.getInstance();

            if (lastJobStatus == DownloadSession.SESSION_SUCCESSFUL) {
                Uri fileUri = dnInfo.getPath();

                // Ask the user if he wants to install the valid apk when found
                if (isValidApkVersion(fileUri, latestVersionCode)) {
                    askInstallDownloadedApk(fileUri);
                }
            }
            else if (lastJobStatus != DownloadSession.SESSION_FAILED) {
                // Download is in progress or scheduled for retry
                DialogH.getInstance(ctx).showDialog(ctx,
                        ResourceTable.String_update_in_progress,
                        ResourceTable.String_update_download_in_progress);
            }
            else {
                // Download id return failed status, remove failed id and retry
                removeOldDownloads();
                DialogH.getInstance(ctx).showDialog(ctx,
                        ResourceTable.String_update_install_update, ResourceTable.String_update_download_failed);
            }
        }
        return lastJobStatus;
    }

    /**
     * Asks the user whether to install downloaded .apk.
     *
     * @param fileUri download file uri of the apk to install.
     */
    private void askInstallDownloadedApk(Uri fileUri) {
        Context ctx = aTalkApp.getInstance();
        DialogH.getInstance(ctx).showConfirmDialog(ctx,
                ResourceTable.String_update_download_completed,
                ResourceTable.String_update_download_ready,
                mIsLatest ? ResourceTable.String_update_reInstall : ResourceTable.String_update_install,
                new DialogH.DialogListener() {
                    @Override
                    public boolean onConfirmClicked(DialogH dialog) {
                        String filePath = fileUri.getDecodedPath();
                        List<String> paths = Collections.singletonList(filePath);
                        InstallParam param = new InstallParam(InstallParam.UNSPECIFIED_USER_ID, InstallParam.INSTALL_FLAG_DEFAULT);
                        InstallerCallback iCallback = new InstallerCallback() {
                            @Override
                            public void onFinished(int status, String message) {
                                Timber.d("Installer callBack %s = > %s", status, message);
                            }
                        };
                        try {
                            IBundleInstaller installer = mContext.getBundleManager().getBundleInstaller();
                            return installer.install(paths, param, iCallback);
                        } catch (RemoteException e) {
                            Timber.e("Installation error :%s", e.getMessage());
                        }
                        return false;
                    }

                    @Override
                    public void onDialogCancelled(@NotNull DialogH dialog) {
                    }
                }, latestVersion);
    }

    /**
     * Schedules .apk download.
     */
    private void downloadApk() {
        Uri uri = Uri.parse(downloadLink);
        String fileName = uri.getLastPath();

        if (dnListener == null) {
            dnListener = new DownloadListener();
        }

        DownloadConfig config = new DownloadConfig.Builder(mContext, uri)
                .setPath(FileBackend.getaTalkStore(FileBackend.TMP, true).getAbsolutePath(), fileName)
                .setDescription(APK_MIME_TYPE + fileName)
                .build();

        downloadSession = new DownloadSession(mContext, config);
        long jobId = downloadSession.start();
        rememberDownloadId(jobId);
    }

    private class DownloadListener implements IDownloadListener {
        public void onCompleted() {
            if (checkLastDLFileAction() < DownloadSession.ERROR_UNKNOWN)
                return;

            try {
                MessageParcel messageParcel = downloadSession.openDownloadedFile();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            if (dnListener != null) {
                downloadSession.removeListener(dnListener);
                dnListener = null;
            }
        }

        public void onFailed(int errorCode) {
            downloadSession.remove();
        }

        public void onProgress(long receivedSize, long totalSize) {
        }

        public void onRemoved() {
            downloadSession.remove();
        }
    }

    private void rememberDownloadId(long id) {
        String storeStr = mPrefs.getString(ENTRY_NAME, "");
        storeStr += id + ",";
        mPrefs.putString(ENTRY_NAME, storeStr).flush();
    }

    private List<Long> getOldDownloads() {
        String storeStr = mPrefs.getString(ENTRY_NAME, "");
        String[] idStrs = storeStr.split(",");
        List<Long> apkIds = new ArrayList<>(idStrs.length);
        for (String idStr : idStrs) {
            try {
                if (!idStr.isEmpty())
                    apkIds.add(Long.parseLong(idStr));
            } catch (NumberFormatException e) {
                Timber.e("Error parsing apk id for string: %s [%s]", idStr, storeStr);
            }
        }
        return apkIds;
    }

    /**
     * Removes old downloads.
     */
    void removeOldDownloads() {
        List<Long> apkIds = getOldDownloads();
        // DownloadSession downloadSession = aTalkApp.getDownloadManager();
        for (long id : apkIds) {
            Timber.d("Removing .apk for id %s", id);
            downloadSession.remove();
        }
        mPrefs.delete(ENTRY_NAME).flush();
    }

    /**
     * Validate the downloaded apk file for correct versionCode and its apk name
     *
     * @param fileUri apk Uri
     * @param versionCode use the given versionCode to check against the apk versionCode
     *
     * @return true if apkFile has the specified versionCode
     */
    private boolean isValidApkVersion(Uri fileUri, long versionCode) {
        // Default to valid as getPackageArchiveInfo() always return null; but sometimes OK
        boolean isValid = true;
        File apkFile = new File(FilePathHelper.getFilePath(aTalkApp.getInstance(), fileUri));

        if (apkFile.exists()) {
            // get downloaded hap actual versionCode and check its versionCode validity
            // PackageManager pm = aTalkApp.getInstance().getPackageManager();
            // PackageInfo pckgInfo = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
            BundleInfo pckgInfo = new BundleInfo();
            long apkVersionCode = pckgInfo.getVersionCode();
            isValid = (versionCode == apkVersionCode);
            if (!isValid) {
                aTalkApp.showToastMessage(ResourceTable.String_update_version_invalid, apkVersionCode, versionCode);
                Timber.d("Downloaded apk actual version code: %s (%s)", apkVersionCode, versionCode);
            }
        }
        return isValid;
    }

    /**
     * Gets the latest available (software) version online.
     *
     * @return the latest (software) version
     */
    public String getLatestVersion() {
        return latestVersion;
    }

    /**
     * Determines whether we are currently running the latest version.
     *
     * @return <code>true</code> if current running application is the latest version; otherwise, <code>false</code>
     */
    public boolean isLatestVersion() {
        VersionServiceImpl versionService = VersionServiceImpl.getInstance();
        currentVersionName = versionService.getCurrentVersionName();
        currentVersionCode = versionService.getCurrentVersionCode();

        for (String aLink : updateLinks) {
            try {
                if (isValidateLink(aLink)) {
                    InputStream inputStream = mHttpConnection.getInputStream();
                    Properties mProperties = new Properties();
                    mProperties.load(inputStream);
                    inputStream.close();

                    latestVersion = mProperties.getProperty("last_version");
                    latestVersionCode = Long.parseLong(mProperties.getProperty("last_version_code"));

                    if (aLink.contains("github")) {
                        downloadLink = urlApk.replace("%s", latestVersion) + fileNameApk;
                    }
                    else {
                        String aLinkPrefix = aLink.substring(0, aLink.lastIndexOf("/") + 1);
                        downloadLink = aLinkPrefix + fileNameApk;
                    }
                    if (isValidateLink(downloadLink)) {
                        // return true if current running application is already the latest
                        return (currentVersionCode >= latestVersionCode);
                    }
                    else {
                        downloadLink = null;
                    }
                }
            } catch (IOException e) {
                Timber.w("Could not retrieve version.properties for checking: %s", e.getMessage());
            }
        }
        // return true if all failed to force update.
        return true;
    }

    /**
     * Check if the given link is accessible.
     *
     * @param link the link to check
     *
     * @return true if link is accessible
     */
    private boolean isValidateLink(String link) {
        try {
            URL mUrl = new URL(link);
            mHttpConnection = (HttpURLConnection) mUrl.openConnection();
            mHttpConnection.setRequestMethod("GET");
            mHttpConnection.setRequestProperty("Content-length", "0");
            mHttpConnection.setUseCaches(false);
            mHttpConnection.setAllowUserInteraction(false);
            mHttpConnection.setConnectTimeout(100000);
            mHttpConnection.setReadTimeout(100000);

            mHttpConnection.connect();
            int responseCode = mHttpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return true;
            }
        } catch (IOException e) {
            Timber.d("Invalid url: %s", e.getMessage());
            return false;
        }
        return false;
    }
}
