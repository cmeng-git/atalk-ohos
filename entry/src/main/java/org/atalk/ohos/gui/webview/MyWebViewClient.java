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
package org.atalk.ohos.gui.webview;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.gui.dialogs.DialogA;
import org.atalk.ohos.util.ComponentUtil;
import org.atalk.ohos.ResourceTable;

import java.util.regex.Pattern;

import ohos.aafwk.content.Intent;
import ohos.agp.components.Checkbox;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.TextField;
import ohos.agp.components.webengine.AuthRequest;
import ohos.agp.components.webengine.ResourceRequest;
import ohos.agp.components.webengine.WebAgent;
import ohos.agp.components.webengine.WebView;
import ohos.app.Context;
import ohos.utils.net.Uri;

import timber.log.Timber;

/**
 * The class implements the WebAgent for App internal web access
 *
 * @author Eng Chong Meng
 */
public class MyWebViewClient extends WebAgent {
    // Domain match pattern for last two segments of host
    private final Pattern pattern = Pattern.compile("^.*?[.](.*?[.].+?)$");

    private final WebViewSlice viewFragment;
    private final Context mContext;

    private TextField mPasswordField;

    public MyWebViewClient(WebViewSlice viewSlice) {
        this.viewFragment = viewSlice;
        mContext = viewSlice.getContext();
    }

    /**
     * If you click on any link inside the webpage of the WebView, that page will not be loaded inside your WebView.
     * In order to do that you need to extend your class from WebViewClient and override the method below.
     * https://developer.android.com/guide/webapps/webview#HandlingNavigation
     *
     * @param webView The WebView that is initiating the callback.
     * @param request Object containing the details of the request.
     *
     * @return {@code true} to cancel the current load, otherwise return {@code false}.
     */
    @Override
    public boolean isNeedLoadUrl(WebView webView, ResourceRequest request) {
        String url = request.getRequestUrl().toString();

        // Timber.d("shouldOverrideUrlLoading for url (webView url): %s (%s)", url, webView.getUrl());
        // This user clicked url is from the same website, so do not override; let MyWebViewClient load the page
        if (isDomainMatch(webView, url)) {
            viewFragment.addLastUrl(url);
            return false;
        }

        // Otherwise, the link is not for a page on my site, so launch another Ability that handle it
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            viewFragment.startAbility(intent);
        } catch (Exception e) {
            // catch ActivityNotFoundException for xmpp:info@example.com. so let own webView load and display the error
            Timber.w("Failed to load url '%s' : %s", url, e.getMessage());
            String origin = Uri.parse(webView.getCurrentUrl()).getDecodedHost();
            String originDomain = pattern.matcher(origin).replaceAll("$1");
            if (url.contains(originDomain))
                return false;
        }
        return true;
    }

    // public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
    // {
    //     view.loadUrl("file:///android_asset/movim/error.html");
    // }

    // public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error)
    // {
    //     // view.loadUrl("file:///android_asset/movim/ssl.html");
    // }

    @Override
    public void onAuthRequested(WebView webView, AuthRequest request, String host, String realm) {
        final String[] httpAuth = new String[2];
        final String[] viewAuth = webView.getHttpAuthUsernamePassword(host, realm);

        httpAuth[0] = (viewAuth != null) ? viewAuth[0] : "";
        httpAuth[1] = (viewAuth != null) ? viewAuth[1] : "";

        if (request.isCredentialsStored()) {
            request.respond(httpAuth[0], httpAuth[1]);
            return;
        }

        LayoutScatter inflater = LayoutScatter.getInstance(mContext);
        Component authView = inflater.parse(ResourceTable.Layout_http_login_dialog, null, false);

        final TextField usernameInput = authView.findComponentById(ResourceTable.Id_username);
        usernameInput.setText(httpAuth[0]);

        mPasswordField = authView.findComponentById(ResourceTable.Id_passwordField);
        // mPasswordField.setInputFilters(InputFilter.TYPE_CLASS_TEXT, InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mPasswordField.setText(httpAuth[1]);

        Checkbox showPasswordCheckBox = authView.findComponentById(ResourceTable.Id_show_password);
        showPasswordCheckBox.setCheckedStateChangedListener(((buttonView, isChecked)
                -> ComponentUtil.showPassword(mPasswordField, isChecked)));

        DialogA.Builder authDialog = new DialogA.Builder(mContext)
                .setTitle(ResourceTable.String_user_login)
                .setComponent(authView);
                // .setCancelable(false);
        final DialogA dialog = authDialog.create();

        authView.findComponentById(ResourceTable.Id_button_signin).setClickedListener(v -> {
            httpAuth[0] = ComponentUtil.toString(usernameInput);
            httpAuth[1] = ComponentUtil.toString(mPasswordField);
            // webView.setsetHttpAuthUsernamePassword(host, realm, httpAuth[0], httpAuth[1]);
            request.respond(httpAuth[0], httpAuth[1]);
            dialog.remove();
        });

        authView.findComponentById(ResourceTable.Id_button_cancel).setClickedListener(v -> {
            request.cancel();
            dialog.remove();
        });
    }

    /**
     * Match non-case sensitive for whole or at least last two segment of host
     *
     * @param webView the current webView
     * @param url to be loaded
     *
     * @return true if match
     */
    private boolean isDomainMatch(WebView webView, String url) {
        String origin = Uri.parse(webView.getCurrentUrl()).getDecodedHost();
        String aim = Uri.parse(url).getDecodedHost();

        // return true if this is the first time url loading or exact match of host
        if (TextUtils.isEmpty(origin) || origin.equalsIgnoreCase(aim))
            return true;

        // return false if aim contains no host string i.e. not a url e.g. mailto:info[at]example.com
        if (TextUtils.isEmpty(aim))
            return false;

        String originDomain = pattern.matcher(origin).replaceAll("$1");
        String aimDomain = pattern.matcher(aim).replaceAll("$1");

        return originDomain.equalsIgnoreCase(aimDomain);
    }
}
