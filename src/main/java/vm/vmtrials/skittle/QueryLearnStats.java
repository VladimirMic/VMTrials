/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.skittle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Vlada
 */
public class QueryLearnStats implements Comparable<QueryLearnStats> {

    private long execTime;
    private float importance;
    private final List<Float> avgNumberOfLBsPerO;
    private final String queryID;

    public QueryLearnStats(String queryID) {
        this.queryID = queryID;
        avgNumberOfLBsPerO = new ArrayList<>();
    }

    public void addLBChecked(int objCheckedCount, float avgNumberOfLBsPerO) {
        this.avgNumberOfLBsPerO.add(avgNumberOfLBsPerO);
    }

    public List<Float> getAvgNumberOfLBsPerO() {
        return Collections.unmodifiableList(avgNumberOfLBsPerO);
    }

    public String getQueryID() {
        return queryID;
    }

    @Override
    public int compareTo(QueryLearnStats o) {
        int ret = Long.compare(execTime, o.execTime);
        if (ret != 0) {
            return ret;
        }
        return queryID.compareTo(o.queryID);
    }

    public long getExecTime() {
        return execTime;
    }

    public void setExecTime(long execTime) {
        this.execTime = execTime;
    }

    public float evaluateImportanceWeight(long seqScanTime) {
        importance = execTime - seqScanTime;
        return importance;
    }

}
