#!/bin/bash

echo "Please make sure you updated the gradle.properties file"
read -p "Press any key to continue..."
echo "Are you sure, this will publish to the public"
read -p "Press any key to continue..."

gradle :fabric:modrinth
gradle :velocity:modrinth