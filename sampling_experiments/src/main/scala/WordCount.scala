/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io._

import org.apache.spark.SparkContext._
import org.apache.spark.SparkContext


/**
 * Computes the WordCount from an input file. Input file should
 * be in format of:
 * URL         neighbor URL
 * URL         neighbor URL
 * URL         neighbor URL
 * ...
 * where URL and their neighbors are separated by space(s).
 */
object WordCount {
  def main(args: Array[String]) {
    if (args.length < 4) {
      System.err.println("Usage: WordCount <master> <file> <k> <sampling>")
      System.exit(1)
    }
    val ctx = new SparkContext(args(0), "WordCount",
      System.getenv("SPARK_HOME"), Seq(System.getenv("SPARK_EXAMPLES_JAR")))
    val lines = ctx.textFile(args(1), 1)
    val counts = lines.flatMap(line => line.toLowerCase().split(" ").filter(x => x!= "")).sample(false, args(3).toFloat, 0xDEADEEF).map(word => (word,1)).reduceByKey(_+_,8)
    val sortedCounts = counts.collect().sortBy(- _._2)

    val output = sortedCounts.take(args(2).toInt)

    if (args(3)=="1") {
      printToFile(new File("output/top_"+args(2)+"_precise.txt"))(p => {output.foreach(tup => p.println(tup._1 + " " + tup._2))})
    } else {
      printToFile(new File("output/top_"+args(2)+"_"+args(3)+".txt"))(p => {output.foreach(tup => p.println(tup._1 + " " + tup._2))})
    }

    System.exit(0)
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }
}

