#! /bin/bash

echo "Starting server"
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Xms256M -Xmx512M -jar velocity.jar