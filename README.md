# Bukkit-Connect [![Jenkins](https://img.shields.io/jenkins/s/http/ci.lilypadmc.org/Bukkit-Connect.svg?maxAge=2592000?style=flat-square)](http://ci.lilypadmc.org/job/Bukkit-Connect)

Paper plugin permitting a Minecraft server to connect to a LilyPad proxy network. 
This plugin supports Paper 1.12+.

## Installation

1. Download a built of the plugin [here](http://ci.lilypadmc.org/job/Bukkit-Connect/)
2. Place the plugin in the `plugins` folder of your Paper server
3. Ensure `online-mode=false` in `server.properties` and BungeeCord support is not enabled
4. Start your server once, then shut it down
5. Edit `plugins/LilyPad-Connect/config.yml` with your desired values
6. Boot up your server and connect through LilyPad

## Compilation

With [Maven 3](http://maven.apache.org/download.html) installed, run `mvn clean package` in the root project directory.
