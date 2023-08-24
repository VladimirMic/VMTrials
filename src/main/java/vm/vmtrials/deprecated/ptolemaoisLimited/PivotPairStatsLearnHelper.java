package vm.vmtrials.deprecated.ptolemaoisLimited;

import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 * @param <T> data type upon which operated the distance function. See the
 * abstractMetricSpace definition.
 */
@Deprecated
public class PivotPairStatsLearnHelper<T> {

    private final Object p1ID;
    private final Object p2ID;

    private final T p1Data;
    private final T p2Data;

    private final DistanceFunctionInterface<T> df;

    private final Set<Point2D.Double>[] coefsForEquations;

    private final Logger LOG = Logger.getLogger(PivotPairStatsLearnHelper.class.getName());

    public PivotPairStatsLearnHelper(Object p1ID, Object p2ID, T p1Data, T p2Data, DistanceFunctionInterface<T> df) {
        this.p1ID = p1ID;
        this.p2ID = p2ID;
        this.p1Data = p1Data;
        this.p2Data = p2Data;
        this.df = df;
        coefsForEquations = new HashSet[4];
        for (int i = 0; i < coefsForEquations.length; i++) {
            coefsForEquations[i] = new HashSet<>();
        }
    }

    public void registerSamplePointsQO(T qData, T oData) {
        float[] sixDists = ToolsMetricDomain.getPairwiseDistsOfFourObjects(df, true, p1Data, p2Data, oData, qData);
        if (sixDists == null) {
            LOG.log(Level.INFO, "Zero distance observed, continuing");
            return;
        }
        // print 6 dists a, b, c, d, e, f
        Tools.printArray(sixDists, false);

        // print 8 angles 0: beta1, 1: delta2, 2: gamma2, 3: alphao, 4: deltao, 5: betaq, 6: alphaq, 7: gamma1
        float[] anglesRad = Tools.get8Angles(sixDists, false);
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
        for (int i = 1; i <= 4; i++) {
            double[] equalitiesCoefs = ToolsPtolemaionsLikeCoefs.evaluateEq(anglesRad, i);// see the artile: c = b*d/a * [0] + e*f/a * [1]
            Tools.printArray(equalitiesCoefs, false);
            float check = (float) (bda * equalitiesCoefs[0] + efa * equalitiesCoefs[1]);
            System.err.print(check + ";" + Math.abs(check - sixDists[2]) + ";");
            coefsForEquations[i - 1].add(new Point2D.Double(equalitiesCoefs[0], equalitiesCoefs[1]));
        }
        System.err.println();
        System.err.flush();
    }

    public String getPivotPairID(String separator) {
        return p1ID.toString() + separator + p2ID.toString();
    }

}
