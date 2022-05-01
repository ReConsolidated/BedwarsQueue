package io.github.reconsolidated.bedwarsqueue;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.net.InetSocketAddress;

public class Utils {

    public static ServerInfo getServerByPort(int port) {
        for (ServerInfo info : ProxyServer.getInstance().getServersCopy().values()) {
            InetSocketAddress address = (InetSocketAddress) info.getSocketAddress();
            if (address.getPort() == port) {
                return info;
            }
        }
        return null;
    }
}
