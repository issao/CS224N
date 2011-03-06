#!/bin/bash

COMMAND="python ${HOME}/cs224n/final/py/bparser.py"

echo Printing extracted sentences from directory $1 to file $2.

touch $2
for file in $(find $1 -type f -print)
do
  TMPFILE=$(mktemp)
  $COMMAND $file $TMPFILE
  cat $TMPFILE >> $2
  rm $TMPFILE
done
