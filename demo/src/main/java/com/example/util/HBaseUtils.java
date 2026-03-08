package com.example.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

public class HBaseUtils {
    private static volatile HBaseUtils instance;
    private Connection conn;

    private HBaseUtils() {
        try {
            Configuration conf = HBaseConfiguration.create();
            conf.set("hbase.zookeeper.quorum", "192.168.83.129,192.168.83.130,192.168.83.131");
            conf.set("hbase.zookeeper.property.clientPort", "2181");
            this.conn = ConnectionFactory.createConnection(conf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HBaseUtils getInstance() {
        if (instance == null) {
            synchronized (HBaseUtils.class) {
                if (instance == null) {
                    instance = new HBaseUtils();
                }
            }
        }
        return instance;
    }

    public void put(String tableName, String rowKey, String cf, String col, String val) {
        try (Table table = conn.getTable(TableName.valueOf(tableName))) {
            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(Bytes.toBytes(cf), Bytes.toBytes(col), Bytes.toBytes(val));
            table.put(put);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
