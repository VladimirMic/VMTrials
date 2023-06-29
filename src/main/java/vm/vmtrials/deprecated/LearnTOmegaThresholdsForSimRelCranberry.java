///*
// * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
// */
//package vm.fs.main.search.filtering.learning;
//
//import java.util.AbstractMap;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import vm.datatools.Tools;
//import vm.fs.dataset.FSDatasetInstanceSingularizator;
//import vm.fs.store.filtering.FSSecondaryFilteringWithSketchesStorage;
//import vm.fs.store.filtering.FSSimRelThresholdsTOmegaStorage;
//import vm.fs.store.voronoiPartitioning.FSVoronoiPartitioningStorage;
//import vm.metricSpace.AbstractMetricSpace;
//import vm.metricSpace.Dataset;
//import vm.metricSpace.ToolsMetricDomain;
//import vm.metricSpace.distance.bounding.nopivot.impl.SecondaryFilteringWithSketches;
//import vm.metricSpace.distance.bounding.nopivot.learning.LearningSecondaryFilteringWithSketches;
//import vm.metricSpace.distance.bounding.nopivot.storeLearned.SecondaryFilteringWithSketchesStoreInterface;
//import vm.search.impl.SimRelSeqScanKNNCandSet;
//import vm.search.impl.VoronoiPartitionsCandSetIdentifier;
//import vm.simRel.impl.learn.SimRelEuclideanPCAForLearning;
//
///**
// *
// * @author Vlada
// */
//public class LearnTOmegaThresholdsForSimRelCranberry {
//
//    private static final Logger LOG = Logger.getLogger(LearnTOmegaThresholdsForSimRelCranberry.class.getName());
//
//    public static void main(String[] args) {
////        vm.javatools.Tools.sleep(8);
//        // parameter for the Secondary filtering with the sketches
////        float pCum = 0.55f;
//        Dataset[] fullDatasets = new Dataset[]{
//            new FSDatasetInstanceSingularizator.LAION_100M_Dataset()
//        };
//        Dataset[] pcaDatasets = new Dataset[]{
//            new FSDatasetInstanceSingularizator.LAION_100M_PCA96Dataset()
//        };
////        Dataset[] sketchesDatasets = new Dataset[]{
////            new FSDatasetInstanceSingularizator.LAION_100M_GHP_50_512Dataset()
////        };
////
////        float[] distIntervalsForPX = new float[]{
////            0.004f,
////            0.004f,
////            0.004f
////        };
////
//        for (int i = 0; i < fullDatasets.length; i++) {
////            run(fullDatasets[i], pcaDatasets[i], sketchesDatasets[i], pCum, distIntervalsForPX[i]);
//            run(fullDatasets[i], pcaDatasets[i], null, -1, -1);
//        }
//    }
//
//    private static void run(Dataset fullDataset, Dataset<float[]> pcaDataset, Dataset sketchesDataset, float pCum, float distIntervalsForPX) {
////        /* max size of the voronoi answer */
//        int kVoronoi = 100000;
//        /* min size of the simRel answer */
//        int kPCA = 800;
//        /* length of the PCA */
//        int pcaLength = 96;
//        /* number of query objects to learn t(\Omega) thresholds. We use different objects than the pivots tested. */
//        int querySampleCount = 100;//200
//        Integer pivotsCount = 20000;
//
//        FSSimRelThresholdsTOmegaStorage simRelStorage = new FSSimRelThresholdsTOmegaStorage(querySampleCount, pcaLength, kPCA, pivotsCount, kVoronoi);
//        VoronoiPartitionsCandSetIdentifier voronoiAlg = new VoronoiPartitionsCandSetIdentifier(fullDataset, new FSVoronoiPartitioningStorage(), pivotsCount);
//
//        AbstractMetricSpace<float[]> fullDatasetMetricSpace = fullDataset.getMetricSpace();
//        AbstractMetricSpace<float[]> pcaDatasetMetricSpace = pcaDataset.getMetricSpace();
//        List<Object> pcaQueryObjects = pcaDataset.getSampleOfDataset(querySampleCount);
//        Map<Object, Object> fullQuerySamplesMap = fullDataset.getKeyValueStorage();
//
//        List<Object> pcaAllObjects = Tools.getObjectsFromIterator(pcaDataset.getMetricObjectsFromDataset());
//        Map<Object, Object> pcaAllObjectsMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(pcaDatasetMetricSpace, pcaAllObjects, false);
//
//        SimRelEuclideanPCAForLearning simRelLearn = new SimRelEuclideanPCAForLearning(pcaLength);
//
//        SimRelSeqScanKNNCandSet simRelAlg = new SimRelSeqScanKNNCandSet(simRelLearn, kPCA);
//
////        SecondaryFilteringWithSketches sketchFiltering = initSecondaryFilteringWithSketches(fullDataset, sketchesDataset, "", pCum, distIntervalsForPX);
//
//        for (int i = 0; i < pcaQueryObjects.size(); i++) {
//            Object pcaQueryObject = pcaQueryObjects.get(i);
//            simRelLearn.resetCounters(pcaLength);
//            Object queryObjId = fullDatasetMetricSpace.getIDOfMetricObject(pcaQueryObject);
//            Object fullQueryObject = fullQuerySamplesMap.get(queryObjId);
//            fullQueryObject = new AbstractMap.SimpleEntry<>(queryObjId, fullQueryObject);
//            List voronoiCandsIDs = voronoiAlg.candSetKnnSearch(fullDatasetMetricSpace, fullQueryObject, kVoronoi, null);
//
//            List pcaOfCandidates = new ArrayList();
//            for (Object voronoiCandID : voronoiCandsIDs) {
//                Object pcaObj = pcaAllObjectsMap.get(voronoiCandID);
//                pcaOfCandidates.add(pcaObj);
//            }
//
//            simRelAlg.candSetKnnSearch(pcaDataset.getMetricSpace(), pcaQueryObject, kPCA, pcaOfCandidates.iterator());
//            LOG.log(Level.INFO, "Learning tresholds with the query obj {0}, i.e., qID {0}", new Object[]{i + 1, queryObjId});
//            System.gc();
//        }
//
//        float[][] ret = simRelLearn.getDiffWhenWrong(FSSimRelThresholdsTOmegaStorage.PERCENTILES);
//        simRelStorage.store(ret, pcaDataset.getDatasetName());
//    }
//
//}
