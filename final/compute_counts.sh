#!/bin/bash
#
# Compute unigram counts

mkdir counts
for file in $(ls *.all)
do
  cat $file | tr " " "\n"  | sort | uniq -c | less | sort -r -g > counts/$file
done

mkdir fracs
cd counts
for file in $(ls *.all)
do
  cat $file | awk "{print \$1/`cat $file | awk '{n+=$1} END {print n}'`, \$2}" > ../fracs/$file
done
