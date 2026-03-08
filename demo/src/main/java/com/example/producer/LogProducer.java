package com.example.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.Random;
import java.util.UUID;

public class LogProducer {
    public static void main(String[] args) throws InterruptedException {
        Properties props = new Properties();
        // 用户配置: listeners=PLAINTEXT://192.168.83.129:9092
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.83.129:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        String topic = "log_topic";
        Random random = new Random();
        String[] keywords = { "苹果手机", "华为Mate60", "小米14", "Spark大数据", "Flink实时计算", "Kafka消息队列", "HBase存储", "阿里云", "腾讯云",
                "深度学习" };
        String[] regions = { "北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "西安" };

        System.out.println("Starting log producer to topic: " + topic);

        while (true) {
            String userId = UUID.randomUUID().toString().substring(0, 8);
            String keyword = keywords[random.nextInt(keywords.length)];
            String region = regions[random.nextInt(regions.length)];
            long timestamp = System.currentTimeMillis();

            // 格式: user_id,keyword,timestamp,region
            String log = String.format("%s,%s,%d,%s", userId, keyword, timestamp, region);

            producer.send(new ProducerRecord<>(topic, userId, log));
            System.out.println("Sent: " + log);

            Thread.sleep(200 + random.nextInt(800)); // 休眠 200-1000ms
        }
    }
}
