package org.nd4j.etl4j.api.transform.sequence.window;

import org.nd4j.etl4j.api.io.data.LongWritable;
import org.nd4j.etl4j.api.writable.Writable;
import org.nd4j.etl4j.api.transform.ColumnType;
import org.nd4j.etl4j.api.transform.metadata.ColumnMetaData;
import org.nd4j.etl4j.api.transform.metadata.TimeMetaData;
import org.nd4j.etl4j.api.transform.schema.Schema;
import org.nd4j.etl4j.api.transform.schema.SequenceSchema;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A windowing function based on time, with potentially overlapping windows. Time for each entry in the sequence is provided by a Time column<br>
 * The overlapping nature of the windowing function allows for things such as a window size of 1 day, produced every hour.<br>
 * Two parameters (and one optional parameter) are necessary to specify how windowing is conducted:
 * - The size of the window (for example, 1 day)<br>
 * - The separation between window periods (for example, 1 hour)<br>
 * - The offet for the start/end time of the windows<br>
 * <p>
 * Example with a window size of 12 hours, with the window separation of 1 hour, we end up with windows as follows:<br>
 * (0:00 to 12:00), (1:00 to 13:00), (2:00 to 14:00) and so on.<br>
 * If the offset was set to 15 minutes, windows would instead be at (0:15 to 12:15), (1:15 to 13:15), (2:15 to 14:15) and so on.<br>
 * <p>
 * Note that the windows generated by this window function need not contain any data - i.e., it can generate empty an empty
 * window if no data occurs in the specified time period.
 *
 * @author Alex Black
 */
public class OverlappingTimeWindowFunction implements WindowFunction {

    private final String timeColumn;
    private final long windowSize;
    private final TimeUnit windowSizeUnit;
    private final long windowSeparation;
    private final TimeUnit windowSeparationUnit;
    private final long offsetAmount;
    private final TimeUnit offsetUnit;
    private final boolean addWindowStartTimeColumn;
    private final boolean addWindowEndTimeColumn;
    private final boolean excludeEmptyWindows;
    private Schema inputSchema;

    private final long offsetAmountMilliseconds;
    private final long windowSizeMilliseconds;
    private final long windowSeparationMilliseconds;

    private DateTimeZone timeZone;

    /**
     * Constructor with zero offset
     *
     * @param timeColumn           Name of the column that contains the time values (must be a time column)
     * @param windowSize           Numerical quantity for the size of the time window (used in conjunction with windowSizeUnit)
     * @param windowSizeUnit       Unit of the time window
     * @param windowSeparation     The separation between consecutive window start times (used in conjunction with WindowSeparationUnit)
     * @param windowSeparationUnit Unit for the separation between windows
     */
    public OverlappingTimeWindowFunction(String timeColumn, long windowSize, TimeUnit windowSizeUnit, long windowSeparation,
                                         TimeUnit windowSeparationUnit) {
        this(timeColumn, windowSize, windowSizeUnit, windowSeparation, windowSeparationUnit, 0, null);
    }

    /**
     * Constructor with zero offset, ability to add window start/end time columns
     *
     * @param timeColumn           Name of the column that contains the time values (must be a time column)
     * @param windowSize           Numerical quantity for the size of the time window (used in conjunction with windowSizeUnit)
     * @param windowSizeUnit       Unit of the time window
     * @param windowSeparation     The separation between consecutive window start times (used in conjunction with WindowSeparationUnit)
     * @param windowSeparationUnit Unit for the separation between windows
     */
    public OverlappingTimeWindowFunction(String timeColumn, long windowSize, TimeUnit windowSizeUnit, long windowSeparation,
                                         TimeUnit windowSeparationUnit, boolean addWindowStartTimeColumn, boolean addWindowEndTimeColumn) {
        this(timeColumn, windowSize, windowSizeUnit, windowSeparation, windowSeparationUnit, 0, null, addWindowStartTimeColumn, addWindowEndTimeColumn, false);
    }

