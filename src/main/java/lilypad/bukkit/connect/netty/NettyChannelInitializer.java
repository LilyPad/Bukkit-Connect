package lilypad.bukkit.connect.netty;

import java.lang.reflect.Method;

import net.minecraft.util.io.netty.channel.Channel;
import net.minecraft.util.io.netty.channel.ChannelInitializer;
import net.minecraft.util.io.netty.channel.socket.SocketChannel;
import net.minecraft.util.io.netty.handler.codec.ByteToMessageDecoder;

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
