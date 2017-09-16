#!/bin/bash
# Main Script - 30.10.16.6

# config
user="hadoop"
logpath="/tmp/$user"
shpath="/home/$user"
servers=('30.10.16.4' '30.10.16.5')

# copy sh
echo "copy startTCServer.sh killTCServer.sh to remote servers"
for i in ${servers[*]} ;do
        scp -p killTCServer.sh startTCServer.sh hadoop@$i:$shpath
done

# show cmd
set -v

# clean logs
./sshCmd.sh ${servers[*]} "echo '' > ${logpath}/tcserver.log"

### TEST LOOP ###
# common modules
killServers() {
./sshCmd.sh ${servers[*]} "${shpath}/killTCServer.sh"
}
startServers() {
./sshCmd.sh ${servers[0]} "${shpath}/startTCServer.sh gbd_tc_server1 ${logpath}/tcserver.log"
./sshCmd.sh ${servers[1]} "${shpath}/startTCServer.sh gbd_tc_server2 ${logpath}/tcserver.log"
}
jarname="CacheTestDist.jar"
classname="CacheTestDist"
acmd="java -cp $jarname:$HIVE_CONF_DIR:`hadoop classpath`:$HIVE_LIB/* $classname 32 10000"
iterate() {
killServers;startServers
$acmd 1.0 $seeds
killServers;startServers
$acmd 0.75 $seeds
killServers;startServers
$acmd 0.5 $seeds
}

# loop
seeds="0 0 0";iterate
seeds="1 1 1";iterate
seeds="2 2 2";iterate
### TEST LOOP ###
