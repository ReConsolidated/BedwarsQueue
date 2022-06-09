package io.github.reconsolidated.bedwarsqueue;

import io.github.reconsolidated.jediscommunicator.JedisCommunicator;
import io.github.reconsolidated.jediscommunicator.JedisServerInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ServersManager implements Runnable{
    private final Map<String, List<JedisServerInfo>> servers;
    private final JedisCommunicator jedis;

    public ServersManager(BedwarsQueue plugin) {
        servers = new HashMap<>();
        jedis = new JedisCommunicator();

        plugin.getProxy().getScheduler().schedule(plugin, this, 500, 500, TimeUnit.MILLISECONDS);
    }

    public List<JedisServerInfo> getServers(String type) {
        return servers.get(type);
    }

    public void update(String type) {
        servers.put(type, jedis.getServers(type));
    }

    @Override
    public void run() {
        for (String type : servers.keySet()) {
            update(type);
        }
    }
}
