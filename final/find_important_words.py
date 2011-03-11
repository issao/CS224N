#!/usr/bin/python2.6

from operator import itemgetter
import os
import sys

word_max_probability = {}
word_min_probability = {}
for line in sys.stdin.readlines():
  v = line.split()
  if len(v) == 2:
    word = v[1]
    p = float(v[0])
    if not word in word_max_probability:
      word_max_probability[word] = p
      word_min_probability[word] = p
    else:
      if p > word_max_probability[word]:
        word_max_probability[word] = p
      if p < word_min_probability[word]:
        word_min_probability[word] = p

word_probability_variation = {}
for word in word_min_probability:
  if word_min_probability[word] > 0.00001:  # Arbitrary?
    word_probability_variation[word] = word_max_probability[word] / word_min_probability[word]

for word, p_var in sorted(word_probability_variation.iteritems(), key=itemgetter(1), reverse=True):
  print word, p_var
