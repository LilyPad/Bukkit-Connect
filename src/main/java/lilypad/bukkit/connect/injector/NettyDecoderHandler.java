package lilypad.bukkit.connect.injector;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import net.minecraft.util.io.netty.buffer.ByteBuf;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import net.minecraft.util.io.netty.handler.codec.ByteToMessageDecoder;

public class NettyDecoderHandler extends ByteToMessageDecoder {

	private NettyInjectHandler handler;
	private ByteToMessageDecoder oldDecoder;
	private Method oldDecoderMethod;
	private boolean enabled = true;
	
	public NettyDecoderHandler(NettyInjectHandler handler, ByteToMessageDecoder oldDecoder) throws Exception {
		this.handler = handler;
		this.oldDecoder = oldDecoder;
		this.oldDecoderMethod = this.oldDecoder.getClass().getDeclaredMethod("decode", ChannelHandlerContext.class, ByteBuf.class, List.class);
		this.oldDecoderMethod.setAccessible(true);
	}

	@Override
	protected void decode(ChannelHandlerContext context, ByteBuf buf, List<Object> out) throws Exception {
		if(this.enabled && this.handler.isEnabled()) {
			// Call Old Decode
			this.oldDecoderMethod.invoke(this.oldDecoder, context, buf, out);
			if(out.isEmpty()) {
				return;
			}
			// Iterate Out
			Iterator<Object> iterator = out.iterator();
			do {
				this.handler.packetReceived(this, context, iterator.next());
			} while(this.enabled && iterator.hasNext());
		} else {
			// Call Old Decode
			this.oldDecoderMethod.invoke(this.oldDecoder, context, buf, out);
		}
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
