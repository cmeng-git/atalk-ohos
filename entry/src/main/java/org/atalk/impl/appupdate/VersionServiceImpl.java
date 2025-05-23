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

import ohos.bundle.BundleInfo;

/**
 * A Version service implementation.
 *
 * @author Eng Chong Meng
 */
public class VersionServiceImpl {
    private static VersionServiceImpl mInstance;

    /**
     * Current version instance.
     */
    private final long currentVersionCode;
    private final String currentVersionName;

    /**
     * Creates a new instance of <tt>VersionServiceImpl</tt> and parses the current version from
     * android:versionName attribute of the PackageInfo.
     */
    public static VersionServiceImpl getInstance() {
        if (mInstance == null) {
            mInstance = new VersionServiceImpl();
        }
        return mInstance;
    }

    public VersionServiceImpl() {
        mInstance = this;
        BundleInfo bundleInfo = new BundleInfo();
        currentVersionName = bundleInfo.getVersionName();
        currentVersionCode = bundleInfo.getVersionCode();
    }

    /**
     * Get the <tt>Version</tt> of the current running hymnchtv app.
     *
     * @return the <tt>Version</tt> of the current running hymntv app.
     */
    public long getCurrentVersionCode() {
        return currentVersionCode;
    }

    public String getCurrentVersionName() {
        return currentVersionName;
    }
}
