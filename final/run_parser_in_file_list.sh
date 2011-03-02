#!/bin/bash

COMMAND="python ../../cs224n/final/py/bparser.py"

echo Printing extracted sentences to file $2

touch $2
for file in $(cat $1)
do
  rm -f tmpfile
  $COMMAND $file tmpfile
  cat tmpfile >> $2
done
