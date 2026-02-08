## Setup
Setup to use the development scripts provided

- Add a fabric jar, and rename it to "fabric-server.jar".
- Start the server and stop it, accept the EULA.
- (For velocity development) Add the FabricProxy-Lite.jar to the mods folder ([modrinth.com](https://modrinth.com/mod/fabricproxy-lite), must be named "FabricProxy-Lite.jar")
- Add the fabric api for the version of your fabric server to the mods folder ([modrinth.com](https://modrinth.com/mod/fabric-api))
- Start the server again, it will generate all the configs
- (For velocity development) Setup the FabricProxy-Lite.toml to accept connections from the velocity proxy
- Run the /run/fabric.sh script, it will generate this plugins jar and start the server (then stop the server again)
- (For fabric development) Continue the setup of the main README.md to configure this mods config