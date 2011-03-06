#!/bin/bash

COMMAND="python ${HOME}/cs224n/final/py/bparser.py"

echo Printing extracted sentences from files listed in $1 to file $2.

touch $2
for file in $(cat $1)
do
  rm -f tmpfile-$1
  $COMMAND $file tmpfile-$1
  cat tmpfile-$1 >> $2
done
