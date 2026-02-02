#!/bin/bash

gradle shadowJar
./test-server/deploy.sh