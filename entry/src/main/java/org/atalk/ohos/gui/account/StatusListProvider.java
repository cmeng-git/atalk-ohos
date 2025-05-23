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
package org.atalk.ohos.gui.account;

import java.util.Iterator;
import java.util.List;

import ohos.agp.components.BaseItemProvider;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.Image;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.Text;
import ohos.app.Context;
import ohos.media.image.PixelMap;

import net.java.sip.communicator.service.protocol.PresenceStatus;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.util.AppImageUtil;

public class StatusListProvider extends BaseItemProvider {
    private final LayoutScatter layoutScatter;

    private Image statusIconView;

    private final int mResId;
    private final List<PresenceStatus> mStatusList;

    /**
     * Creates new instance of {@link StatusListProvider}
     *
     * @param presenceStatuses {@link Iterator} for a set of {@link PresenceStatus}
     */
    public StatusListProvider(Context context, int resId, List<PresenceStatus> presenceStatuses) {
        mResId = resId;
        mStatusList = presenceStatuses;
        layoutScatter = LayoutScatter.getInstance(context);
    }

    @Override
    public int getCount() {
        return mStatusList.size();
    }

    @Override
    public PresenceStatus getItem(int idx) {
        return mStatusList.get(idx);
    }

    @Override
    public long getItemId(int idx) {
        return idx;
    }

    public int getItemPosition(PresenceStatus status) {
        return mStatusList.indexOf(status);
    }

    @Override
    public Component getComponent(int position, Component convertView, ComponentContainer parent) {
        final Component statusItemView;

        // Retrieve views
        if (convertView == null) {
            statusItemView = layoutScatter.parse(ResourceTable.Layout_account_presence_status_row, parent, false);
        }
        else {
            statusItemView = convertView;
        }

        statusIconView = statusItemView.findComponentById(ResourceTable.Id_presenceStatusIconView);
        Text statusNameView = statusItemView.findComponentById(ResourceTable.Id_presenceStatusNameView);

        // Set status name
        PresenceStatus presenceStatus = (PresenceStatus) getItem(position);
        String statusName = presenceStatus.getStatusName();
        statusNameView.setText(statusName);

        // Set status icon
        PixelMap presenceIcon = AppImageUtil.pixelMapFromBytes(presenceStatus.getStatusIcon());
        statusIconView.setPixelMap(presenceIcon);
        return statusItemView;
    }

    // @Override
    public Component getDropDownView(int position, Component convertView, ComponentContainer parent) {
        return getComponent(position, convertView, parent);
    }

    public PixelMap getStatusIcon() {
        return statusIconView.getPixelMap();
    }
}
