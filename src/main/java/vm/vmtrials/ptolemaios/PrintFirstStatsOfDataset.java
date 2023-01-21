package vm.vmtrials.ptolemaios;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.List;
import vm.datatools.Tools;
import vm.db.metricSpaceImpl.DBMetricSpaceImpl;
import vm.db.metricSpaceImpl.DBMetricSpacesStorage;
import vm.distEstimation.limitedAngles.foursome.ToolsPtolemaionsLikeCoefs;
import vm.metricspace.AbstractMetricSpace;
import vm.metricspace.MetricSpacesStorageInterface;
import vm.metricspace.dataToStringConvertors.impl.FloatVectorConvertor;
import vm.metricspace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
public class PrintFirstStatsOfDataset {

    public static final Integer NUMBER_OF_TETRAHEDRONS = 10000;
    public static final Integer SAMPLE_SET_SIZE = 100000;
    public static final Integer PIVOT_PAIRS = 4;

    public static void main(String[] args) throws SQLException, IOException {
        String datasetName = "decaf_1m";

        AbstractMetricSpace metricSpace = new DBMetricSpaceImpl<>();
        MetricSpacesStorageInterface metricSpacesStorage = new DBMetricSpacesStorage<>(metricSpace, new FloatVectorConvertor());

        DistanceFunctionInterface df = metricSpace.getDistanceFunctionForDataset(datasetName);

        System.setErr(new PrintStream("h:\\Skola\\2022\\Ptolemaions_limited\\EFgetBD\\DeCAF_stats_per_pairs_oriented_" + NUMBER_OF_TETRAHEDRONS + ".csv"));

        List<Object> metricObjects = metricSpacesStorage.getSampleOfDataset(datasetName, SAMPLE_SET_SIZE);
        List<Object> pivots = metricSpacesStorage.getPivots(datasetName);
        for (int p = 0; p < 2 * PIVOT_PAIRS; p += 2) {
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
                // print objects IDs
                for (int j = 0; j < fourObjects.length; j++) {
                    System.err.print(metricSpace.getIDOfMetricObject(fourObjects[j]));
                    if (j == fourObjects.length - 1) {
                        System.err.print(";");
                    } else {
                        System.err.print("-");
                    }
                }
                // print 6 dists a, b, c, d, e, f
                Tools.printArray(sixDists, false);

                // print 8 angles 0: beta1, 1: delta2, 2: gamma2, 3: alphao, 4: deltao, 5: betaq, 6: alphaq, 7: gamma1
                float[] anglesRad = ToolsPtolemaionsLikeCoefs.get8Angles(sixDists, false);
                float[] anglesDeg = vm.math.Tools.radsToDeg(anglesRad);
                Tools.printArray(anglesDeg, false);

                // print fractions bd/a and ef/a
                float bda = sixDists[1] * sixDists[3] / sixDists[0];
                float efa = sixDists[4] * sixDists[5] / sixDists[0];
                System.err.print(bda + ";" + efa + ";");

                // print ptolemy bounds on c
                float[] ptolemyBoundsOnC = new float[2];
                ptolemyBoundsOnC[0] = Math.abs(efa - bda);
                ptolemyBoundsOnC[1] = bda + efa;
                Tools.printArray(ptolemyBoundsOnC, false);

                float secCondde = Math.abs(sixDists[4] - sixDists[3]);
                float thirdCondbe = Math.abs(sixDists[4] - sixDists[1]);
                System.err.print(secCondde + ";");
                System.err.print(thirdCondbe + ";");
                System.err.print((secCondde * thirdCondbe) + ";");
                for (int j = 1; j <= 4; j++) {
                    double[] equalitiesCoefs = ToolsPtolemaionsLikeCoefs.evaluateEq(anglesRad, j);// see the artile: c = b*d/a * [0] + e*f/a * [1]
                    Tools.printArray(equalitiesCoefs, false);
                    float check = (float) (bda * equalitiesCoefs[0] + efa * equalitiesCoefs[1]);
                    System.err.print(check + ";" + Math.abs(check - sixDists[2]) + ";");
                }
                System.err.println();
                System.err.flush();
            }
        }
        System.err.close();
    }

    public static Object[] getData(Object[] fourObjects, AbstractMetricSpace metricSpace) {
        Object[] ret = new Object[fourObjects.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = metricSpace.getDataOfMetricObject(fourObjects[i]);
        }
        return ret;
    }

}
