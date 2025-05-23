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
package org.atalk.ohos.gui.settings;

import ohos.aafwk.content.Intent;

import org.atalk.impl.neomedia.MediaServiceImpl;
import org.atalk.impl.neomedia.NeomediaActivator;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.account.settings.MediaEncodingAbility;
import org.atalk.ohos.gui.account.settings.MediaEncodingsSlice;
import org.atalk.service.neomedia.codec.EncodingConfiguration;
import org.atalk.service.neomedia.format.MediaFormat;
import org.atalk.util.MediaType;

import java.util.List;

/**
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class EncodingSettings extends BaseAbility {
    public static final String EXTRA_MEDIA_TYPE = "media_type";
    public static final String MEDIA_TYPE_AUDIO = "media_type.AUDIO";
    public static final String MEDIA_TYPE_VIDEO = "media_type.VIDEO";
    private MediaEncodingsSlice mMediaEncodings;
    private MediaType mMediaType;

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        String mediaTypeStr = intent.getStringParam(EXTRA_MEDIA_TYPE);
        if (MEDIA_TYPE_AUDIO.equals(mediaTypeStr)) {
            mMediaType = MediaType.AUDIO;
            setMainTitle(ResourceTable.String_settings_audio_codecs);
        }
        else if (MEDIA_TYPE_VIDEO.equals(mediaTypeStr)) {
            mMediaType = MediaType.VIDEO;
            setMainTitle(ResourceTable.String_settings_video_codec);
        }

        if (savedInstanceState == null) {
            MediaServiceImpl mediaSrvc = NeomediaActivator.getMediaServiceImpl();
            if (mediaSrvc != null) {
                EncodingConfiguration encConfig = mediaSrvc.getCurrentEncodingConfiguration();

                List<MediaFormat> formats = MediaEncodingAbility.getEncodings(encConfig, mMediaType);
                List<String> encodings = MediaEncodingAbility.getEncodingsStr(formats.iterator());
                List<Integer> priorities = MediaEncodingAbility.getPriorities(formats, encConfig);

                mMediaEncodings = MediaEncodingsSlice.newInstance(encodings, priorities);
                getSupportFragmentManager().beginTransaction().add(ResourceTable.Id_content, mMediaEncodings).commit();
            }
        }
        else {
            mMediaEncodings = (MediaEncodingsFragment) getSupportFragmentManager().findFragmentById(android.ResourceTable.Id_content);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBackPressed() {
        MediaServiceImpl mediaSrvc = NeomediaActivator.getMediaServiceImpl();

        if (mediaSrvc != null) {
            MediaEncodingAbility.commitPriorities(
                    NeomediaActivator.getMediaServiceImpl().getCurrentEncodingConfiguration(),
                    mMediaType, mMediaEncodings);
            terminateAbility();
        }
        super.onBackPressed();
    }
}
