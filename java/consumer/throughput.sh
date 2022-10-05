#!/bin/bash
pod1=$(kubectl get pods | grep consumer | awk 'NR==1 {print $1}')
pod2=$(kubectl get pods | grep consumer | awk 'NR==2 {print $1}')
pod3=$(kubectl get pods | grep consumer | awk 'NR==3 {print $1}')

kubectl logs $pod1 | grep Customer > pod1.txt
kubectl logs $pod2 | grep Customer > pod2.txt
kubectl logs $pod3 | grep Customer > pod3.txt

tp1=$(sort pod1.txt | uniq -c | wc -l)
tp2=$(sort pod2.txt | uniq -c | wc -l)
tp3=$(sort pod3.txt | uniq -c | wc -l)

echo $tp1
echo $tp2
echo $tp3