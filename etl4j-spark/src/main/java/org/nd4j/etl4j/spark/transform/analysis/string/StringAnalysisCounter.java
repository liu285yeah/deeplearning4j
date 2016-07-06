package org.nd4j.etl4j.spark.transform.analysis.string;

import org.nd4j.etl4j.spark.transform.analysis.AnalysisCounter;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.nd4j.etl4j.api.writable.Writable;

/**
 * A counter function for doing analysis on integer columns, on Spark
 *
 * @author Alex Black
 */
@AllArgsConstructor @Data
public class StringAnalysisCounter implements AnalysisCounter<StringAnalysisCounter> {

    private long countZeroLength;
    private long countMinLength;
    private int minLengthSeen = Integer.MAX_VALUE;
    private long countMaxLength;
    private int maxLengthSeen = Integer.MIN_VALUE;
    private long sumLength = 0;
    private long countTotal = 0;


    public StringAnalysisCounter(){

    }


    @Override
    public StringAnalysisCounter add(Writable writable) {
        int length = writable.toString().length();

        if(length == 0) countZeroLength++;

        if(length == minLengthSeen) countMinLength++;
        else if(length < minLengthSeen){
            minLengthSeen = length;
            countMinLength = 1;
        }

        if(length == maxLengthSeen) countMaxLength++;
        else if(length > maxLengthSeen){
            maxLengthSeen = length;
            countMaxLength = 1;
        }

        sumLength += length;
        countTotal++;

        return this;
    }

    public StringAnalysisCounter merge(StringAnalysisCounter other){
        int otherMin = other.getMinLengthSeen();
        int newMinLengthSeen;
        long newCountMinLength;
        if(minLengthSeen == otherMin){
            newMinLengthSeen = minLengthSeen;
            newCountMinLength = countMinLength + other.countMinLength;
        } else if(minLengthSeen > otherMin) {
            //Keep other, take count from other
            newMinLengthSeen = otherMin;
            newCountMinLength = other.countMinLength;
        } else {
            //Keep this min, no change to count
            newMinLengthSeen = minLengthSeen;
            newCountMinLength = countMinLength;
        }

        int otherMax = other.getMaxLengthSeen();
        int newMaxLengthSeen;
        long newCountMaxLength;
        if(maxLengthSeen == otherMax){
            newMaxLengthSeen = maxLengthSeen;
            newCountMaxLength = countMaxLength + other.countMaxLength;
        } else if(maxLengthSeen < otherMax) {
            //Keep other, take count from other
            newMaxLengthSeen = otherMax;
            newCountMaxLength = other.countMaxLength;
        } else {
            //Keep this max, no change to count
            newMaxLengthSeen = maxLengthSeen;
            newCountMaxLength = countMaxLength;
        }


        return new StringAnalysisCounter(countZeroLength+other.countZeroLength,
                newCountMinLength,
                newMinLengthSeen,
                newCountMaxLength,
                newMaxLengthSeen,
                sumLength + other.sumLength,
                countTotal + other.countTotal);
    }

}
