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

import ohos.aafwk.ability.Ability;
import ohos.app.Context;
import ohos.media.image.PixelMap;

import net.java.sip.communicator.service.protocol.ProtocolNames;

import org.atalk.ohos.ResourceTable;
import org.atalk.ohos.aTalkApp;
import org.atalk.ohos.util.AppImageUtil;

/**
 * Class containing utility methods that may concern accounts. Provide default values for some fields.
 *
 * @author Eng Chong Meng
 */
public class AccountUtil
{
    /**
     * Returns {@link ohos.media.codec.PixelMap} representing default presence status for specified <code>protocolName</code>
     *
     * @param context {@link Context} of current {@link Ability}
     * @param protocolName the name of the protocol
     * @return {@link PixelMap} for default presence status or <code>null</code> otherwise
     */
    static public PixelMap getDefaultPresenceIcon(Context context, String protocolName)
    {
        if (protocolName.equals(ProtocolNames.JABBER)) {
            return AppImageUtil.getPixelMap(aTalkApp.getInstance(), ResourceTable.Media_default_jabber_status);
        }
        return null;
    }

    /**
     * Returns the default avatar {@link PixelMap}
     *
     * @param context current application {@link Context}
     * @return the default avatar {@link PixelMap}
     */
    static public PixelMap getDefaultAvatarIcon(Context context)
    {
        return AppImageUtil.getPixelMap (context, ResourceTable.Media_avatar);
    }
}
