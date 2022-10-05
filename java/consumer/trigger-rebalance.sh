#!/bin/bash
while true;
do
    pod=$(kubectl get pods | grep consumer | awk 'NR == 1 {print $1}')
    kubectl delete pod $pod
    sleep 60
done
