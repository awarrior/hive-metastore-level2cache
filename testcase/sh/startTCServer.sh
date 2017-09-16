#!/bin/bash

tc_home="/appcom/terracotta"
servername=$1
logfile=$2

nohup "${tc_home}/server/bin/start-tc-server.sh" -f "${tc_home}/server/tc-config-dist.xml" -n $servername &>> $logfile &
while true
do
        cnt=`grep 'successfully' $logfile | wc -l`
        if [ $cnt -gt 0 ] ;then
                break
        fi
	sleep 1s
done
