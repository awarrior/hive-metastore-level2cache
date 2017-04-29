# change table column name/type

hive-cli-1: drop table if exists xxx;
hive-cli-1: create table xxx(a int);
hive-cli-1: desc xxx;
hive-cli-2: desc xxx;
hive-cli-2: alter table xxx change a b bigint;
hive-cli-2: desc xxx;
hive-cli-1: desc xxx; # validation-1
hive-cli-1: alter table xxx change b c string;
hive-cli-1: desc xxx;
hive-cli-2: desc xxx; # validation-2

# add/drop table column

hive-cli-1: drop table if exists yyy;
hive-cli-1: create table yyy(a int);
hive-cli-1: desc yyy;
hive-cli-2: desc yyy;
