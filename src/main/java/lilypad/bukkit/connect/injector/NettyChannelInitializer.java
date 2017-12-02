package lilypad.bukkit.connect.injector;

import org.bukkit.Bukkit;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.lang.reflect.Method;

public class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {

	private NettyInjectHandler handler;
	private ChannelInitializer<SocketChannel> oldChildHandler;
	private Method oldChildHandlerMethod;
	
	public NettyChannelInitializer(NettyInjectHandler handler, ChannelInitializer<SocketChannel> oldChildHandler) throws Exception {
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
			channel.pipeline().addAfter("decoder", "lilypad_decoder", new NettyDecoderHandler(this.handler));
		}
	}

}
