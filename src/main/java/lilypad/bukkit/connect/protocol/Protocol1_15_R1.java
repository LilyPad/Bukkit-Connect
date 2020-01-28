package lilypad.bukkit.connect.protocol;

public class Protocol1_15_R1 implements IProtocol {
    @Override
    public String getGeneralVersion() {
        return "1.15";
    }

    @Override
    public String getNettyInjectorChannelFutureList() {
        return "listeningChannels";
    }

    @Override
    public String getOfflineInjectorServerConnection() {
        return "e";
    }

    @Override
    public boolean isOfflineBlankConstructor() {
        return false;
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
        return "socketAddress";
    }

}
