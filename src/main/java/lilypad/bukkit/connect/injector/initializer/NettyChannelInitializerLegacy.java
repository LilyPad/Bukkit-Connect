package lilypad.bukkit.connect.injector.initializer;

import lilypad.bukkit.connect.injector.decoder.NettyDecoderHandlerLegacy;
import lilypad.bukkit.connect.injector.handler.NettyInjectHandlerLegacy;
import net.minecraft.util.io.netty.channel.Channel;
import net.minecraft.util.io.netty.channel.ChannelInitializer;
import net.minecraft.util.io.netty.channel.socket.SocketChannel;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;

public class NettyChannelInitializerLegacy extends ChannelInitializer<SocketChannel> {

	private NettyInjectHandlerLegacy handler;
	private ChannelInitializer<SocketChannel> oldChildHandler;
	private Method oldChildHandlerMethod;

	public NettyChannelInitializerLegacy(NettyInjectHandlerLegacy handler, ChannelInitializer<SocketChannel> oldChildHandler) throws Exception {
		this.handler = handler;
		this.oldChildHandler = oldChildHandler;
		this.oldChildHandlerMethod = this.oldChildHandler.getClass().getDeclaredMethod("initChannel", Channel.class);
		this.oldChildHandlerMethod.setAccessible(true);
	}
	
	@Override
	protected void initChannel(SocketChannel channel) throws Exception {
		// Call Old InitChannel
		this.oldChildHandlerMethod.invoke(this.oldChildHandler, channel);
		// Add Handlers
		if (this.handler.isEnabled()) {
			if (channel.pipeline().names().contains("legacy_query") && Bukkit.getServer().getPluginManager().getPlugin("ProtocolSupport") == null) {
				channel.pipeline().remove("legacy_query");
			}
			channel.pipeline().addAfter("decoder", "lilypad_decoder", new NettyDecoderHandlerLegacy(this.handler));
		}
	}

}
