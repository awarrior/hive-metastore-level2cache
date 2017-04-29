https://cwiki.apache.org/confluence/display/Hive/LanguageManual+DDL#LanguageManualDDL-RulesforColumnNames

# change table column name/type

* hive-cli-1: `drop table if exists xxx;`
* hive-cli-1: `create table xxx(a int);`
* hive-cli-2: alter table xxx change a b bigint;
* hive-cli-2: desc xxx;
* hive-cli-1: desc xxx;

# add/drop table column

* hive-cli-1: drop table if exists xxx;
* hive-cli-1: create table xxx(a int);
* hive-cli-2: alter table xxx add columns (b bigint);
* hive-cli-2: desc xxx;
* hive-cli-1: desc xxx;

* hive-cli-1: drop table if exists xxx;
* hive-cli-1: create table xxx(a int);
* hive-cli-2: alter table xxx replace columns (a bigint);
* hive-cli-2: desc xxx;
* hive-cli-1: desc xxx;
