package vm.vmtrials.ptolemaios;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.distEstimation.limitedAngles.foursome.ToolsPtolemaionsLikeCoefs;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.auxiliaryForDistBounding.FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.distance.DistanceFunctionInterface;
import static vm.metricSpace.distance.bounding.twopivots.impl.PtolemaiosFilteringWithLimitedAnglesSimpleCoef.CONSTANT_FOR_PRECISION;
import static vm.vmtrials.ptolemaios.PrintFirstStatsOfDataset.getData;

/**
 *
 * @author Vlada
 */
public class LearnCAOverBDPlusEF {

    public static final Float RATIO_OF_OUTLIERS_TO_CUT = 0.10f / 100f; // percentile defining the minimum and the maximum. I.e., 2 times this is ignored.
    public static final Integer NUMBER_OF_TETRAHEDRONS = 100000;
    public static final Integer SAMPLE_SET_SIZE = 100000;
    public static final Integer PIVOT_PAIRS = 128;

    public static void main(String[] args) throws IOException {
        Dataset dataset;
        dataset = new FSDatasetInstanceSingularizator.DeCAFDataset();
        run(dataset);
        dataset = new FSDatasetInstanceSingularizator.SIFTdataset();
        run(dataset);
        dataset = new FSDatasetInstanceSingularizator.MPEG7dataset();
        run(dataset);
        dataset = new FSDatasetInstanceSingularizator.RandomDataset20Uniform();
        run(dataset);
    }

    private static void run(Dataset dataset) throws FileNotFoundException {
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();

        DistanceFunctionInterface df = dataset.getDistanceFunction();

        List<Object> metricObjects = dataset.getSampleOfDataset(SAMPLE_SET_SIZE);
        List<Object> pivots = dataset.getPivotsForTheSameDataset(2 * PIVOT_PAIRS);
        Map<String, float[]> results = new HashMap<>();
        for (int p = 0; p < pivots.size(); p += 2) {
            float[] fractionSums = new float[NUMBER_OF_TETRAHEDRONS];
            float[] fractionDiffs = new float[NUMBER_OF_TETRAHEDRONS];
            Object[] fourObjects = new Object[4];
            fourObjects[0] = pivots.get(p);
            fourObjects[1] = pivots.get(p + 1);
            for (int i = 0; i < NUMBER_OF_TETRAHEDRONS; i++) {
                Object[] twoObjects = Tools.randomUniqueObjects(metricObjects, 2);
                fourObjects[2] = twoObjects[0];
                fourObjects[3] = twoObjects[1];
                Object[] fourObjectsData = getData(fourObjects, metricSpace);
                float[] sixDists = ToolsPtolemaionsLikeCoefs.getPairwiseDistsOfFourObjects(df, true, fourObjectsData);
                if (sixDists == null) {
                    i--;
                    continue;
                }
                float ac = Math.abs(sixDists[0] * sixDists[2]);
                float ef = Math.abs(sixDists[4] * sixDists[5]);
                float bd = Math.abs(sixDists[1] * sixDists[3]);
                float fractionSum = CONSTANT_FOR_PRECISION * ac / (bd + ef);
                float fractionDiff = ac / Math.abs(bd - ef);
                fractionSums[i] = fractionSum;
                fractionDiffs[i] = fractionDiff;
            }
            Arrays.sort(fractionSums);
            Arrays.sort(fractionDiffs);
            int posMin = Math.round(NUMBER_OF_TETRAHEDRONS * RATIO_OF_OUTLIERS_TO_CUT);
            int posMax = Math.round(NUMBER_OF_TETRAHEDRONS * (1 - RATIO_OF_OUTLIERS_TO_CUT)) - 1;
            String pivotPairsID = metricSpace.getIDOfMetricObject(fourObjects[0]) + "-" + metricSpace.getIDOfMetricObject(fourObjects[1]);
            results.put(pivotPairsID, new float[]{fractionSums[posMin], fractionSums[posMax], fractionDiffs[posMin], fractionDiffs[posMax]});
            Logger.getLogger(LearnCAOverBDPlusEF.class.getName()).log(Level.INFO, "Evaluated coefs for pivot pair {0}", p / 2);
        }
        FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl storage = new FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl();
        String resultDescription = storage.getResultDescription(dataset.getDatasetName(), NUMBER_OF_TETRAHEDRONS, PIVOT_PAIRS, RATIO_OF_OUTLIERS_TO_CUT);
        storage.storeCoefficients(results, resultDescription);
    }
}
