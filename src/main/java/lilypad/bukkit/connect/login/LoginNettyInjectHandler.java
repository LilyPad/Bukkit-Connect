package lilypad.bukkit.connect.login;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelHandlerContext;
import lilypad.bukkit.connect.ConnectPlugin;
import lilypad.bukkit.connect.injector.NettyDecoderHandler;
import lilypad.bukkit.connect.injector.NettyInjectHandler;
import lilypad.bukkit.connect.injector.OfflineInjector;
import lilypad.bukkit.connect.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.UUID;

public class LoginNettyInjectHandler implements NettyInjectHandler {

	private String requestedStateFieldCache;
	private String serverHostFieldCache;
	private Class<?> serverHostFieldClass;

	private ConnectPlugin connectPlugin;
	private LoginPayloadCache payloadCache;

	public LoginNettyInjectHandler(ConnectPlugin connectPlugin, LoginPayloadCache payloadCache) {
		this.connectPlugin = connectPlugin;
		this.payloadCache = payloadCache;
	}

	public void packetReceived(NettyDecoderHandler handler, ChannelHandlerContext context, Object object) throws Exception {
		String packetName = object.getClass().getSimpleName();
		if (packetName.startsWith("PacketHandshakingInSetProtocol")) {
			handleSetProtocol(context, object);
		} else if (packetName.equals("PacketLoginInStart")) {
			handlePacketLoginStart(context, object);
			handler.disable();
		}
	}

	private void handleSetProtocol(ChannelHandlerContext context, Object object) {
		// Get requested state
		try {
			if (this.requestedStateFieldCache == null) {
				for (Field field : object.getClass().getDeclaredFields()) {
					if (!field.getType().getSimpleName().equals("EnumProtocol")) {
						continue;
					}
					this.requestedStateFieldCache = field.getName();
					break;
				}
			}
			Object requestedStateEnum = ReflectionUtils.getPrivateField(object.getClass(), object, Object.class, this.requestedStateFieldCache);
			if (requestedStateEnum.toString().equals("STATUS")) {
				return;
			}
		} catch (Exception exception) {
			exception.printStackTrace();
			context.close();
			return;
		}

		// Get server host
		String serverHost;
		try {
			if (this.serverHostFieldCache == null) {
				for (Field field : object.getClass().getSuperclass().getDeclaredFields()) {
					if (!field.getType().getSimpleName().equals("String")) {
						continue;
					}
					this.serverHostFieldCache = field.getName();
					this.serverHostFieldClass = object.getClass().getSuperclass();
					break;
				}
				if (this.serverHostFieldCache == null) {
					for (Field field : object.getClass().getDeclaredFields()) {
						if (!field.getType().getSimpleName().equals("String")) {
							continue;
						}
						this.serverHostFieldCache = field.getName();
						this.serverHostFieldClass = object.getClass();
						break;
					}
				}
			}
			serverHost = ReflectionUtils.getPrivateField(this.serverHostFieldClass, object, String.class, this.serverHostFieldCache);
		} catch (Exception exception) {
			exception.printStackTrace();
			context.close();
			return;
		}

		// Get login payload
		LoginPayload payload;
		try {
			payload = LoginPayload.decode(serverHost);
			if (payload == null) {
				throw new Exception(); // for lack of a better solution
			}
		} catch (Exception exception) {
			context.close();
			return;
		}

		// Check the security key
		if (!payload.getSecurityKey().equals(this.connectPlugin.getSecurityKey())) {
			// TODO tell the client the security key failed?
			context.close();
			return;
		}

		// Store the host
		try {
			ReflectionUtils.setFinalField(this.serverHostFieldClass, object, this.serverHostFieldCache, payload.getHost());
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		// Store the real ip & port
		try {
			InetSocketAddress newRemoteAddress = new InetSocketAddress(payload.getRealIp(), payload.getRealPort());
			// Netty
			ReflectionUtils.setFinalField(AbstractChannel.class, context.channel(), "remoteAddress", newRemoteAddress);
			// MC
			Object networkManager = context.channel().pipeline().get("packet_handler");

			if (ConnectPlugin.getProtocol().getGeneralVersion().equalsIgnoreCase("1.7")) {
				String[] fields = ConnectPlugin.getProtocol().getLoginNettyInjectHandlerNetworkManager().split(",");
				try {
					ReflectionUtils.setFinalField(networkManager.getClass(), networkManager, fields[0], newRemoteAddress);
				} catch (Exception e) {
					ReflectionUtils.setFinalField(networkManager.getClass(), networkManager, fields[1], newRemoteAddress);
				}
			} else {
				ReflectionUtils.setFinalField(networkManager.getClass(), networkManager, ConnectPlugin.getProtocol().getLoginNettyInjectHandlerNetworkManager(), newRemoteAddress);
			}

		} catch (Exception exception) {
			exception.printStackTrace();
		}

		// Submit to cache
		this.payloadCache.submit(payload);
	}

	private void handlePacketLoginStart(ChannelHandlerContext context, Object object) {
		// inject LoginListener
		try {
			Object networkManager = context.channel().pipeline().get("packet_handler");
			try {
				Class loginListenerProxyClass = LoginListenerProxy.get(networkManager);
				Constructor loginListenerProxyConstructor = loginListenerProxyClass.getConstructors()[0];
				Object offlineMinecraftServer = OfflineInjector.getOfflineMinecraftServer();
				Object loginListenerProxy = loginListenerProxyConstructor.newInstance(offlineMinecraftServer, networkManager);
				loginListenerProxyClass.getField("injectUuidCallback").set(loginListenerProxy, (Runnable) () -> {
					try {
						Field profileField = LoginListenerProxy.getProfileField();
						GameProfile profile = (GameProfile) profileField.get(loginListenerProxy);
						LoginPayload payload = payloadCache.getByName(profile.getName());
						profile = new GameProfile(payload.getUUID(), profile.getName());
						for (LoginPayload.Property payloadProperty : payload.getProperties()) {
							Property property = new Property(payloadProperty.getName(), payloadProperty.getValue(), payloadProperty.getSignature());
							profile.getProperties().put(payloadProperty.getName(), property);
						}
						profileField.set(loginListenerProxy, profile);
	
						Field hostnameField = LoginListenerProxy.getLoginListenerClass().getField("hostname");
						hostnameField.set(loginListenerProxy, payload.getHost());
					} catch (Exception exception) {
						exception.printStackTrace();
					}
				});
				LoginListenerProxy.getPacketListenerField().set(networkManager, loginListenerProxy);
			} catch(Exception exception) {
				if (this.connectPlugin.getServer().getPluginManager().getPlugin("ProtocolSupport") == null) {
					throw exception;
				}
				GameProfile profile = ReflectionUtils.getPrivateField(object.getClass(), object, GameProfile.class, "a");
				LoginPayload payload = payloadCache.getByName(profile.getName());
				LoginPayload.Property[] payloadProperties = payload.getProperties();
				Property[] properties = new Property[payloadProperties.length];
				for (int i = 0; i < properties.length; i++) {
					LoginPayload.Property payloadProperty = payloadProperties[i];
					Property property = new Property(payloadProperty.getName(), payloadProperty.getValue(), payloadProperty.getSignature());
					properties[i] = property;
				}
				ReflectionUtils.setFinalField(networkManager.getClass(), networkManager, "spoofedUUID", payload.getUUID());
				ReflectionUtils.setFinalField(networkManager.getClass(), networkManager, "spoofedProfile", properties);
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public boolean isEnabled() {
		return this.connectPlugin.isEnabled();
	}

}
