/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.deprecated.metricSpace.distance.bounding.twopivots.impl;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.fs.store.auxiliaryForDistBounding.FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl;
import static vm.fs.store.auxiliaryForDistBounding.FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl.getFile;
import static vm.fs.store.auxiliaryForDistBounding.FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl.transformsCoefsToArrays;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.twopivots.impl.DataDependentPtolemaicFiltering;
import vm.metricSpace.distance.bounding.twopivots.impl.PtolemaicFilterForVoronoiPartitioning;

/**
 *
 * @author Vlada
 * @param <T>
 */
@Deprecated
public class DataDependentPtolemaicFilteringForVoronoiPartitioning<T> extends DataDependentPtolemaicFiltering implements PtolemaicFilterForVoronoiPartitioning {

    private final float[][][][] dPCurrPiOverdPiPj;

    public DataDependentPtolemaicFilteringForVoronoiPartitioning(String namePrefix, float[][][] coefsPivotPivot, List<T> pivotsData, DistanceFunctionInterface<T> df, boolean queryDynamicPivotPairs) {
        super(namePrefix, coefsPivotPivot, queryDynamicPivotPairs);
        dPCurrPiOverdPiPj = new float[pivotsData.size()][pivotsData.size()][pivotsData.size()][2];
        for (int pCurr = 0; pCurr < pivotsData.size(); pCurr++) {
            T pCurrData = pivotsData.get(pCurr);
            for (int i = 0; i < pivotsData.size(); i++) {
                T piData = pivotsData.get(i);
                float dPCurrPi = df.getDistance(pCurrData, piData);
                if (dPCurrPi == 0) {
                    continue;
                }
                for (int j = 0; j < pivotsData.size(); j++) {
                    float coefLB = getCoefPivotPivotForLB(i, j);
                    float coefUB = getCoefPivotPivotForUB(i, j);
                    dPCurrPiOverdPiPj[pCurr][i][j][0] = coefLB * dPCurrPi;
                    dPCurrPiOverdPiPj[pCurr][i][j][1] = coefUB * dPCurrPi;
                }
            }
        }
        super.coefsPivotPivot = null;
        System.gc();
    }

    @Override
    public float lowerBound(Object... args) {
        return lowerBound((float) args[0], (float) args[1], (int) args[2], (int) args[3], (int) args[4]);
    }

    @Override
    public float upperBound(Object... args) {
        return upperBound((float) args[0], (float) args[1], (int) args[2], (int) args[3], (int) args[4]);
    }

    @Override
    public float lowerBound(float distOPi, float distOPj, int iIdx, int jIdx, int pCur) {
        return Math.abs(distOPi * dPCurrPiOverdPiPj[pCur][jIdx][iIdx][0] - distOPj * dPCurrPiOverdPiPj[pCur][iIdx][jIdx][0]);
    }

    @Override
    public float upperBound(float distOPi, float distOPj, int iIdx, int jIdx, int pCur) {
        return distOPi * dPCurrPiOverdPiPj[pCur][jIdx][iIdx][1] + distOPj * dPCurrPiOverdPiPj[pCur][iIdx][jIdx][1];
    }

    @Override
    protected String getTechName() {
        String ret = "data-dependent_ptolemaic_filtering";
        if (!isQueryDynamicPivotPairs()) {
            ret += "_random_pivots";
        }
        return ret;
    }

    @Override
    public int[] pivotsOrderForLB() {
        if (isQueryDynamicPivotPairs()) {
            int[] ret = new int[dPCurrPiOverdPiPj.length];
            Set<Integer> remain = new HashSet<>();
            for (int i = 0; i < ret.length; i++) {
                remain.add(i);
            }
            ret = addExtremePivot(ret, remain);
            return ret;
        }
        return getTrivialPivotOrder(dPCurrPiOverdPiPj.length);
    }

