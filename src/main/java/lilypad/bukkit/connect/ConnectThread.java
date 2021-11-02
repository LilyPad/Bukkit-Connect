package lilypad.bukkit.connect;

import lilypad.client.connect.api.Connect;
import lilypad.client.connect.api.ConnectSettings;
import lilypad.client.connect.api.request.RequestException;
import lilypad.client.connect.api.request.impl.AsServerRequest;
import lilypad.client.connect.api.request.impl.AuthenticateRequest;
import lilypad.client.connect.api.request.impl.GetKeyRequest;
import lilypad.client.connect.api.result.impl.AsServerResult;
import lilypad.client.connect.api.result.impl.AuthenticateResult;
import lilypad.client.connect.api.result.impl.GetKeyResult;
import org.bukkit.Bukkit;

public class ConnectThread implements Runnable {

	private ConnectPlugin connectPlugin;
	private Thread thread;

	public ConnectThread(ConnectPlugin connectPlugin) {
		this.connectPlugin = connectPlugin;
	}

	public void start() {
		if (this.thread != null) {
			return;
		}
		this.thread = new Thread(this);
		this.thread.setName("Connect Thread");
		this.thread.start();
	}

	public void stop() {
		if (this.thread == null) {
			return;
		}
		this.thread.interrupt();
		this.thread = null;
	}

	public void run() {
		Connect connect = this.connectPlugin.getConnect();
		ConnectSettings settings = connect.getSettings();
		try {
			while(!connect.isClosed()) {
				// connect
				try {
					connect.connect();
				} catch(InterruptedException interruptedException) {
					throw interruptedException;
				} catch(Throwable throwable) {
					connect.disconnect();
					Bukkit.getLogger().info("[Connect] Couldn't connect to remote host: \"" + throwable.getMessage() + "\", retrying");
					Thread.sleep(1000L);
					continue;
				}

				// key
				GetKeyResult getKeyResult;
				try {
					getKeyResult = connect.request(new GetKeyRequest()).await(2500L);
				} catch(RequestException exception) {
					connect.disconnect();
					Bukkit.getLogger().info("[Connect] Request exception while keying, retrying: " + exception.getMessage());
					Thread.sleep(1000L);
					continue;
				}
				if (getKeyResult == null) {
					connect.disconnect();
					Bukkit.getLogger().info("[Connect] Connection timed out while keying, retrying");
					Thread.sleep(1000L);
					continue;
				}

				// authenticate
				AuthenticateResult authenticationResult;
				try {
					authenticationResult = connect.request(new AuthenticateRequest(settings.getUsername(), settings.getPassword(), getKeyResult.getKey())).await(2500L);
				} catch(RequestException exception) {
					connect.disconnect();
					Bukkit.getLogger().info("[Connect] Request exception while authenticating, retrying: " + exception.getMessage());
					Thread.sleep(1000L);
					continue;
				}
				if (authenticationResult == null) {
					connect.disconnect();
					Bukkit.getLogger().info("[Connect] Connection timed out while authenticating, retrying");
					Thread.sleep(1000L);
					continue;
				}
				switch(authenticationResult.getStatusCode()) {
				case SUCCESS:
					break;
				case INVALID_GENERIC:
					connect.disconnect();
					Bukkit.getLogger().info("[Connect] Invalid username or password, retrying");
					Thread.sleep(1000L);
					continue;
				default:
					connect.disconnect();
					Bukkit.getLogger().info("[Connect] Unknown error while authenticating: \"" + authenticationResult.getStatusCode() + "\", retrying");
					Thread.sleep(1000L);
					continue;
				}

				// announce
				AsServerResult asServerResult;
				try {
					AsServerRequest asServerRequest = new AsServerRequest(this.connectPlugin.getInboundAddress().getAddress().getHostAddress(), this.connectPlugin.getInboundAddress().getPort());
					asServerResult = connect.request(asServerRequest).await(2500L);
				} catch(RequestException exception) {
					connect.disconnect();
					Bukkit.getLogger().info("[Connect] Request exception while acquiring role, retrying: " + exception.getMessage());
					Thread.sleep(1000L);
					continue;
				}
				if (asServerResult == null) {
					connect.disconnect();
					Bukkit.getLogger().info("[Connect] Connection timed out while acquiring role, retrying");
					Thread.sleep(1000L);
					continue;
				}
				switch(asServerResult.getStatusCode()) {
				case SUCCESS:
					break;
				case INVALID_GENERIC:
					connect.disconnect();
					Bukkit.getLogger().info("[Connect] Invalid username, already in use");
					Thread.sleep(1000L);
					break;
				default:
					connect.disconnect();
					Bukkit.getLogger().info("[Connect] Unknown error while acquiring role: \"" + asServerResult.getStatusCode() + "\", retrying");
					Thread.sleep(1000L);
					continue;
				}

				// pause
				Bukkit.getLogger().info("[Connect] Connected to the cloud");
				this.connectPlugin.setSecurityKey(asServerResult.getSecurityKey());
				while(connect.isConnected()) {
					Thread.sleep(1000L);
				}
				this.connectPlugin.setSecurityKey(null);
				Bukkit.getLogger().info("[Connect] Lost connection to the cloud, reconnecting");
			}
		} catch(InterruptedException exception) {
			// ignore
		}
	}

}
