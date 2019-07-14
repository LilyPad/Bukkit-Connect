package lilypad.bukkit.connect.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.server.TemporaryPlayerFactory;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lilypad.bukkit.connect.ConnectPlugin;
import lilypad.bukkit.connect.login.LoginPayload;
import lilypad.bukkit.connect.login.LoginPayloadCache;
import lilypad.bukkit.connect.util.ReflectionUtils;
import org.bukkit.Server;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.List;

public class ProtocolLibPacketAdapter extends PacketAdapter {

    private ConnectPlugin connectPlugin;
    private LoginPayloadCache payloadCache;

    public ProtocolLibPacketAdapter(ConnectPlugin plugin, LoginPayloadCache payloadCache) {
        super(plugin, PacketType.Handshake.Client.SET_PROTOCOL);
        this.connectPlugin = plugin;
        this.payloadCache = payloadCache;
    }

    public static int hookAndFindPort(Server server, ConnectPlugin plugin, LoginPayloadCache payloadCache) throws Exception {
        ProtocolLibrary.getProtocolManager().addPacketListener(new ProtocolLibPacketAdapter(plugin, payloadCache));

        Method serverGetHandle = server.getClass().getDeclaredMethod("getServer");
        Object minecraftServer = serverGetHandle.invoke(server);
        // Get Server Connection
        Method serverConnectionMethod = null;
        for (Method method : minecraftServer.getClass().getSuperclass().getDeclaredMethods()) {
            if (!method.getReturnType().getSimpleName().equals("ServerConnection")) {
                continue;
            }
            serverConnectionMethod = method;
            break;
        }
        Object serverConnection = serverConnectionMethod.invoke(minecraftServer);
        // Get ChannelFuture List // TODO find the field dynamically
        List<ChannelFuture> channelFutureList = ReflectionUtils.getPrivateField(serverConnection.getClass(), serverConnection, List.class, ConnectPlugin.getProtocol().getNettyInjectorChannelFutureList());
        // Iterate ChannelFutures
        int commonPort = 0;
        for (ChannelFuture channelFuture : channelFutureList) {
            // Update Common Port
            commonPort = ((InetSocketAddress) channelFuture.channel().localAddress()).getPort();
        }
        return commonPort;
    }

    private void kick(PacketEvent event) {
        event.getPlayer().kickPlayer("");
        event.setCancelled(true);
    }

    public void onPacketReceiving(PacketEvent event) {
        // Check if requested state is not status
        if (event.getPacket().getIntegers().read(1) == 1) {
            return;
        }

        // Get Server Host
        String serverHost = event.getPacket().getStrings().read(0);

        // Get login payload
        LoginPayload payload;
        try {
            payload = LoginPayload.decode(serverHost);
            if (payload == null) {
                throw new Exception(); // for lack of a better solution
            }
        } catch (Exception ex) {
            kick(event);
            return;
        }

        // Check the security key
        if (!payload.getSecurityKey().equals(this.connectPlugin.getSecurityKey())) {
            // TODO tell the client the security key failed?
            kick(event);
            return;
        }

        event.getPacket().getStrings().write(0, payload.getHost());

        // Store the real ip & port
        try {
            Object socketInjector = TemporaryPlayerFactory.getInjectorFromPlayer(event.getPlayer());
            Object injector = ReflectionUtils.getPrivateField(socketInjector.getClass(), socketInjector, Object.class, "injector");
            Object networkManager = ReflectionUtils.getPrivateField(injector.getClass(), injector, Object.class, "networkManager");
            Channel channel = ReflectionUtils.getPrivateField(injector.getClass(), injector, Channel.class, "originalChannel");

            InetSocketAddress newRemoteAddress = new InetSocketAddress(payload.getRealIp(), payload.getRealPort());
            // Netty
            ReflectionUtils.setFinalField(AbstractChannel.class, channel, "remoteAddress", newRemoteAddress);
            // MC
            ReflectionUtils.setFinalField(networkManager.getClass(), networkManager, ConnectPlugin.getProtocol().getLoginNettyInjectHandlerNetworkManager(), newRemoteAddress);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        // Submit to cache
        this.payloadCache.submit(payload);
    }

}