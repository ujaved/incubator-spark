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

import scala.math._

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
    if (args.length < 8) {
      System.err.println("Usage: PageRank <master> <ifile> <ofile> <number_of_splits> <iterations> <updates> <topk> <numPartitions> <usePartitioner>")
      System.exit(1)
    }

    // Input arguments
    val ctx = new SparkContext(args(0), "PageRank",
      System.getenv("SPARK_HOME"), Seq(System.getenv("SPARK_EXAMPLES_JAR")))
    val lines = ctx.textFile(args(1), 1)
    val ofile = args(2)
    val splits = args(3).toInt
    val iters = args(4).toInt
    val updates = args(5).toInt
    val update_rate = (iters/updates).toInt
    val topk = args(6).toInt
    val numPartitions = args(7).toInt
    val usePartitioner = args(8).toBoolean

    // Read in files the results
    var merged_results = parseFile(ctx, lines, usePartitioner)

    // Precise execution
    val precise = computePR(ctx, merged_results, numPartitions, iters)

    // Approximate execution
    for (i <- 1 to updates) {
      val total_size = merged_results.count()
      val (s1, s2) = merged_results.collect().splitAt((total_size/2).toInt)
      val results1 = computePR(ctx, ctx.parallelize(s1), numPartitions, update_rate-1)
      val results2 = computePR(ctx, ctx.parallelize(s2), numPartitions, update_rate-1)
      merged_results = computePR(ctx, results1++results2, numPartitions, 1)
    }
    
    val approx = merged_results

    // Compute the RMSE
    val errors = (approx++precise).map{
      case (id,v) => (id, v.value)
    }.reduceByKey(
        (v1,v2) => (v1-v2)*(v1-v2)
    ).map{
      case (id,v) => v
    }.sum

    val RMSE = pow(errors/(approx.count()), 0.5)

    // Print the results
    printToFile((ofile+"_"+topk+"_precise.txt"), precise, topk)
    printToFile((ofile+"_"+topk+"_"+updates+".txt"), approx, topk)
    println("RMSE = " + RMSE)

    System.exit(0)
  }

  def parseFile(ctx: SparkContext, lines: RDD[String], usePartitioner: Boolean): RDD[(String, PRVertex)] = {
    println("Parsing input file...")
    var v = lines.map{ s =>
      val parts = s.split("\\s+")
      val id = parts(0)//
      var outEdges = parts.drop(1)//parts(1).split("\\s+")
      (id, new PRVertex(1.0E-4, outEdges))
    }
    println("Done parsing input file.")

    if (usePartitioner)
      v = v.partitionBy(new HashPartitioner(ctx.defaultParallelism)).cache
    else
      v = v.cache

    v
  }
  
  def computePR(ctx: SparkContext, inputs: RDD[(String, PRVertex)], numPartitions: Int, iter: Int): RDD[(String, PRVertex)] = {
    val numVertices = inputs.count()
    val epsilon = 0.01 / numVertices
    val messages = ctx.parallelize(Array[(String, PRMessage)]())
    val utils = new PageRankUtils
    val result =
        Bagel.run(
          ctx, inputs, messages, combiner = new PRCombiner(),
          numPartitions = numPartitions)(
          utils.computeWithCombiner(numVertices, epsilon, iter))

    result
  }

  def printToFile(file: String, results: RDD[(String, PRVertex)], topk: Integer) {
    val p = new PrintWriter(new File(file))
    val top =
      (results
       .map { case (id, vertex) => (id, vertex.value) }
       .collect
       .sortBy(- _._2)
       .take(topk))

    try { top.foreach(tup => p.println(tup._1 + " " + tup._2)) } finally { p.close() }
  }

}

