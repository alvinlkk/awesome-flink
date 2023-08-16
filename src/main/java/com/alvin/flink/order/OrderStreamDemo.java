package com.alvin.flink.order;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * <p>描 述：</p>
 *
 * @author cxw (332059317@qq.com)
 * @version 1.0.0
 * @since 2023/8/14  14:35
 */
public class OrderStreamDemo {

    public static void main(String[] args) throws Exception {
        // 1.创建执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());
        // 设置并行度
        env.setParallelism(1);

        // 2.读取数据:从文件读
        DataStreamSource<String> socketDS = env.socketTextStream("10.100.1.13", 7776);
        
        // 3.转换成对象
        SingleOutputStreamOperator<Order> wordAndOneDS = socketDS
                .map(new MapFunction<String, Order>() {
                    @Override
                    public Order map(String value) {
                        // 按照 空格 切分
                        String[] orders = value.split(",");
                        Order order = new Order();
                        if(orders.length >= 4) {
                            String orderId = orders[0];
                            order.setOrderId(orderId);
                            String userId = orders[1];
                            order.setUserId(userId);
                            Long amt = Long.valueOf(orders[2]);
                            order.setAmount(amt);
                            String time = orders[3];
                            order.setDate(time);
                        }
                        return order;
                    }
                });

        // 4 分组
        KeyedStream<Order, String> wordAndOneKS = wordAndOneDS.keyBy(
                new KeySelector<Order, String>() {
                    @Override
                    public String getKey(Order value) {
                        return value.getUserId();
                    }
                }
        );

        // 5 聚合
        SingleOutputStreamOperator<Order> sumDS = wordAndOneKS.sum("amount");

        // 6. 转换输出结果
        SingleOutputStreamOperator<String> mapRes = sumDS.map(new MapFunction<Order, String>() {
            @Override
            public String map(Order value) throws Exception {
                String str = String.format("时间: %s, 用户 %s 累计交易金额:%d元", value.getDate(), value.getUserId(), value.getAmount());
                return str;
            }
        });

        // 7.输出数据
        mapRes.print();

        // 8.执行
        env.execute();
    }

}
