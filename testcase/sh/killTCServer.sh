#!/bin/bash

lst=`jps -lm | grep TCServerMain | awk '{print $1}'`
for id in "$lst"; do
	echo ${#id}
	if [ ! -z $id ] ;then
		echo `kill -9 $id`
	fi 
done
