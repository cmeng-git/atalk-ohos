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
import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.Text;
import ohos.agp.utils.Color;

import org.atalk.impl.neomedia.codec.video.CodecInfo;
import org.atalk.ohos.BaseAbility;
import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.gui.util.ThemeHelper;
import org.atalk.ohos.gui.util.ThemeHelper.Theme;

import java.util.ArrayList;

/**
 * Ability that lists video <code>Codec</code>s available in the system.
 * <p>
 * Meaning of the colors:</br><br/>
 * * blue - codec will be used in call<br/>
 * * white / black - one of the codecs for particular media type, but it won't be used
 * as there is another codec before it on the list<br/>
 * * grey500 - codec is banned and won't be used<br/>
 * <p>
 * Click on codec to toggle it's banned state. Changes are not persistent between
 * aTalk restarts so restarting aTalk restores default values.
 *
 * @author Eng Chong Meng
 */
public class MediaCodecList extends BaseAbility implements ListContainer.ItemClickedListener {
    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_list_layout);

        ListContainer list = findComponentById(ResourceTable.Id_list);
        list.setItemProvider(new MediaCodecProvider());
        list.setItemClickedListener(this);
    }

    @Override
    public void onItemClicked(ListContainer parent, Component component, int position, long id) {
        MediaCodecProvider adapter = (MediaCodecProvider) parent.getItemProvider();
        CodecInfo codec = (CodecInfo) adapter.getItem(position);

        // Toggle codec banned state
        codec.setBanned(!codec.isBanned());
        adapter.notifyDataChanged();
    }

    class MediaCodecProvider extends BaseItemProvider {
        private final ArrayList<CodecInfo> codecs;

        MediaCodecProvider() {
            codecs = new ArrayList<>(CodecInfo.getSupportedCodecs());
        }

        @Override
        public int getCount() {
            return codecs.size();
        }

        @Override
        public Object getItem(int position) {
            return codecs.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public Component getComponent(int position, Component convertView, ComponentContainer parent) {
            Text row = (Text) convertView;
            if (row == null) {
                row = (Text) LayoutScatter.getInstance(getContext()).parse(ResourceTable.Layout_simple_list_item, parent, false);
            }

            row.setTextSize(15, Text.TextSizeType.PX);
            CodecInfo codec = codecs.get(position);
            String codecStr = codec.toString();
            row.setText(codecStr);

            int color = codec.isBanned() ? ResourceTable.Color_grey500 : ResourceTable.Color_textColorWhite;
            if (ThemeHelper.isAppTheme(Theme.LIGHT)) {
                color = codec.isBanned() ? ResourceTable.Color_grey500 : ResourceTable.Color_textColorBlack;
            }
            if (codec.isNominated()) {
                color = ResourceTable.Color_blue;
            }
            row.setTextColor(new Color(getColor(color)));
            return row;
        }
    }
}