    /**
     * Constructor with optional offset
     *
     * @param timeColumn           Name of the column that contains the time values (must be a time column)
     * @param windowSize           Numerical quantity for the size of the time window (used in conjunction with windowSizeUnit)
     * @param windowSizeUnit       Unit of the time window
     * @param windowSeparation     The separation between consecutive window start times (used in conjunction with WindowSeparationUnit)
     * @param windowSeparationUnit Unit for the separation between windows
     * @param offset               Optional offset amount, to shift start/end of the time window forward or back
     * @param offsetUnit           Optional offset unit for the offset amount.
     */
    public OverlappingTimeWindowFunction(String timeColumn, long windowSize, TimeUnit windowSizeUnit, long windowSeparation,
                                         TimeUnit windowSeparationUnit, long offset, TimeUnit offsetUnit) {
        this(timeColumn, windowSize, windowSizeUnit, windowSeparation, windowSeparationUnit, offset, offsetUnit, false, false, false);
    }

    /**
     * Constructor with optional offset, ability to add window start/end time columns
     *
     * @param timeColumn               Name of the column that contains the time values (must be a time column)
     * @param windowSize               Numerical quantity for the size of the time window (used in conjunction with windowSizeUnit)
     * @param windowSizeUnit           Unit of the time window
     * @param windowSeparation         The separation between consecutive window start times (used in conjunction with WindowSeparationUnit)
     * @param windowSeparationUnit     Unit for the separation between windows
     * @param offset                   Optional offset amount, to shift start/end of the time window forward or back
     * @param offsetUnit               Optional offset unit for the offset amount.
     * @param addWindowStartTimeColumn If true: add a time column (name: "windowStartTime") that contains the start time
     *                                 of the window
     * @param addWindowEndTimeColumn   If true: add a time column (name: "windowEndTime") that contains the end time
     *                                 of the window
     * @param excludeEmptyWindows      If true: exclude any windows that don't have any values in them
     */
    public OverlappingTimeWindowFunction(String timeColumn, long windowSize, TimeUnit windowSizeUnit, long windowSeparation,
                                         TimeUnit windowSeparationUnit, long offset, TimeUnit offsetUnit,
                                         boolean addWindowStartTimeColumn, boolean addWindowEndTimeColumn, boolean excludeEmptyWindows) {
        this.timeColumn = timeColumn;
        this.windowSize = windowSize;
        this.windowSizeUnit = windowSizeUnit;
        this.windowSeparation = windowSeparation;
        this.windowSeparationUnit = windowSeparationUnit;
        this.offsetAmount = offset;
        this.offsetUnit = offsetUnit;
        this.addWindowStartTimeColumn = addWindowStartTimeColumn;
        this.addWindowEndTimeColumn = addWindowEndTimeColumn;
        this.excludeEmptyWindows = excludeEmptyWindows;

        if (offsetAmount == 0 || offsetUnit == null) this.offsetAmountMilliseconds = 0;
        else {
            this.offsetAmountMilliseconds = TimeUnit.MILLISECONDS.convert(offset, offsetUnit);
        }

        this.windowSizeMilliseconds = TimeUnit.MILLISECONDS.convert(windowSize, windowSizeUnit);
        this.windowSeparationMilliseconds = TimeUnit.MILLISECONDS.convert(windowSeparation, windowSeparationUnit);
    }

    private OverlappingTimeWindowFunction(Builder builder){
        this(builder.timeColumn, builder.windowSize, builder.windowSizeUnit, builder.windowSeparation, builder.windowSeparationUnit,
                builder.offsetAmount, builder.offsetUnit, builder.addWindowStartTimeColumn, builder.addWindowEndTimeColumn, builder.excludeEmptyWindows);
    }

