# hive-metastore-level2cache-distributed

This project describes how to install Ehcache as a third-party Level-2 cache in the DataNucleus module used by Hive metastore. I deployed Ehcache using Terracotta server to meet distributed needs. The reason why I plug this module in Hive is to solve metadata consistence problem in multi-metastore running environment. Depending on the distributed cache realization of Terracotta server, Ehcache can replace the original SOFT/WEAK type of Level-2 cache in Hive for metadata visiting cost.

# procedure

1. add jars (hive clients/metastore servers)

`mv [jar] /hive/lib`

  * datanucleus-cache-4.0.5.jar  # add third-party cache extendibility to DataNucleus
  * ehcache-2.10.3.jar  # cache module
  * terracotta-toolkit-runtime-4.3.4.jar  # support terracotta cache server
  
2. fix datanucleus-4.1.6 bug about using local Map\<Class,Meta\> but running in ehcache distributed way (hive clients/metastore servers)

`vi EnhancementHelper.java`

  * modify - Meta getMeta(Class pcClass)
  * try to initialize class once not in the local map
  * repackage datanucleus-core-4.1.6.jar in /hive/lib

P.S. The original method throws 'Cannot lookup meta info for class xxx - nothing found' exception in the second metastore service startup, because Datanucleus notices the missing class had been registered before (in the first metastore) through distributed cache so that it doesn't register again. Without local save, the second metastore startup find nothing for meta info initialization.

3. modify ehcache-terracotta.xml and move to /hive/conf (hive clients)

`vi ehcache-terracotta.xml`

  * set terracottaConfig url like \<terracottaConfig url="ip1:port1,ip2:port2" /\>
  * define cache 

4. modify tc-conf.xml and start terracotta server

`vi tc-conf.xml`

  * define one server host or sever array (copy \<server\> config) in tc-conf.xml
  * set data/logs storage directories
  
`terracotta-4.3.4/server/bin/start-tc-server.sh -f tc-config.xml`

5. modify hive-site.xml and start hive cli to validate (metastore servers)

`vi hive-site.xml`

  * datanucleus.cache.level2.type=ehcache
  * datanucleus.cache.level2.configurationFile=/ehcache-terracotta.xml
  * datanucleus.cache.level2.cacheName=basicCache
  * hive.metastore.cache.pinobjtypes= (leave one space)
  
P.S. The reason why I set pinobjtypes null is that 'pinAll(Class,boolean) method not supported by this plugin'. Maybe this will be supported if we add pinAll realization.
  
6. validation

  * read testcase/functional_test.md

# performance

To ehcache+terracotta across hive metastore server, for single Table(hive) object, the read speed through 1 PC (8 cores) can reach 30,675 times per second, and the write speed can reach more than 5,219 times per second.

To hive metastore server with ehcache+terracotta, for single Table object, the read speed through 1 PC slows down to 121 times per second, and the write speed is 46 times per second. Even though we add 2 more PCs, the read/write speed does not change obviously (so that we know the bottleneck isn't in the client endpoint).

The cache mode explained above only owns 14% read speed of SOFT level2-cache type in metastore, but nearly the same to cache write speed. That is the question waiting to research.
