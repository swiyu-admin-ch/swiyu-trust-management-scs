#!/bin/bash

# With this command you can set a new version in all pom.xml and ui project npm files.
# Run it in the root folder of your project
#
# usage in Terminal:  ./setVersions.sh 1.0.0-SNAPSHOT
#
# after this mvn clean install and commit and push it to the develop branch
#

if [ "$#" -ne 1 ]; then
  echo "Usage: ./setVersions.sh <new-version>"
  echo "Example: ./setVersions.sh 1.0.0-SNAPSHOT"
  exit 1
fi

# set version in all poms
./mvnw versions:set -DnewVersion=$1
./mvnw versions:commit

# set npm ui version (package.json & package-lock.json)
cd *-ui*
npm version --allow-same-version "${1/-SNAPSHOT/}" # remove '-SNAPSHOT'