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
package org.atalk.ohos.util;

import org.minidns.DnsClient;
import org.minidns.dnsserverlookup.AbstractDnsServerLookupMechanism;
import org.minidns.dnsserverlookup.AndroidUsingExec;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import ohos.app.Context;
import ohos.net.ConnectionProperties;
import ohos.net.NetCapabilities;
import ohos.net.NetHandle;
import ohos.net.NetManager;
import ohos.net.RouteInfo;

/**
 * A DNS server lookup mechanism using Android's Link Properties method available on Android API 21 or higher.
 * Use {@link #setup(Context)} to setup this mechanism.
 * Requires the ACCESS_NETWORK_STATE permission.
 *
 */
public class AndroidUsingLinkProperties extends AbstractDnsServerLookupMechanism
{
    private final NetManager netManager;

    /**
     * Setup this DNS server lookup mechanism. You need to invoke this method only once,
     * ideally before you do your first DNS lookup.
     *
     * @param context a Context instance.
     * @return the instance of the newly setup mechanism
     */
    public static AndroidUsingLinkProperties setup(Context context)
    {
        AndroidUsingLinkProperties androidUsingLinkProperties = new AndroidUsingLinkProperties(context);
        DnsClient.addDnsServerLookupMechanism(androidUsingLinkProperties);
        return androidUsingLinkProperties;
    }

    public AndroidUsingLinkProperties(Context context)
    {
        super(AndroidUsingLinkProperties.class.getSimpleName(), AndroidUsingExec.PRIORITY - 1);
        netManager = NetManager.getInstance(context);
    }

    @Override
    public boolean isAvailable()
    {
		return true;
    }

    /**
     * Get DnsServerAddresses; null if unavailable so DnsClient#findDNS() will proceed with next available mechanism .
     *
     * @return servers list or null
     */
    @Override
    public List<String> getDnsServerAddresses()
    {
        final List<String> servers = new ArrayList<>();
        final NetHandle netHandle = netManager.getDefaultNet();
        if (netHandle == null) {
            return null;
        }

        ConnectionProperties connectionProperties = netManager.getConnectionProperties(netHandle);
        if (connectionProperties == null) {
            return null;
        }

        int vpnOffset = 0;
        NetCapabilities nc = netManager.getNetCapabilities(netHandle);
        final boolean isVpn = nc != null && nc.hasCap(NetCapabilities.BEARER_VPN);
        final List<String> v4v6Servers = getIPv4First(connectionProperties.getDnsServers());
        // Timber.d("hasDefaultRoute: %s activeNetwork: %s || isVpn: %s || IP: %s",
        //        hasDefaultRoute(linkProperties), network, isVpn, toListOfStrings(linkProperties.getDnsServers()));

        if (isVpn) {
            servers.addAll(0, v4v6Servers);
            // vpnOffset += v4v6Servers.size();
        }
        // Prioritize the DNS servers of links which have a default route
        else if (hasDefaultRoute(connectionProperties)) {
            servers.addAll(vpnOffset, v4v6Servers);
        }
        else {
            servers.addAll(v4v6Servers);
        }

        // Timber.d("dns Server Addresses (linkProperty): %s", servers);
        return servers;
    }

    /**
     * Sort and return the list of given InetAddress in IPv4-IPv6 order, and keeping original in order
     *
     * @param in list of unsorted InetAddress
     * @return sorted vp4 vp6 IP addresses
     */
    private static List<String> getIPv4First(List<InetAddress> in)
    {
        List<String> out = new ArrayList<>();
        int i = 0;
        for (InetAddress addr : in) {
            if (addr instanceof Inet4Address) {
                out.add(i++, addr.getHostAddress());
            }
            else {
                out.add(addr.getHostAddress());
            }
        }
        return out;
    }

    private static boolean hasDefaultRoute(ConnectionProperties linkProperties)
    {
        for (RouteInfo route : linkProperties.getRoutes()) {
            if (route.isDefaultRoute()) {
                return true;
            }
        }
        return false;
    }
}
