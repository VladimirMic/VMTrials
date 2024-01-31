package vm.vmtrials.deprecated.ptolemaoisLimited.hulls;

import vm.metricSpace.distance.bounding.twopivots.AbstractTwoPivotsFilter;

/**
 *
 * @author Vlada
 */
public class PtolemaiosFilteringWithLimitedAnglesOrigProposal { //extends AbstractTwoPivotsFilter {

//    private final Map<String, List<Point2D.Double>> hullsForPivotPairs;
//    private static final Logger LOGGER = Logger.getLogger(PtolemaiosFilteringWithLimitedAnglesOrigProposal.class.getName());
//
//    public PtolemaiosFilteringWithLimitedAnglesOrigProposal(String namePrefix, Map<String, List<Point2D.Double>> hullsForPivotPairs) {
//        super(namePrefix);
//        this.hullsForPivotPairs = hullsForPivotPairs;
//    }
//
//    @Override
//    public float lowerBound(float distP1P2, float distP2O, float distQP1, float distP1O, float distP2Q, int p1ID, int p2ID, Float range) {
//        return returnBound(distP1P2, distP2O, distQP1, distP1O, distP2Q, p1ID, p2ID, true);
//    }
//
//    @Override
//    public float upperBound(float distP1P2, float distP2O, float distQP1, float distP1O, float distP2Q, int p1ID, int p2ID, Float range) {
//        return returnBound(distP1P2, distP2O, distQP1, distP1O, distP2Q, p1ID, p2ID, false);
//    }
//
//    private float returnBound(float distP1P2, float distP2O, float distQP1, float distP1O, float distP2Q, int p1ID, int p2ID, boolean lower) {
//        float ret = lower ? 0 : Float.MAX_VALUE;
//        for (int i = 1; i <= 3; i++) {
//            List<Point2D.Double> hull = getHull(p1ID, p2ID, i);
//            if (hull == null) {
//                LOGGER.log(Level.WARNING, "No hull (" + p1ID + "-" + p2ID + ": Eq.:" + i + ") given. Return trivial bound.");
//                continue;
//            }
//            float bdOvera = distP2O * distQP1 / distP1P2;
//            float efOvera = distP1O * distP2Q / distP1P2;
//            if (bdOvera > efOvera) {
//                float tmp = bdOvera;
//                bdOvera = efOvera;
//                efOvera = tmp;
//            }
//            for (Point2D.Double point : hull) {
//                float estimate = (float) (bdOvera * point.x + efOvera * point.y);
//                if ((lower && estimate > ret) || (!lower && estimate > ret)) {
//                    ret = estimate;
//                }
//            }
//        }
//        return ret;
//    }
//
//    private List<Point2D.Double> getHull(String p1ID, String p2ID, int equationNumber) {
//        if (hullsForPivotPairs.containsKey(p1ID + "-" + p2ID + ": Eq.:" + equationNumber)) {
//            return hullsForPivotPairs.get(p1ID + "-" + p2ID + ": Eq.:" + equationNumber);
//        }
//        return hullsForPivotPairs.get(p2ID + "-" + p1ID + ": Eq.:" + equationNumber);
//    }
//
//    @Override
//    protected String getTechName() {
//        return "ptolemaios_limited_angles_orig";
//    }
//
}
