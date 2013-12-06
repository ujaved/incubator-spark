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
import org.apache.spark.bagel._
import org.apache.spark.bagel.Bagel._

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext._
import java.io._

/**
 * Computes the PageRank of URLs from an input file. Input file should
 * be in format of:
 * URL         neighbor URL
 * URL         neighbor URL
 * URL         neighbor URL
 * ...
 * where URL and their neighbors are separated by space(s).
 */
object PageRank {
  def main(args: Array[String]) {
    if (args.length < 7) {
      System.err.println("Usage: PageRank <master> <ifile1> <ifile2> <ofile> <number_of_iterations> <numPartitions> <usePartitioner>")
      System.err.println("example: ../sbt/sbt 'run local input/tiny_1.txt input/tiny_2.txt output/tiny 10 1 false'")
      System.exit(1)
    }

    // Input arguments
    val ctx = new SparkContext(args(0), "PageRank",
      System.getenv("SPARK_HOME"), Seq(System.getenv("SPARK_EXAMPLES_JAR")))
    val lines1 = ctx.textFile(args(1), 1)
    val lines2 = ctx.textFile(args(2), 1)
    val lines = lines1 ++ lines2
    val ofile = args(3)
    var iter = args(4).toInt
    val numPartitions = args(5).toInt
    val usePartitioner = args(6).toBoolean

    // Compute the results
    var result1 = parseFile(ctx, lines1, usePartitioner, numPartitions, iter)
    val result2 = parseFile(ctx, lines2, usePartitioner, numPartitions, iter)
    val result  = parseFile(ctx, lines, usePartitioner, numPartitions, iter+1).sortByKey(true)
    val merged_result = reduceResults(ctx, result1, result2, usePartitioner, numPartitions).sortByKey(true)

    // Print the results
    printToFile((ofile+"_1.txt"), result1)
    printToFile((ofile+"_2.txt"), result2)
    printToFile((ofile+".txt"), result)
    printToFile((ofile+"_merged.txt"), merged_result)

    System.exit(0)
  }

  def parseFile(ctx: SparkContext, lines: RDD[String], usePartitioner: Boolean, numPartitions: Int, iter: Int): RDD[(String, PRVertex)] = {
    println("Parsing input file...")
    val numVertices = lines.count()
    var v = lines.map{ s =>
      val parts = s.split(":")
      val id = parts(0)
      val outEdges = parts(1).split("\\s+")

      (id, new PRVertex(1.0E-4, outEdges))
    }
    println("Done parsing input file.")
    
    if (usePartitioner)
      v = v.partitionBy(new HashPartitioner(ctx.defaultParallelism)).cache
    else
      v = v.cache

    val epsilon = 0.01 / numVertices
    val messages = ctx.parallelize(Array[(String, PRMessage)]())
    val utils = new PageRankUtils
    val result =
        Bagel.run(
          ctx, v, messages, combiner = new PRCombiner(),
          numPartitions = numPartitions)(
          utils.computeWithCombiner(numVertices, epsilon, iter))

    result
  }

  def reduceResults(ctx: SparkContext, v1: RDD[(String, PRVertex)], v2: RDD[(String, PRVertex)], usePartitioner: Boolean, numPartitions: Int): RDD[(String, PRVertex)] = {
        val v = v1++v2
        val vCount = v.count()
        val epsilon = 0.01 / vCount
        val messages = ctx.parallelize(Array[(String, PRMessage)]())
        val utils = new PageRankUtils
        Bagel.run(
          ctx, v, messages, combiner = new PRCombiner(),
          numPartitions = numPartitions)(
          utils.computeWithCombiner(vCount, epsilon, 1))
  }

  def printToFile(file: String, results: RDD[(String, PRVertex)]) {
    val p = new PrintWriter(new File(file))
    val top =
      (results
       .map { case (id, vertex) => "%s\t%s".format(id, vertex.value) }
       .collect)

    try { top.foreach(tup => p.println(tup)) } finally { p.close() }
  }

}

