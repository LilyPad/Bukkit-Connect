package lilypad.bukkit.connect.protocol;

public interface IProtocol {
	
	String getGeneralVersion();
	
	String getNettyInjectorChannelFutureList();
	String getOfflineInjectorServerConnection();
	boolean isOfflineBlankConstructor();
	String getPacketInjectorProtocolDirections();
	String getPacketInjectorDecodeCtMethod();
	String getPacketInjectorHandleCtMethod();
	String getLoginNettyInjectHandlerNetworkManager();
	
}
