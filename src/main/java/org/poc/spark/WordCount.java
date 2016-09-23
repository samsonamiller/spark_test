package org.poc.spark;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import scala.Tuple2;

public class WordCount {
  public static void main(String args[]) {
    String inputFile = args[0];
    String outputFile = args[1];

    // Create a Java Spark Context.
    SparkConf conf = new SparkConf().setAppName("JavaWordCount");
    JavaSparkContext sc = new JavaSparkContext(conf);

    // Create RDD from on cluster from input file
    JavaRDD<String> input = sc.textFile(inputFile);

    // map/split each line to multiple words
    JavaRDD<String> words = input.flatMap(new FlatMapFunction<String, String>() {
      public Iterable<String> call(String x) {
        return Arrays.asList(x.split(" "));
      }
    });

    // map each word
    JavaPairRDD<String, Integer> wordOnePairs = words.mapToPair(new PairFunction<String, String, Integer>() {
      public Tuple2<String, Integer> call(String x) {
        return new Tuple2(x, 1);
      }
    });

    // reduce add the pairs by key to produce counts
    JavaPairRDD<String, Integer> counts = wordOnePairs.reduceByKey(new Function2<Integer, Integer, Integer>() {
      public Integer call(Integer x, Integer y) {
        return x + y;
      }
    });

    // last step is to take top ten ordered by value, turn into an RDD an evaluate by saving as text file
    //counts.takeOrdered(10, new WordCountComparator());
    sc.parallelize(counts.takeOrdered(10, new WordCountComparator())).saveAsTextFile(outputFile);
  }

  private static class WordCountComparator implements Comparator<Tuple2<String, Integer>>, Serializable {

    @Override
    public int compare(Tuple2<String, Integer> o1, Tuple2<String, Integer> o2) {
      return o2._2() - o1._2();
    }

  }

}
