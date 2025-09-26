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

import ohos.aafwk.content.Intent;
import ohos.agp.components.Component;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ProgressBar;
import ohos.agp.components.webengine.AsyncCallback;
import ohos.agp.components.webengine.BrowserAgent;
import ohos.agp.components.webengine.PickFilesParams;
import ohos.agp.components.webengine.WebConfig;
import ohos.agp.components.webengine.WebView;
import ohos.media.codec.PixelMap;
import ohos.media.image.ImageSource;
import ohos.multimodalinput.event.KeyEvent;
import ohos.utils.net.Uri;

import net.java.sip.communicator.util.ConfigurationUtils;

import org.atalk.ohos.BaseSlice;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Stack;
import timber.log.Timber;

/**
 * The class displays the content accessed via given web link
 * <a href="https://developer.android.com/guide/webapps/webview">...</a>
 *
 * @author Eng Chong Meng
 */
public class WebViewSlice extends BaseSlice implements Component.KeyEventListener {
    private WebView webview;
    private ProgressBar progressbar;
    private static final Stack<String> urlStack = new Stack<>();

    // stop webView.goBack() once we have started reload from urlStack
    private boolean isLoadFromStack = false;

    private String webUrl = null;
    private AsyncCallback<Uri[]> mUrisCallback;

    @Override
    public void onStart(Intent intent) {
        LayoutScatter layoutScatter = LayoutScatter.getInstance(getContext());
        Component contentView = layoutScatter.parse(ResourceTable.Layout_webview_main, null, false);
        progressbar = contentView.findComponentById(ResourceTable.Id_progress);
        progressbar.setIndeterminate(true);

        webview = contentView.findComponentById(ResourceTable.Id_webview);
        final WebConfig webConfig = webview.getWebConfig();
        webConfig.setJavaScriptPermit(true);
        webConfig.setWebStoragePermit(true);
        webConfig.setWebCachePriority(WebConfig.PRIORITY_CACHE_EXPIRED_FIRST);

        // https://developer.android.com/guide/webapps/webview#BindingJavaScript
//        webview.addJsCallback(aTalkApp.getInstance(), "Android");
//        if (BuildConfig.DEBUG) {
//            WebView.ConsetWebContentsDebuggingEnabled(true);
//        }
        webConfig.setSecurityMode(WebConfig.SECURITY_ALLOW);
        // webConfig.setAllowUniversalAccessFromFileURLs(true);

        webview.setBrowserAgent(new BrowserAgent() {
            @Override
            public void onProgressUpdated(WebView view, int progress) {
                progressbar.setProgressValue(progress);
                if (progress < 100 && progress > 0 && progressbar.getVisibility() == ProgressBar.HIDE) {
                    progressbar.setIndeterminate(true);
                    progressbar.setVisibility(ProgressBar.VISIBLE);
                }
                if (progress == 100) {
                    progressbar.setVisibility(ProgressBar.HIDE);
                }
            }

            @Override
            public boolean onPickFiles(WebView webView, AsyncCallback<Uri[]> urisCallback, PickFilesParams params) {
                if (mUrisCallback != null)
                    mUrisCallback.onReceive(null);

                mUrisCallback = urisCallback;
                getFileUris().launch("*/*");
                return true;
            }
        });

        // https://developer.android.com/guide/webapps/webview#HandlingNavigation
        webview.setWebAgent(new MyWebViewClient(this));

        // init webUrl with urlStack.pop() if non-empty, else load from default in DB
        if (urlStack.isEmpty()) {
            webUrl = ConfigurationUtils.getWebPage();
            urlStack.push(webUrl);
        }
        else {
            webUrl = urlStack.pop();
        }
        webview.load(webUrl);
    }

    @Override
    public void onActive() {
        super.onActive();

        // setup keyPress listener - must re-enable every time on resume
        webview.setFocusable(Component.FOCUS_ADAPTABLE);
        webview.requestFocus();
        webview.setKeyEventListener(this);
    }

    /**
     * Init webView so it download root url stored in DB on next init
     */
    public static void initWebView() {
        urlStack.clear();
    }

    /**
     * Push the last loaded/user clicked url page to the urlStack for later retrieval in onStart(),
     * allow same web page to be shown when user slides and returns to the webView
     *
     * @param url loaded/user clicked url
     */
    public void addLastUrl(String url) {
        urlStack.push(url);
        isLoadFromStack = false;
    }

    /**
     * Opens a FileChooserDialog to let the user pick files for upload
     */
    private ActivityResultLauncher<String> getFileUris() {
        return registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
            if (uris != null) {
                if (mUrisCallback == null)
                    return;

                Uri[] uriArray = new Uri[uris.size()];
                uriArray = uris.toArray(uriArray);

                mUrisCallback.onReceive(uriArray);
                mUrisCallback = null;
            }
            else {
                aTalkApp.showToastMessage(ResourceTable.String_file_does_not_exist);
            }
        });
    }

    public static PixelMap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            ImageSource.SourceOptions srcOptions = new ImageSource.SourceOptions();
            ImageSource.DecodingOptions decOptions = new ImageSource.DecodingOptions();
            ImageSource.create(input, srcOptions).createPixelmap(decOptions);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Handler for user enter Back Key
     * User Back Key entry will return to previous web access pages until root; before return to caller
     *
     * @param component Component View
     * @param event the key Event
     *
     * @return true if process
     */
    @Override
    public boolean onKeyEvent(Component component, KeyEvent event) {
        if (event.isKeyDown()) {
            int keyCode = event.getKeyCode();
            // android OS will not pass in KEYCODE_MENU???
            if (keyCode == KeyEvent.KEY_MENU) {
                webview.load("javascript:MovimTpl.toggleMenu()");
                return true;
            }

            if (keyCode == KeyEvent.KEY_BACK) {
                if (!isLoadFromStack && webview.canScroll(Component.DRAG_UP)) {
                    // Remove the last saved/displayed url push in addLastUrl, so an actual previous page is shown
                    if (!urlStack.isEmpty())
                        urlStack.pop();
                    webview.pageUp(false);
                    return true;
                }
                // else continue to reload url from urlStack if non-empty.
                else if (!urlStack.isEmpty()) {
                    isLoadFromStack = true;
                    webUrl = urlStack.pop();
                    Timber.w("urlStack pop(): %s", webUrl);
                    webview.load(webUrl);
                    return true;
                }
            }
        }
        return false;
    }
}