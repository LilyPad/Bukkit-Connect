package lilypad.bukkit.connect.netty;

import io.netty.channel.ChannelHandlerContext;

public interface NettyInjectHandler {

	public void packetReceived(NettyDecoderHandler handler, ChannelHandlerContext context, Object object) throws Exception;

	public boolean isEnabled();
	
}
