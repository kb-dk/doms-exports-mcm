## This is the initial query which generates the date files

DOMS=http://alhena:7880/fedora
USER=fedoraReadOnlyAdmin
PASS=fedoraReadOnlyPass

curl --user ${USER}:${PASS} "${DOMS}/risearch?type=tuples&lang=iTQL&flush=true&format=CSV&query=select%20%24label%20%20%24url%20from%20%3C%23ri%3E%20where%20%24object%20%3Cfedora-model%3Alabel%3E%20%24label%20and%20%24object%20%3Cfedora-model%3AhasModel%3E%20%3Cfedora%3Adoms%3AContentModel_Shard%3E%20and%20%24object%20%3Cfedora-model%3Astate%3E%20%3Cfedora-model%3AActive%3E%20and%20%24object%20%3Chttp%3A%2F%2Fdoms.statsbiblioteket.dk%2Frelations%2Fdefault%2F0%2F1%2F%23consistsOf%3E%20%24file%20and%20%24file%20%3Cfedora-model%3Alabel%3E%20%24url"
#wget -O devel_pid.txt -q --user=${USER} --password=${PASS} "${DOMS}/risearch?type=tuples&lang=iTQL&flush=true&format=CSV&query=select%20%24label%20%20%24url%20from%20%3C%23ri%3E%20where%20%24object%20%3Cfedora-model%3Alabel%3E%20%24label%20and%20%24object%20%3Cfedora-model%3AhasModel%3E%20%3Cfedora%3Adoms%3AContentModel_Shard%3E%20and%20%24object%20%3Cfedora-model%3Astate%3E%20%3Cfedora-model%3AActive%3E%20and%20%24object%20%3Chttp%3A%2F%2Fdoms.statsbiblioteket.dk%2Frelations%2Fdefault%2F0%2F1%2F%23consistsOf%3E%20%24file%20and%20%24file%20%3Cfedora-model%3Alabel%3E%20%24url"