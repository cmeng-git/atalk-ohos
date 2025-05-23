package com.pierfrancescosoffritti.youtubeplayer.player.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import ohos.app.Context;
import ohos.net.NetCapabilities;
import ohos.net.NetHandle;
import ohos.net.NetManager;

public class Utils {

    public static boolean isOnline(Context context) {
        boolean connected = false;

        NetManager netManager = NetManager.getInstance(context);
        NetHandle network = netManager.getDefaultNet();
        if (network != null) {
            NetCapabilities activeNetwork = netManager.getNetCapabilities(network);
            connected = activeNetwork.hasBearer(NetCapabilities.BEARER_ETHERNET);
        }
        return connected;
    }

    public static String readHTMLFromUTF8File(InputStream inputStream) {
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String readLine;
            StringBuilder sb = new StringBuilder();

            while ((readLine = bufferedReader.readLine()) != null) {
                sb.append(readLine).append("\n");
            }
            inputStream.close();
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Can't parse HTML file.");
        }
    }
}
