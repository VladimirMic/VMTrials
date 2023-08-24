package vm.vmtrials.deprecated.ptolemaoisLimited;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.auxiliaryForDistBounding.FSPtolemyInequalityWithLimitedAnglesHullsStorageImpl;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.structures.ConvexHull2DEuclid;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
@Deprecated
public class LearnConvexHullsForPivotPairs_OrigProposal {

    public static final Float RATIO_OF_OUTLIERS_TO_CUT = 0.0f; // in total, i.e., half if this number is cut from each side on the x axis
    public static final Integer NUMBER_OF_TETRAHEDRONS = 100000;
    public static final Integer SAMPLE_SET_SIZE = 100000;
    public static final Integer PIVOT_PAIRS = 128;

    public static void main(String[] args) throws SQLException, IOException {
        Dataset dataset;
//        dataset = new FSDatasetInstanceSingularizator.DeCAFDataset();
//        run(dataset);
//        dataset = new FSDatasetInstanceSingularizator.SIFTdataset();
//        run(dataset);
        dataset = new FSDatasetInstanceSingularizator.MPEG7dataset();
        run(dataset);
        dataset = new FSDatasetInstanceSingularizator.RandomDataset20Uniform();
        run(dataset);
    }

    private static void run(Dataset dataset) throws FileNotFoundException {
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();

        DistanceFunctionInterface df = dataset.getDistanceFunction();

        List<Object> metricObjects = dataset.getSampleOfDataset(SAMPLE_SET_SIZE);
        List<Object> pivots = dataset.getPivots(2 * PIVOT_PAIRS);
        for (int p = 0; p < pivots.size(); p += 2) {
            Object[] fourObjects = new Object[4];
            fourObjects[0] = pivots.get(p);
            fourObjects[1] = pivots.get(p + 1);
            List<double[]>[] pointsForHulls = new List[4];
            for (int i = 0; i < 4; i++) {
                pointsForHulls[i] = new ArrayList();
            }
            for (int i = 0; i < NUMBER_OF_TETRAHEDRONS; i++) {
                Object[] twoObjects = Tools.randomUniqueObjects(metricObjects, 2);
                fourObjects[2] = twoObjects[0];
                fourObjects[3] = twoObjects[1];
                Object[] fourObjectsData = ToolsMetricDomain.getData(fourObjects, metricSpace);
                float[] sixDists = ToolsMetricDomain.getPairwiseDistsOfFourObjects(df, true, fourObjectsData);
                if (sixDists == null) {
                    i--;
                    continue;
                }
                float[] anglesRad = Tools.get8Angles(sixDists, false);
                float diffED = Math.abs(sixDists[4] - sixDists[3]);
                float diffEB = Math.abs(sixDists[4] - sixDists[1]);
                for (int j = 0; j < 4; j++) {
                    switch (j) {
                        case 1: {
                            if (diffED < 1) {
                                continue;
                            }
                            break;
                        }
                        case 2: {
                            if (diffEB < 1) {
                                continue;
                            }
                            break;
                        }
                        case 3: {
                            if (diffED * diffEB < 20) {
                                continue;
                            }
                            break;
                        }
                    }
                    double[] equalitiesCoefs = ToolsPtolemaionsLikeCoefs.evaluateEq(anglesRad, j + 1);// see the artile: c = b*d/a * [0] + e*f/a * [1]
                    pointsForHulls[j].add(equalitiesCoefs);
                }
            }
            PointComparatorX comparatorX = new PointComparatorX();
            PointComparatorY comparatorY = new PointComparatorY();
            for (int i = 0; i < 4; i++) {
                pointsForHulls[i].sort(comparatorX);
                int start = i == 0 ? 0 : (int) (pointsForHulls[i].size() * RATIO_OF_OUTLIERS_TO_CUT);
                int end = (int) (pointsForHulls[i].size() * (1f - RATIO_OF_OUTLIERS_TO_CUT));
                pointsForHulls[i] = pointsForHulls[i].subList(start, end);
                pointsForHulls[i].sort(comparatorY);
                start = i == 0 ? 0 : (int) (pointsForHulls[i].size() * RATIO_OF_OUTLIERS_TO_CUT);
                end = (int) (pointsForHulls[i].size() * (1f - RATIO_OF_OUTLIERS_TO_CUT));
                pointsForHulls[i] = pointsForHulls[i].subList(start, end);
                ConvexHull2DEuclid hullsForPivotPair = new ConvexHull2DEuclid();
                for (int j = 0; j < pointsForHulls[i].size(); j++) {
                    double[] point = pointsForHulls[i].get(j);
                    hullsForPivotPair.addPoint(point[0], point[1], false);
                }
                hullsForPivotPair.evaluateHull();
                Logger.getLogger(LearnConvexHullsForPivotPairs_OrigProposal.class.getName()).log(Level.INFO, "Evaluated hull for pivot pair {0}", p / 2);
                FSPtolemyInequalityWithLimitedAnglesHullsStorageImpl storage = new FSPtolemyInequalityWithLimitedAnglesHullsStorageImpl();
                String hullID = metricSpace.getIDOfMetricObject(fourObjects[0]) + "-" + metricSpace.getIDOfMetricObject(fourObjects[1]) + ": Eq.:" + (i + 1);
                String resultDescription = storage.getResultDescription(dataset.getDatasetName(), NUMBER_OF_TETRAHEDRONS, PIVOT_PAIRS, RATIO_OF_OUTLIERS_TO_CUT);
                storage.storeHull(resultDescription, hullID, hullsForPivotPair);
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
