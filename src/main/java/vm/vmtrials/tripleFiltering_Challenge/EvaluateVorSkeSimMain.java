/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.tripleFiltering_Challenge;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.dataTransforms.FSGHPSketchesPivotPairsStorageImpl;
import vm.fs.store.filtering.FSSecondaryFilteringWithSketchesStorage;
import vm.fs.store.filtering.FSSimRelThresholdsTOmegaStorage;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.fs.store.queryResults.FSQueryExecutionStatsStoreImpl;
import vm.fs.store.queryResults.recallEvaluation.FSRecallOfCandidateSetsStorageImpl;
import vm.fs.store.voronoiPartitioning.FSVoronoiPartitioningStorage;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.bounding.nopivot.impl.SecondaryFilteringWithSketches;
import vm.metricSpace.distance.bounding.nopivot.learning.LearningSecondaryFilteringWithSketches;
import vm.metricSpace.distance.bounding.nopivot.storeLearned.SecondaryFilteringWithSketchesStoreInterface;
import vm.objTransforms.objectToSketchTransformators.AbstractObjectToSketchTransformator;
import vm.objTransforms.objectToSketchTransformators.SketchingGHP;
import vm.objTransforms.storeLearned.GHPSketchingPivotPairsStoreInterface;
import vm.queryResults.recallEvaluation.RecallOfCandsSetsEvaluator;
import vm.search.SearchingAlgorithm;
import vm.search.devel.CheckingOfNearestNeighbours;
import vm.search.impl.VoronoiPartitionsCandSetIdentifier;
import vm.search.impl.multiFiltering.VorSkeSimSorting;
import vm.simRel.impl.SimRelEuclideanPCAImpl;
import vm.simRel.impl.SimRelEuclideanPCAImplForTesting;

/**
 *
 * @author Vlada
 */
public class EvaluateVorSkeSimMain {

    private static final Logger LOG = Logger.getLogger(EvaluateVorSkeSimMain.class.getName());
    public static final Boolean STORE_RESULTS = true;

