package org.nd4j.etl4j.spark.transform.quality.integer;

import lombok.AllArgsConstructor;
import org.apache.spark.api.java.function.Function2;
import org.nd4j.etl4j.api.io.data.NullWritable;
import org.nd4j.etl4j.api.io.data.Text;
import org.nd4j.etl4j.api.writable.Writable;
import org.nd4j.etl4j.api.transform.metadata.IntegerMetaData;
import org.nd4j.etl4j.api.transform.dataquality.columns.IntegerQuality;

/**
 * Created by Alex on 5/03/2016.
 */
@AllArgsConstructor
public class IntegerQualityAddFunction implements Function2<IntegerQuality, Writable, IntegerQuality> {

    private final IntegerMetaData meta;

    @Override
    public IntegerQuality call(IntegerQuality v1, Writable writable) throws Exception {

        long valid = v1.getCountValid();
        long invalid = v1.getCountInvalid();
        long countMissing = v1.getCountMissing();
        long countTotal = v1.getCountTotal() + 1;
        long nonInteger = v1.getCountNonInteger();

        if (meta.isValid(writable)) valid++;
        else if (writable instanceof NullWritable || writable instanceof Text && (writable.toString() == null || writable.toString().isEmpty()))
            countMissing++;
        else invalid++;

        String str = writable.toString();
        try {
            Integer.parseInt(str);
        } catch (NumberFormatException e) {
            nonInteger++;
        }

        return new IntegerQuality(valid, invalid, countMissing, countTotal, nonInteger);
    }
}
