package lilypad.bukkit.connect.injector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class NettyDecoderHandler extends MessageToMessageDecoder<Object> {

	private NettyInjectHandler handler;
	private boolean enabled = true;
	
	public NettyDecoderHandler(NettyInjectHandler handler) throws Exception {
		this.handler = handler;
	}

	@Override
	protected void decode(ChannelHandlerContext context, Object packet, List<Object> out) throws Exception {
		if (this.enabled && this.handler.isEnabled()) {
			this.handler.packetReceived(this, context, packet);
		}
		out.add(packet);
		if (!this.enabled) {
			context.pipeline().remove("lilypad_decoder");
		}
	}
	
	public void disable() {
		this.enabled = false;
	}

}
