import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.scheduler.JobLogger
import org.apache.spark.scheduler

object WordCount {
  def main(args: Array[String]) {
    
    if (args.length < 3) {
      println("Usage: wc <master> <input dir> <log file>")
      exit()
    }

    val master = args(0)
    val input_dir = args(1)
    val log_file = args(2)

    val sc = new SparkContext(master, "job", "/homes/network/revtr/ujaved/incubator-spark",
      List("target/scala-2.9.3/simple-project_2.9.3-1.0.jar"))

    val listener = new JobLogger("ujaved")
    sc.addSparkListener(listener)

    println(System.getenv("SPARK_LOG_DIR"))

    val lines = sc.textFile(input_dir + "*")
    val counts = lines.flatMap(line => line.split(' ')).map(word => (word,1)).reduceByKey(_+_)
    val output = counts.collect().length
    println("count : %s".format(output))
  }
}
