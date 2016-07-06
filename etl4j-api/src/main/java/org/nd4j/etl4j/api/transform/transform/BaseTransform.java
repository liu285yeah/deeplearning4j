package org.nd4j.etl4j.api.transform.transform;

import org.nd4j.etl4j.api.writable.Writable;
import org.nd4j.etl4j.api.transform.Transform;
import org.nd4j.etl4j.api.transform.schema.Schema;

import java.util.ArrayList;
import java.util.List;

/**BaseTransform: an abstact transform class, that handles transforming sequences by transforming each example individally
 *
 * @author Alex Black
 */
public abstract class BaseTransform implements Transform {

    protected Schema inputSchema;

    @Override
    public void setInputSchema(Schema inputSchema) {
        this.inputSchema = inputSchema;
    }

    @Override
    public Schema getInputSchema(){
        return inputSchema;
    }

    @Override
    public List<List<Writable>> mapSequence(List<List<Writable>> sequence){

        List<List<Writable>> out = new ArrayList<>(sequence.size());
        for(List<Writable> c : sequence){
            out.add(map(c));
        }
        return out;
    }

    @Override
    public abstract String toString();
}
