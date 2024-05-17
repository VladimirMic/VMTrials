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
import vm.metricSpace.Dataset;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.queryResults.QueryExecutionStatsStoreInterface;
import vm.search.algorithm.SearchingAlgorithm;
import vm.search.algorithm.impl.GroundTruthEvaluator;
import static vm.search.algorithm.impl.KNNSearchWithPtolemaicFiltering.LB_COUNT;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class LearnSkittleForDataDepPtolemaios<T> {

    public static final Integer O_COUNT_STEP = 2000;
    public static final Float MAX_RATIO_OF_DATASET = 0.5f;
    public static final Integer K = 30;
    public static final Logger LOG = Logger.getLogger(LearnSkittleForDataDepPtolemaios.class.getName());
    private final KNNSearchWithPtolemaicFilteringLearnSkittle alg;
    private final AbstractMetricSpace metricSpace;
    private final List<Object> queriesSamples;
    private final Dataset dataset;
    private final DistanceFunctionInterface<T> df;
    private final String datasetName;
    private final String querySetName;

    public LearnSkittleForDataDepPtolemaios(KNNSearchWithPtolemaicFilteringLearnSkittle alg, Dataset dataset, List<Object> queriesSamples) {
        alg.setObjBeforeSeqScan(O_COUNT_STEP);
        this.alg = alg;
        this.metricSpace = dataset.getMetricSpace();
        this.queriesSamples = queriesSamples;
        this.dataset = dataset;
        this.df = dataset.getDistanceFunction();
        this.datasetName = dataset.getDatasetName();
        this.querySetName = dataset.getQuerySetName();
    }

    /**
     * Returns (0) the number of o that should be proessed before making a
     * decision whether go to brute force seq scan, (1) the average number of
     * lower bounds that should be checked per o to
     *
     */
    public void learn() {
        long timeOfSequentialScan = loadTimeOfSequentialScan();
        LOG.log(Level.INFO, "Median brute force time over queries: {0} ms", Long.toString(timeOfSequentialScan));
        Iterator<Object> oIt = dataset.getMetricObjectsFromDataset();
        int datasetSize = 0;
        while (oIt.hasNext()) {
            List<Object> batch = Tools.getObjectsFromIterator(oIt, SearchingAlgorithm.BATCH_SIZE);
            datasetSize += batch.size();
            for (Object q : queriesSamples) {
                alg.completeKnnSearch(metricSpace, q, K, batch.iterator());
            }
        }
        Map<Object, AtomicLong> timesPerQueries = alg.getTimesPerQueries();
// evaluate estimate of raw times for each step.
        List<float[][]> estimatedTimes = new ArrayList<>();
        List<Integer> qTimes = null;
        for (int i = 0; i < queriesSamples.size(); i++) {
            Object q = queriesSamples.get(i);
            String qId = metricSpace.getIDOfMetricObject(q).toString();
            long qTimeComplete = timesPerQueries.get(qId).get();
            QueryLearnStats stats = alg.getQueryStats(qId);
            List<Float> qLBCounts = stats.getAvgNumberOfLBsPerO();
            qTimes = stats.getqTimes();
            qTimes = sumQTimesOverBatches(qTimes, SearchingAlgorithm.BATCH_SIZE / O_COUNT_STEP);
            estimatedTimes.add(estimateTimesForSkittle(timeOfSequentialScan, qTimeComplete, qTimes, qLBCounts, datasetSize));
        }

        float[][] estimatedTimesForSkittleOCountLBCount = estimateAvgTimesForSkittleOCountLBCount(estimatedTimes, (int) (qTimes.size() * MAX_RATIO_OF_DATASET));
        try {
            System.setErr(new PrintStream("h:\\Similarity_search\\Trials\\Skittle_time_estimations.csv"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(LearnSkittleForDataDepPtolemaios.class.getName()).log(Level.SEVERE, null, ex);
        }
        Tools.printMatrix(estimatedTimesForSkittleOCountLBCount);
    }

    private float[][] estimateTimesForSkittle(long timeOfSequentialScan, long qTimeComplete, List<Integer> qTimes, List<Float> qLBCounts, int datasetSize) {
        int maxSampleChecked = (int) (qTimes.size() * MAX_RATIO_OF_DATASET);
        float[][] ret = new float[maxSampleChecked][LB_COUNT];
        for (int batchIndex = 0; batchIndex < maxSampleChecked; batchIndex++) {
            long qTime = qTimes.get(batchIndex);
            float lbCountBatch = qLBCounts.get(batchIndex);
            float processedO = O_COUNT_STEP * (batchIndex + 1);
            float remainingO = datasetSize - processedO;
            long timeOfRemainingSeqScan = (long) (timeOfSequentialScan * remainingO / datasetSize);
            long estimatedTime = timeOfRemainingSeqScan + qTime;
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
        gtEval.completeKnnFilteringWithQuerySet(metricSpace, queriesSamples, K, dataset.getMetricObjectsFromDataset(), 1);
        Map<Object, AtomicLong> timesMap = gtEval.getTimesPerQueries();
        Iterator<AtomicLong> it = timesMap.values().iterator();
        for (int i = 0; it.hasNext(); i++) {
            times[i] = it.next().get();
        }
        Arrays.sort(times);
        return times[times.length / 2];
    }

    private List<Integer> sumQTimesOverBatches(List<Integer> qTimes, int interval) {
        List<Integer> ret = new ArrayList<>();
        for (int i = 0; i < qTimes.size(); i++) {
            int value = qTimes.get(i);
            int idx = (int) vm.math.Tools.round(i, interval, true);
            while (idx > 0) {
                value += qTimes.get(idx - 1);
                idx -= interval;
                idx = (int) vm.math.Tools.round(idx, interval, true);
            }
            ret.add(value);
        }
        return ret;
    }

}
