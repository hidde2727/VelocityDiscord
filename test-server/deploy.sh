#!/bin/bash

cd test-server
./end.sh
cp ../build/libs/velocity-discord-all.jar plugins/velocity-discord.jar
./start.sh