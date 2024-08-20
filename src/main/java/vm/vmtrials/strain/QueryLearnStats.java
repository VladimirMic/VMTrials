/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.strain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Vlada
 */
public class QueryLearnStats {

    private final List<Float> avgNumberOfLBsPerO;
    private final List<Integer> qTimes;
    private final Comparable queryID;

    public QueryLearnStats(Comparable queryID) {
        this.queryID = queryID;
        avgNumberOfLBsPerO = new ArrayList<>();
        qTimes = new ArrayList<>();
    }

    public void addLBChecked(float avgNumberOfLBsPerO) {
        this.avgNumberOfLBsPerO.add(avgNumberOfLBsPerO);
    }

    public void addTime(int qTime) {
        this.qTimes.add(qTime);
    }

    public List<Float> getAvgNumberOfLBsPerO() {
        return Collections.unmodifiableList(avgNumberOfLBsPerO);
    }

    public List<Integer> getqTimes() {
        return Collections.unmodifiableList(qTimes);
    }

    public Comparable getQueryID() {
        return queryID;
    }

}
