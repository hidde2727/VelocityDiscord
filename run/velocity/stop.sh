#!/bin/bash

# Make sure the working directory is the correct folder
WORKING_DIR=${PWD##*/}
WORKING_DIR=${WORKING_DIR:-/}
if [ $WORKING_DIR == "run" ]; then
    cd velocity;
fi

echo "Stopping server"

PUID=$(ps -aux | grep velocity\.jar | head -n 1)
if [[ $PUID != *"grep"* ]]; then
    PUID=$(echo "${PUID}" | sed -E 's,^[^0-9]*([0-9]+).*$,\1,');
    echo "Killing ${PUID}";
    kill ${PUID};
fi