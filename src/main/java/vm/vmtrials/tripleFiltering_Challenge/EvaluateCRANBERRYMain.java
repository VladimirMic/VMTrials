/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.tripleFiltering_Challenge;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.dataTransforms.FSGHPSketchesPivotPairsStorageImpl;
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
import vm.objTransforms.objectToSketchTransformators.AbstractObjectToSketchTransformator;
import vm.objTransforms.objectToSketchTransformators.SketchingGHP;
import vm.objTransforms.storeLearned.GHPSketchingPivotPairsStoreInterface;
import vm.queryResults.errorOnDistEvaluation.ErrorOnDistEvaluator;
import vm.queryResults.recallEvaluation.RecallOfCandsSetsEvaluator;
import vm.search.algorithm.impl.VoronoiPartitionsCandSetIdentifier;
import vm.search.algorithm.impl.multiFiltering.CranberryAlgorithm;
import vm.simRel.SimRelInterface;
import vm.simRel.impl.DumbSimRel;
import vm.simRel.impl.SimRelEuclideanPCAImplForTesting;
import vm.simRel.impl.learn.SimRelEuclideanPCAForLearning;

/**
 *
 * @author Vlada
 */
public class EvaluateCRANBERRYMain {

    private static final Logger LOG = Logger.getLogger(EvaluateCRANBERRYMain.class.getName());
    // To learn the simRel, switch here the boolean value and make sure to max 30M dataset due to memory limitations.
    //Then provide full 256 dim PCA dataset, and decrease the paralelisation to 1. 
    //Do not have to care about the parameter QUERY_COUNT_LIMIT
    public static final Boolean LEARN_SIMREL = false;
    public static final Integer QUERY_COUNT_LIMIT = -1;

