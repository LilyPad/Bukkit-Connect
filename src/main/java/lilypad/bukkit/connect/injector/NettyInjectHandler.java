package lilypad.bukkit.connect.injector;

import io.netty.channel.ChannelHandlerContext;

public interface NettyInjectHandler {

    void packetReceived(NettyDecoderHandler handler, ChannelHandlerContext context, Object object);

    boolean isEnabled();

}
