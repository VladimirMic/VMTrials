/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.strain;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import vm.datatools.Tools;
import static vm.search.algorithm.SearchingAlgorithm.adjustAndReturnSearchRadiusAfterAddingOne;
import vm.search.algorithm.impl.KNNSearchWithPtolemaicFiltering;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.distance.AbstractDistanceFunction;
import vm.searchSpace.distance.bounding.twopivots.AbstractPtolemaicBasedFiltering;

/**
 *
 * @author Vlada
 * @param <T>
 */
public class KNNSearchWithPtolemaicFilteringLearnSkittle<T> extends KNNSearchWithPtolemaicFiltering<T> {

    private final SortedMap<Comparable, QueryLearnStats> queryStats = new TreeMap<>();

    public KNNSearchWithPtolemaicFilteringLearnSkittle(AbstractSearchSpace metricSpace, AbstractPtolemaicBasedFiltering ptolemaicFilter, List pivots, float[][] poDists, Map<Comparable, Integer> rowHeaders, AbstractDistanceFunction df) {
        super(metricSpace, ptolemaicFilter, pivots, poDists, rowHeaders, df);
    }

    @Override
    public TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch(AbstractSearchSpace<T> metricSpace, Object q, int k, Iterator<Object> objects, Object... params) {
        Comparable qId = metricSpace.getIDOfObject(q);
        if (!queryStats.containsKey(qId)) {
            queryStats.put(qId, new QueryLearnStats(qId));
        }
        QueryLearnStats stats = queryStats.get(qId);
        long t = -System.currentTimeMillis();
        TreeSet<Map.Entry<Comparable, Float>> ret = params.length == 0 ? new TreeSet<>(new Tools.MapByFloatValueComparator()) : (TreeSet<Map.Entry<Comparable, Float>>) params[0];
        long lbChecked = 0;
        T qData = metricSpace.getDataOfObject(q);

        float[][] qpDistMultipliedByCoefForPivots = qpMultipliedByCoefCached.get(qId);
        if (qpDistMultipliedByCoefForPivots == null) {
            qpDistMultipliedByCoefForPivots = computeqpDistMultipliedByCoefForPivots(qData, pivotsData, df, filter);
            qpMultipliedByCoefCached.put(qId, qpDistMultipliedByCoefForPivots);
        }
        int[] pivotArrays = qPivotArraysCached.get(qId);
        if (pivotArrays == null) {
            pivotArrays = identifyExtremePivotPairs(qpDistMultipliedByCoefForPivots, qpDistMultipliedByCoefForPivots.length);
            qPivotArraysCached.put(qId, pivotArrays);
        }
        int distComps = 0;
        float range = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, Float.MAX_VALUE);
        int oIdx, p1Idx, p2Idx, p;
        float distP1O, distP2O, distP2Q, distQP1, lowerBound, distance;
        float[] poDistsArray;
        Object o;
        Comparable oId;
        T oData;
        int oCounter = 0;
        objectsLoop:
        while (objects.hasNext()) {
            oCounter++;
            long tNotCount = -System.currentTimeMillis();
            if (oCounter % objBeforeSeqScan == 0) {
                float avg = lbChecked / (float) oCounter;
                stats.addLBChecked(avg);
                stats.addTime((int) (System.currentTimeMillis() + t));
            }
            tNotCount += System.currentTimeMillis();
            t += tNotCount;
            o = objects.next();
            oId = metricSpace.getIDOfObject(o);
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
            oData = metricSpace.getDataOfObject(o);
            distance = df.getDistance(qData, oData);
            if (distance < range) {
                ret.add(new AbstractMap.SimpleEntry<>(oId, distance));
                range = adjustAndReturnSearchRadiusAfterAddingOne(ret, k, Float.MAX_VALUE);
            }
        }
        t += System.currentTimeMillis();
        System.err.println("Query " + qId + " time: " + t);
        incTime(qId, t);
        incDistsComps(qId, distComps);
        incAdditionalParam(qId, lbChecked, 0);
        return ret;
    }

    public QueryLearnStats getQueryStats(Comparable qId) {
        return queryStats.get(qId);
    }

}
