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

NEW_VERSION=$1
[[ "$NEW_VERSION" != *-SNAPSHOT ]] && NEW_VERSION="${NEW_VERSION}-SNAPSHOT"

BASE_VERSION="${NEW_VERSION%-SNAPSHOT}"
CHANGELOG="$(dirname "$0")/CHANGELOG.md"

if ! grep -q "^## ${BASE_VERSION}$" "$CHANGELOG"; then
    echo "No changelog entry found for ${BASE_VERSION} — adding TODO placeholder."
    sed -i "0,/^## /{s/^## /## ${BASE_VERSION}\n\n### Changed\n\n- TODO: Add changelog entry for ${BASE_VERSION}\n\n## /}" "$CHANGELOG"
fi

# set version in all poms
./mvnw versions:set -DnewVersion="$NEW_VERSION"
./mvnw versions:commit

# set npm ui version (package.json & package-lock.json)
cd *-ui*
npm version --allow-same-version "$BASE_VERSION"