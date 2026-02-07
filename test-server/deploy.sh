#!/bin/bash

cd test-server
./end.sh
cp ../velocity/build/libs/velocity-all.jar plugins/velocity-discord.jar
./start.sh