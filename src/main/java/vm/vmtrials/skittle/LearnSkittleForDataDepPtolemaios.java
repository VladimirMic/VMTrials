/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.skittle;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.search.algorithm.impl.GroundTruthEvaluator;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class LearnSkittleForDataDepPtolemaios<T> {

    public static final Float PERCENTILE = 0.9f;
    public static final Integer O_COUNT_STEP = 100;
    public static final Integer K = 30;
    private final KNNSearchWithPtolemaicFilteringLearnSkittle alg;
    private final AbstractMetricSpace metricSpace;
    private final List<Object> queriesSamples;
    private final List<Object> sampleObjects;
    private final DistanceFunctionInterface<T> df;

    public LearnSkittleForDataDepPtolemaios(KNNSearchWithPtolemaicFilteringLearnSkittle alg, AbstractMetricSpace metricSpace, List<Object> queriesSamples, List<Object> sampleObjects, DistanceFunctionInterface<T> df) {
        alg.setObjBeforeSeqScan(O_COUNT_STEP);
        this.alg = alg;
        this.metricSpace = metricSpace;
        this.queriesSamples = queriesSamples;
        this.sampleObjects = sampleObjects;
        this.df = df;
    }

    /**
     * Returns (0) the number of o that should be proessed before making a
     * decision whether go to brute force seq scan, (1) the average number of
     * lower bounds that should be checked per o to
     *
     * @return
     */
    public Object[] learn() {
        long timeOfSequentialScan = evaluateMedSeqScanTime();
        for (Object q : queriesSamples) {
            alg.completeKnnSearch(metricSpace, q, K, sampleObjects.iterator());
        }
        Map<Object, AtomicLong> timesPerQueries = alg.getTimesPerQueries();

        SortedSet<Map.Entry<Float, List<Float>>> lbsWithImportance = new TreeSet<>(new MyComparator());
        int batchCounts = -1;
        for (Object q : queriesSamples) {
            String qId = metricSpace.getIDOfMetricObject(q).toString();
            long time = timesPerQueries.get(qId).get();
            QueryLearnStats stats = alg.getQueryStats(qId);
            stats.setExecTime(time);
            float importance = stats.evaluateImportanceWeight(timeOfSequentialScan);
            List<Float> lbCounts = stats.getAvgNumberOfLBsPerO();
            if (batchCounts < 0) {
                batchCounts = lbCounts.size();
            }
            AbstractMap.SimpleEntry<Float, List<Float>> entry = new AbstractMap.SimpleEntry(importance, lbCounts);
            lbsWithImportance.add(entry);
        }
        for (int i = 0; i < batchCounts; i++) {
            boolean sufficientSplitOfQuickAndSLowQueries = isSplitForBatch(i, lbsWithImportance);
            if (sufficientSplitOfQuickAndSLowQueries) {
                float lbThreshold = getPercentileOfSlow(lbsWithImportance);
                return new Object[]{(i + 1) * O_COUNT_STEP, lbThreshold};
            }
        }
        return null;
    }

    private long evaluateMedSeqScanTime() {
        GroundTruthEvaluator gtEval = new GroundTruthEvaluator(df);
        long[] times = new long[queriesSamples.size()];
        gtEval.completeKnnFilteringWithQuerySet(metricSpace, queriesSamples, K, sampleObjects.iterator(), 1);
        Map<Object, AtomicLong> timesMap = gtEval.getTimesPerQueries();
        Iterator<AtomicLong> it = timesMap.values().iterator();
        for (int i = 0; it.hasNext(); i++) {
            times[i] = it.next().get();
        }
        Arrays.sort(times);
        return times[times.length / 2];
    }

    private boolean isSplitForBatch(int i, SortedSet<Map.Entry<Float, List<Float>>> lbsWithImportance) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private float getPercentileOfSlow(SortedSet<Map.Entry<Float, List<Float>>> lbsWithImportance) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private static class MyComparator implements Comparator<Map.Entry<Float, List<Float>>> {

        @Override
        public int compare(Map.Entry<Float, List<Float>> o1, Map.Entry<Float, List<Float>> o2) {
            int ret = Float.compare(o1.getKey(), o2.getKey());
            if (ret != 0) {
                return ret;
            }
            if (o1 == o2) {
                return 0;
            }
            List<Float> v1 = o1.getValue();
            List<Float> v2 = o2.getValue();
            for (int i = 0; i < v1.size(); i++) {
                ret = v1.get(i).compareTo(v2.get(i));
                if (ret != 0) {
                    return ret;
                }
            }
            return 0;
        }
    };

}
