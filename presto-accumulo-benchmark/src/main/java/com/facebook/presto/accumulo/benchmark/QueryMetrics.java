/**
 * Copyright 2016 Bloomberg L.P.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.accumulo.benchmark;

import com.facebook.presto.jdbc.QueryStats;

public class QueryMetrics
{
    public boolean error = false;
    public boolean timedout = false;
    public Float scale;
    public Integer numAccumuloSplits;
    public Long queryTimeMS;
    public String queryId;
    public String schema;
    public String script;
    public boolean optimizeRangeSplitsEnabled;
    public boolean secondaryIndexEnabled;
    public QueryStats queryStats;

    public static String getHeader()
    {
        StringBuilder bldr = new StringBuilder();
        bldr.append("Query ID").append(",");
        bldr.append("Script").append(",");
        bldr.append("Schema").append(",");
        bldr.append("Scale").append(",");
        bldr.append("Accumulo Tablets").append(",");
        bldr.append("Query Time (ms)").append(",");
        bldr.append("Range Splits Enabled").append(",");
        bldr.append("Secondary Index Enabled").append(",");
        bldr.append("Error").append(",");
        bldr.append("Timed Out").append(",");
        bldr.append("CPU Time (ms)").append(",");
        bldr.append("User Time (ms)").append(",");
        bldr.append("Wall Time (ms)").append(",");
        bldr.append("Nodes").append(",");
        bldr.append("Processed Bytes").append(",");
        bldr.append("Processed Rows").append(",");
        bldr.append("Completed Splits").append(",");
        bldr.append("Queued Splits").append(",");
        bldr.append("Running Splits").append(",");
        bldr.append("Total Splits").append(",");
        bldr.append("State");

        return bldr.toString();
    }

    @Override
    public String toString()
    {
        StringBuilder bldr = new StringBuilder();
        bldr.append(queryId).append(",");
        bldr.append(script).append(",");
        bldr.append(schema).append(",");
        bldr.append(scale).append(",");
        bldr.append(numAccumuloSplits + 1).append(",");
        bldr.append(queryTimeMS).append(",");
        bldr.append(optimizeRangeSplitsEnabled).append(",");
        bldr.append(secondaryIndexEnabled).append(",");
        bldr.append(error).append(",");
        bldr.append(timedout).append(",");

        if (queryStats != null) {
            bldr.append(queryStats.getCpuTimeMillis()).append(",");
            bldr.append(queryStats.getUserTimeMillis()).append(",");
            bldr.append(queryStats.getWallTimeMillis()).append(",");
            bldr.append(queryStats.getNodes()).append(",");
            bldr.append(queryStats.getProcessedBytes()).append(",");
            bldr.append(queryStats.getProcessedRows()).append(",");
            bldr.append(queryStats.getCompletedSplits()).append(",");
            bldr.append(queryStats.getQueuedSplits()).append(",");
            bldr.append(queryStats.getRunningSplits()).append(",");
            bldr.append(queryStats.getTotalSplits()).append(",");
            bldr.append(queryStats.getState());
        }
        else {
            bldr.append(",,,,,,,,,,");
        }

        return bldr.toString();
    }
}