/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.skittle;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import vm.datatools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.twopivots.AbstractPtolemaicBasedFiltering;
import static vm.search.algorithm.SearchingAlgorithm.adjustAndReturnSearchRadiusAfterAddingOne;
import vm.search.algorithm.impl.KNNSearchWithPtolemaicFiltering;
import static vm.search.algorithm.impl.KNNSearchWithPtolemaicFiltering.LB_COUNT;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class KNNSearchWithPtolemaicFilteringLearnSkittle<T> extends KNNSearchWithPtolemaicFiltering<T> {

    private final SortedMap<String, QueryLearnStats> queryStats = new TreeMap<>();

    public KNNSearchWithPtolemaicFilteringLearnSkittle(AbstractMetricSpace metricSpace, AbstractPtolemaicBasedFiltering ptolemaicFilter, List pivots, float[][] poDists, Map rowHeaders, Map columnHeaders, DistanceFunctionInterface df) {
        super(metricSpace, ptolemaicFilter, pivots, poDists, rowHeaders, columnHeaders, df);
    }

    @Override
    public TreeSet<Map.Entry<Object, Float>> completeKnnSearch(AbstractMetricSpace<T> metricSpace, Object q, int k, Iterator<Object> objects, Object... params) {
        String qId = metricSpace.getIDOfMetricObject(q).toString();
        if (!queryStats.containsKey(qId)) {
            queryStats.put(qId, new QueryLearnStats(qId));
        }
        QueryLearnStats stats = queryStats.get(qId);
        long t = -System.currentTimeMillis();
        TreeSet<Map.Entry<Object, Float>> ret = params.length == 0 ? new TreeSet<>(new Tools.MapByFloatValueComparator()) : (TreeSet<Map.Entry<Object, Float>>) params[0];
        long lbChecked = 0;
        T qData = metricSpace.getDataOfMetricObject(q);

        float[][] qpDistMultipliedByCoefForPivots = qpMultipliedByCoefCached.get(qId);
        if (qpDistMultipliedByCoefForPivots == null) {
            qpDistMultipliedByCoefForPivots = computeqpDistMultipliedByCoefForPivots(qData);
            qpMultipliedByCoefCached.put(qId, qpDistMultipliedByCoefForPivots);
        }
        int[] pivotArrays = qPivotArraysCached.get(qId);
        if (pivotArrays == null) {
            pivotArrays = identifyExtremePivotPairs(qpDistMultipliedByCoefForPivots, LB_COUNT);
            qPivotArraysCached.put(qId, pivotArrays);
        }
        int distComps = 0;
        float range = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, Float.MAX_VALUE);
        int oIdx, p1Idx, p2Idx, p;
        float distP1O, distP2O, distP2Q, distQP1, lowerBound, distance;
        float[] poDistsArray;
        Object o, oId;
        T oData;
        int oCounter = 0;
        objectsLoop:
        while (objects.hasNext()) {
            oCounter++;
            long tNotCount =- System.currentTimeMillis();
            if (oCounter % objBeforeSeqScan == 0) {
                float avg = lbChecked / (float) oCounter;
                stats.addLBChecked(avg);
                stats.addTime((int) (System.currentTimeMillis() + t));
            }
            tNotCount += System.currentTimeMillis();
            t += tNotCount;
            o = objects.next();
            oId = metricSpace.getIDOfMetricObject(o);
            if (range < Float.MAX_VALUE) {
                oIdx = rowHeaders.get(oId);
                poDistsArray = poDists[oIdx];
                for (p = 0; p < pivotArrays.length; p += 2) {
                    p1Idx = pivotArrays[p];
                    p2Idx = pivotArrays[p + 1];
                    distP1O = poDistsArray[p1Idx];
                    distP2O = poDistsArray[p2Idx];
                    distP2Q = qpDistMultipliedByCoefForPivots[p2Idx][p1Idx];
                    distQP1 = qpDistMultipliedByCoefForPivots[p1Idx][p2Idx];
                    lowerBound = filter.lowerBound(distP2O, distQP1, distP1O, distP2Q);
                    if (lowerBound > range) {
                        lbChecked += p / 2 + 1;
                        continue objectsLoop;
                    }
                }
                lbChecked += p / 2;
            }
            distComps++;
            oData = metricSpace.getDataOfMetricObject(o);
            distance = df.getDistance(qData, oData);
            if (distance < range) {
                ret.add(new AbstractMap.SimpleEntry<>(oId, distance));
                range = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, Float.MAX_VALUE);
            }
        }
        t += System.currentTimeMillis();
        System.err.println("XXX:" + t);
        incTime(qId, t);
        incDistsComps(qId, distComps);
        incLBChecked(qId, lbChecked);
        return ret;
    }

    public QueryLearnStats getQueryStats(String qId) {
        return queryStats.get(qId);
    }

}
