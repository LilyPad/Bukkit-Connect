package lilypad.bukkit.connect.protocol;

public class Protocol1_9_R2 implements IProtocol {

	@Override
	public String getGeneralVersion() {
		return "1.9";
	}
	
	@Override
	public String getNettyInjectorChannelFutureList() {
		return "g";
	}

	@Override
	public String getOfflineInjectorServerConnection() {
		return "f";
	}

	@Override
	public String getPacketInjectorProtocolDirections() {
		return "j";
	}

	@Override
	public String getPacketInjectorDecodeCtMethod() {
		return "a";
	}

	@Override
	public String getPacketInjectorHandleCtMethod() {
		return "a";
	}

	@Override
	public String getLoginListenerGameProfile() {
		return "bS";
	}

	@Override
	public String getLoginListenerCacheProfile() {
		return "a";
	}

	@Override
	public String getLoginListenerPropertyConstructor() {
		return "com.mojang.authlib.properties.Property";
	}

	@Override
	public String getLoginNettyInjectHandlerNetworkManager() {
		return "l";
	}

}
