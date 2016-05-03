package lilypad.bukkit.connect.hooks;

import lilypad.bukkit.connect.util.ReflectionUtils;

public class SpigotHook {

      private boolean isSpigot;
      private String whitelistMessage;
      private String serverFullMessage;

      public SpigotHook() {
            Class<?> spigotConfig;

            try {
                  spigotConfig = Class.forName("org.spigotmc.SpigotConfig");
                  this.isSpigot = true;
            } catch(Exception exception) {
                  this.isSpigot = false;
                  return;
            }

            try {
                  this.whitelistMessage = ReflectionUtils.getPrivateField(spigotConfig, null, String.class, "whitelistMessage");
                  this.serverFullMessage = ReflectionUtils.getPrivateField(spigotConfig, null, String.class, "serverFullMessage");
                  ReflectionUtils.setFinalField(spigotConfig, null, "saveUserCacheOnStopOnly", true);
            } catch(Exception exception) {
                  exception.printStackTrace();
            }
      }

      public boolean isSpigot() {
            return this.isSpigot;
      }

      public String getWhitelistMessage() {
            return this.whitelistMessage;
      }

      public String getServerFullMessage() {
            return this.serverFullMessage;
      }

}
