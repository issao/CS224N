#!/usr/bin/python2.6

import os
import re
import string
import sys
from BeautifulSoup import BeautifulSoup
from BeautifulSoup import Comment


def gettext(el):
  if isinstance(el.string, Comment):
    return ""
  if el.string:
    return el.string
  return ' '.join([gettext(e) for e in el.contents])


def predecode(text):
  return re.sub(
      'https?://[^\s]*',
      '[URL]',
      re.sub(
      '[\t\r\f\v ]+', ' ',
      text.encode('ascii', 'ignore').replace('"', '').replace('*', '')).strip().lower())


def outputParsedText(raw_text, outfile):
  if not raw_text:
    return
  text = predecode(raw_text)
  # Remove short paragraphs.
  if string.count(re.sub('\s+', ' ', text), ' ') < 10:
    return
  # Get rid of single sentences.
  if string.count(re.sub('\.\.\.', '.', text), '. ') < 2:
    return
  # Split by line first.
  for line in text.split('\n'):
    outputParsedTextFromLine(line, outfile)


def outputParsedTextFromLine(text, outfile):
  # Remove short lines.
  if string.count(re.sub('\s+', ' ', text), ' ') < 10:
    return
  if text.count('copyright'):
    return
  outfile.write(text + '\n')


def parse(doc, output):
  file = open(doc)
  outfile = open(output, 'a+')
  root = BeautifulSoup(file.read(), convertEntities=BeautifulSoup.HTML_ENTITIES)
  for foo in root.findAll(['option', 'style', 'script']):
    foo.extract()
  for element in root.findAll('p'):
    outputParsedText(gettext(element), outfile)


if len(sys.argv) < 3:
  print "Please include a dir to parse and a destination file"
else:
  for root, _, files in os.walk(sys.argv[1]):
    for f in files:
      parse(os.path.join(root, f), sys.argv[2])
