#!/bin/bash
#
# To run use: cat sources | ./run_parser.sh

COMMAND=../repo/final/py/bparser.py

while read line
do
  set $line
  $COMMAND $1 $2&
done

wait
