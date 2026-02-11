#!/bin/bash

WORKING_DIR=${PWD##*/}
WORKING_DIR=${WORKING_DIR:-/}
if [ $WORKING_DIR == "run" ]; then
    cd ../;
fi
./run/fabric/stop.sh

gradle :fabric:build
cp ./fabric/build/libs/fabric.jar ./run/fabric/mods/fabric-discord.jar

cd ./run/fabric
# Disable the proxy mod
mv ./mods/FabricProxy-Lite.jar ./mods/FabricProxy-Lite.jar.disabled

# Remove the message.properties for easier testing
rm ./config/discordio/messages.properties

./start.sh