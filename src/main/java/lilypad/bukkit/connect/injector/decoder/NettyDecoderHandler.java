package lilypad.bukkit.connect.injector.decoder;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import lilypad.bukkit.connect.injector.handler.NettyInjectHandler;


import java.util.List;

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
