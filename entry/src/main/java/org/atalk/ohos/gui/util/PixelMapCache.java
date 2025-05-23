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

import ohos.media.image.PixelMap;
import ohos.utils.LruBuffer;

import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.util.AppImageUtil;

/**
 * Implements bitmap cache using <code>LruBuffer</code> utility class. Single cache instance uses
 * up to 1/8 of total runtime memory available.
 *
 * @author Eng Chong Meng
 */
public class PixelMapCache {
    private final LruBuffer<String, PixelMap> cache;

    /**
     * Creates new instance of <code>PixelMapCache</code>.
     */
    public PixelMapCache() {
        // Get max available VM memory, exceeding this amount will throw an OutOfMemory exception.
        // Stored in kilobytes as LruBuffer takes an int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        cache = new LruBuffer<String, PixelMap>(cacheSize) {
            public int sizeOf(String key, PixelMap pixelMap) {
                long byteSize = pixelMap.getPixelBytesNumber();
                return (int) byteSize / 1024;
            }
        };
    }

    /**
     * Gets cached <code>PixelMap</code> for given <code>resId</code>. If it doesn't exist in the cache it will be loaded and stored for later
     * use.
     *
     * @param resId PixelMap resource Id
     *
     * @return <code>PixelMap</code> for given <code>resId</code>
     */
    public PixelMap getPixelMapFromMemCache(Integer resId) {
        String key = "res:" + resId;
        // Check for cached pixelMap
        PixelMap img = cache.get(key);
        // Eventually loads the pixelMap
        if (img == null) {
            // Load and store the pixelMap
            img = AppImageUtil.getPixelMap(aTalkApp.getInstance(), resId);
            if (img != null)
                cache.put(key, img);
            else
                return null;
        }
        return cache.get(key);
    }

    /**
     * Gets bitmap from the cache.
     *
     * @param key PixelMap key string.
     *
     * @return PixelMap from the cache if it exists or <code>null</code> otherwise.
     */
    public PixelMap getPixelMapFromMemCache(String key) {
        return cache.get(key);
    }

    /**
     * Puts given <code>PixelMap</code> to the cache.
     *
     * @param key PixelMap key string.
     * @param bmp the <code>PixelMap</code> to be cached.
     */
    public void cacheImage(String key, PixelMap bmp) {
        cache.put(key, bmp);
    }
}
