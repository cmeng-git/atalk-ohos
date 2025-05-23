/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.ohos.gui.util;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.core.content.res.ResourcesCompat;

import net.java.sip.communicator.service.protocol.ProtocolNames;

import org.atalk.ohos.R;
import org.atalk.ohos.aTalkApp;

/**
 * Class containing utility methods that may concern accounts. Provide default values for some fields.
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class AccountUtil
{
    /**
     * Returns {@link Drawable} representing default presence status for specified <code>protocolName</code>
     *
     * @param context {@link Context} of current {@link android.app.Activity}
     * @param protocolName the name of the protocol
     * @return {@link Drawable} for default presence status or <code>null</code> otherwise
     */
    static public Drawable getDefaultPresenceIcon(Context context, String protocolName)
    {
        if (protocolName.equals(ProtocolNames.JABBER)) {
            return new BitmapDrawable(aTalkApp.getAppResources(),
                    BitmapFactory.decodeResource(context.getResources(), R.drawable.default_jabber_status));
        }
        return null;
    }

    /**
     * Returns the default avatar {@link Drawable}
     *
     * @param context current application {@link Context}
     * @return the default avatar {@link Drawable}
     */
    static public LayerDrawable getDefaultAvatarIcon(Context context)
    {
        return (LayerDrawable) ResourcesCompat.getDrawable(context.getResources(), R.drawable.avatar_layer_drawable, null);
    }
}
