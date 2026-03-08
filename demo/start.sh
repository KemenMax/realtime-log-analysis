#!/bin/bash

cd "$(dirname "$0")" || exit

# Ensure the project is built
if [ ! -f "target/demo-1.0-SNAPSHOT-jar-with-dependencies.jar" ]; then
    echo "Jar file not found. Building project..."
    mvn clean package -DskipTests
fi

echo "Starting Web Server..."
nohup mvn exec:java -Dexec.mainClass="com.example.web.WebServer" > web_server.log 2>&1 &
echo $! > web_server.pid
echo "Web Server started. PID: $(cat web_server.pid)"

sleep 2

echo "Starting Spark Streaming App..."
nohup spark-submit \
  --class com.example.spark.HotKeywordApp \
  --master local[2] \
  target/demo-1.0-SNAPSHOT-jar-with-dependencies.jar > spark_app.log 2>&1 &
echo $! > spark_app.pid
echo "Spark App started. PID: $(cat spark_app.pid)"

sleep 5

echo "Starting Log Producer..."
nohup mvn exec:java -Dexec.mainClass="com.example.producer.LogProducer" > producer.log 2>&1 &
echo $! > producer.pid
echo "Log Producer started. PID: $(cat producer.pid)"

echo "System started successfully! logs: web_server.log, spark_app.log, producer.log"
