package com.alvin.flink.wc;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.tuple.Tuple2;
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
public class WordCountStreamDemo {

    public static void main(String[] args) throws Exception {
        // 1.创建执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 2.读取数据:从文件读
        DataStreamSource<String> lineDS = env.readTextFile("input/word.txt");

        // 3.处理数据: 切分、转换、分组、聚合
        // 3.1 切分、转换
        SingleOutputStreamOperator<Tuple2<String, Integer>> wordAndOneDS = lineDS
                .flatMap(new FlatMapFunction<String, Tuple2<String, Integer>>() {
                    @Override
                    public void flatMap(String value, Collector<Tuple2<String, Integer>> out) throws Exception {
                        // 按照 空格 切分
                        String[] words = value.split(" ");
                        for (String word : words) {
                            // 转换成 二元组 （word，1）
                            Tuple2<String, Integer> wordsAndOne = Tuple2.of(word, 1);
                            // 通过 采集器 向下游发送数据
                            out.collect(wordsAndOne);
                        }
                    }
                });

        // 3.2 分组
        KeyedStream<Tuple2<String, Integer>, String> wordAndOneKS = wordAndOneDS.keyBy(
                new KeySelector<Tuple2<String, Integer>, String>() {
                    @Override
                    public String getKey(Tuple2<String, Integer> value) throws Exception {
                        return value.f0;
                    }
                }
        );

        // 3.3 聚合
        SingleOutputStreamOperator<Tuple2<String, Integer>> sumDS = wordAndOneKS.sum(1);

        // 4.输出数据
        sumDS.print();

        // 5.执行：类似 sparkstreaming最后 ssc.start()
        env.execute();
    }

}
