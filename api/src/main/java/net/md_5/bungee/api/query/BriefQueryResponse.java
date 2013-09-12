/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.md_5.bungee.api.query;

import lombok.Data;

/**
 *
 * @author Robin
 */
@Data
public class BriefQueryResponse
{
    /**
     * Server MOTD.
     */
    private final String motd;
    /**
     * Gametype, usually hardcoded to SMP.
     */
    private final String gametype;
    /**
     * Main map name.
     */
    private final String map;
    /**
     * Current amount of players on the server.
     */
    private final String numPlayers;
    /**
     * Max amount of players the server will allow.
     */
    private final String maxPlayers;
    /**
     * Server port.
     */
    private final short hostport;
    /**
     * The IP the server is listening on.
     */
    private final String hostip;
}
