//package vm.vmtrials.ptolemaios;
//
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.List;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import vm.datatools.Tools;
//import vm.distEstimation.limitedAngles.foursome.ToolsPtolemaionsLikeCoefs;
//import vm.fs.dataset.FSDatasetInstanceSingularizator;
//import vm.fs.store.auxiliaryForDistBounding.FSPtolemyInequalityWithLimitedAnglesHullsStorageImpl;
//import vm.metricSpace.AbstractMetricSpace;
//import vm.metricSpace.Dataset;
//import vm.metricSpace.distance.DistanceFunctionInterface;
//import vm.structures.ConvexHull2DEuclid;
//import static vm.vmtrials.ptolemaios.PrintFirstStatsOfDataset.getData;
//
///**
// *
// * @author Vlada
// */
//public class LearnCAOverBDPlusEF {
//
//    public static final Integer NUMBER_OF_TETRAHEDRONS = 100000;
//    public static final Integer SAMPLE_SET_SIZE = 100000;
//    public static final Integer PIVOT_PAIRS = 128;
//
//    public static void main(String[] args) throws SQLException, IOException {
//        Dataset dataset;
////        dataset = new FSDatasetInstanceSingularizator.DeCAFDataset();
////        run(dataset);
////        dataset = new FSDatasetInstanceSingularizator.SIFTdataset();
////        run(dataset);
//        dataset = new FSDatasetInstanceSingularizator.MPEG7dataset();
//        run(dataset);
//        dataset = new FSDatasetInstanceSingularizator.RandomDataset20Uniform();
//        run(dataset);
//    }
//
//    private static void run(Dataset dataset) throws FileNotFoundException {
//        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
//
//        DistanceFunctionInterface df = dataset.getDistanceFunction();
//
//        List<Object> metricObjects = dataset.getSampleOfDataset(SAMPLE_SET_SIZE);
//        List<Object> pivots = dataset.getPivotsForTheSameDataset(2 * PIVOT_PAIRS);
//        for (int p = 0; p < pivots.size(); p += 2) {
//            String pivotPairsID = metricSpace.getIDOfMetricObject(fourObjects[0]) + "-" + metricSpace.getIDOfMetricObject(fourObjects[1]);
//            Object[] fourObjects = new Object[4];
//            fourObjects[0] = pivots.get(p);
//            fourObjects[1] = pivots.get(p + 1);
//            for (int i = 0; i < NUMBER_OF_TETRAHEDRONS; i++) {
//                Object[] twoObjects = Tools.randomUniqueObjects(metricObjects, 2);
//                fourObjects[2] = twoObjects[0];
//                fourObjects[3] = twoObjects[1];
//                Object[] fourObjectsData = getData(fourObjects, metricSpace);
//                float[] sixDists = ToolsPtolemaionsLikeCoefs.getPairwiseDistsOfFourObjects(df, true, fourObjectsData);
//                if (sixDists == null) {
//                    i--;
//                    continue;
//                }
//                float ac = Math.abs(sixDists[0] * sixDists[2]);
//                float ef = Math.abs(sixDists[4] * sixDists[5]);
//                float bd = Math.abs(sixDists[1] * sixDists[3]);
//            }
//            Logger.getLogger(LearnCAOverBDPlusEF.class.getName()).log(Level.INFO, "Evaluated hull for pivot pair {0}", p / 2);
//        }
//        PtolemyInequalityWithLimitedAnglesCoefsStoreInterface storage = new FSPtolemyInequalityWithLimitedAnglesHullsStorageImpl();
//        String resultDescription = storage.getResultDescription(dataset.getDatasetName(), NUMBER_OF_TETRAHEDRONS, PIVOT_PAIRS, RATIO_OF_OUTLIERS_TO_CUT);
//        storage.storeHull(resultDescription, hullID, hullsForPivotPair);
//    }
//}
