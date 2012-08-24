#!/bin/bash
while read pid; do
  echo ${pid} | ./get_programpid_from_shardpid.sh | ./get_pbcore_from_programpid.sh
done
