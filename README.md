Bukkit-Connect [![Jenkins](https://img.shields.io/jenkins/s/http/ci.lilypadmc.org/Bukkit-Connect.svg?maxAge=2592000?style=flat-square)](http://ci.lilypadmc.org/job/Bukkit-Connect)
===========

This version of Bukkit-Connect supports Spigot 1.8 through 1.17

Note for 1.17+ users
-----------

Make sure to use the following Java startup params:
```
--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED
```

Compilation
-----------

We use maven to handle our dependencies.

* Install [Maven 3](http://maven.apache.org/download.html)
* Clone this repo and: `mvn clean install`
