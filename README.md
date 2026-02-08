## Features
- Works on both Velocity proxies and Fabric servers
- Highly configurable (see [config.yaml](/core/src/main/resources/config.yml) and [messages.properties](/core/src/main/resources/messages.properties) for all the things you can mess with)
- Custom discord messages on server start and stop
- Custom discord messages when players connect and disconnect
- Custom discord message when players message in game
- Custom minecraft messages when user message on discord
- Whitelisting feature to people make whitelist requests via discord



## Download
Go to [modrinth.com](https://modrinth.com/plugin/discord-server) to download this mod

## Setup
See [discord.com/developers/docs/quick-start/getting-started](https://discord.com/developers/docs/quick-start/getting-started) for more information.

1. Start your (fabric, or velocity) server, and stop it again
2. The server will generate either a mods or a plugin folder, put the jar file corresponding to your server type in that folder (Go to [modrinth.com](https://modrinth.com/plugin/discord-server) to download)
3. Start your server again, this mod will generate a config.yml file for you, open it (the following steps will guide you through configuring it)
4. Go to the discord development portal [discord.com/developers/applications](https://discord.com/developers/applications)
5. Click on "New application"
![A photo of the discord panel when visiting discord.com/developers/applications](/docs/discord-add-app.png)
6. Choose a name and accept the user agreement
![A photo of the discord panel after clicking on "New application"](/docs/discord-create-app.png)
7. Use the sidebar to go to the bots section
![A photo of the discord panel after creating your bot](/docs/discord-navigate-to-bot.png)
8. Click on reset token and click confirm
![A photo of the discord panel bots section](/docs/discord-reset-token.png)
9. Copy your bot token and paste it into the botToken field of your config
![A photo of the config.yaml file](/docs/config.png)
10. Now use the sidebar to go to installation and configure the following:
![A photo of the install screen. User install disabled, scopes=bot, permissions=Send messages, Bypass slow mode, Manage roles and Add reactions](/docs/discord-install.png)
11. Copy the url provided by discord of the previous screen and visit it, you can now invite it to the server you want the bot to send messages to

Now we want to setup the features, you can use the following tutorial to get a channel ID:
[support.discord.com](https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID)
You can then copy your channel IDs to the features you want to enable in the config (!disable a feature if you do not want to use it!)

## Bugs
If you find bugs or weird behavior, please open an issue on github ([github.com](https://github.com/hidde2727/VelocityDiscord/issues)) or send an email to 93196280+hidde2727@users.noreply.github.com