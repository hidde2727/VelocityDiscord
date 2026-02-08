#!/bin/bash

# Make sure the working directory is the correct folder
WORKING_DIR=${PWD##*/}
WORKING_DIR=${WORKING_DIR:-/}
if [ $WORKING_DIR == "run" ]; then
    cd fabric;
fi

echo "Starting the fabric server"
unset DISPLAY
java -Xms1G -Xmx2G -jar fabric-server.jar