package org.nd4j.etl4j.api.transform.transform.string;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nd4j.etl4j.api.io.data.Text;
import org.nd4j.etl4j.api.writable.Writable;

/**
 * Replace empty String values with the specified String
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ReplaceEmptyStringTransform extends BaseStringTransform {

    private String newValueOfEmptyStrings;

    public ReplaceEmptyStringTransform(String columnName, String newValueOfEmptyStrings) {
        super(columnName);
        this.newValueOfEmptyStrings = newValueOfEmptyStrings;
    }

    @Override
    public Text map(Writable writable) {
        String s = writable.toString();
        if(s == null || s.isEmpty()) return new Text(newValueOfEmptyStrings);
        else if(writable instanceof Text) return (Text)writable;
        else return new Text(writable.toString());
    }
}
