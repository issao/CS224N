#!/bin/bash
#
# One-liner that computes 
#

wc -l data/parsed/*.all | awk '{printf "%d %d %s\n", $1/10, $1/10 * 9, $2}' > splits
