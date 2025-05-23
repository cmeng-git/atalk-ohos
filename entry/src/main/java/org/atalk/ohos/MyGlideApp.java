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
package org.atalk.ohos;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import org.atalk.persistance.FileBackend;

import java.io.File;

import ohos.agp.components.Image;
import ohos.app.Context;

@GlideModule
public class MyGlideApp extends AppGlideModule
{
    /**
     * Display file as thumbnail preview if it is a media file
     *
     * @param viewHolder image preview holder
     * @param file the image file
     * @param isHistory History file image view is only a small preview
     */
    public static void loadImage(Image viewHolder, File file, Boolean isHistory)
    {
        if (!file.exists()) {
            viewHolder.setPixelMap(null);
            return;
        }

        Context ctx = aTalkApp.getInstance();
        if (FileBackend.isMediaFile(file)) {
            // History file image view is only a small preview (192 px max height)
            if (isHistory) {
                GlideApp.with(ctx)
                        .load(file)
                        .override(640, 192)
                        .placeholder(ResourceTable.Media_ic_file_open)
                        .into(viewHolder);
            }
            // sent or received file will be large image
            else {
                GlideApp.with(ctx)
                        .load(file)
                        .override(1280, 608)
                        .error(ResourceTable.Media_ic_file_open)
                        .into(viewHolder);
            }
        }
        else {
            viewHolder.setPixelMap(ResourceTable.Media_ic_file_open);
        }
    }
}
