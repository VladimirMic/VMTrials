package vm.vmtrials.ptolemaios;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.distEstimation.limitedAngles.foursome.ToolsPtolemaionsLikeCoefs;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.auxiliaryForDistBounding.FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl;
import vm.fs.store.precomputedDists.FSPrecomputedDistPairsStorageImpl;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import static vm.metricSpace.distance.bounding.twopivots.impl.PtolemaiosFilteringWithLimitedAnglesSimpleCoef.CONSTANT_FOR_PRECISION;
import static vm.vmtrials.ptolemaios.LearnConvexHullsForPivotPairs_OrigProposal.RATIO_OF_OUTLIERS_TO_CUT;

/**
 *
 * @author Vlada
 */
public class LearnCAOverBDPlusEF {

    public static final Integer SAMPLE_SET_SIZE = 10000;
    public static final Integer SAMPLE_QUERY_SET_SIZE = 1000;
    public static final Integer PIVOTS = 512;

    public static void main(String[] args) throws IOException {
        Dataset dataset;
        dataset = new FSDatasetInstanceSingularizator.DeCAFDataset();
        run(dataset);
        dataset = new FSDatasetInstanceSingularizator.SIFTdataset();
        run(dataset);
//        dataset = new FSDatasetInstanceSingularizator.MPEG7dataset();
//        run(dataset);
        dataset = new FSDatasetInstanceSingularizator.RandomDataset20Uniform();
        run(dataset);
    }

    private static void run(Dataset dataset) throws FileNotFoundException {
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();

        DistanceFunctionInterface df = dataset.getDistanceFunction();

        List<Object> metricObjects = dataset.getSampleOfDataset(SAMPLE_SET_SIZE + SAMPLE_QUERY_SET_SIZE);
        Map<Object, Object> metricObjectsAsIdObjectMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, metricObjects);
        List<Object> pivots = dataset.getPivotsForTheSameDataset(PIVOTS);

        FSPrecomputedDistPairsStorageImpl smallDistStorage = new FSPrecomputedDistPairsStorageImpl(dataset.getDatasetName(), SAMPLE_SET_SIZE, SAMPLE_QUERY_SET_SIZE);
        TreeSet<Map.Entry<String, Float>> smallDists = smallDistStorage.loadPrecomputedDistances();
        Map<String, float[]> results = new HashMap<>();
        int tetrahedronsPerPivots = smallDists.size();
        for (int p = 0; p < pivots.size(); p++) {
            List<Float> fractionSums = new ArrayList<>();
            List<Float> fractionDiffs = new ArrayList<>();
            Object[] fourObjects = new Object[4];
            fourObjects[0] = pivots.get(p);
            fourObjects[1] = pivots.get((p + 1) % pivots.size());
            for (Map.Entry<String, Float> smallDist : smallDists) {
                String[] qoIDs = smallDist.getKey().split(";");
                fourObjects[2] = metricObjectsAsIdObjectMap.get(qoIDs[0]);
                fourObjects[3] = metricObjectsAsIdObjectMap.get(qoIDs[1]);
                Object[] fourObjectsData = ToolsMetricDomain.getData(fourObjects, metricSpace);
                float[] sixDists = ToolsPtolemaionsLikeCoefs.getPairwiseDistsOfFourObjects(df, true, fourObjectsData);
                if (sixDists == null || Tools.isZeroInArray(sixDists)) {
                    continue;
                }
                float ac = Math.abs(sixDists[0] * sixDists[2]);
                float ef = Math.abs(sixDists[4] * sixDists[5]);
                float bd = Math.abs(sixDists[1] * sixDists[3]);
                float fractionSum = CONSTANT_FOR_PRECISION * ac / (bd + ef);
                float fractionDiff = ac / Math.abs(bd - ef);
                fractionSums.add(fractionSum);
                fractionDiffs.add(fractionDiff);
            }
            Collections.sort(fractionSums);
            Collections.sort(fractionDiffs);
            int posMin = Math.round(fractionSums.size() * RATIO_OF_OUTLIERS_TO_CUT);
            int posMax = Math.round(fractionSums.size() * (1 - RATIO_OF_OUTLIERS_TO_CUT)) - 1;
            String pivotPairsID = metricSpace.getIDOfMetricObject(fourObjects[0]) + "-" + metricSpace.getIDOfMetricObject(fourObjects[1]);
            results.put(pivotPairsID, new float[]{fractionSums.get(posMin), fractionSums.get(posMax), fractionDiffs.get(posMin), fractionDiffs.get(posMax)});
            Logger.getLogger(LearnCAOverBDPlusEF.class.getName()).log(Level.INFO, "Evaluated coefs for pivot pair {0} ({1}, {2}, {3}, {4})", new Object[]{p, fractionSums.get(posMin), fractionSums.get(posMax), fractionDiffs.get(posMin), fractionDiffs.get(posMax)});
        }
        FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl storage = new FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl();
        String resultDescription = storage.getResultDescription(dataset.getDatasetName(), tetrahedronsPerPivots, PIVOTS, RATIO_OF_OUTLIERS_TO_CUT);
        storage.storeCoefficients(results, resultDescription);
    }
}
