package lilypad.bukkit.connect;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import lilypad.bukkit.connect.util.ReflectionUtils;
import lilypad.client.connect.api.Connect;
import lilypad.client.connect.lib.ConnectImpl;

public class ConnectPlugin extends JavaPlugin {

	private Connect connect;
	private ConnectThread connectThread;
	private String securityKey;

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
		ConnectPluginListener listener = new ConnectPluginListener(this);
		super.getServer().getPluginManager().registerEvents(listener, this);
		super.getServer().getMessenger().registerIncomingPluginChannel(this, "LilyPad", listener);
		super.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				try {
					Object craftServer = ConnectPlugin.super.getServer();
					Object minecraftServer = ReflectionUtils.getPrivateField(craftServer.getClass(), craftServer, Object.class, "console");

					// Set Offline Mode
					try {
						Object booleanWrapperOnline = ReflectionUtils.getPrivateField(craftServer.getClass(), craftServer, Object.class, "online");
						ReflectionUtils.setFinalField(booleanWrapperOnline.getClass(), booleanWrapperOnline, "value", false);
					} catch(Exception exception) {
						System.out.println("[Connect] Unable to set offline mode in CraftBukkit - older version?");
					}
					Method setOnlineMode = minecraftServer.getClass().getMethod("setOnlineMode", boolean.class);
					setOnlineMode.invoke(minecraftServer, Boolean.FALSE);

					// Connection Throttle
					YamlConfiguration configuration = ReflectionUtils.getPrivateField(craftServer.getClass(), craftServer, YamlConfiguration.class, "configuration");
					configuration.set("settings.connection-throttle", 0);

					ConnectPlugin.this.connectThread.start();
				} catch(Exception exception) {
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
		} catch(Exception exception) {
			exception.printStackTrace();
		} finally {
			this.connect = null;
			this.connectThread = null;
		}
	}

	public InetSocketAddress getInboundAddress() {
		return new InetSocketAddress(super.getServer().getIp().isEmpty() ? "0.0.0.0" : super.getServer().getIp(), super.getServer().getPort());
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

}
