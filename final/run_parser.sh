#!/bin/bash
#
# Helper script that runs the html parser that extracts the relevant
# English article text from the raw HTML that we crawled.
#
# To run use: cat sources | ./run_parser.sh

COMMAND=../repo/final/py/bparser.py

while read line
do
  set $line
  $COMMAND $1 $2&
done

wait
