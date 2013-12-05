#!/bin/bash

echo "Running sweeps on K"
for k in 10 20 30 40 50 60 70 80 90 100 200 300 400 500 600 700 800 900 1000 2000 3000 4000 5000 6000 7000 8000 9000 10000
do
    for s in 1 0.5 0.1 0.05 0.01 0.005 0.001 0.0005 0.0001
    do 
        echo "K = $k, sampl = $s"
        eval "../sbt/sbt 'run local prideandprejudice.txt ${k} ${s}'"
    done
done