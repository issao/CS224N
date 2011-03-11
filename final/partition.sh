#!/bin/bash
#
# After creating the file splits with ./create_train_and_tune_data.sh,
# use this by runnign "cat splits | ./partition.sh"
#
# That will create a .tune and .train file for each *.all corpora file,
# with the first 10% of lines and last 90% of lines, respectively.

while read line
do
  set $line
  head -n $1 $3 > $3.tune
  tail -n $2 $3 > $3.train
done
