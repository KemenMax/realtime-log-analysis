#!/bin/bash

cd "$(dirname "$0")" || exit

if [ -f "producer.pid" ]; then
    PID=$(cat producer.pid)
    echo "Stopping Log Producer (PID: $PID)..."
    kill $PID
    rm producer.pid
else
    echo "Log Producer is not active (no pid file)."
fi

if [ -f "spark_app.pid" ]; then
    PID=$(cat spark_app.pid)
    echo "Stopping Spark App (PID: $PID)..."
    kill $PID
    rm spark_app.pid
else
    echo "Spark App is not active (no pid file)."
fi

if [ -f "web_server.pid" ]; then
    PID=$(cat web_server.pid)
    echo "Stopping Web Server (PID: $PID)..."
    kill $PID
    rm web_server.pid
else
    echo "Web Server is not active (no pid file)."
fi

echo "System stopped."
