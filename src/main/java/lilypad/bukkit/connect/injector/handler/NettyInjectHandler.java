package lilypad.bukkit.connect.injector.handler;

import io.netty.channel.ChannelHandlerContext;
import lilypad.bukkit.connect.injector.decoder.NettyDecoderHandler;

public interface NettyInjectHandler {

	void packetReceived(NettyDecoderHandler handler, ChannelHandlerContext context, Object object) throws Exception;

	boolean isEnabled();
	
}
