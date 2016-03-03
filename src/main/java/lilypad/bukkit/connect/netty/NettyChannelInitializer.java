package lilypad.bukkit.connect.netty;

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
		if(this.handler.isEnabled()) {
			channel.pipeline().replace("decoder", "decoder", new NettyDecoderHandler(this.handler, (ByteToMessageDecoder) channel.pipeline().get("decoder")));
		}
	}

}
