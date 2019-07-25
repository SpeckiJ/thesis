#!/bin/sh

# Base code taken from https://stackoverflow.com/questions/17600622/shell-script-for-insert-multiple-records-into-a-database
while true;
do
  randomVarchar=`echo $(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 40 | head -n 1)`
  randomInteger=`echo $(cat /dev/urandom | tr -dc '0-9' | fold -w 9 | head -n 1)`
  psql -U postgres -d intueri_demo -c 'INSERT INTO demo VALUES(DEFAULT,'\'$randomVarchar\'', '\'$randomInteger\'', null, null)'
  sleep 10s
done
