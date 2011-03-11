#!/usr/bin/python2.6

from operator import itemgetter
import os
import sys


file = open('a', 'r')

words = [line[0:-1] for line in file.readlines()]

print words
