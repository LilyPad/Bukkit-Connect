package lilypad.bukkit.connect.protocol;

public interface IProtocol {
	
	String getGeneralVersion();
	
	String getNettyInjectorChannelFutureList();
	String getOfflineInjectorServerConnection();
	String getPacketInjectorProtocolDirections();
	String getPacketInjectorDecodeCtMethod();
	String getPacketInjectorHandleCtMethod();
	String getLoginListenerGameProfile();
	String getLoginListenerCacheProfile();
	String getLoginListenerPropertyConstructor();
	String getLoginNettyInjectHandlerNetworkManager();
	
}
