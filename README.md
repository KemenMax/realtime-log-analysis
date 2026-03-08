# 实时日志处理与热点词分析系统

这是一个基于 Spark 的实时流处理的大数据架构 Demo。项目利用了 Spark Streaming 从 Kafka 接入实时行为日志，计算热点词汇分布，并将其结果进行可视化展示。

## 目录结构
- `demo/`：项目的主体核心代码。包含模拟日志生产者 (Log Producer)、实时流计算应用 (Spark Streaming App) 和数据可视化展示端 (Web Server)。
- `maven/`：包含本地构建该项目所需的特定 Maven 环境 (Apache Maven 3.8.8)。

## 技术栈
- 构建工具：Maven
- 消息中间件：Kafka
- 分布式计算框架：Apache Spark (Spark SQL, Spark Streaming)
- 数据库：HBase (NoSQL) & MySQL (关系型)
- 基础依赖：Fastjson (阿里)

## 如何运行
进入 `demo` 目录，执行整合好的运行脚本启动整个数据管线：
```bash
cd demo
./start.sh
```
日志将会分别输出在 `demo` 目录下的：`web_server.log`, `spark_app.log` 和 `producer.log` 中。

要停止所有服务，通过以下命令安全关闭各后台进程：
```bash
./stop.sh
```
