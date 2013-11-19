#!/usr/bin/python 
from pyspark import SparkContext
import sys
import os
import logging
import logging.handlers
import resource
import time
import subprocess, threading
from collections import defaultdict
from os.path import realpath
from operator import add
from helper import parseRecord, getCDFList, FIELD, HO_TYPE
    
if __name__ == "__main__":
    if len(sys.argv) < 3:
        print >> sys.stderr, "Usage: wc <master> <input dir> <log file>"
        exit(-1)

    sys.stdout = open('o.txt', 'w')
    master = sys.argv[1]
    input_dir = sys.argv[2]
    log_file = sys.argv[3]
    sys.stderr = open(log_file, 'w')
    
    #sc = SparkContext("local[" + numCores + "]" , "job", pyFiles=[realpath('helper.py')])
    sc = SparkContext(master , "job", pyFiles=[realpath('helper.py')])
    lines = sc.textFile(input_dir + '*')
    counts = lines.flatMap(lambda x: x.split(' ')).map(lambda x: (x, 1)).reduceByKey(add,64)
    output = counts.collect()
    
