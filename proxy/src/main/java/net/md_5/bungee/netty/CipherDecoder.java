package net.md_5.bungee.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import org.bouncycastle.crypto.BufferedBlockCipher;

public class CipherDecoder extends MessageToMessageDecoder<ByteBuf>
{

    private final CipherBase cipher;

    public CipherDecoder(BufferedBlockCipher cipher)
    {
        this.cipher = new CipherBase( cipher );
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception
    {
        out.add( cipher.cipher( ctx, msg ) );
    }
}
