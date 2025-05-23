/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.util;

import ohos.media.image.PixelMap;

import org.apache.http.util.TextUtils;
import org.atalk.ohos.aTalkApp;

import timber.log.Timber;

/**
 * Utility class that implements <code>Html.ImageGetter</code> interface and can be used to display images in
 * <code>Text</code> through the HTML syntax.<br/>
 * Source image URI should be formatted as follows:<br/>
 * <br/>
 * atalk.resource://{Integer drawable id}, example: atalk.resource://2130837599 <br/>
 * <br/>
 * This format is used by Android <code>ResourceManagementService</code> to return image URLs.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class HtmlImageGetter implements Html.ImageGetter
{
    /**
     * {@inheritDoc}
     */
    @Override
    public PixelMap getDrawable(String source)
    {
        try {
            // Image resource id is returned here in form:
            // atalk.resource://{Integer drawable id} e.g.: atalk.resource://2130837599
            String resIdStr = source.replaceAll(".*?//(\\d+)", "$1");
            if (!source.equals(resIdStr) && !TextUtils.isEmpty(resIdStr)) {
                Integer resId = Integer.parseInt(resIdStr);
                // Gets application global bitmap cache
                PixelMapCache cache = aTalkApp.getImageCache();
                return cache.getPixelMapFromMemCache(resId);
            }
        } catch (IndexOutOfBoundsException | NumberFormatException | Resources.NotFoundException e) {
            // Invalid string format for source.substring(17); Error parsing Integer.parseInt(source.substring(17));
            // Resource for given id is not found
            Timber.e(e, "Error parsing: %s", source);
        }
        return null;
    }
}
