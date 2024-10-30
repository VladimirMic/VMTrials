/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.deprecated.metricSpace.distance.bounding.twopivots.impl;

import java.util.List;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.twopivots.impl.PtolemaicFilterForVoronoiPartitioning;
import vm.metricSpace.distance.bounding.twopivots.impl.PtolemaicFiltering;
import static vm.vmtrials.deprecated.metricSpace.distance.bounding.twopivots.impl.DataDependentPtolemaicFilteringForVoronoiPartitioning.getTrivialPivotOrder;

/**
 *
 * @author Vlada
 * @param <T>
 */
@Deprecated
public class PtolemaicFilteringForVoronoiPartitioning<T> extends PtolemaicFiltering<T> implements PtolemaicFilterForVoronoiPartitioning {

    private final float[][][] dPCurrPiOverdPiPj;

    public PtolemaicFilteringForVoronoiPartitioning(String resultNamePrefix, List<T> pivotsData, DistanceFunctionInterface<T> df, boolean queryDynamicPivotPairs) {
        super(resultNamePrefix, pivotsData, df, queryDynamicPivotPairs);
        dPCurrPiOverdPiPj = new float[pivotsData.size()][pivotsData.size()][pivotsData.size()];
        for (int pCurr = 0; pCurr < pivotsData.size(); pCurr++) {
            T pCurrData = pivotsData.get(pCurr);
            for (int i = 0; i < pivotsData.size(); i++) {
                T piData = pivotsData.get(i);
                float dPCurrPi = df.getDistance(pCurrData, piData);
                if (dPCurrPi == 0) {
                    continue;
                }
                float[] row = coefsPivotPivot[i];
                for (int j = 0; j < pivotsData.size(); j++) {
                    float dPiPjInv = row[j];
                    dPCurrPiOverdPiPj[pCurr][i][j] = dPiPjInv * dPCurrPi;
                }
            }
        }
    }

    @Override
    public String getTechName() {
        String ret = "ptolemaios_for_voronoi_partitioning";
        if (!isQueryDynamicPivotPairs()) {
            ret += "_random_pivots";
        }
        return ret;
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
        return Math.abs(distOPi * dPCurrPiOverdPiPj[pCur][jIdx][iIdx] - distOPj * dPCurrPiOverdPiPj[pCur][iIdx][jIdx]);
    }

    @Override
    public float upperBound(float distOPi, float distOPj, int iIdx, int jIdx, int pCur) {
        return distOPi * dPCurrPiOverdPiPj[pCur][jIdx][iIdx] + distOPj * dPCurrPiOverdPiPj[pCur][iIdx][jIdx];
    }

    @Override
    public float lowerBound(float distP2O, float distP1QMultipliedByCoef, float distP1O, float distP2QMultipliedByCoef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float upperBound(float distP2O, float distP1QMultipliedByCoef, float distP1O, float distP2QMultipliedByCoef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int[] pivotsOrderForLB() {
        if (isQueryDynamicPivotPairs()) {
            throw new UnsupportedOperationException();
//            int[] ret = new int[dPCurrPiOverdPiPj.length];
//            Set<Integer> remain = new HashSet<>();
//            for (int i = 0; i < ret.length; i++) {
//                remain.add(i);
//            }
//            ret = addExtremePivot(ret, remain);
//            return ret;
        }
        return getTrivialPivotOrder(dPCurrPiOverdPiPj.length);
    }
}
