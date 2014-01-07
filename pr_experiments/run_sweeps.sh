#!/bin/bash

echo "Running sweeps on K"
for k in 10 100
do
    for s in 1 2 4 8 16 32
    do 
        echo "K = $k, merges = $s"
        #eval "../sbt/sbt 'run local input/toy.txt output/top 2 64 ${s} ${k} 1 false'"
        eval "../sbt/sbt 'run local input/sample.txt output/sample 2 64 ${s} ${k} 100 false'"
    done
done