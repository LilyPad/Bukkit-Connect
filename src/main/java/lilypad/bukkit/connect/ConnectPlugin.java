package lilypad.bukkit.connect;

import lilypad.bukkit.connect.hooks.SpigotHook;
import lilypad.bukkit.connect.injector.HandlerListInjector;
import lilypad.bukkit.connect.injector.OfflineInjector;
import lilypad.bukkit.connect.injector.PacketInjector;
import lilypad.bukkit.connect.login.LoginListener;
import lilypad.bukkit.connect.login.LoginNettyInjectHandler;
import lilypad.bukkit.connect.login.LoginPayloadCache;
import lilypad.bukkit.connect.netty.NettyInjector;
import lilypad.bukkit.connect.protocollib.ProtocolLibPacketAdapter;
import lilypad.bukkit.connect.util.ReflectionUtils;
import lilypad.client.connect.api.Connect;
import lilypad.client.connect.lib.ConnectImpl;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;

public class ConnectPlugin extends JavaPlugin {

    private SpigotHook spigotHook = new SpigotHook();
    private Connect connect;
    private ConnectThread connectThread;
    private String securityKey;
    private int commonPort;

    @Override
    public void onLoad() {
        super.getConfig().options().copyDefaults(true);
        super.saveConfig();
        super.reloadConfig();
    }

    @Override
    public void onEnable() {
        this.connect = new ConnectImpl(new ConnectSettingsImpl(super.getConfig()), this.getInboundAddress().getAddress().getHostAddress());
        this.connectThread = new ConnectThread(this);
        super.getServer().getServicesManager().register(Connect.class, this.connect, this, ServicePriority.Normal);

        LoginPayloadCache payloadCache = new LoginPayloadCache();
        try {
            // Modify handshake packet max string size
            PacketInjector.injectStringMaxSize(super.getServer(), "handshaking", 0x00, 65535);
            // Handle LilyPad handshake packet
            if (super.getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
                commonPort = ProtocolLibPacketAdapter.hookAndFindPort(this, payloadCache);
            } else {
                commonPort = NettyInjector.injectAndFindPort(super.getServer(), new LoginNettyInjectHandler(this, payloadCache));
            }
            // If we are in online-mode
            if (super.getServer().getOnlineMode()) {
                // Login listener will restore UUIDs
                super.getServer().getPluginManager().registerEvents(new LoginListener(this, payloadCache), this);
                // Prioritize our events
                HandlerListInjector.prioritize(this, AsyncPlayerPreLoginEvent.class);
                HandlerListInjector.prioritize(this, PlayerLoginEvent.class);
                // Pseudo offline mode
                OfflineInjector.inject(super.getServer());
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            System.out.println("[Connect] Unable to start plugin - unsupported version?");
            return;
        }

        super.getServer().getScheduler().runTask(this, new Runnable() {
            public void run() {
                try {
                    // Connection Throttle
                    Object craftServer = ConnectPlugin.super.getServer();
                    YamlConfiguration configuration = ReflectionUtils.getPrivateField(craftServer.getClass(), craftServer, YamlConfiguration.class, "configuration");
                    configuration.set("settings.connection-throttle", 0);
                    // Start
                    ConnectPlugin.this.connectThread.start();
                } catch (Exception exception) {
                    exception.printStackTrace();
                    System.out.println("[Connect] Unable to start plugin - unsupported version?");
                }
            }
        });
    }

    @Override
    public void onDisable() {
        try {
            if (this.connectThread != null) {
                this.connectThread.stop();
            }
            if (this.connect != null) {
                this.connect.close();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            this.connect = null;
            this.connectThread = null;
        }
    }

    public InetSocketAddress getInboundAddress() {
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

    public Connect getConnect() {
        return this.connect;
    }

    public String getSecurityKey() {
        return this.securityKey;
    }

    public void setSecurityKey(String securityKey) {
        this.securityKey = securityKey;
    }

    public SpigotHook getSpigotHook() {
        return this.spigotHook;
    }

}