    public static void main(String[] args) {
        int sketchLength = 256;
        // parameter for the Secondary filtering with the sketches
        float pCum = 0.5f;
        Dataset[] fullDatasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_10M_Dataset(),
            new FSDatasetInstanceSingularizator.LAION_30M_Dataset(),
            new FSDatasetInstanceSingularizator.LAION_100M_Dataset()
        };
        Dataset[] pcaDatasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_10M_PCA96Dataset(),
            new FSDatasetInstanceSingularizator.LAION_30M_PCA96Dataset(),
            new FSDatasetInstanceSingularizator.LAION_100M_PCA96Dataset()
        };

        Dataset[] sketchesDatasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_10M_GHP_50_256Dataset(),
            new FSDatasetInstanceSingularizator.LAION_30M_GHP_50_256Dataset(),
            new FSDatasetInstanceSingularizator.LAION_100M_GHP_50_256Dataset()
        };

        int[] voronoiK = new int[]{
            400000,
            1000000,
            3000000
        };

        int[] minKSimRel = new int[]{
            300,
            500,
            500
        };
        int[] maxKSimRel = new int[]{
            100000,
            100000,
            100000
        };
        float[] distIntervalsForPX = new float[]{
            0.004f,
            0.004f,
            0.004f
        };

        for (int i = 2; i < fullDatasets.length; i++) {
            run(fullDatasets[i], pcaDatasets[i], sketchesDatasets[i], voronoiK[i], minKSimRel[i], maxKSimRel[i], distIntervalsForPX[i], sketchLength, pCum);
            System.exit(0);
        }
    }

    private static void run(Dataset fullDataset, Dataset pcaDataset, Dataset sketchesDataset, int kVoronoi, int simRelMinAnswerSize, int simRelMaxAnswerSize, float distIntervalsForPX, int sketchLength, float pCum) {
        /* kNN queries - the result set size */
        int k = 10;
        /*  prefix of the shortened vectors used by the simRel */
        int prefixLength = 24;
        /*  prefix of the shortened vectors used by the simRel */
        int pcaLength = 96;
        /* number of query objects to learn t(\Omega) thresholds. We use different objects than the queries tested. */
        int querySampleCount = 100;
        /* size of the data sample to learn t(\Omega) thresholds: SISAP: 100 000 */
        int dataSampleCount = 100000;
        /* percentile - defined in the paper. Defines the precision of the simRel */
        float percentile = 0.9f;

        SimRelEuclideanPCAImpl simRel = initSimRel(querySampleCount, pcaLength, simRelMinAnswerSize, dataSampleCount, pcaDataset.getDatasetName(), percentile, prefixLength);
        String resultName = "VorskesimSorting_" + fullDataset.getDatasetName() + "_kVoronoi" + kVoronoi + "_simRelMinAnswerSize" + simRelMinAnswerSize + "simRelMaxAnswerSize" + simRelMaxAnswerSize + "_prefix" + prefixLength + "_learntOmegaOn_" + querySampleCount + "q__" + dataSampleCount + "o__k" + k + "_perc" + percentile + "_pCum" + pCum + "_sketchLength" + sketchLength;

        testQueries(fullDataset, pcaDataset, sketchesDataset, simRel, kVoronoi, simRelMinAnswerSize, simRelMaxAnswerSize, k, prefixLength, resultName, sketchLength, pCum, distIntervalsForPX);
    }

    private static void testQueries(
            Dataset fullDataset,
            Dataset pcaDataset,
            Dataset sketchesDataset,
            SimRelEuclideanPCAImpl simRel,
            int voronoiK,
            int simRelMinAnswerSize,
            int simRelMaxAnswerSize,
            int k,
            int prefixLength,
            String resultName,
            int sketchLength,
            float pCum,
            float distIntervalsForPX
    ) {

        //queries
        List<Object> fullQueries = fullDataset.getMetricQueryObjects();

        //original metric space objects
        AbstractMetricSpace fullMetricSpace = fullDataset.getMetricSpace();
        List pivots = fullDataset.getPivots(-1);

        // pca space for the simRel
        AbstractMetricSpace pcaDatasetMetricSpace = pcaDataset.getMetricSpace();

        // sketching technique to transform query object to sketch
        GHPSketchingPivotPairsStoreInterface storageOfPivotPairs = new FSGHPSketchesPivotPairsStorageImpl();
        AbstractObjectToSketchTransformator sketchingTechnique = new SketchingGHP(fullDataset.getDistanceFunction(), fullDataset.getMetricSpace(), pivots, true, fullDataset.getDatasetName(), 0.5f, sketchLength, storageOfPivotPairs);

        // filtering algorithms and filters
        VoronoiPartitionsCandSetIdentifier algVoronoi = new VoronoiPartitionsCandSetIdentifier(fullDataset, new FSVoronoiPartitioningStorage(), 2048);

        String resultNamePrefix = "Voronoi" + voronoiK + "_pCum" + pCum;
        SecondaryFilteringWithSketches sketchFiltering = initSecondaryFilteringWithSketches(fullDataset, sketchesDataset, resultNamePrefix, pCum, distIntervalsForPX);

        Map pcaOMap = getMapOfPrefixes(pcaDatasetMetricSpace, pcaDataset.getMetricObjectsFromDataset(), prefixLength);
        // key value map to PCA of the query objects
        Map pcaQMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(pcaDatasetMetricSpace, pcaDataset.getMetricQueryObjects(), false);

        SearchingAlgorithm alg = new VorSkeSimSorting(
                algVoronoi,
                voronoiK,
                sketchFiltering,
                sketchingTechnique,
                sketchesDataset.getMetricSpace(),
                simRel,
                simRelMinAnswerSize,
                simRelMaxAnswerSize,
                pcaOMap,
                fullDataset.getKeyValueStorage(),
                fullDataset.getDistanceFunction());

        TreeSet[] results = new TreeSet[fullQueries.size()];

        CheckingOfNearestNeighbours DEVEL = new CheckingOfNearestNeighbours(new FSNearestNeighboursStorageImpl(), fullDataset.getDatasetName(), fullDataset.getQuerySetName());
//339
        for (int i = 339; i < fullQueries.size(); i++) {
            Object query = fullQueries.get(i);
            Object qId = fullMetricSpace.getIDOfMetricObject(query);
            Object pcaQData = pcaQMap.get(qId);
            Set<String> ANSWER = DEVEL.getIDsOfNNForQuery(qId.toString(), k);
//            Set<String> ANSWER = null;
            results[i] = alg.completeKnnSearch(fullMetricSpace, query, k, null, pcaDatasetMetricSpace, pcaQData, ANSWER);

//            if (i == 499) {
//                break;
//            }
            break;
        }
        LOG.log(Level.INFO, "Storing statistics of queries");
        FSQueryExecutionStatsStoreImpl statsStorage = new FSQueryExecutionStatsStoreImpl(fullDataset.getDatasetName(), fullDataset.getQuerySetName(), k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), resultName, null);
        statsStorage.storeStatsForQueries(alg.getDistCompsPerQueries(), alg.getTimesPerQueries());
        statsStorage.saveFile();

        LOG.log(Level.INFO, "Storing results of queries");
        FSNearestNeighboursStorageImpl resultsStorage = new FSNearestNeighboursStorageImpl();
        resultsStorage.storeQueryResults(fullMetricSpace, fullQueries, results, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), resultName);

        LOG.log(Level.INFO, "Evaluating accuracy of queries");
        FSRecallOfCandidateSetsStorageImpl recallStorage = new FSRecallOfCandidateSetsStorageImpl(fullDataset.getDatasetName(), fullDataset.getQuerySetName(), k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), resultName, null);
        RecallOfCandsSetsEvaluator evaluator = new RecallOfCandsSetsEvaluator(new FSNearestNeighboursStorageImpl(), recallStorage);
        evaluator.evaluateAndStoreRecallsOfQueries(fullDataset.getDatasetName(), fullDataset.getQuerySetName(), k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), resultName, k);
        recallStorage.saveFile();

    }

    public static Map<Object, Object> getMapOfPrefixes(AbstractMetricSpace<float[]> metricSpace, Iterator metricObjectsFromDataset, int prefixLength) {
        Map<Object, Object> ret = new HashMap<>();
        for (int i = 1; metricObjectsFromDataset.hasNext(); i++) {
            Object next = metricObjectsFromDataset.next();
            Object id = metricSpace.getIDOfMetricObject(next);
            float[] vector = metricSpace.getDataOfMetricObject(next);
            float[] shortVec = new float[prefixLength];
            System.arraycopy(vector, 0, shortVec, 0, prefixLength);
//            AbstractMap.SimpleEntry obj = new AbstractMap.SimpleEntry(id, shortVec);
//            ret.put(id, obj);
            ret.put(id, shortVec);
            if (i % 500000 == 0) {
                LOG.log(Level.INFO, "Loaded {0} prefixes", i);
            }
        }
        return ret;
    }

    public static SimRelEuclideanPCAImpl initSimRel(int querySampleCount, int pcaLength, int kPCA, int dataSampleCount, String pcaDatasetName, float percentile, int prefixLength) {
        FSSimRelThresholdsTOmegaStorage simRelStorage = new FSSimRelThresholdsTOmegaStorage(querySampleCount, pcaLength, kPCA, dataSampleCount);
        float[][] simRelThresholds = simRelStorage.load(pcaDatasetName);
        int idx = FSSimRelThresholdsTOmegaStorage.percentileToArrayIdx(percentile);
        float[] learnedErrors = simRelThresholds[idx];
        // TEST QUERIES
        return new SimRelEuclideanPCAImplForTesting(learnedErrors, prefixLength);
    }

    public static SecondaryFilteringWithSketches initSecondaryFilteringWithSketches(Dataset fullDataset, Dataset sketchesDataset, String filterNamePrefix, float pCum, float distIntervalForPX) {
        SecondaryFilteringWithSketchesStoreInterface secondaryFilteringStorage = new FSSecondaryFilteringWithSketchesStorage();
        return new SecondaryFilteringWithSketches(
                filterNamePrefix,
                fullDataset.getDatasetName(),
                sketchesDataset,
                secondaryFilteringStorage,
                pCum,
                LearningSecondaryFilteringWithSketches.SKETCHES_SAMPLE_COUNT_FOR_IDIM_PX,
                LearningSecondaryFilteringWithSketches.DISTS_COMPS_FOR_SK_IDIM_AND_PX,
                distIntervalForPX);
    }

}
