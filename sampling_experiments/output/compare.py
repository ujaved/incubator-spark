#!/usr/bin/python

import sys
from os import listdir
from os.path import isfile, join


# Read in Arguments
if len(sys.argv) < 1:
    print 'Usage: python compare.py'
    exit()

# Iterate through all Ks
Ks = [10,20,30,40,50,60,70,80,90,100,200,300,400,500,600,700,800,900,1000,2000,3000,4000,5000,6000,7000,8000,9000,10000]

for k in Ks:
    # List all files in the directory
    files = [ f for f in listdir('.') if isfile(join('.',f)) ]
    files = [ f for f in files if ('_'+str(k)+'_') in f]

    # Rankings
    rankings = []

    # Matches
    matches = []

    # Read in the rankings
    for fname in files:
        ranking = []
        topk = []
        with open(fname) as f:
            content = f.readlines()
            for line in content:
                topk.append(line.strip().split(' ')[0])
        rankings.append([fname.strip('.txt').split('_')[2], topk])

    # Now compute the similarity
    precise = [r for r in rankings if r[0]=='precise'][0]
    rankings = [r for r in rankings if r[0]!='precise']
    for r in rankings:
        match = 0
        for w in r[1]:
            if w in precise[1]:
                match = match + 1
        matches.append([r[0], 1-float(match)/k])

    # Print output
    out_str = str(k)
    for m in matches:
        out_str = out_str + "\t" + str(m[1])
    print out_str
