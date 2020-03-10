package lilypad.bukkit.connect.injector.handler;


import lilypad.bukkit.connect.injector.decoder.NettyDecoderHandlerLegacy;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;

public interface NettyInjectHandlerLegacy {

	void packetReceived(NettyDecoderHandlerLegacy handler, ChannelHandlerContext context, Object object) throws Exception;

	boolean isEnabled();
	
}
