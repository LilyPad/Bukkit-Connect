package lilypad.bukkit.connect.login;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;

import net.minecraft.util.io.netty.channel.AbstractChannel;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import lilypad.bukkit.connect.ConnectPlugin;
import lilypad.bukkit.connect.injector.NettyDecoderHandler;
import lilypad.bukkit.connect.injector.NettyInjectHandler;
import lilypad.bukkit.connect.util.ReflectionUtils;

public class LoginNettyInjectHandler implements NettyInjectHandler {

	private String requestedStateFieldCache;
	private String serverHostFieldCache;
	
	private ConnectPlugin connectPlugin;
	private LoginPayloadCache payloadCache;
	
	public LoginNettyInjectHandler(ConnectPlugin connectPlugin, LoginPayloadCache payloadCache) {
		this.connectPlugin = connectPlugin;
		this.payloadCache = payloadCache;
	}
	
	public void packetReceived(NettyDecoderHandler handler, ChannelHandlerContext context, Object object) throws Exception {
		if(!object.getClass().getSimpleName().startsWith("PacketHandshakingInSetProtocol")) {
			return;
		}
		handler.setEnabled(false);
		
		// Get requested state
		try {
			if(this.requestedStateFieldCache == null) {
				for(Field field : object.getClass().getDeclaredFields()) {
					if(!field.getType().getSimpleName().equals("EnumProtocol")) {
						continue;
					}
					this.requestedStateFieldCache = field.getName();
					break;
				}
			}
			Object requestedStateEnum = ReflectionUtils.getPrivateField(object.getClass(), object, Object.class, this.requestedStateFieldCache);
			if(requestedStateEnum.toString().equals("STATUS")) {
				return;
			}
		} catch(Exception exception) {
			exception.printStackTrace();
			context.close();
			return;
		}
		
		// Get server host
		String serverHost;
		try {
			if(this.serverHostFieldCache == null) {
				for(Field field : object.getClass().getSuperclass().getDeclaredFields()) {
					if(!field.getType().getSimpleName().equals("String")) {
						continue;
					}
					this.serverHostFieldCache = field.getName();
					break;
				}
			}
			serverHost = ReflectionUtils.getPrivateField(object.getClass().getSuperclass(), object, String.class, this.serverHostFieldCache);
		} catch(Exception exception) {
			exception.printStackTrace();
			context.close();
			return;
		}
		
		// Get login payload
		LoginPayload payload;
		try {
			payload = LoginPayload.decode(serverHost);
			if(payload == null) {
				throw new Exception(); // for lack of a better solution
			}
		} catch(Exception exception) {
			context.close();
			return;
		}
		
		// Check the security key
		if(!payload.getSecurityKey().equals(this.connectPlugin.getSecurityKey())) {
			// TODO tell the client the security key failed?
			context.close();
			return;
		}
		
		// Store the host
		try {
			ReflectionUtils.setFinalField(object.getClass().getSuperclass(), object, this.serverHostFieldCache, payload.getHost());
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		
		// Store the real ip & port
		try {
			InetSocketAddress newRemoteAddress = new InetSocketAddress(payload.getRealIp(), payload.getRealPort());
			// Netty
			ReflectionUtils.setFinalField(AbstractChannel.class, context.channel(), "remoteAddress", newRemoteAddress);
			// MC
			Object networkManager = context.channel().pipeline().get("packet_handler");
			try {
				ReflectionUtils.setFinalField(networkManager.getClass(), networkManager, "n", newRemoteAddress);
			} catch(Exception exception1) {
				ReflectionUtils.setFinalField(networkManager.getClass(), networkManager, "l", newRemoteAddress);
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		
		// Submit to cache
		this.payloadCache.submit(payload);
	}

	public boolean isEnabled() {
		return this.connectPlugin.isEnabled();
	}

}
