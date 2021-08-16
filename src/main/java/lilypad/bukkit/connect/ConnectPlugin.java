package lilypad.bukkit.connect;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.ChannelFuture;
import lilypad.client.connect.api.Connect;
import lilypad.client.connect.api.ConnectSettings;
import lilypad.client.connect.api.request.Request;
import lilypad.client.connect.api.request.RequestException;
import lilypad.client.connect.api.request.impl.AsServerRequest;
import lilypad.client.connect.api.request.impl.AuthenticateRequest;
import lilypad.client.connect.api.request.impl.GetKeyRequest;
import lilypad.client.connect.api.result.Result;
import lilypad.client.connect.api.result.StatusCode;
import lilypad.client.connect.api.result.impl.AsServerResult;
import lilypad.client.connect.api.result.impl.AuthenticateResult;
import lilypad.client.connect.api.result.impl.GetKeyResult;
import lilypad.client.connect.lib.ConnectImpl;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class ConnectPlugin extends JavaPlugin {

    private int commonPort;
    private ConnectSettings connectSettings;
    private Connect connect;
    private ScheduledExecutorService executorService;
    private final AtomicReference<String> securityKey = new AtomicReference<>();

    @Override
    public void onLoad() {
        super.getConfig().options().copyDefaults(true);
        super.saveConfig();
        super.reloadConfig();
    }

    @Override
    public void onEnable() {
        if (super.getServer().getOnlineMode()) {
            throw new IllegalStateException("LilyPad requires \"online-mode=false\" in server.properties");
        }
        if (super.getServer().spigot().getSpigotConfig().getBoolean("settings.bungeecord", false)) {
            throw new IllegalStateException("LilyPad requires \"settings.bungeecord: false\" in spigot.yml");
        }

        super.getServer().spigot().getBukkitConfig().set("settings.connection-throttle", -1);

        commonPort = getCommonPort();
        connectSettings = new ConnectSettingsImpl(super.getConfig());
        connect = new ConnectImpl(connectSettings, getInboundAddress().getAddress().getHostAddress());
        executorService = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat("LilyPad Connect Thread %d")
                        .setDaemon(true)
                        .build());

        executorService.scheduleAtFixedRate(this::ensureConnected, 0L, 1L, TimeUnit.SECONDS);

        super.getServer().getPluginManager().registerEvents(new PlayerHandshakeListener(this), this);
        super.getServer().getServicesManager().register(Connect.class, connect, this, ServicePriority.Normal);
    }

    @Override
    public void onDisable() {
        executorService.shutdownNow();
        connect.close();
    }

    private void ensureConnected() {
        if (connect.isClosed() || connect.isConnected()) {
            return;
        }

        securityKey.set(null);
        log.info("Attempting cloud connection...");

        try {
            connect.connect();
        } catch (Throwable throwable) {
            log.warn("Couldn't connect to remote host, retrying", throwable);
            return;
        }

        try {
            final GetKeyResult keyResult = makeConnectRequest(new GetKeyRequest());
            final AuthenticateResult authenticateResult = makeConnectRequest(new AuthenticateRequest(connectSettings.getUsername(), connectSettings.getPassword(), keyResult.getKey()));
            final StatusCode authenticateStatusCode = authenticateResult.getStatusCode();
            switch (authenticateStatusCode) {
                case SUCCESS:
                    break;
                case INVALID_GENERIC:
                    throw new RequestFailureException("Invalid username or password");
                default:
                    throw new RequestFailureException("Unknown error " + authenticateStatusCode + " while authenticating");
            }

            final AsServerResult asServerResult = makeConnectRequest(new AsServerRequest(getInboundAddress().getPort()));
            final StatusCode asServerStatusCode = asServerResult.getStatusCode();
            switch (asServerStatusCode) {
                case SUCCESS:
                    break;
                case INVALID_GENERIC:
                    throw new RequestFailureException("Username already in use");
                default:
                    throw new RequestFailureException("Unknown error " + asServerStatusCode + " while acquiring role");
            }

            securityKey.set(asServerResult.getSecurityKey());
            log.info("Connected to the cloud!");
        } catch (RequestFailureException exception) {
            connect.disconnect();
            log.warn("Failed to connect, retrying", exception);
        }
    }

    private <T extends Result> T makeConnectRequest(Request<T> request) throws RequestFailureException {
        final T result;
        try {
            result = connect.request(request).await(2500L);
        } catch (InterruptedException | RequestException exception) {
            throw new RequestFailureException("Exception occurred while fetching response for " + request.getClass(), exception);
        }
        if (result == null) {
            throw new RequestFailureException("Timed out fetching response for " + request.getClass());
        }
        return result;
    }

    private int getCommonPort() {
        try {
            final Object minecraftServer = super.getServer().getClass().getMethod("getServer").invoke(super.getServer());
            final Object serverConnection = minecraftServer.getClass().getMethod("getServerConnection").invoke(minecraftServer);
            return Arrays.stream(serverConnection.getClass().getDeclaredFields())
                    .filter(field -> field.getType().equals(List.class))
                    .peek(field -> field.setAccessible(true))
                    .map(field -> {
                        try {
                            return (List) field.get(serverConnection);
                        } catch (IllegalAccessException exception) {
                            log.warn("Failed to get list from ServerConnection field \"" + field.getName() + "\", defaulting to empty list", exception);
                            return Collections.emptyList();
                        }
                    })
                    .peek(list -> log.info("found list: " + list))
                    .filter(list -> !list.isEmpty() && ChannelFuture.class.isAssignableFrom(list.get(0).getClass()))
                    .map(list -> (ChannelFuture) list.get(0))
                    .map(channelFuture -> ((InetSocketAddress) channelFuture.channel().localAddress()).getPort())
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Failed to find common port"));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            throw new IllegalStateException("Failed to resolve channelFuture list", exception);
        }
    }

    private InetSocketAddress getInboundAddress() {
        String ip = super.getServer().getIp();
        if (ip.isEmpty()) {
            ip = "0.0.0.0";
        }
        int port = super.getServer().getPort();
        if (port == 0) {
            port = this.commonPort;
        }
        return new InetSocketAddress(ip, port);
    }

    public String getSecurityKey() {
        return securityKey.get();
    }

}
