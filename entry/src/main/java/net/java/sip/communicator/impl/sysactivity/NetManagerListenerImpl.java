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
package net.java.sip.communicator.impl.sysactivity;

import ohos.net.ConnectionProperties;
import ohos.net.NetCapabilities;
import ohos.net.NetHandle;
import ohos.net.NetManager;
import ohos.net.NetSpecifier;
import ohos.net.NetStatusCallback;

import net.java.sip.communicator.service.sysactivity.event.SystemActivityEvent;

import org.atalk.ohos.aTalkApp;

/**
 * Listens callback from netManager to get notified for network changes.
 *
 * @author Eng Chong Meng
 */
public class NetManagerListenerImpl extends NetStatusCallback
        implements SystemActivityManager {
    /**
     * The only instance of this impl.
     */
    private static NetManagerListenerImpl netManagerListenerImpl;

    private NetManager netManager;
    NetStatusCallback netStatusCallback;
    /**
     * Whether we are working.
     */
    private boolean connected = false;

    /**
     * Gets the instance of <code>NetManagerListenerImpl</code>.
     *
     * @return the NetManagerListenerImpl.
     */
    public static NetManagerListenerImpl getInstance() {
        if (netManagerListenerImpl == null)
            netManagerListenerImpl = new NetManagerListenerImpl();

        return netManagerListenerImpl;
    }

    /**
     * Starts
     */
    public void start() {
        netManager = NetManager.getInstance(aTalkApp.getInstance());
        NetSpecifier netSpecifier = new NetSpecifier.Builder()
                .addCapability(NetCapabilities.BEARER_WIFI)
                .addCapability(NetCapabilities.BEARER_ETHERNET)
                .addCapability(NetCapabilities.BEARER_CELLULAR)
                .build();

        netStatusCallback = new NetStatusCallback() {
            @Override
            public void onConnectionPropertiesChanged(NetHandle handle, ConnectionProperties connectionProperties) {
                SystemActivityEvent evt = new SystemActivityEvent(
                        SysActivityActivator.getSystemActivityService(), SystemActivityEvent.EVENT_NETWORK_CHANGE);

                SysActivityActivator.getSystemActivityService().fireSystemActivityEvent(evt);
            }
        };

        netManager.addNetStatusCallback(netSpecifier, netStatusCallback);
        connected = true;
    }

    /**
     * Stops.
     */
    public void stop() {
        netManager.removeNetStatusCallback(netStatusCallback);
        connected = false;
    }

    /**
     * Whether the underlying implementation is currently connected and working.
     *
     * @return whether we are connected and working.
     */
    public boolean isConnected() {
        return connected;
    }
}
