#!/bin/bash

DOMS=http://naiad:7880/fedora
USER=fedoraReadOnlyAdmin
PASS=7HRphHtn
while read pid; do
  # OSX
  curl --user ${USER}:${PASS} ${DOMS}/objects/uuid%3A${pid}/datastreams/SHARD_METADATA/content
  # Linux
  # wget -q -O - --http-user=$USER --http-password=$PASS ${DOMS}/objects/uuid%3A${pid}/datastreams/SHARD_METADATA/content
done
