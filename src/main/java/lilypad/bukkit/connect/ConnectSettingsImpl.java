package lilypad.bukkit.connect;

import java.net.InetSocketAddress;

import org.bukkit.configuration.file.FileConfiguration;

import lilypad.client.connect.api.ConnectSettings;

public class ConnectSettingsImpl implements ConnectSettings {

	private String outboundIp;
	private int outboundPort;
	private String username;
	private String password;

	public ConnectSettingsImpl(FileConfiguration fileConfiguration) {
		this.outboundIp = fileConfiguration.getString("settings.address");
		this.outboundPort = fileConfiguration.getInt("settings.port");
		this.username = fileConfiguration.getString("settings.credentials.username");
		this.password = fileConfiguration.getString("settings.credentials.password");
	}

	public InetSocketAddress getOutboundAddress() {
		return new InetSocketAddress(this.outboundIp, this.outboundPort);
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

}
