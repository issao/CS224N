#!/usr/bin/python2.6

from operator import itemgetter
import os
import sys


file = open('words', 'r')

words = [line[0:-1] for line in file.readlines()]

words_p = {}
for line in sys.stdin.readlines():
  w, v = line.split()
  words_p[w] = v

for word in words:
  if word in words_p:
    print word, words_p[word]
  else:
    print word, "0.0"
