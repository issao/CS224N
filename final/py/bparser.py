#!/usr/bin/python2.6
import re
import string
import sys
from BeautifulSoup import BeautifulSoup

def gettext(el):
  if el.string:
    return el.string
  return ''.join([gettext(e) for e in el.contents])

def decode(text):
  return text.replace('&quot;', '"')

def parse(doc):
  file = open(doc)
  root = BeautifulSoup(file.read())
  for foo in root.findAll('option'):
    foo.extract()
  for element in root.findAll('p'):
    if element.string:
      print decode(element.string)
    else:
     text = gettext(element)
     if text and string.count(re.sub('\s+', ' ', text), ' ') > 4:
       print decode(text)

if len(sys.argv) < 2:
  print "Please include a file to parse"
else:
  parse(sys.argv[1])
