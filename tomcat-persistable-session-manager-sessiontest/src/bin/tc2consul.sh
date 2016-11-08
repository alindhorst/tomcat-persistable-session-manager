#!/bin/bash

BASE_DIR=${0%/*}
DOCKER_IDS=$(sudo docker ps|grep -v "^CONTAINER"|awk '/sessiontest/{printf "%s ",$1}')
DOCKER_IPS=$(sudo docker inspect $DOCKER_IDS|jq .[].NetworkSettings.IPAddress|sed 's/"//g'|awk '{printf "%s ",$1}')

typeset -i INDEX=1;
for DOCKER_IP in $DOCKER_IPS; do
 sed "s/<IP>/$DOCKER_IP/g ; s/<INDEX>/$INDEX/g" $BASE_DIR/consul_templates/tc_node.json| curl -s -XPUT 'http://127.0.0.1:8500/v1/catalog/register' -d @- >/dev/null
 sed "s/<IP>/$DOCKER_IP/g ; s/<INDEX>/$INDEX/g" $BASE_DIR/consul_templates/tc_svc.json| curl -s -XPUT 'http://127.0.0.1:8500/v1/agent/service/register' -d @- >/dev/null
 INDEX=$INDEX+1
done