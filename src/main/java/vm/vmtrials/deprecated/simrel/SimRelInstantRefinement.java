/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.deprecated.simrel;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.search.algorithm.SearchingAlgorithm;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.distance.AbstractDistanceFunction;
import vm.simRel.SimRelInterface;
import vm.simRel.impl.SimRelEuclideanPCAImplForTesting;

/**
 *
 * @author Vlada
 * @param <T>
 */
@Deprecated
public class SimRelInstantRefinement<T> extends SearchingAlgorithm<T> {

    private static final Logger LOG = Logger.getLogger(SimRelInstantRefinement.class.getName());
    private final SimRelInterface<float[]> simRelFunc;

    private int distCompsOfLastExecutedQuery;
    private long simRelEvalCounter;
    private int kPCA;
    private AbstractDistanceFunction<T> fullDF;

    public SimRelInstantRefinement(AbstractDistanceFunction<T> fullDF, SimRelInterface simRelFunc, int kPCA) {
        this.fullDF = fullDF;
        this.simRelFunc = simRelFunc;
        this.kPCA = kPCA;
    }

    @Override
    public TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch(AbstractSearchSpace<T> fullMetricSpace, Object fullQueryObject, int k, Iterator<Object> pcaOfCandidates, Object... additionalParams) {
        long t = -System.currentTimeMillis();
        TreeSet<Map.Entry<Comparable, Float>> currAnswer = null;
        int paramIDX = 0;
        if (additionalParams.length > 0 && additionalParams[0] instanceof TreeSet) {
            currAnswer = (TreeSet<Map.Entry<Comparable, Float>>) additionalParams[0];
            paramIDX++;
        }
        T qData = fullMetricSpace.getDataOfObject(fullQueryObject);
        Comparable qId = fullMetricSpace.getIDOfObject(fullQueryObject);
        TreeSet<Map.Entry<Comparable, Float>> ret = currAnswer == null ? new TreeSet<>(new Tools.MapByFloatValueComparator()) : currAnswer;

        if (simRelFunc instanceof SimRelEuclideanPCAImplForTesting) {
            SimRelEuclideanPCAImplForTesting euclid = (SimRelEuclideanPCAImplForTesting) simRelFunc;
            euclid.resetEarlyStopsOnCoordsCounts();
        }

        Map<Object, Object> mapOfAllFullObjects = (Map<Object, Object>) additionalParams[paramIDX++];
        AbstractSearchSpace<float[]> pcaMetricSpace = (AbstractSearchSpace<float[]>) additionalParams[paramIDX++];
        Object pcaQueryObject = additionalParams[paramIDX++];

        float[] pcaQueryObjData = pcaMetricSpace.getDataOfObject(pcaQueryObject);

        List<Object> ansOfSimRel = new ArrayList<>();
        Set<Object> ansOfSimRelSet = new HashSet<>();
        Map<Object, float[]> candSetData = new HashMap<>();
        simRelEvalCounter = 0;

        float lastRad = -1;
        for (int i = 1; pcaOfCandidates.hasNext(); i++) {
            Object oPCA = pcaOfCandidates.next();
            Comparable oPCAID = pcaMetricSpace.getIDOfObject(oPCA);
            float[] oPCAData = pcaMetricSpace.getDataOfObject(oPCA);
            boolean toBeAdded = addOToAnswer(kPCA, pcaQueryObjData, oPCAData, oPCAID, ansOfSimRel, ansOfSimRelSet, candSetData);
            if (toBeAdded) {
                T fullOData = (T) mapOfAllFullObjects.get(oPCAID);
                float distance = fullDF.getDistance(qData, fullOData);
                distCompsOfLastExecutedQuery++;
                ret.add(new AbstractMap.SimpleEntry<>(oPCAID, distance));
                float radius = adjustAndReturnSearchRadiusAfterAddingOne(ret, kPCA, Float.MAX_VALUE);
                if (distance <= radius) {
                    candSetData.put(oPCAID, oPCAData);
                    if (!ansOfSimRelSet.contains(oPCAID)) {
                        ansOfSimRel.add(oPCAID);
                        ansOfSimRelSet.add(oPCAID);
                    }
                }
                if (radius != lastRad && ret.size() != ansOfSimRel.size()) {
                    lastRad = radius;
                    Set preserve = new HashSet();
                    for (Map.Entry<Comparable, Float> entry : ret) {
                        preserve.add(entry.getKey());
                    }
                    for (Object key : ansOfSimRelSet) {
                        if (!preserve.contains(key)) {
                            ansOfSimRel.remove(key);
                            candSetData.remove(key);
                        }
                    }
                    ansOfSimRelSet = preserve;
                }
            }
        }
        t += System.currentTimeMillis();
        incTime(qId, t);
        incDistsComps(qId, distCompsOfLastExecutedQuery);
        LOG.log(Level.INFO, "Evaluated query {2} using {0} dist comps. Time: {1}", new Object[]{getDistCompsForQuery(qId), getTimeOfQuery(qId), qId.toString()});
        return ret;
    }

