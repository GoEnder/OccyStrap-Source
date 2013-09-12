/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.md_5.bungee.api.query;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Data;

/**
 *
 * @author Robin
 */
@Data
public class FullQueryResponse
{
    private final ImmutableMap<String, String> keyValues;
    private final ImmutableList<String> players;
}
