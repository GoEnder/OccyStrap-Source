/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.md_5.bungee.query;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import lombok.Data;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.BriefQueryEvent;
import net.md_5.bungee.api.event.FullQueryEvent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.query.BriefQueryResponse;
import net.md_5.bungee.api.query.FullQueryResponse;
import net.md_5.bungee.netty.PipelineUtils;

/**
 *
 * @author roblabla
 */
public class QueryServerHandler extends SimpleChannelInboundHandler<DatagramPacket>
{

    private Map<SocketAddress, QueryIdentity> identities = new HashMap<>();
    private long clearedTime = 0;
    private ByteBuf fullReplyCache;
    private long cacheTime = 0;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception
    {
        ListenerInfo info = ctx.channel().attr( PipelineUtils.LISTENER ).get();
        // No virtual host in query protocol. This is the best way, i'm afraid.
        ServerInfo serv = BungeeCord.getInstance().getServerInfo( info.getDefaultServer() );

        cleanChallenges();

        ByteBuf bytes = packet.content();
        byte magic1 = bytes.readByte();
        byte magic2 = bytes.readByte();
        if ( !(magic1 == (byte) 0xFE && magic2 == (byte) 0xFD ) ) // Magic Bytes.
        {
            return;
        }
        byte type = bytes.readByte();
        int sessID = bytes.readInt();
        switch ( type )
        {
            case 0x00:
                if ( !hasChallenge( packet ) )
                {
                    return;
                }
                if ( bytes.isReadable() )
                {
                    ctx.writeAndFlush( getPacket0Full( identities.get( packet.sender() ), info, serv ) );
                } else
                {
                    ctx.writeAndFlush( getPacket0Brief( identities.get( packet.sender() ), info, serv ) );
                }
                break;
            case 0x09:
                QueryIdentity id = new QueryIdentity( packet.sender(), Ints.toByteArray( sessID ), new Random().nextInt() );
                ctx.writeAndFlush( getPacket9Response( id ) ); // not sent ???
                identities.put( packet.sender(), id );
        }
    }

    public void cleanChallenges()
    {
        long i = System.currentTimeMillis();
        if ( i - this.clearedTime > 30_000L )
        {
            this.clearedTime = System.currentTimeMillis();
            Iterator<QueryIdentity> it = identities.values().iterator();
            while ( it.hasNext() )
            {
                QueryIdentity id = it.next();
                if ( i - id.getTimecreated() > 30_000L )
                {
                    it.remove();
                }
            }
        }
    }

    public DatagramPacket getPacket9Response(QueryIdentity resp)
    {
        String challStr = Integer.toString( resp.getChallenge() ) + "\0";
        return new DatagramPacket( Unpooled.copiedBuffer( new byte[] { 0x09 }, resp.sessID, challStr.getBytes() ), resp.addr );
    }

    private boolean hasChallenge(DatagramPacket packet)
    {
        SocketAddress socketaddress = packet.sender();

        if ( !this.identities.containsKey( socketaddress ) )
        {
            return false;
        } else
        {
            int chall = packet.content().readInt();
            return identities.get( socketaddress ).challenge == chall;
        }
    }

    private DatagramPacket getPacket0Full(QueryIdentity identity, ListenerInfo info, ServerInfo serv)
    {
        long currentTime = System.currentTimeMillis();
        if ( currentTime - cacheTime > 5000L )
        {
            this.cacheTime = currentTime;
            FullQueryResponse response = getFullQueryResponse( identity, info, serv);
            fullReplyCache = Unpooled.buffer();
            fullReplyCache.writeByte( 0 );
            fullReplyCache.writeBytes( identity.sessID );
            fullReplyCache.writeBytes( new byte[]
            {
                (byte) 0x73, (byte) 0x70, (byte) 0x6C, (byte) 0x69, (byte) 0x74,
                (byte) 0x6E, (byte) 0x75, (byte) 0x6D, (byte) 0x00, (byte) 0x80,
                (byte) 0x00
            } ); // Padding
            for ( Map.Entry<String, String> entry : response.getKeyValues().entrySet() )
            {
                fullReplyCache.writeBytes( entry.getKey().getBytes() );
                fullReplyCache.writeByte( 0 );
                fullReplyCache.writeBytes( entry.getValue().getBytes() );
                fullReplyCache.writeByte( 0 );
            }
            fullReplyCache.writeByte( 0 );
            fullReplyCache.writeBytes( new byte[]
            {
                (byte) 0x01, (byte) 0x70, (byte) 0x6C, (byte) 0x61, (byte) 0x79,
                (byte) 0x65, (byte) 0x72, (byte) 0x5F, (byte) 0x00, (byte) 0x00
            } ); // Padding
            for ( String p : response.getPlayers() )
            {
                fullReplyCache.writeBytes( p.getBytes() );
                fullReplyCache.writeByte( 0 );
            }
            fullReplyCache.writeByte( 0 );

            return new DatagramPacket( fullReplyCache, identity.addr );

        } else
        {
            fullReplyCache.setBytes( 1, identity.sessID );
            return new DatagramPacket( fullReplyCache, identity.addr );
        }
    }

