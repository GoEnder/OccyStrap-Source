/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.md_5.bungee.api.event;

import java.net.InetAddress;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.query.BriefQueryResponse;


/**
 * Called when the proxy is queried through the query protocol.
 */
@Data
@AllArgsConstructor
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false)
public class BriefQueryEvent extends Event
{

    /**
     * The connection asking for a query response.
     */
    private final InetAddress from;
    /**
     * The data to respond with.
     */
    private BriefQueryResponse response;
}
