/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.skittle;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.fs.store.queryResults.FSQueryExecutionStatsStoreImpl;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.queryResults.QueryExecutionStatsStoreInterface;
import vm.search.algorithm.impl.GroundTruthEvaluator;
import static vm.search.algorithm.impl.KNNSearchWithPtolemaicFiltering.LB_COUNT;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class LearnSkittleForDataDepPtolemaios<T> {

    public static final Integer O_COUNT_STEP = 1000;
    public static final Integer K = 30;
    public static final Logger LOG = Logger.getLogger(LearnSkittleForDataDepPtolemaios.class.getName());
    private final KNNSearchWithPtolemaicFilteringLearnSkittle alg;
    private final AbstractMetricSpace metricSpace;
    private final List<Object> queriesSamples;
    private final Iterator<Object> sampleObjects;
    private final DistanceFunctionInterface<T> df;
    private final String datasetName;
    private final String querySetName;

    public LearnSkittleForDataDepPtolemaios(KNNSearchWithPtolemaicFilteringLearnSkittle alg, String datasetName, String querySetName, AbstractMetricSpace metricSpace, List<Object> queriesSamples, Iterator<Object> sampleObjects, DistanceFunctionInterface<T> df) {
        alg.setObjBeforeSeqScan(O_COUNT_STEP);
        this.alg = alg;
        this.metricSpace = metricSpace;
        this.queriesSamples = queriesSamples;
        this.sampleObjects = sampleObjects;
        this.df = df;
        this.datasetName = datasetName;
        this.querySetName = querySetName;
    }

    /**
     * Returns (0) the number of o that should be proessed before making a
     * decision whether go to brute force seq scan, (1) the average number of
     * lower bounds that should be checked per o to
     *
     * @return
     */
    public void learn() {
        long timeOfSequentialScan = loadTimeOfSequentialScan();
        LOG.log(Level.INFO, "Median brute force time over queries: {0} ms", Long.toString(timeOfSequentialScan));
        for (Object q : queriesSamples) {
            alg.completeKnnSearch(metricSpace, q, K, sampleObjects);
        }
        Map<Object, AtomicLong> timesPerQueries = alg.getTimesPerQueries();
// evaluate estimate of raw times for each step.
        List<float[][]> estimatedTimes = new ArrayList<>();
        int datasetSize = alg.getDatasetSize();
        List<Long> qTimes = null;
        for (int i = 0; i < queriesSamples.size(); i++) {
            Object q = queriesSamples.get(i);
            String qId = metricSpace.getIDOfMetricObject(q).toString();
            long qTimeComplete = timesPerQueries.get(qId).get();
            QueryLearnStats stats = alg.getQueryStats(qId);
            List<Float> qLBCounts = stats.getAvgNumberOfLBsPerO();
            qTimes = stats.getqTimes();
            estimatedTimes.add(estimateTimesForSkittle(timeOfSequentialScan, qTimeComplete, qTimes, qLBCounts, datasetSize));
        }

        float[][] estimatedTimesForSkittleOCountLBCount = estimateAvgTimesForSkittleOCountLBCount(estimatedTimes, qTimes.size());
        try {
            System.setOut(new PrintStream("h:\\Similarity_search\\Trials\\¨Skittle_time_estimations.csv"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LearnSkittleForDataDepPtolemaios.class.getName()).log(Level.SEVERE, null, ex);
        }
        Tools.printMatrix(estimatedTimesForSkittleOCountLBCount);
    }

    private float[][] estimateTimesForSkittle(long timeOfSequentialScan, long qTimeComplete, List<Long> qTimes, List<Float> qLBCounts, int datasetSize) {
        float[][] ret = new float[qTimes.size()][LB_COUNT];
        for (int batchIndex = 0; batchIndex < qTimes.size(); batchIndex++) {
            long batchTime = qTimes.get(batchIndex);
            float lbCountBatch = qLBCounts.get(batchIndex);
            float processedO = O_COUNT_STEP * batchIndex;
            float remainingO = datasetSize - processedO;
            long timeOfRemainingSeqScan = (long) (timeOfSequentialScan * remainingO / datasetSize);
            long estimatedTime = timeOfRemainingSeqScan + batchTime;
            for (int lbStop = 0; lbStop < LB_COUNT; lbStop++) {
                if (lbCountBatch >= lbStop) { // would be stopped and continued by the seq scan
                    ret[batchIndex][lbStop] = estimatedTime;
                } else {                    // would not be stopped
                    ret[batchIndex][lbStop] = qTimeComplete;
                }
            }
        }
        return ret;
    }

    private float[][] estimateAvgTimesForSkittleOCountLBCount(List<float[][]> estimatedTimes, int batchesCount) {
        float[][] ret = new float[batchesCount][LB_COUNT];
        for (float[][] qEstimatedTime : estimatedTimes) {
            for (int batchIndex = 0; batchIndex < batchesCount; batchIndex++) {
                for (int i = 0; i < LB_COUNT; i++) {
                    ret[batchIndex][i] += qEstimatedTime[batchIndex][i];
                }
            }
        }
        for (int batchIndex = 0; batchIndex < batchesCount; batchIndex++) {
            for (int i = 0; i < LB_COUNT; i++) {
                ret[batchIndex][i] = ret[batchIndex][i] / estimatedTimes.size();
            }
        }
        return ret;
    }

    private long loadTimeOfSequentialScan() {
        QueryExecutionStatsStoreInterface stats = new FSQueryExecutionStatsStoreImpl(datasetName, querySetName, GroundTruthEvaluator.K_IMPLICIT_FOR_QUERIES, datasetName, querySetName, "ground_truth", null);
        Map<Object, Long> queryTimes = stats.getQueryTimes();
        List<Long> ret = new ArrayList<>(queryTimes.values());
        Collections.sort(ret);
        int idx = ret.size() / 2;
        return ret.get(idx);
    }

    private long evaluateTimeOfSequentialScan() {
        GroundTruthEvaluator gtEval = new GroundTruthEvaluator(df);
        long[] times = new long[queriesSamples.size()];
        gtEval.completeKnnFilteringWithQuerySet(metricSpace, queriesSamples, K, sampleObjects, 1);
        Map<Object, AtomicLong> timesMap = gtEval.getTimesPerQueries();
        Iterator<AtomicLong> it = timesMap.values().iterator();
        for (int i = 0; it.hasNext(); i++) {
            times[i] = it.next().get();
        }
        Arrays.sort(times);
        return times[times.length / 2];
    }

}