    public static int[] getTrivialPivotOrder(int count) {
        int[] ret = new int[count];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = i;
        }
        return ret;
    }

    private int[] addExtremePivot(int[] ret, Set<Integer> remain) {
        Logger.getLogger(DataDependentPtolemaicFilteringForVoronoiPartitioning.class.getName()).log(Level.INFO, "Deciding best pivot permutation");
        float extreme = Float.MAX_VALUE;
        // find extreme pivot pair - considering a single value
        if (remain.size() == ret.length) {
            for (int i = 0; i < dPCurrPiOverdPiPj.length; i++) {
                for (int j = 0; j < dPCurrPiOverdPiPj.length; j++) {
                    if (i == j) {
                        continue;
                    }
                    for (int pCurr = 0; pCurr < dPCurrPiOverdPiPj.length; pCurr++) {
                        if (pCurr == i || pCurr == j) {
                            continue;
                        }
                        float ratio = dPCurrPiOverdPiPj[pCurr][i][j][0] / dPCurrPiOverdPiPj[pCurr][j][i][0];
                        ratio = Math.min(ratio, 1 / ratio);
                        if (ratio < extreme) {
                            extreme = ratio;
                            ret[0] = i;
                            ret[1] = j;
                            ret[2] = pCurr;
                        }
                    }
                }
            }
            remain.remove(ret[0]);
            remain.remove(ret[1]);
            remain.remove(ret[2]);
        }
        int stop = ret.length - remain.size();
        while (!remain.isEmpty()) {
            float lowestMin = Float.MAX_VALUE;
            int bestCand = 0;
            for (int cand : remain) {
                for (int i = 0; i < stop; i++) {
                    int retI = ret[i];
                    if (retI == cand) {
                        continue;
                    }
                    for (int j = 0; j < stop; j++) {
                        int retJ = ret[j];
                        if (retJ == cand || retJ == retI) {
                            continue;
                        }
                        float ratio = dPCurrPiOverdPiPj[cand][retI][retJ][0] / dPCurrPiOverdPiPj[cand][retJ][retI][0];
                        ratio = Math.min(ratio, 1 / ratio);
                        if (ratio < lowestMin) {
                            lowestMin = ratio;
                            bestCand = cand;
                        }
                    }
                }
            }
            ret[stop] = bestCand;
            remain.remove(bestCand);
            stop++;
        }
        Logger.getLogger(DataDependentPtolemaicFilteringForVoronoiPartitioning.class.getName()).log(Level.INFO, "Finished deciding best pivot permutation");
        return ret;
    }
    
        public static DataDependentPtolemaicFilteringForVoronoiPartitioning getLearnedInstanceForVoronoiPartitioning(String resultPreffixName, Dataset dataset, int pivotCount) {
        return getLearnedInstanceForVoronoiPartitioning(resultPreffixName, dataset, pivotCount, true);
    }

    public static DataDependentPtolemaicFilteringForVoronoiPartitioning getLearnedInstanceForVoronoiPartitioning(String resultPreffixName, Dataset dataset, int pivotCount, boolean wisePivotSelection) {
        FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl storageOfCoefs = new FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl();
        String fileName = storageOfCoefs.getNameOfFileWithCoefs(dataset.getDatasetName(), pivotCount, true);
        File file = getFile(fileName, false);
        Map<String, float[]> coefs = Tools.parseCsvMapKeyFloatValues(file.getAbsolutePath());
        List pivots = dataset.getPivots(pivotCount);
        List pivotsData = dataset.getMetricSpace().getDataOfMetricObjects(pivots);
        List pivotIDs = ToolsMetricDomain.getIDsAsList(pivots.iterator(), dataset.getMetricSpace());
        float[][][] coefsToArrays = transformsCoefsToArrays(coefs, pivotIDs);
        return new DataDependentPtolemaicFilteringForVoronoiPartitioning(resultPreffixName, coefsToArrays, pivotsData, dataset.getDistanceFunction(), wisePivotSelection);
    }

}