    // differs from the method in SimRelSeqScanKNNCandSet!!!
    private boolean addOToAnswer(int k, float[] queryObjectData, float[] oData, Object idOfO, List<Object> ansOfSimRel, Set ansOfSimRelSet, Map<Object, float[]> mapOfData) {
        if (ansOfSimRel.isEmpty()) {
            ansOfSimRel.add(idOfO);
            ansOfSimRelSet.add(idOfO);
            mapOfData.put(idOfO, oData);
            return true;
        }
        int idxWhereAdd = Integer.MAX_VALUE;
        List<Integer> indexesToRemove = new ArrayList<>();
        for (int i = ansOfSimRel.size() - 1; i >= 0; i--) {
            if (!mapOfData.containsKey(ansOfSimRel.get(i))) {
                String s = "";
            }
            float[] oLastData = mapOfData.get(ansOfSimRel.get(i));
            simRelEvalCounter++;
            short sim = simRelFunc.getMoreSimilar(queryObjectData, oLastData, oData);
            if (sim == 1) {
                if (i < k - 1) {
                    deleteIndexes(ansOfSimRel, k, indexesToRemove, mapOfData);
                    ansOfSimRel.add(i + 1, idOfO);
                    ansOfSimRelSet.add(idOfO);
                    mapOfData.put(idOfO, oData);
                    return true;
                }
                return false;
            }
            if (sim == 2) {
                idxWhereAdd = i;
                indexesToRemove.add(i);
            }
        }
        if (idxWhereAdd != Integer.MAX_VALUE) {
            deleteIndexes(ansOfSimRel, k, indexesToRemove, mapOfData);
            ansOfSimRel.add(idxWhereAdd, idOfO);
            ansOfSimRelSet.add(idOfO);
            mapOfData.put(idOfO, oData);
            return true;
        }
        return ansOfSimRelSet.size() < kPCA;
    }

    private void deleteIndexes(List<Object> ret, int k, List<Integer> indexesToRemove, Map<Object, float[]> retData) {
        while (ret.size() >= k && !indexesToRemove.isEmpty()) {
            Integer idx = indexesToRemove.get(0);
            Object id = ret.get(idx);
            retData.remove(id);
            ret.remove(id);
            indexesToRemove.remove(0);
        }
    }

    @Override
    public List<Comparable> candSetKnnSearch(AbstractSearchSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    public Object getSimRelStatsOfLastExecutedQuery() {
        if (simRelFunc instanceof SimRelEuclideanPCAImplForTesting) {
            SimRelEuclideanPCAImplForTesting euclid = (SimRelEuclideanPCAImplForTesting) simRelFunc;
            return euclid.getEarlyStopsOnCoordsCounts();
        }
        throw new RuntimeException("No simRel stats for the last query");
    }

    @Override
    public String getResultName() {
        return "Deprecated";
    }
}