    private DatagramPacket getPacket0Brief(QueryIdentity identity, ListenerInfo info, ServerInfo serv)
    {
        BriefQueryResponse response = getBriefQueryResponse( identity, info, serv ); // fire an event.
        ByteBuf out = Unpooled.buffer();
        out.writeByte( 0 );
        out.writeBytes( identity.sessID );
        out.writeBytes( response.getMotd().getBytes() );
        out.writeByte( 0 );
        out.writeBytes( response.getGametype().getBytes() );
        out.writeByte( 0 );
        out.writeBytes( response.getMap().getBytes() );
        out.writeByte( 0 );
        out.writeBytes( response.getNumPlayers().getBytes() );
        out.writeByte( 0 );
        out.writeBytes( response.getMaxPlayers().getBytes() );
        out.writeByte( 0 );
        out.writeShort(response.getHostport() );
        out.writeBytes( response.getHostip().getBytes() );
        out.writeByte( 0 );
        return new DatagramPacket( out, identity.addr );
    }

    private String getPluginsValue()
    {
        StringBuilder builder = new StringBuilder( BungeeCord.getInstance().getVersion() );
        if ( BungeeCord.getInstance().getPluginManager().getPlugins().size() > 0 )
        {
            builder.append( ": " );
        }

        for ( Plugin plugin : BungeeCord.getInstance().getPluginManager().getPlugins() )
        {
            builder.append( plugin.getDescription().getName() );
            builder.append( " " );
            builder.append( plugin.getDescription().getVersion() );
            builder.append( "; " );
        }

        if ( BungeeCord.getInstance().getPluginManager().getPlugins().size() > 0 )
        {
            builder.delete( builder.length() - 2, builder.length() );
        }
        return builder.toString();
    }

    private FullQueryResponse getFullQueryResponse(QueryIdentity identity, ListenerInfo info, ServerInfo serv)
    {
        ImmutableMap.Builder<String, String> mapbuilder = ImmutableMap.builder();
        mapbuilder.put( "hostname", serv.getMotd() );
        mapbuilder.put( "gametype", "SMP" );
        mapbuilder.put( "game_id", "MINECRAFT" );
        mapbuilder.put( "version", BungeeCord.getInstance().getGameVersion() );
        mapbuilder.put( "plugins", getPluginsValue() );
        mapbuilder.put( "map", "unknown" );
        mapbuilder.put( "numplayers", Integer.toString( BungeeCord.getInstance().getOnlineCount() ) );
        mapbuilder.put( "maxplayers", Integer.toString( serv.getMaxPlayers() ) );
        mapbuilder.put( "hostport", Integer.toString( info.getHost().getPort() ) );
        mapbuilder.put( "hostip", info.getHost().getAddress().getHostAddress() );
        ImmutableMap map = mapbuilder.build();
        ImmutableList<String> players = ImmutableList.copyOf(
                Collections2.transform( BungeeCord.getInstance().getPlayers(),
                new Function<ProxiedPlayer, String>()
        {
            @Override
            public String apply(ProxiedPlayer p)
            {
                return p.getName();
            }
        } ) );
        FullQueryEvent ev = BungeeCord.getInstance().getPluginManager().callEvent(
                new FullQueryEvent( identity.addr.getAddress(), new FullQueryResponse( map, players ) ) );

        return ev.getResponse();
    }

    private BriefQueryResponse getBriefQueryResponse(QueryIdentity identity, ListenerInfo info, ServerInfo serv)
    {
        BriefQueryEvent ev = BungeeCord.getInstance().getPluginManager().callEvent(
                new BriefQueryEvent( identity.addr.getAddress(), new BriefQueryResponse(
                serv.getMotd(),
                "SMP",
                "unknown",
                Integer.toString( BungeeCord.getInstance().getOnlineCount() ),
                Integer.toString( serv.getMaxPlayers() ),
                (short) info.getHost().getPort(),
                info.getHost().getAddress().getHostAddress() ) ) );
        return ev.getResponse();
    }

    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause )
    {
        cause.printStackTrace();
    }

    @Data
    public class QueryIdentity
    {

        private final InetSocketAddress addr;
        private final byte[] sessID;
        private final int challenge;
        private final long timecreated = System.currentTimeMillis();
    }
}
