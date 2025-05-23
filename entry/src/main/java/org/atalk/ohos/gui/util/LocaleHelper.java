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
package org.atalk.ohos.gui.util;

import java.util.Locale;

import ohos.app.Context;
import ohos.global.configuration.Configuration;
import ohos.global.configuration.LocaleProfile;

import org.apache.http.util.TextUtils;

/**
 * Implementation of LocaleHelper to support proper Locale setting for Application/Ability classes.
 *
 * @author Eng Chong Meng
 */
public class LocaleHelper {

    // Default to system locale language; get init from DB by aTalkApp first call
    private static String mLanguage = "";

    // mLocale will have 'regional preference' value stripped off; mainly use for smack xml:lang
    private static Locale xmlLocale = Locale.getDefault();

    /**
     * Set aTalk Locale to the current mLanguage
     *
     * @param ctx Context
     */
    public static Context setLocale(Context ctx) {
        return wrap(ctx, mLanguage);
    }

    /**
     * Set the locale as per specified language; must use Application instance
     *
     * @param ctx Base Context
     * @param language the new UI language
     */
    public static Context setLocale(Context ctx, String language) {
        mLanguage = language;
        return wrap(ctx, language);
    }

    public static Locale getXmlLocale() {
        return xmlLocale;
    }

    public static String getLanguage() {
        return mLanguage;
    }

    public static void setLanguage(String language) {
        mLanguage = language;
    }

    /**
     * Update the app local as per specified language.
     *
     * @param context Base Context (ContextImpl)
     * @param language the new UI language
     * #return The new ContextImpl for use by caller
     */
    public static Context wrap(Context context, String language) {
        Configuration config = context.getResourceManager().getConfiguration();

        Locale locale;
        if (TextUtils.isEmpty(language)) {
            // System default may contain regional preference i.e. 'en-US-#u-fw-sun-mu-celsius'
            locale =  config.getLocaleProfile().getLocales()[0];

            // Strip off any regional preferences in the language
            language = locale.toString().split("_#")[0];
            int idx = language.indexOf("_");
            xmlLocale = (idx == -1) ? locale : new Locale(language.substring(0, idx), language.substring(idx + 1));
        }
        else {
            int idx = language.indexOf("_");
            if (idx != -1) {
                // language is in the form: en_US
                locale = new Locale(language.substring(0, idx), language.substring(idx + 1));
            }
            else {
                locale = new Locale(language);
            }
            xmlLocale = locale;
        }

        Locale[] locales = {locale};
        new LocaleProfile(locales);

        config.setLocaleProfile(new LocaleProfile(locales));

        // Timber.d(new Exception(), "set locale: %s: %s", language, context);
        // Context createBundleContext(String var1, int var2);
        return context; //.createConfigurationContext(config);
    }
}
