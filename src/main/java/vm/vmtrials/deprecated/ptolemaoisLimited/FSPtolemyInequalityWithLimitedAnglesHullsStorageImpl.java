//package vm.vmtrials.deprecated.ptolemaoisLimited;
//
//import java.awt.geom.Point2D;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.PrintStream;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import vm.fs.FSGlobal;
//import vm.fs.store.auxiliaryForDistBounding.FSTriangleInequalityWithLimitedAnglesCoefsStorageImpl;
//import vm.vmtrials.deprecated.ptolemaoisLimited.hulls.PtolemaiosFilteringWithLimitedAnglesOrigProposal;
//import vm.structures.ConvexHull2DEuclid;
//import vm.metricSpace.distance.bounding.twopivots.storeLearned.PtolemyInequalityWithLimitedAnglesHullsStoreInterface;
//
///**
// *
// * @author Vlada
// */
//public class FSPtolemyInequalityWithLimitedAnglesHullsStorageImpl implements PtolemyInequalityWithLimitedAnglesHullsStoreInterface {
//
//    public final Logger LOG = Logger.getLogger(FSPtolemyInequalityWithLimitedAnglesHullsStorageImpl.class.getName());
//
//    public File getFile(String resultName) {
//        File folderFile = new File(FSGlobal.AUXILIARY_FOR_PTOLEMAIOS_WITH_LIMITED_ANGLES);
//        folderFile.mkdirs();
//        File ret = new File(folderFile, resultName);
//        if (ret.exists()) {
//            Logger.getLogger(FSTriangleInequalityWithLimitedAnglesCoefsStorageImpl.class.getName()).log(Level.WARNING, "The file already existed");
//        }
//        LOG.log(Level.INFO, "File path: {0}", ret.getAbsolutePath());
//        return ret;
//    }
//
//    @Override
//    public String getResultDescription(String datasetName, int numberOfTetrahedrons, int pivotPairs, float ratioOfSmallestDists) {
//        String ret = datasetName + "__tetrahedrons_" + numberOfTetrahedrons + "__ratio_of_outliers_to_cut_" + ratioOfSmallestDists + "__pivot_pairs_" + pivotPairs + ".csv";
//        LOG.log(Level.INFO, "File name: {0}", ret);
//        return ret;
//    }
//
//    public PtolemaiosFilteringWithLimitedAnglesOrigProposal loadFromFile(String resultPreffixName, String resultName) {
//        File file = getFile(resultName);
//        Map<String, List<Point2D.Double>> hulls = ConvexHull2DEuclid.parsePivotsHulls(file.getAbsolutePath(), true);
//        return new PtolemaiosFilteringWithLimitedAnglesOrigProposal(resultPreffixName, hulls);
//    }
//
//    @Override
//    public void storeHull(String resultName, String hullID, ConvexHull2DEuclid hullsForPivotPair) {
//        try {
//            File resultFile = getFile(resultName);
//            PrintStream err = System.err;
//            System.setErr(new PrintStream(new FileOutputStream(resultFile, true)));
//            System.err.print(hullID + ";");
//            System.err.println(hullsForPivotPair.toString());
//            System.setErr(err);
//            System.setOut(new PrintStream(new FileOutputStream(resultFile.getAbsolutePath() + "_redable.csv", true)));
//            System.out.print(hullID);
//            System.out.println("");
//            hullsForPivotPair.printAsCoordinatesInColumns();
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(FSPtolemyInequalityWithLimitedAnglesHullsStorageImpl.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//
//    public static PtolemaiosFilteringWithLimitedAnglesOrigProposal getLearnedInstance(String resultPreffixName, String datasetName) {
//        FSPtolemyInequalityWithLimitedAnglesHullsStorageImpl storage = new FSPtolemyInequalityWithLimitedAnglesHullsStorageImpl();
//        String fileName = storage.getResultName(datasetName);
//        return storage.loadFromFile(resultPreffixName, fileName);
//    }
//
//    public String getResultName(String datasetName) {
//        return getResultDescription(datasetName, 100000, 128, 0.01f);
//    }
//
//}
