# hive-metastore-level2cache-distributed

This project describes how to install Ehcache as a third-party Level-2 cache in the DataNucleus module used by Hive metastore. I deployed Ehcache using Terracotta server to meet distributed needs. The reason why I plug this module in Hive is to solve metadata consistence problem in multi-metastore running environment. Depending on the distributed cache realization of Terracotta server, Ehcache can replace the original SOFT/WEAK type of Level-2 cache in Hive for metadata visiting cost.

# procedure

1. add jars

`mv [jar] /hive/lib`

  * datanucleus-cache-4.0.5.jar  # add third-party cache extendibility to DataNucleus
  * ehcache-2.10.3.jar  # cache module
  * terracotta-toolkit-runtime-4.3.4.jar  # support terracotta cache server
  
2. fix datanucleus[4.1.6] bug about using local Map\<Class,Meta\> but running in ehcache distributed way

`vi EnhancementHelper.java`

  * modify - Meta getMeta(Class pcClass)
  * try to initialize class once not in the local map
  * repackage datanucleus-core-4.1.6.jar in /hive/lib

3. modify ehcache-terracotta.xml and move to /hive/conf

`vi ehcache-terracotta.xml`

  * set terracottaConfig url
  * define cache 

4. modify tc-conf.xml and start terracotta server

`vi tc-conf.xml`

  * set server host in tc-conf.xml
  
`terracotta-4.3.4/server/bin/start-tc-server.sh -f tc-config.xml`

5. modify hive-site.xml and start hive cli to validate

`vi hive-site.xml`

  * datanucleus.cache.level2.type=ehcache
  * datanucleus.cache.level2.configurationFile=/ehcache-terracotta.xml
  * datanucleus.cache.level2.cacheName=basicCache
  * hive.metastore.cache.pinobjtypes= (leave one space)
  
6. validation

  * read naive_test_case.md

