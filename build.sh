#!/bin/bash

gradle :velocity:shadowJar
./test-server/deploy.sh