    public static void main(String[] args) {
        int sketchLength = 512;
        // parameter for the Secondary filtering with the sketches
//        vm.javatools.Tools.sleep(360);
        float pCum = LearningSecondaryFilteringWithSketches.THRESHOLDS_P_CUM[0];
        String tOmegaThresholdsFile = "laion2B-en-clip768v2-n=10M.h5_PCA256_q100voronoiP20000_voronoiK200101_pcaLength256_kPCA750.csv";
        Dataset[] fullDatasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_10M_Dataset(),
            new FSDatasetInstanceSingularizator.LAION_30M_Dataset(),
            new FSDatasetInstanceSingularizator.LAION_100M_Dataset()
        };
        Dataset[] pcaDatasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_10M_PCA256Prefixes24Dataset(),
            new FSDatasetInstanceSingularizator.LAION_30M_PCA256Prefixes24Dataset(),
            new FSDatasetInstanceSingularizator.LAION_100M_PCA256Prefixes24Dataset()
        };

        Dataset[] sketchesDatasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_10M_GHP_50_512Dataset(),
            new FSDatasetInstanceSingularizator.LAION_30M_GHP_50_512Dataset(),
            new FSDatasetInstanceSingularizator.LAION_100M_GHP_50_512Dataset()
        };

        float distIntervalForPX = 0.004f;
        for (int i = 2; i < fullDatasets.length; i++) {
            run(fullDatasets[i], pcaDatasets[i], sketchesDatasets[i], distIntervalForPX, sketchLength, pCum, tOmegaThresholdsFile);
            break;
        }
    }

    private static void run(Dataset fullDataset, Dataset pcaDataset, Dataset sketchesDataset, float distIntervalsForPX, int sketchLength, float pCum, String tOmegaThresholdsFile) {
        /* kNN queries - the result set size */
        int k = 10;
        /*  prefix of the PCA shortened vectors used by the simRel */
        int prefixLength = 24;
        int pivotCountForVoronoi = 20020;
        /*  prefix of the shortened vectors used by the simRel */
        int pcaLength = 256;
        /* number of query objects to learn t(\Omega) thresholds. We use different objects than the queries tested. */
        int querySampleCount = 200;
        /* percentile - defined in the paper. Defines the precision of the simRel */
        float percentile = 0.99f;

        testQueries(fullDataset, pcaDataset, sketchesDataset, percentile, pivotCountForVoronoi, k, prefixLength, pcaLength, sketchLength, pCum, distIntervalsForPX, querySampleCount, tOmegaThresholdsFile);
    }

    private static void testQueries(
            Dataset fullDataset,
            Dataset pcaDataset,
            Dataset sketchesDataset,
            float percentile,
            int pivotCountForVoronoi,
            int k,
            int prefixLength,
            int pcaLength,
            int sketchLength,
            float pCum,
            float distIntervalsForPX,
            int querySampleCount,
            String tOmegaThresholdsFile
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
        AbstractObjectToSketchTransformator sketchingTechnique = new SketchingGHP(fullDataset.getDistanceFunction(), fullDataset.getMetricSpace(), pivots, fullDataset.getDatasetName(), 0.5f, sketchLength, storageOfPivotPairs);

        // filtering algorithms and filters
        VoronoiPartitionsCandSetIdentifier algVoronoi = new VoronoiPartitionsCandSetIdentifier(fullDataset, new FSVoronoiPartitioningStorage(), pivotCountForVoronoi);

        SecondaryFilteringWithSketches sketchFiltering = SISAPChallengeAlgBuilder.initSecondaryFilteringWithSketches(fullDataset, sketchesDataset, "", pCum, distIntervalsForPX);

        int datasetSize = sketchFiltering.getNumberOfSketches();

        int voronoiK = getVoronoiK(datasetSize);
        int simRelMinAnswerSize = getMinSimRelAnswerSize(datasetSize);
        SimRelInterface<float[]> simRel = initSimRel(querySampleCount, pcaLength, simRelMinAnswerSize, voronoiK, pcaDataset.getDatasetName(), percentile, prefixLength, pivotCountForVoronoi, tOmegaThresholdsFile);

        Map pcaOMap;
        if (simRel instanceof DumbSimRel) {
            pcaOMap = new HashMap<>();
        } else {
            pcaOMap = getMapOfPrefixes(pcaDatasetMetricSpace, pcaDataset.getMetricObjectsFromDataset(), prefixLength);
        }
        // key value map to PCA of the query objects
        Map pcaQMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(pcaDatasetMetricSpace, pcaDataset.getMetricQueryObjects(), false);

        CranberryAlgorithm alg = new CranberryAlgorithm(
                algVoronoi,
                voronoiK,
                sketchFiltering,
                sketchingTechnique,
                sketchesDataset.getMetricSpace(),
                simRel,
                simRelMinAnswerSize,
                pcaOMap,
                fullDataset.getKeyValueStorage(),
                datasetSize,
                fullDataset.getDistanceFunction()
        );

        String resultName = "CRANBERRY_CHALLENGE_10MtOmega01_PAR_" + CranberryAlgorithm.QUERIES_PARALELISM + "_" + alg.getMaxDistComps() + "maxDists_" + fullDataset.getDatasetName() + "_kVoronoi" + voronoiK + "_pca" + pcaLength + "_simRelMinAns" + simRelMinAnswerSize + "_prefix" + prefixLength + "_learntOmegaOn_" + querySampleCount + "q__k" + k + "_perc" + percentile + "_pCum" + pCum + "_sketches" + sketchLength + "";
        System.gc();
        vm.javatools.Tools.sleepSeconds(5);
        int queryCount = LEARN_SIMREL ? querySampleCount : QUERY_COUNT_LIMIT;
        long overallTime = -System.currentTimeMillis();
        TreeSet[] results = alg.completeKnnSearchOfQuerySet(fullMetricSpace, fullQueries, k, null, pcaDatasetMetricSpace, pcaQMap, queryCount);
        overallTime += System.currentTimeMillis();

        if (LEARN_SIMREL) {
            float[][] ret = ((SimRelEuclideanPCAForLearning) simRel).getDiffWhenWrong(FSSimRelThresholdsTOmegaStorage.PERCENTILES);
            FSSimRelThresholdsTOmegaStorage simRelStorage = new FSSimRelThresholdsTOmegaStorage(querySampleCount, pcaLength, simRelMinAnswerSize, pivotCountForVoronoi, voronoiK);
            simRelStorage.store(ret, pcaDataset.getDatasetName());
            sketchFiltering.shutdownThreadPool();
            return;
        }

        LOG.log(Level.INFO, "Storing statistics of queries");
        FSQueryExecutionStatsStoreImpl statsStorage = new FSQueryExecutionStatsStoreImpl(fullDataset.getDatasetName(), fullDataset.getQuerySetName(), k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), resultName, null);
        statsStorage.storeStatsForQueries(alg.getDistCompsPerQueries(), alg.getTimesPerQueries(), alg.getSimRelsPerQueries());
        statsStorage.save();

        LOG.log(Level.INFO, "Storing results of queries");
        FSNearestNeighboursStorageImpl resultsStorage = new FSNearestNeighboursStorageImpl();
        resultsStorage.storeQueryResults(fullMetricSpace, fullQueries, results, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), resultName);

        LOG.log(Level.INFO, "Evaluating accuracy of queries");
        FSRecallOfCandidateSetsStorageImpl recallStorage = new FSRecallOfCandidateSetsStorageImpl(fullDataset.getDatasetName(), fullDataset.getQuerySetName(), k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), resultName, null);
        RecallOfCandsSetsEvaluator evaluator = new RecallOfCandsSetsEvaluator(new FSNearestNeighboursStorageImpl(), recallStorage);
        evaluator.evaluateAndStoreRecallsOfQueries(fullDataset.getDatasetName(), fullDataset.getQuerySetName(), k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), resultName, k);
        recallStorage.save();
        LOG.log(Level.INFO, "Evaluating error on distance");
        ErrorOnDistEvaluator eodEvaluator = new ErrorOnDistEvaluator(resultsStorage, recallStorage);
        eodEvaluator.evaluateAndStoreErrorsOnDist(fullDataset.getDatasetName(), fullDataset.getQuerySetName(), k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), resultName);
        recallStorage.save();

        LOG.log(Level.INFO, "Overall time: {0}", overallTime);
        sketchFiltering.shutdownThreadPool();
    }

    public static Map<Object, Object> getMapOfPrefixes(AbstractMetricSpace<float[]> metricSpace, Iterator metricObjectsFromDataset, int prefixLength) {
        Map<Object, Object> ret = new HashMap<>();
        int counter = 0;
        boolean add = false;
        while (metricObjectsFromDataset.hasNext()) {
            List<Object> batch = Tools.getObjectsFromIterator(metricObjectsFromDataset, 10000000);
            counter += batch.size();
            for (Object next : batch) {
                Object id = metricSpace.getIDOfMetricObject(next);
                float[] vector = metricSpace.getDataOfMetricObject(next);
                if (add || vector.length == prefixLength || LEARN_SIMREL) {
                    ret.put(id, vector);
                    add = true;
                } else {
                    float[] shortVec = new float[prefixLength];
                    System.arraycopy(vector, 0, shortVec, 0, prefixLength);
                    ret.put(id, shortVec);
                }
            }
            LOG.log(Level.INFO, "Loaded {0} prefixes", counter);
        }
        return ret;
    }

    public static SimRelInterface<float[]> initSimRel(int querySampleCount, int pcaLength, int kPCA, int dataSampleCount, String pcaDatasetName, float percentile, int prefixLength, Integer pivotsCount) {
        return initSimRel(querySampleCount, pcaLength, kPCA, dataSampleCount, pcaDatasetName, percentile, prefixLength, pivotsCount, null);
    }

    public static SimRelInterface<float[]> initSimRel(int querySampleCount, int pcaLength, int kPCA, int dataSampleCount, String pcaDatasetName, float percentile, int prefixLength, Integer pivotsCount, String directFileNameString) {
//        return new DumbSimRel<>();
        if (LEARN_SIMREL) {
            SimRelEuclideanPCAForLearning simRelLearn = new SimRelEuclideanPCAForLearning(pcaLength);
            return simRelLearn;
        }
        FSSimRelThresholdsTOmegaStorage simRelStorage = new FSSimRelThresholdsTOmegaStorage(directFileNameString);
        float[][] simRelThresholds = simRelStorage.load(pcaDatasetName);
        int idx = FSSimRelThresholdsTOmegaStorage.percentileToArrayIdx(percentile);
        float[] learnedErrors = simRelThresholds[idx];
        for (int i = learnedErrors.length - 1; i >= 0; i--) {
            if (learnedErrors[i] == 0f) {
                learnedErrors[i] = Float.MAX_VALUE;
            } else {
                break;
            }
        }
        return new SimRelEuclideanPCAImplForTesting(learnedErrors, prefixLength);
    }

    /**
     * *************************************************
     * Init params for datasets given by their size ****
     * *************************************************
     */
    public static final int getMinSimRelAnswerSize(int datasetSize) {
        if (LEARN_SIMREL) {
            return 300;
        }
        if (datasetSize <= 300000) {
            return 1000;
        }
        return 100;
//        int smallSize = 10109960;
//        int midSize = 30369256;
//        int bigSize = 102041055;
//        if (datasetSize <= midSize) {
//            double deltaVoronoiK = -2300;
//            int deltaDatasetSize = midSize - smallSize;
//            double derivative = deltaVoronoiK / deltaDatasetSize;
//            return (int) (derivative * (datasetSize - midSize) + 100);
//        }
//        if (datasetSize > midSize) {
//            double deltaMinSimRelAnswer = -100;
//            int deltaDatasetSize = bigSize - midSize;
//            double derivative = deltaMinSimRelAnswer / deltaDatasetSize;
//            return (int) (derivative * (datasetSize - bigSize) + 100);
//        }
//        throw new Error();
    }

    public static final int getPivotCountForVoronoi(int datasetSize) {
        if (datasetSize <= 300000) {
            return 2000;
        }
        return 20020;
    }

    public static final int getVoronoiK(int datasetSize) {
        return 550000;
//        if (datasetSize <= 300000) {
//            return (int) (0.3f * datasetSize);
//        }
//        int smallSize = 10109960;
//        int midSize = 30369256;
//        int bigSize = 102041055;
//        if (datasetSize <= midSize) {
//            double deltaVoronoiK = 200000;
//            int deltaDatasetSize = midSize - smallSize;
//            double derivative = deltaVoronoiK / deltaDatasetSize;
//            int ret = (int) (derivative * (datasetSize - midSize) + 400000);
//            return (int) Math.min(ret, 0.9f * datasetSize);
//        }
//        if (datasetSize > midSize) {
//            double deltaVoronoiK = 600000;
//            int deltaDatasetSize = bigSize - midSize;
//            double derivative = deltaVoronoiK / deltaDatasetSize;
//            return (int) (derivative * (datasetSize - bigSize) + 1000000);
//        }
//        throw new Error();
    }

}
