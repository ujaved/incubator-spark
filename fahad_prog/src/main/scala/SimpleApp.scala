/*** SimpleApp.scala ***/
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import java.io._
import org.apache.spark.scheduler
import org.apache.spark.scheduler.JobLogger

object SimpleApp {
  def main(args: Array[String]) {
    /*if (args.length < 3) {
      System.err.println("Usage: PageRank <master> <file> <number_of_iterations>")
      System.exit(1)
    }*/
    var iters = 6
    //args(2).toInt
    val ctx = new SparkContext("local", "PageRank",
      System.getenv("SPARK_HOME"), Seq(System.getenv("SPARK_EXAMPLES_JAR")))

    val listener = new JobLogger("job")
	ctx.addSparkListener(listener)

    val lines = ctx.textFile("mr_wiki_data2.txt", 1)//"sm_data1.txt",1)
    val links = lines.map{ s =>
      val parts = s.split("\\s+")
      (parts(0), parts(1))
    }.distinct().groupByKey().cache()
    var ranks = links.mapValues(v => 1.0)
    //ranks.foreach(rank => println(rank))
    for (i <- 1 to iters) {
      val contribs = links.join(ranks).values.flatMap{ case (urls, rank) =>
        val size = urls.size
        urls.map(url => (url, rank / size))
      }
      //contribs.saveAsTextFile("rdd.txt")
      //contribs.toArray().foreach(ele => println(ele))
      ranks = contribs.reduceByKey(_ + _).mapValues(0.15 + 0.85 * _)
      //ranks.toArray().foreach(ele => println(ele))
    }

    /*if (1!=0) {
    	val lines = ctx.textFile("partial_results.txt", 1)//"sm_data1.txt",1)
	    val links = lines.map{ s =>
	      val parts = s.split(",")
	      (parts(0), parts(1))
	    }.distinct().groupByKey().cache()

    }*/

    val writer = new PrintWriter(new File("res6_2.txt" ))
    val output = ranks.collect()
    //output.foreach(tup => println(tup._1 + " has rank: " + tup._2 ))
    output.foreach(tup => writer.write(tup._1 + "," + tup._2 + "\n"))
    //output.foreach(tup => writer.write(tup+ "\n"))
    

    //writer.write("Hello Scala")
    writer.close()

    System.exit(0)
  }
}