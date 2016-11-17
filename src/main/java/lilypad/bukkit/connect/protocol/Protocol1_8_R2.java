package lilypad.bukkit.connect.protocol;

public class Protocol1_8_R2 implements IProtocol {

	@Override
	public String getGeneralVersion() {
		return "1.8";
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
	public boolean isOfflineBlankConstructor() {
		return true;
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
	public String getLoginNettyInjectHandlerNetworkManager() {
		return "l";
	}

}
