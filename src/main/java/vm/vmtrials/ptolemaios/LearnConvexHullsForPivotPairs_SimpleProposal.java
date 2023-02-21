package vm.vmtrials.ptolemaios;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.distEstimation.limitedAngles.foursome.ToolsPtolemaionsLikeCoefs;
import vm.fs.FSGlobal;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.auxiliaryForDistBounding.FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl;
import vm.metricspace.AbstractMetricSpace;
import vm.metricspace.Dataset;
import vm.metricspace.distance.DistanceFunctionInterface;
import vm.structures.ConvexHull2DEuclid;
import static vm.vmtrials.ptolemaios.PrintFirstStatsOfDataset.getData;

/**
 *
 * @author Vlada
 */
public class LearnConvexHullsForPivotPairs_SimpleProposal {

    public static final Float RATIO_OF_OUTLIERS_TO_CUT = 0f; // in total, i.e., half if this number is cut from each side on the x axis
    public static final Integer NUMBER_OF_TETRAHEDRONS = 500000;
    public static final Integer SAMPLE_SET_SIZE = 100000;
    public static final Integer PIVOT_PAIRS = 128;

    public static void main(String[] args) throws FileNotFoundException {
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

    private static final void run(Dataset dataset) throws FileNotFoundException {
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();

        DistanceFunctionInterface df = dataset.getDistanceFunction();
        String output = FSGlobal.TRIALS_FOLDER + "Ptolemaions_limited\\EFgetBD\\Hulls_simple\\" + dataset.getDatasetName() + "__tetrahedrons_" + NUMBER_OF_TETRAHEDRONS + "__ratio_of_outliers_to_cut_" + RATIO_OF_OUTLIERS_TO_CUT + "__pivot_pairs_" + PIVOT_PAIRS + ".csv";
        new File(output).getParentFile().mkdirs();
        PrintStream err = System.err;
        System.setOut(new PrintStream(output + "_redable.csv"));

        List<Object> metricObjects = dataset.getSampleOfDataset(SAMPLE_SET_SIZE);
        List<Object> pivots = dataset.getPivotsForTheSameDataset(2 * PIVOT_PAIRS);
        for (int p = 0; p < pivots.size(); p += 2) {
            Object[] fourObjects = new Object[4];
            fourObjects[0] = pivots.get(p);
            fourObjects[1] = pivots.get(p + 1);
            List<double[]>[] pointsForHulls = new List[2];
            for (int i = 0; i < 2; i++) {
                pointsForHulls[i] = new ArrayList();
            }
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
                pointsForHulls[0].add(new double[]{bd, ac});
                pointsForHulls[1].add(new double[]{ef, ac});
            }
//            PointComparatorX comparatorX = new PointComparatorX();
//            PointComparatorY comparatorY = new PointComparatorY();
            for (int i = 0; i < 2; i++) {
//                pointsForHulls[i].sort(comparatorX);
//                int start = i == 0 ? 0 : (int) (pointsForHulls[i].size() * RATIO_OF_OUTLIERS_TO_CUT);
//                int end = (int) (pointsForHulls[i].size() * (1f - RATIO_OF_OUTLIERS_TO_CUT));
//                pointsForHulls[i] = pointsForHulls[i].subList(start, end);
//                pointsForHulls[i].sort(comparatorY);
//                start = i == 0 ? 0 : (int) (pointsForHulls[i].size() * RATIO_OF_OUTLIERS_TO_CUT);
//                end = (int) (pointsForHulls[i].size() * (1f - RATIO_OF_OUTLIERS_TO_CUT));
//                pointsForHulls[i] = pointsForHulls[i].subList(start, end);
                ConvexHull2DEuclid hullsForPivotPair = new ConvexHull2DEuclid();
                for (int j = 0; j < pointsForHulls[i].size(); j++) {
                    double[] point = pointsForHulls[i].get(j);
                    hullsForPivotPair.addPoint(point[0], point[1], false);
                }
                hullsForPivotPair.evaluateHull();
                Logger.getLogger(LearnConvexHullsForPivotPairs_OrigProposal.class.getName()).log(Level.INFO, "Evaluated hull for pivot pair {0}", p / 2);
                FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl storage = new FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl();
                String hullID = metricSpace.getIDOfMetricObject(fourObjects[0]) + "-" + metricSpace.getIDOfMetricObject(fourObjects[1]) + ": Eq.:" + (i + 1);
                storage.storeHull(output, hullID, hullsForPivotPair);
                System.out.print(hullID);
                System.out.println("");
                hullsForPivotPair.printAsCoordinatesInColumns();
            }
        }
        System.out.close();
        System.err.close();
    }

    private static class PointComparatorX implements Comparator<double[]> {

        @Override
        public int compare(double[] o1, double[] o2) {
            if (o1[0] != o2[0]) {
                return Double.compare(o1[0], o2[0]);
            }
            return Double.compare(o1[1], o2[1]);
        }
    }

    private static class PointComparatorY implements Comparator<double[]> {

        @Override
        public int compare(double[] o1, double[] o2) {
            if (o1[0] != o2[0]) {
                return Double.compare(o1[1], o2[1]);
            }
            return Double.compare(o1[0], o2[0]);
        }
    }

}
