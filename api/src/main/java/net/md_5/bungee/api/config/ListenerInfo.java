package net.md_5.bungee.api.config;

import java.net.InetSocketAddress;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.tab.TabListHandler;

/**
 * Class representing the configuration of a server listener. Used for allowing
 * multiple listeners on different ports.
 */
@Data
@RequiredArgsConstructor
public class ListenerInfo
{

    /**
     * BungeeCord non-modified constructor. Provided for plugin compability.
     * @param host 
     * @param motd
     * @param maxPlayers
     * @param tabListSize
     * @param defaultServer
     * @param fallbackServer
     * @param forceDefault
     * @param forcedHosts
     * @param tabList
     * @param setLocalAddress 
     */
    public ListenerInfo( InetSocketAddress host, String motd, int maxPlayers, int tabListSize, String defaultServer, String fallbackServer, boolean forceDefault, Map<String, String> forcedHosts, Class<? extends TabListHandler> tabList, boolean setLocalAddress )
    {
        this(host, false, 25565, motd, maxPlayers, tabListSize, defaultServer, fallbackServer, forceDefault, forcedHosts, tabList, setLocalAddress);
    }

    /**
     * Host to bind to.
     */
    private final InetSocketAddress host;
    /**
     * Enable query protocol.
     */
    private final boolean enableQuery;
    /**
     * Port for query protocol.
     */
    private final int queryPort;
    /**
     * Displayed MOTD.
     */
    private final String motd;
    /**
     * Max amount of slots displayed on the ping page.
     */
    private final int maxPlayers;
    /**
     * Number of players to be shown on the tab list.
     */
    private final int tabListSize;
    /**
     * Name of the server which users will be taken to by default.
     */
    private final String defaultServer;
    /**
     * Name of the server which users will be taken when current server goes
     * down.
     */
    private final String fallbackServer;
    /**
     * Whether reconnect locations will be used, or else the user is simply
     * transferred to the default server on connect.
     */
    private final boolean forceDefault;
    /**
     * A list of host to server name mappings which will force a user to be
     * transferred depending on the host they connect to.
     */
    private final Map<String, String> forcedHosts;
    /**
     * Class used to build tab lists for this player.
     */
    private final Class<? extends TabListHandler> tabList;
    /**
     * Whether to set the local address when connecting to servers.
     */
    private final boolean setLocalAddress;
}