    @Override
    public void setInputSchema(Schema schema) {
        if (!(schema instanceof SequenceSchema))
            throw new IllegalArgumentException("Invalid schema: OverlappingTimeWindowFunction can only operate on SequenceSchema");
        if (!schema.hasColumn(timeColumn))
            throw new IllegalStateException("Input schema does not have a column with name \"" + timeColumn + "\"");

        if (schema.getMetaData(timeColumn).getColumnType() != ColumnType.Time) throw new IllegalStateException(
                "Invalid column: column \"" + timeColumn + "\" is not of type " + ColumnType.Time + "; is " +
                        schema.getMetaData(timeColumn).getColumnType());

        this.inputSchema = schema;

        timeZone = ((TimeMetaData) schema.getMetaData(timeColumn)).getTimeZone();
    }

    @Override
    public Schema getInputSchema() {
        return inputSchema;
    }

    @Override
    public Schema transform(Schema inputSchema) {
        if (!addWindowStartTimeColumn && !addWindowEndTimeColumn) return inputSchema;
        List<String> newNames = new ArrayList<>();
        List<ColumnMetaData> newMeta = new ArrayList<>();

        newNames.addAll(inputSchema.getColumnNames());
        newMeta.addAll(inputSchema.getColumnMetaData());

        if (addWindowStartTimeColumn) {
            newNames.add("windowStartTime");
            newMeta.add(new TimeMetaData());
        }

        if (addWindowEndTimeColumn) {
            newNames.add("windowEndTime");
            newMeta.add(new TimeMetaData());
        }

        return inputSchema.newSchema(newNames, newMeta);
    }

    @Override
    public String toString() {
        return "OverlappingTimeWindowFunction(column=\"" + timeColumn + "\",windowSize=" + windowSize + windowSizeUnit +
                ",windowSeparation=" + windowSeparation + windowSeparationUnit + ",offset="
                + offsetAmount + (offsetAmount != 0 && offsetUnit != null ? offsetUnit : "") +
                (addWindowStartTimeColumn ? ",addWindowStartTimeColumn=true" : "") + (addWindowEndTimeColumn ? ",addWindowEndTimeColumn=true" : "")
                + (excludeEmptyWindows ? ",excludeEmptyWindows=true" : "") + ")";
    }


    @Override
    public List<List<List<Writable>>> applyToSequence(List<List<Writable>> sequence) {

        int timeColumnIdx = inputSchema.getIndexOfColumn(this.timeColumn);

        List<List<List<Writable>>> out = new ArrayList<>();

        //We are assuming here that the sequence is already ordered (as is usually the case)

        //First: work out the window to start on. The window to start on is the first window that includes the first time step values
        long firstTimeStepTimePlusOffset = sequence.get(0).get(timeColumnIdx).toLong() + offsetAmountMilliseconds;
        long windowBorder = firstTimeStepTimePlusOffset - (firstTimeStepTimePlusOffset % windowSeparationMilliseconds);     //Round down to time where a window starts/ends
        //At this windowBorder time: the window that _ends_ at windowBorder does NOT include the first time step
        // Therefore the window that ends at windowBorder+1*windowSeparation is first window that includes the first data point

        //Second: work out the window to end on. The window to end on is the last window that includes the last time step values
        long lastTimeStepTimePlusOffset = sequence.get(sequence.size() - 1).get(timeColumnIdx).toLong() + offsetAmountMilliseconds;
        long windowBorderLastTimeStep = lastTimeStepTimePlusOffset - (lastTimeStepTimePlusOffset % windowSeparationMilliseconds);
        //At this windowBorderLastTimeStep time: the window that _starts_ this time is the last window to include the last time step

        long lastWindowStartTime = windowBorderLastTimeStep;


        long currentWindowStartTime = windowBorder + windowSeparationMilliseconds - windowSizeMilliseconds;
        long nextWindowStartTime = currentWindowStartTime + windowSeparationMilliseconds;
        long currentWindowEndTime = currentWindowStartTime + windowSizeMilliseconds;
        List<List<Writable>> currentWindow = new ArrayList<>();

        int currentWindowStartIdx = 0;
        int sequenceLength = sequence.size();
        boolean foundIndexForNextWindowStart = false;
        while (currentWindowStartTime <= lastWindowStartTime) {

            for (int i = currentWindowStartIdx; i < sequenceLength; i++) {
                List<Writable> timeStep = sequence.get(i);
                long currentTime = timeStep.get(timeColumnIdx).toLong();

                //As we go through: let's keep track of the index of the first element in the next window
                if (!foundIndexForNextWindowStart && currentTime >= nextWindowStartTime) {
                    foundIndexForNextWindowStart = true;
                    currentWindowStartIdx = i;
                }
                boolean nextWindow = false;
                if (currentTime < currentWindowEndTime) {
                    //This time step is included in the current window
                    if(addWindowStartTimeColumn || addWindowEndTimeColumn){
                        List<Writable> timeStep2 = new ArrayList<>(timeStep);
                        if(addWindowStartTimeColumn) timeStep2.add(new LongWritable(currentWindowStartTime));
                        if(addWindowEndTimeColumn) timeStep2.add(new LongWritable(currentWindowStartTime + windowSizeMilliseconds));
                        currentWindow.add(timeStep2);
                    } else {
                        currentWindow.add(timeStep);
                    }
                } else {
                    //This time step is NOT included in the current window -> done with the current window -> start the next window
                    nextWindow = true;
                }

                //Once we reach the end of the input sequence: we might have added it to the current time step, but still
                // need to create the next window
                if (i == sequenceLength - 1) nextWindow = true;

                if (nextWindow) {
                    if (!(excludeEmptyWindows && currentWindow.size() == 0)) out.add(currentWindow);
                    currentWindow = new ArrayList<>();
                    currentWindowStartTime = currentWindowStartTime + windowSeparationMilliseconds;
                    currentWindowEndTime = currentWindowStartTime + windowSizeMilliseconds;
                    foundIndexForNextWindowStart = false;
                    nextWindowStartTime = currentWindowStartTime + windowSeparationMilliseconds;
                    break;
                }
            }
        }

        return out;
    }

