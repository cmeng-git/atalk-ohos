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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Executors;

import ohos.agp.components.Text;
import ohos.media.image.ImageSource;
import ohos.media.image.PixelMap;
import ohos.media.image.common.PixelFormat;
import ohos.media.image.common.Size;

import org.atalk.ohos.BaseAbility;

/**
 * Utility class that implements <code>Html.ImageGetter</code> interface and can be used
 * to display url images in <code>Text</code> through the HTML syntax.
 *
 * @author Eng Chong Meng
 */
public class XhtmlImageParser implements Html.ImageGetter {
    private final Text mText;
    private final String XhtmlString;

    /**
     * Construct the XhtmlImageParser which will execute in async and refresh the Text
     * Usage: htmlTextView.setText(Html.fromHtml(HtmlString, new XhtmlImageParser(htmlTextView, HtmlString), null));
     *
     * @param tv the textView to be populated with return result
     * @param str the xhtml string
     */
    public XhtmlImageParser(Text tv, String str) {
        mText = tv;
        XhtmlString = str;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PixelMap getDrawable(String source) {
        HttpGetDrawableTask httpGetDrawableTask = new HttpGetDrawableTask();
        httpGetDrawableTask.execute(source);
        return null;
    }

    /**
     * Execute fetch url image as async task: else 'android.os.NetworkOnMainThreadException'
     */
    public class HttpGetDrawableTask {
        public void execute(String... params) {
            Executors.newSingleThreadExecutor().execute(() -> {
                String urlString = params[0];
                final PixelMap urlDrawable = getDrawable(urlString);

                BaseAbility.runOnUiThread(() -> {
                    if (urlDrawable != null) {
                        mText.setText(Html.fromHtml(XhtmlString, Html.FROM_HTML_MODE_LEGACY, source -> urlDrawable, null));
                    }
                    else {
                        mText.setText(Html.fromHtml(XhtmlString, Html.FROM_HTML_MODE_LEGACY, null, null));
                    }
                    mText.setMovementMethod(LinkMovementMethod.getInstance());
                });
            });
        }

        /***
         * Get the Drawable from the given URL (change to secure https if necessary)
         * aTalk/android supports only secure https connection
         *
         * @param urlString url string
         * @return drawable
         */
        public PixelMap getPixelMap(String urlString) {
            try {
                // urlString = "https://cmeng-git.github.io/atalk/img/09.atalk_avatar.png";
                urlString = urlString.replace("http:", "https:");

                URL sourceURL = new URL(urlString);
                URLConnection urlConnection = sourceURL.openConnection();
                urlConnection.connect();
                InputStream inputStream = urlConnection.getInputStream();

                ImageSource.SourceOptions srcOptions = new ImageSource.SourceOptions();
                ImageSource imageSrc = ImageSource.create(inputStream, srcOptions);
                Size imageSize = imageSrc.getImageInfo().size;

                ImageSource.DecodingOptions decodingOpts = new ImageSource.DecodingOptions();
                decodingOpts.desiredSize = new Size(imageSize.width, imageSize.height);
                decodingOpts.desiredPixelFormat = PixelFormat.ARGB_8888;
                decodingOpts.rotateDegrees = 0;
                return imageSrc.createPixelmap(decodingOpts);
            } catch (IOException e) {
                return null;
            }
        }
    }
}
