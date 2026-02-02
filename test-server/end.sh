#!/bin/bash

echo "Stopping server"

PUID=$(ps -aux | grep velocity\.jar | head -n 1)
if [[ $PUID != *"grep"* ]]; then
    PUID=$(echo "${PUID}" | sed -E 's,^[^0-9]*([0-9]+).*$,\1,');
    echo "Killing ${PUID}";
    kill ${PUID};
fi