    public static class Builder {
        private String timeColumn;
        private long windowSize = -1;
        private TimeUnit windowSizeUnit;
        private long windowSeparation = -1;
        private TimeUnit windowSeparationUnit;
        private long offsetAmount;
        private TimeUnit offsetUnit;
        private boolean addWindowStartTimeColumn = false;
        private boolean addWindowEndTimeColumn = false;
        private boolean excludeEmptyWindows = false;

        public Builder timeColumn(String timeColumn) {
            this.timeColumn = timeColumn;
            return this;
        }

        public Builder windowSize(long windowSize, TimeUnit windowSizeUnit) {
            this.windowSize = windowSize;
            this.windowSizeUnit = windowSizeUnit;
            return this;
        }

        public Builder windowSeparation(long windowSeparation, TimeUnit windowSeparationUnit){
            this.windowSeparation = windowSeparation;
            this.windowSeparationUnit = windowSeparationUnit;
            return this;
        }

        public Builder offset(long offsetAmount, TimeUnit offsetUnit) {
            this.offsetAmount = offsetAmount;
            this.offsetUnit = offsetUnit;
            return this;
        }

        public Builder addWindowStartTimeColumn(boolean addWindowStartTimeColumn) {
            this.addWindowStartTimeColumn = addWindowStartTimeColumn;
            return this;
        }

        public Builder addWindowEndTimeColumn(boolean addWindowEndTimeColumn) {
            this.addWindowEndTimeColumn = addWindowEndTimeColumn;
            return this;
        }

        public Builder excludeEmptyWindows(boolean excludeEmptyWindows) {
            this.excludeEmptyWindows = excludeEmptyWindows;
            return this;
        }

        public OverlappingTimeWindowFunction build() {
            if (timeColumn == null) throw new IllegalStateException("Time column is null (not specified)");
            if (windowSize == -1 || windowSizeUnit == null) throw new IllegalStateException("Window size/unit not set");
            if (windowSeparation == -1 || windowSeparationUnit == null) throw new IllegalStateException("Window separation and/or unit not set");
            return new OverlappingTimeWindowFunction(this);
        }
    }
}
