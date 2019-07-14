package lilypad.bukkit.connect;

import lilypad.client.connect.api.ConnectSettings;
import org.bukkit.configuration.file.FileConfiguration;

import java.net.InetSocketAddress;

public class ConnectSettingsImpl implements ConnectSettings {

    private final String outboundIp;
    private final int outboundPort;
    private final String username;
    private final String password;

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
