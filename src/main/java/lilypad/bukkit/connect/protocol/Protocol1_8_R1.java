package lilypad.bukkit.connect.protocol;

public class Protocol1_8_R1 implements IProtocol {

	@Override
	public String getGeneralVersion() {
		return "1.8";
	}
	
	@Override
	public String getNettyInjectorChannelFutureList() {
		return "f";
	}

	@Override
	public String getOfflineInjectorServerConnection() {
		return "e";
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
		return "a";
	}

	@Override
	public String getLoginNettyInjectHandlerNetworkManager() {
		return "j";
	}

}

