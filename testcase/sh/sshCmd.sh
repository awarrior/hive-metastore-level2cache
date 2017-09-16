#!/bin/bash
#set -e
echo "(sshCmd) start"
if [ $# -lt 2 ];then
	echo "Usage: $0 host1 host2 ... 'command'"
	exit 0
else
	cmd=${!#}
fi
i=1
for ip in $@;do
	if [ $i -eq $# ];then
		break
	fi
	logfile="/tmp/$0-${ip}.log"
	ssh $ip $cmd &> $logfile
	if [ $? -eq 0 ];then
		echo "[success] $ip"
	else
		echo "!!!!!!!!![failed] $ip"
	fi
	((i++))
done
echo "(sshCmd) end"
