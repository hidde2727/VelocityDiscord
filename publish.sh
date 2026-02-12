#!/bin/bash

echo "Did you update the version in gradle.properties"
read -p "Press any key to continue..."
echo "Did you update changelog.md"
read -p "Press any key to continue..."
echo "Are you sure, this will publish to the public"
read -p "Press any key to continue..."

gradle :fabric:modrinth
gradle :velocity:modrinth