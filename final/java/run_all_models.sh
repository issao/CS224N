#!/bin/bash
#
# Train models for all the corpora we have obtained and evaluate each
# corpora against each of these models.

for m in $(cat ../model_list | tr "," " ")
do
  echo ./run -train $m -tes $(cat ../model_list)
done