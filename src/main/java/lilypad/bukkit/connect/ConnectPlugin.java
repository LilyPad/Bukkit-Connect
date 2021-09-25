package lilypad.bukkit.connect;

import lilypad.bukkit.connect.hooks.SpigotHook;
import lilypad.bukkit.connect.injector.HandlerListInjector;
import lilypad.bukkit.connect.injector.NettyInjector;
import lilypad.bukkit.connect.injector.OfflineInjector;
import lilypad.bukkit.connect.login.LoginListener;
import lilypad.bukkit.connect.login.LoginNettyInjectHandler;
import lilypad.bukkit.connect.login.LoginPayloadCache;
import lilypad.bukkit.connect.protocol.*;
import lilypad.bukkit.connect.util.ReflectionUtils;
import lilypad.client.connect.api.Connect;
import lilypad.client.connect.lib.ConnectImpl;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetSocketAddress;

public class ConnectPlugin extends JavaPlugin {

	private LoginPayloadCache payloadCache = new LoginPayloadCache();
	private SpigotHook spigotHook = new SpigotHook();
	private Connect connect;
	private ConnectThread connectThread;
	private String securityKey;
	private int commonPort;
	private static IProtocol protocol;

	@Override
	public void onLoad() {
		super.getConfig().options().copyDefaults(true);
		super.saveConfig();
		super.reloadConfig();
	}

	@Override
	public void onEnable() {
		String version = super.getServer().getClass().getPackage().getName().replace(".",  ",").split(",")[3];
		switch (version) {
		case "v1_8_R1":
			protocol = new Protocol1_8_R1();
			break;
		case "v1_8_R2":
		case "v1_8_R3":
			protocol = new Protocol1_8_R2();
			break;
		case "v1_9_R1":
			protocol = new Protocol1_9_R1();
			break;
		case "v1_9_R2":
			protocol = new Protocol1_9_R2();
			break;
		case "v1_10_R1":
			protocol = new Protocol1_10_R1();
			break;
		case "v1_11_R1":
			protocol = new Protocol1_11_R1();
			break;
		case "v1_12_R1":
			protocol = new Protocol1_12_R1();
			break;
		case "v1_14_R1":
			protocol = new Protocol1_14_R1();
			break;
		case "v1_15_R1":
			protocol = new Protocol1_15_R1();
			break;
		case "v1_16_R1":
		case "v1_16_R2":
		case "v1_16_R3":
			protocol = new Protocol1_16_R1();
			break;
		case "v1_17_R1":
			protocol = new Protocol1_17_R1();
			break;
		default:
			Bukkit.getLogger().info("[Connect] Unable to start plugin - unsupported version (" + version + "). Please retrieve the newest version at http://lilypadmc.org");
			return;
		}

		try {
			// Modify handshake packet max string size
			// -- as of 1.8 I do not believe this is necessary anymore
			/*if (!protocol.getGeneralVersion().equals("1.10")) {
				PacketInjector.injectStringMaxSize(super.getServer(), "handshaking", 0x00, 65535);
			}*/
			// Handle LilyPad handshake packet
			commonPort = NettyInjector.injectAndFindPort(super.getServer(), new LoginNettyInjectHandler(this, payloadCache));
			// If we are in online-mode
			if (super.getServer().getOnlineMode()) {
				// Prioritize our events
				HandlerListInjector.prioritize(this, AsyncPlayerPreLoginEvent.class);
				HandlerListInjector.prioritize(this, PlayerLoginEvent.class);
				// Pseudo offline mode
				OfflineInjector.inject(super.getServer());
			}
		} catch(Exception exception) {
			exception.printStackTrace();
			Bukkit.getLogger().info("[Connect] Unable to start plugin - unsupported version?");
			return;
		}

		this.connect = new ConnectImpl(new ConnectSettingsImpl(super.getConfig()), this.getInboundAddress().getAddress().getHostAddress());
		this.connectThread = new ConnectThread(this);
		super.getServer().getServicesManager().register(Connect.class, this.connect, this, ServicePriority.Normal);

		super.getServer().getPluginManager().registerEvents(new LoginListener(this, payloadCache), this);
		super.getServer().getScheduler().runTask(this, new Runnable() {
			public void run() {
				try {
					// Connection Throttle
					Object craftServer = ConnectPlugin.super.getServer();
					YamlConfiguration configuration = ReflectionUtils.getPrivateField(craftServer.getClass(), craftServer, YamlConfiguration.class, "configuration");
					configuration.set("settings.connection-throttle", 0);
					// Start
					ConnectPlugin.this.connectThread.start();
				} catch(Exception exception) {
					exception.printStackTrace();
					Bukkit.getLogger().info("[Connect] Unable to start plugin - unsupported version?");
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
			if (!super.getConfig().getBoolean("shutdown-lazy", false)) {
				if (this.connect != null) {
					this.connect.close();
				}
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		} finally {
			this.connect = null;
			this.connectThread = null;
			ConnectPlugin.protocol = null;
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

	public static IProtocol getProtocol() {
		return protocol;
	}

}
