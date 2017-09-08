# hive-metastore-level2cache-distributed

This project describes how to install Ehcache as a third-party Level-2 cache in the DataNucleus module used by Hive metastore. I deployed Ehcache using Terracotta server to meet distributed needs. The reason why I plug this module in Hive is to solve metadata consistence problem in multi-metastore running environment, supporting HA (High Availability). With the help of  Terracotta server (data management solution), Ehcache in different metastore servers can cache data with consistence so that replace the original SOFT/WEAK type of Level-2 cache in Hive.

# procedure

1. add these jars to hive lib (hive clients/metastore servers)

  * `datanucleus-cache-4.0.5.jar`  # add third-party cache extendibility to DataNucleus
  * `ehcache-2.10.3.jar`  # cache module
  * `terracotta-toolkit-runtime-4.3.4.jar`  # support terracotta cache server
  
2. fix the bug in datanucleus-4.1.6 that using local Map\<Class,Meta\> registeredClasses collection but running in distributed way (hive clients/metastore servers)

  * patch `EnhancementHelper.java` with `EnhancementHelper.patch`
  * repackage `datanucleus-core-4.1.6.jar` and move to hive lib

P.S. The exception named 'Cannot lookup meta info for class xxx - nothing found' is threw out at the startup of second Hive Metastore service. The reason I found is that Datanucleus fetches Class object from distributed cache through Ehcache and get its metadata directly which is not loaded and registered locally in the second Hive Metastore.

https://github.com/datanucleus/datanucleus-core/pull/260

3. modify `ehcache-terracotta.xml` and move to hive conf (hive clients)

  * set terracottaConfig url like \<terracottaConfig url="ip1:port1,ip2:port2" /\>
  * modify basic cache definition 

4. modify `tc-conf.xml` in terracotta conf and start terracotta server with command `terracotta-4.3.4/server/bin/start-tc-server.sh -f tc-config.xml` (terracotta)

  * define one server host or sever array (copy existing \<server\> config for cache HA) in tc-conf.xml
  * change data/logs storage directories if necessary

5. modify `hive-site.xml` in hive conf and start two hive metastore servers (metastore servers)

  * datanucleus.cache.level2.type=ehcache
  * hive.metastore.cache.pinobjtypes= (leave one space)
  
  add these properties to `hive-site.xml`
  
  * datanucleus.cache.level2.configurationFile=/ehcache-terracotta.xml
  * datanucleus.cache.level2.cacheName=basicCache
  
P.S. The reason why I set pinobjtypes null is that 'pinAll(Class,boolean) method not supported by this plugin'. Ehcache plugin cannot pin specified objects but any cache block.
  
6. validation

  * read testcase/README.md

# performance

To ehcache+terracotta across hive metastore server, for single Table(hive) object, the read speed through 1 PC (8 cores) can reach 30,675 times per second, and the write speed can reach more than 5,219 times per second.

To hive metastore server with ehcache+terracotta, for single Table object, the read speed through 1 PC slows down to 121 times per second, and the write speed is 46 times per second. Even though we add 2 more PCs, the read/write speed does not change obviously (so that we know the bottleneck isn't in the client endpoint).

The cache mode explained above only owns 14% read speed of SOFT level2-cache type in metastore, but nearly the same to cache write speed. That is the question waiting to research.
