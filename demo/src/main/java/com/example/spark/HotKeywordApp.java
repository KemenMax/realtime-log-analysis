package com.example.spark;

import com.example.util.HBaseUtils;
import com.example.util.MySQLUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.streaming.kafka010.*;
import scala.Tuple2;

import java.util.*;

public class HotKeywordApp {

    public static void main(String[] args) throws InterruptedException {
        // Spark 配置
        SparkConf conf = new SparkConf()
                .setAppName("HotKeywordApp");
        // .setMaster("local[*]"); // 在提交命令中使用提供的作用域/master，或者
        // 设置条件

        // 如果在 IDE 中运行且未提交，用户可能需要设置 setMaster。
        // 我们遵循标准的 spark-submit 实践。

        JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(5));

        // Kafka 参数
        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put("bootstrap.servers", "192.168.83.129:9092");
        kafkaParams.put("key.deserializer", StringDeserializer.class);
        kafkaParams.put("value.deserializer", StringDeserializer.class);
        kafkaParams.put("group.id", "spark_hot_group");
        kafkaParams.put("auto.offset.reset", "latest");
        kafkaParams.put("enable.auto.commit", false);

        Collection<String> topics = Arrays.asList("log_topic");

        JavaInputDStream<ConsumerRecord<String, String>> stream = KafkaUtils.createDirectStream(
                jssc,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.<String, String>Subscribe(topics, kafkaParams));

        // 处理流
        stream.foreachRDD(rdd -> {
            rdd.foreachPartition(records -> {
                // 连接对象（分区内懒加载）
                HBaseUtils hbase = HBaseUtils.getInstance();
                MySQLUtils mysql = MySQLUtils.getInstance();

                while (records.hasNext()) {
                    ConsumerRecord<String, String> record = records.next();
                    String value = record.value(); // "userId,keyword,timestamp,region"

                    if (value == null || value.isEmpty())
                        continue;

                    try {
                        String[] parts = value.split(",");
                        if (parts.length == 4) {
                            String userId = parts[0];
                            String keyword = parts[1];
                            long ts = Long.parseLong(parts[2]);
                            String region = parts[3];

                            // 1. 保存原始数据到 HBase
                            hbase.put("raw_logs", userId + "_" + ts, "info", "keyword", keyword);
                            hbase.put("raw_logs", userId + "_" + ts, "info", "region", region);

                            // 2. 累积 MySQL 统计信息（简单计数）
                            // 在实际生产中，使用 mapWithState 或 reduceByKeyAndWindow。
                            // 这里为了简单起见进行每个分区的更新，或者先聚合。
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        });

        // 热词窗口聚合（Top N）
        // 将 DStream 转换为 PairDStream
        JavaPairDStream<String, Integer> keywordCounts = stream
                .map(ConsumerRecord::value)
                .filter(v -> v != null && v.split(",").length == 4)
                .mapToPair(v -> new Tuple2<>(v.split(",")[1], 1))
                .reduceByKeyAndWindow((i1, i2) -> i1 + i2, Durations.seconds(20), Durations.seconds(10));

        keywordCounts.foreachRDD(rdd -> {
            // 按计数降序排序
            List<Tuple2<String, Integer>> topList = rdd.mapToPair(Tuple2::swap)
                    .sortByKey(false)
                    .mapToPair(Tuple2::swap)
                    .take(10); // 前 10 名

            // 写入 MySQL
            if (!topList.isEmpty()) {
                MySQLUtils.getInstance().upsertHotStats(topList);
            }
        });

        jssc.start();
        jssc.awaitTermination();
    }
}
