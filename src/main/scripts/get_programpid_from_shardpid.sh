#!/bin/bash
#select $program from <#ri> where $program <http://doms.statsbiblioteket.dk/relations/default/0/1/#hasShard> <info:fedora/uuid:0fd9644a-78f9-4598-bcb9-c5b351d815a8> 

DOMS=http://naiad:7880/fedora
USER=fedoraReadOnlyAdmin
PASS=7HRphHtn

while read pid; do
  QUERY=select%20%24program%20from%20%3C%23ri%3E%20where%20%24program%20%3Chttp%3A%2F%2Fdoms.statsbiblioteket.dk%2Frelations%2Fdefault%2F0%2F1%2F%23hasShard%3E%20%3Cinfo%3Afedora%2Fuuid%3A${pid}%3E%20
  # OSX
  curl --user ${USER}:${PASS} ${DOMS}'/risearch?type=tuples&lang=iTQL&flush=true&format=CSV&query='${QUERY} | grep -v program | sed 's!info:fedora/uuid:!!'
  # Linux
  # wget -q -O - --http-user=$USER --http-password=$PASS ${DOMS}'/risearch?type=tuples&lang=iTQL&flush=true&format=CSV&query='${QUERY} | grep -v program | sed 's!info:fedora/uuid:!!'
done
