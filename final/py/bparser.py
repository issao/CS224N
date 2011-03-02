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
  return text.encode('ascii', 'ignore').replace('&quot;', '"')

def parse(doc, output):
  file = open(doc)
  outfile = open(output, 'w')
  root = BeautifulSoup(file.read())
  for foo in root.findAll('option'):
    foo.extract()
  for element in root.findAll('p'):
    if element.string:
      outfile.write(decode(element.string) + '\n')
    else:
     text = gettext(element)
     if text and string.count(re.sub('\s+', ' ', text), ' ') > 4:
       outfile.write(decode(text) + '\n')

if len(sys.argv) < 3:
  print "Please include a file to parse and a destination file"
else:
  parse(sys.argv[1], sys.argv[2])
