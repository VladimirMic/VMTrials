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
public class QueryLearnStats {

    private final List<Float> avgNumberOfLBsPerO;
    private final List<Long> qTimes;
    private final String queryID;

    public QueryLearnStats(String queryID) {
        this.queryID = queryID;
        avgNumberOfLBsPerO = new ArrayList<>();
        qTimes = new ArrayList<>();
    }

    public void addLBChecked(float avgNumberOfLBsPerO) {
        this.avgNumberOfLBsPerO.add(avgNumberOfLBsPerO);
    }

    public void addTime(long qTime) {
        this.qTimes.add(qTime);
    }

    public List<Float> getAvgNumberOfLBsPerO() {
        return Collections.unmodifiableList(avgNumberOfLBsPerO);
    }

    public List<Long> getqTimes() {
        return Collections.unmodifiableList(qTimes);
    }

    public String getQueryID() {
        return queryID;
    }

}
