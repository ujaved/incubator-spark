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
    if (args.length < 6) {
      System.err.println("Usage: PageRank <master> <ifile> <ofile> <number_of_iterations> <numPartitions> <usePartitioner>")
      System.exit(1)
    }

    val ctx = new SparkContext(args(0), "PageRank",
      System.getenv("SPARK_HOME"), Seq(System.getenv("SPARK_EXAMPLES_JAR")))
    val lines = ctx.textFile(args(1), 1)
    var iters = args(3).toInt
    val numPartitions = args(4).toInt
    val usePartitioner = args(5).toBoolean

    println("Parsing input file...")
    val numVertices = lines.count()
    var vertices = lines.map{ s =>
      val parts = s.split(":")
      val id = parts(0)
      val outEdges = parts(1).split("\\s+")

      (id, new PRVertex(1.0 / numVertices, outEdges))
    }
    if (usePartitioner)
      vertices = vertices.partitionBy(new HashPartitioner(ctx.defaultParallelism)).cache
    else
      vertices = vertices.cache
    println("Done parsing input file.")

    // Do the computation
    val epsilon = 0.01 / numVertices
    val messages = ctx.parallelize(Array[(String, PRMessage)]())
    val utils = new PageRankUtils
    val result =
        Bagel.run(
          ctx, vertices, messages, combiner = new PRCombiner(),
          numPartitions = numPartitions)(
          utils.computeWithCombiner(numVertices, epsilon))

    val writer = new PrintWriter(new File(args(2)))


    // Print the results
    val top =
      (result
       .map { case (id, vertex) => "%s\t%s".format(id, vertex.value) }
       .collect)
    printToFile(new File(args(2)))(p => {top.foreach(tup => p.println(tup))})

    System.exit(0)
  }

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }
}

