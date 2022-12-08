package vm.vmtrials.ptolemaios;

import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.List;
import vm.datatools.Tools;
import vm.db.metricSpaceImpl.DBMetricSpaceImpl;
import vm.db.metricSpaceImpl.DBMetricSpacesStorage;
import vm.distEstimation.limitedAngles.foursome.ToolsPtolemaionsLikeCoefs;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.MetricSpacesStorageInterface;
import vm.metricSpace.dataToStringConvertors.impl.FloatVectorConvertor;
import vm.metricSpace.distance.DistanceFunction;

/**
 *
 * @author Vlada
 */
public class PrintFirstStatsOfDataset {

    public static final Integer NUMBER_OF_QUATERNIONS = 10000;
    public static final Integer SAMPLE_SET_SIZE = 100000;

    public static void main(String[] args) throws SQLException, IOException {
        String datasetName = "decaf_1m";

        AbstractMetricSpace metricSpace = new DBMetricSpaceImpl<>();
        MetricSpacesStorageInterface metricSpacesStorage = new DBMetricSpacesStorage<>(metricSpace, new FloatVectorConvertor());

        DistanceFunction df = metricSpace.getDistanceFunctionForDataset(datasetName);

        System.setErr(new PrintStream("h:\\Skola\\2022\\Ptolemaions_limited\\DeCAF_stats_" + NUMBER_OF_QUATERNIONS + ".csv"));

        List<Object> metricObjects = metricSpacesStorage.getSampleOfDataset(datasetName, SAMPLE_SET_SIZE);
        for (int i = 0; i < NUMBER_OF_QUATERNIONS; i++) {
            Object[] fourObjects = Tools.randomUniqueObjects(metricObjects, 4);
            Object[] fourObjectsData = getData(fourObjects, metricSpace);
            float[] sixDists = ToolsPtolemaionsLikeCoefs.getPairwiseDistsOfFourObjects(df, true, fourObjectsData);
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
        System.err.close();
    }

    private static Object[] getData(Object[] fourObjects, AbstractMetricSpace metricSpace) {
        Object[] ret = new Object[fourObjects.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = metricSpace.getMetricObjectData(fourObjects[i]);
        }
        return ret;
    }

}
