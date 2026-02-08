#! /bin/bash

# Make sure the working directory is the correct folder
WORKING_DIR=${PWD##*/}
WORKING_DIR=${WORKING_DIR:-/}
if [ $WORKING_DIR == "run" ]; then
    cd velocity;
fi

echo "Starting the velocity server"
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Xms256M -Xmx512M -jar velocity.jar