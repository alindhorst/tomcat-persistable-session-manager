#!/bin/bash

CONSUL_IP=$(sudo docker inspect consul|jq .[].NetworkSettings.IPAddress|sed 's/"//g')
SERVICE_IDS=$(curl -s http://$CONSUL_IP:8500/v1/agent/services|jq ".[].ID"|egrep -v "\<consul\>"|sed 's/"//g')
NODE_IDS=$(curl -s http://localhost:8500/v1/catalog/nodes|jq .[].Node|egrep -v "\<master\>"|sed 's/"//g')

for SERVICE_ID in $SERVICE_IDS; do
 curl -s http://$CONSUL_IP:8500/v1/agent/service/deregister/$SERVICE_ID && echo "Service $SERVICE_ID removed"
done

for NODE_ID in $NODE_IDS; do
 echo "{ \"Datacenter\": \"dc1\",  \"Node\": \"$NODE_ID\"}"| curl -s -XPUT http://$CONSUL_IP:8500/v1/catalog/deregister -d @- && echo "Node $NODE_ID removed"
done