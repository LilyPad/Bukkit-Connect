package lilypad.bukkit.connect.protocol;

public class Protocol1_7_R4 implements IProtocol {

	@Override
	public String getGeneralVersion() {
		return "1.7";
	}
	
	@Override
	public String getNettyInjectorChannelFutureList() {
		return "e";
	}

	@Override
	public String getOfflineInjectorServerConnection() {
		return "d";
	}

	@Override
	public String getPacketInjectorProtocolDirections() {
		return "h";
	}

	@Override
	public String getPacketInjectorDecodeCtMethod() {
		return "a";
	}

	@Override
	public String getPacketInjectorHandleCtMethod() {
		return "handle";
	}

	@Override
	public String getLoginListenerGameProfile() {
		return "i";
	}

	@Override
	public String getLoginListenerCacheProfile() {
		return "a";
	}

	@Override
	public String getLoginListenerPropertyConstructor() {
		return "net.minecraft.util.com.mojang.authlib.properties.Property";
	}

	@Override
	public String getLoginNettyInjectHandlerNetworkManager() {
		return "n,l";
	}

}

