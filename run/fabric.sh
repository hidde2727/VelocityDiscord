#!/bin/bash

WORKING_DIR=${PWD##*/}
WORKING_DIR=${WORKING_DIR:-/}
if [ $WORKING_DIR == "run" ]; then
    cd ../;
    echo "Going up a directory";
fi
./run/fabric/stop.sh

gradle platforms:fabric:build
cp ./platforms/fabric/build/libs/fabric-all.jar ./run/fabric/mods/fabric-discord.jar

cd ./run/fabric
# Disable the proxy mod
mv ./mods/FabricProxy-Lite.jar ./mods/FabricProxy-Lite.jar.disabled

# Remove the language.yml for easier testing
rm ./config/discordio/language.yml

./start.sh