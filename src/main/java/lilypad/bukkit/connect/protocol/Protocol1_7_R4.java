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
    public boolean isOfflineBlankConstructor() {
        return true;
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
    public String getLoginNettyInjectHandlerNetworkManager() {
        return "n,l";
    }

}

