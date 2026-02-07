#!/bin/bash

gradle :fabric:build
cp ./fabric/build/libs/fabric.jar ./test-server-2/mods/fabric-discord.jar
cd test-server-2

echo "Starting server"
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Xms1G -Xmx2G -jar fabric-server.jar
