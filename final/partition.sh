#!/bin/bash

while read line
do
  set $line
  head -n $1 $3 > $3.tune
  tail -n $2 $3 > $3.train
done
