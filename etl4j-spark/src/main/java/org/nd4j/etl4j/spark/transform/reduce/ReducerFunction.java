package org.nd4j.etl4j.spark.transform.reduce;

import lombok.AllArgsConstructor;
import org.apache.spark.api.java.function.Function;
import org.nd4j.etl4j.api.writable.Writable;
import org.nd4j.etl4j.api.transform.reduce.IReducer;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.List;

/**
 * Spark function for executing a reduction of a set of examples by key
 *
 * @author Alex Black
 */
@AllArgsConstructor
public class ReducerFunction implements Function<Tuple2<String,Iterable<List<Writable>>>,List<Writable>> {

    private final IReducer reducer;

    @Override
    public List<Writable> call(Tuple2<String, Iterable<List<Writable>>> t2) throws Exception {
        List<List<Writable>> list = new ArrayList<>();
        for(List<Writable> c : t2._2()){
            list.add(c);
        }
        return reducer.reduce(list);
    }
}
