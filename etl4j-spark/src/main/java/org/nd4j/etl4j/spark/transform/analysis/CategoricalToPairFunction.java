package org.nd4j.etl4j.spark.transform.analysis;

import org.apache.spark.api.java.function.PairFunction;
import org.nd4j.etl4j.api.writable.Writable;
import scala.Tuple2;

/**
 * Created by Alex on 4/03/2016.
 */
public class CategoricalToPairFunction implements PairFunction<Writable,String,Integer> {
    @Override
    public Tuple2<String, Integer> call(Writable writable) throws Exception {
        return new Tuple2<>(writable.toString(),1);
    }
}
