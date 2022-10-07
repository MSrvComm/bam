#!/bin/bash
tm=10
while true;
do
    kubectl scale --replicas=4 deployment/consumer-svc
    sleep $tm
    kubectl scale --replicas=3 deployment/consumer-svc
    sleep $tm
done
