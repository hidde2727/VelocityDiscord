#!/bin/bash

WORKING_DIR=${PWD##*/}
WORKING_DIR=${WORKING_DIR:-/}
if [ $WORKING_DIR == "run" ]; then
    cd ../;
fi
gradle :velocity:build

cd ./run
./velocity/stop.sh
./fabric/stop.sh

cp ../velocity/build/libs/velocity-all.jar ./velocity/plugins/velocity-discord.jar

# Disable the discord mod on the fabric server, enable the proxy mod
mv ./fabric/mods/FabricProxy-Lite.jar.disabled ./fabric/mods/FabricProxy-Lite.jar
rm ./fabric/mods/fabric-discord.jar

./fabric/start.sh &
./velocity/start.sh

echo "Shutting the fabric server down"
./fabric/stop.sh