package WordCount
 
import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{ State, StateSpec }
 
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.clients.consumer.ConsumerRecord
 
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
 
object stateFulWordCount {
 
  def main(args: Array[String]) {
 
    val conf = new SparkConf().setMaster("local[*]").setAppName("KafkaReceiver")
 
    val ssc = new StreamingContext(conf, Seconds(10))
 
    /*
     * Defingin the Kafka server parameters
     */
 
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "localhost:9092,localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "use_a_separate_group_id_for_each_stream",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean))
 
    val topics = Array("acadgild-topic") //topics list
 
    val kafkaStream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams))
 
    val splits = kafkaStream.map(record => (record.key(), record.value.toString)).flatMap(x => x._2.split(" "))
 
    val updateFunc = (values: Seq[Int], state: Option[Int]) => {
      val currentCount = values.foldLeft(0)(_ + _)
 
      val previousCount = state.getOrElse(0)
 
      Some(currentCount + previousCount)
    }
 
    //Defining a check point directory for performing stateful operations
    ssc.checkpoint("hdfs://localhost:9000/WordCount_checkpoint")
 
    val wordCounts = splits.map(x => (x, 1)).reduceByKey(_+_).updateStateByKey(updateFunc)
 
    kafkaStream.print() //prints the stream of data received
    wordCounts.print() //prints the wordcount result of the stream
 
    ssc.start()
    ssc.awaitTermination()
 
  }
 
}