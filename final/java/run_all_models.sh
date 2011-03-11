#!/bin/bash
#
# Train models for all the corpora we have obtained and evaluate each
# corpora against each of these models.

for m in $(cat ../models_list | tr "," " ")
do
  ./run -train $m.train -valid $m.tune -test $(cat ../models_list)
done