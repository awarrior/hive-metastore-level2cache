# hive-metastore-level2cache

This project describes how to install Ehcache as a third-party level-2 cache in the DataNucleus module used by Hive metastore.

# procedure

1. add jars
`mv [jar] /hive/lib`
  * datanucleus-cache-4.0.5.jar  # add third-party cache extendibility to DataNucleus
  * ehcache-2.10.3.jar  # cache module
  * terracotta-toolkit-runtime-4.3.4.jar  # support terracotta cache server

2. modify tc-conf.xml and start terracotta server
`vi tc-conf.xml`
  * set server host in tc-conf.xml
`terracotta-4.3.4/server/bin/start-tc-server.sh -f tc-config.xml`

3. modify hive-site.xml and start hive cli to validate
`vi hive-site.xml`
  * <name>datanucleus.cache.level2.type</name><value>ehcache</value>
  * <name>datanucleus.cache.level2.configurationFile</name><value>/ehcache-terracotta.xml</value>
  * <name>datanucleus.cache.level2.cacheName</name><value>basicCache</value>
  
