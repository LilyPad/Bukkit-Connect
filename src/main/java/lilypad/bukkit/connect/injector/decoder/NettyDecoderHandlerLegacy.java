package lilypad.bukkit.connect.injector.decoder;

import lilypad.bukkit.connect.injector.handler.NettyInjectHandlerLegacy;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import net.minecraft.util.io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class NettyDecoderHandlerLegacy extends MessageToMessageDecoder<Object> {

	private NettyInjectHandlerLegacy handler;
	private boolean enabled = true;

	public NettyDecoderHandlerLegacy(NettyInjectHandlerLegacy handler) throws Exception {
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
