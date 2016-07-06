package org.nd4j.etl4j.api.transform.comparator;

import org.nd4j.etl4j.api.writable.Writable;

import java.io.Serializable;
import java.util.Comparator;

public class LongWritableComparator implements Comparator<Writable>, Serializable {
    @Override
    public int compare(Writable o1, Writable o2) {
        return Long.compare(o1.toLong(), o2.toLong());
    }
}
