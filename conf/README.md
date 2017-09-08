# ehcache-terracotta.xml

* basic cache property

maxBytesLocalHeap (must smaller than the metastore server jvm maximum heap size)

# tc-config.xml

* install terracotta cache array

`<server host="192.168.56.101" name="tc_server1" bind="0.0.0.0">...</server>`

`<server host="192.168.56.102" name="tc_server2" bind="0.0.0.0">...</server>`

# server/bin/start-tc-server.sh

* change heap size of terracotta 

`eval ${JAVA_COMMAND} -Xms4g -Xmx4g -XX:+HeapDumpOnOutOfMemoryError \`
