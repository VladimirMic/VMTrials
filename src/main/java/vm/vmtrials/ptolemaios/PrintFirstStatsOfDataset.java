package vm.vmtrials.ptolemaios;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import vm.datatools.Tools;
import vm.distEstimation.limitedAngles.foursome.ToolsPtolemaionsLikeCoefs;
import vm.fs.FSGlobal;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.metricSpaceImpl.FSMetricSpaceImpl;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
public class PrintFirstStatsOfDataset {

    public static final Integer NUMBER_OF_TETRAHEDRONS = 10000;
    public static final Integer SAMPLE_SET_SIZE = 100000;
    public static final Integer PIVOT_PAIRS = 1;

    public static void main(String[] args) throws IOException {
        Dataset dataset;
        dataset = new FSDatasetInstanceSingularizator.DeCAFDataset();
        run(dataset);
        dataset = new FSDatasetInstanceSingularizator.SIFTdataset();
        run(dataset);
        dataset = new FSDatasetInstanceSingularizator.RandomDataset20Uniform();
        run(dataset);
        dataset = new FSDatasetInstanceSingularizator.MPEG7dataset();
        run(dataset);
    }

    public static void run(Dataset dataset) throws FileNotFoundException {
        AbstractMetricSpace metricSpace = new FSMetricSpaceImpl<>();

        DistanceFunctionInterface df = dataset.getDistanceFunction();

        File folder = new File(FSGlobal.TRIALS_FOLDER + "Ptolemaions_limited\\EFgetBD\\" + dataset.getDatasetName());
        File file = new File(folder.getAbsolutePath() + "\\Stats_per_pairs_oriented_" + NUMBER_OF_TETRAHEDRONS + "_" + PIVOT_PAIRS + ".csv");
        file = FSGlobal.checkFileExistence(file, true);
        System.setErr(new PrintStream(file));

        List<Object> metricObjects = dataset.getSampleOfDataset(SAMPLE_SET_SIZE);
        List<Object> pivots = dataset.getPivotsForTheSameDataset(-1);
        for (int p = 0; p < 2 * PIVOT_PAIRS; p += 2) {
            Object[] fourObjects = new Object[4];
            fourObjects[0] = pivots.get(p);
            fourObjects[1] = pivots.get(p + 1);
            for (int i = 0; i < NUMBER_OF_TETRAHEDRONS; i++) {
                Object[] twoObjects = Tools.randomUniqueObjects(metricObjects, 2);
                fourObjects[2] = twoObjects[0];
                fourObjects[3] = twoObjects[1];
                Object[] fourObjectsData = ToolsMetricDomain.getData(fourObjects, metricSpace);
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
                Tools.printArray(trialGetKnownPartsOfF1F2ForEq1(sixDists, anglesRad), false);
                System.err.println();
                System.err.flush();
            }
        }
        System.err.close();
    }

    // 6 dists a, b, c, d, e, f
    // 8 angles 0: beta1, 1: delta2, 2: gamma2, 3: alphao, 4: deltao, 5: betaq, 6: alphaq, 7: gamma1
    private static double[] trialGetKnownPartsOfF1F2ForEq1(float[] sixDists, float[] anglesRad) {
        float beta1 = anglesRad[0];
        float alphao = anglesRad[3];
        float delta2 = anglesRad[1];
        float alphaq = anglesRad[6];
        float a = sixDists[0];
        float b = sixDists[1];
        float d = sixDists[3];
        float e = sixDists[4];
        float f = sixDists[5];
        double z1 = (1 - Math.cos(alphao)) / (Math.cos(beta1) - Math.cos(alphao + beta1));
        double z2 = Math.sin(alphao + beta1) / Math.sin(beta1);
        double z3 = Math.sin(delta2) / Math.sin(delta2 + alphaq);
        double f1Known = z1 / (d + e) * (z2 * e / d + 1);
        double f2Known = z1 / (d + e) * (b / f + z3);
        return new double[]{f1Known, f2Known};
    }

}
