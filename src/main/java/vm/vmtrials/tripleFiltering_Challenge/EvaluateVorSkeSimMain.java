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
import vm.datatools.DataTypeConvertor;
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
import vm.objTransforms.objectToSketchTransformators.AbstractObjectToSketchTransformator;
import vm.objTransforms.objectToSketchTransformators.SketchingGHP;
import vm.objTransforms.storeLearned.GHPSketchingPivotPairsStoreInterface;
import vm.queryResults.recallEvaluation.RecallOfCandsSetsEvaluator;
import vm.search.impl.VoronoiPartitionsCandSetIdentifier;
import vm.search.impl.multiFiltering.VorSkeSimSorting;
import vm.simRel.SimRelInterface;
import vm.simRel.impl.SimRelEuclideanPCAImplForTesting;
import vm.simRel.impl.learn.SimRelEuclideanPCAForLearning;

/**
 *
 * @author Vlada
 */
public class EvaluateVorSkeSimMain {

    private static final Logger LOG = Logger.getLogger(EvaluateVorSkeSimMain.class.getName());
    public static final Boolean STORE_RESULTS = true;
    public static final Boolean LEARN_SIMREL = false;

    public static void main(String[] args) {
//        vm.javatools.Tools.sleep(120);
        int sketchLength = 512;
        // parameter for the Secondary filtering with the sketches
        float pCum = 0.5f;
        Dataset[] fullDatasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_10M_Dataset(),
            new FSDatasetInstanceSingularizator.LAION_30M_Dataset(),
            new FSDatasetInstanceSingularizator.LAION_100M_Dataset()
        };
        Dataset[] pcaDatasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_10M_PCA256Dataset(),
            new FSDatasetInstanceSingularizator.LAION_30M_PCA256Dataset(),
            new FSDatasetInstanceSingularizator.LAION_100M_PCA96Dataset()
        };

        Dataset[] sketchesDatasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_10M_GHP_50_512Dataset(),
            new FSDatasetInstanceSingularizator.LAION_30M_GHP_50_512Dataset(),
            new FSDatasetInstanceSingularizator.LAION_100M_GHP_50_512Dataset()
        };

        int[] voronoiK = new int[]{
            500000,
            900000,
            1000000
        };

        int[] minKSimRel = new int[]{
            100,
            100,
            1000
        };
        int[] maxKSimRel = new int[]{
            1000000,
            1000000,
            1000000
        };
        float[] distIntervalsForPX = new float[]{
            0.004f,
            0.004f,
            0.004f
        };

        for (int i = 2; i < fullDatasets.length; i++) {
            run(fullDatasets[i], pcaDatasets[i], sketchesDatasets[i], voronoiK[i], minKSimRel[i], maxKSimRel[i], distIntervalsForPX[i], sketchLength, pCum);
            break;
        }
    }

    private static void run(Dataset fullDataset, Dataset pcaDataset, Dataset sketchesDataset, int kVoronoi, int simRelMinAnswerSize, int simRelMaxAnswerSize, float distIntervalsForPX, int sketchLength, float pCum) {
        /* kNN queries - the result set size */
        int k = 10;
        /*  prefix of the PCA shortened vectors used by the simRel */
        int prefixLength = 24;
        int pivotCountForVoronoi = 20000;
        /*  prefix of the shortened vectors used by the simRel */
        int pcaLength = 96;
        /* number of query objects to learn t(\Omega) thresholds. We use different objects than the queries tested. */
        int querySampleCount = 100;
        /* size of the data sample to learn t(\Omega) thresholds: SISAP: 100 000 */
        int dataSampleCount = kVoronoi;
        /* percentile - defined in the paper. Defines the precision of the simRel */
        float percentile = 0.9f;

        SimRelInterface<float[]> simRel = initSimRel(querySampleCount, pcaLength, simRelMinAnswerSize, dataSampleCount, pcaDataset.getDatasetName(), percentile, prefixLength, pivotCountForVoronoi);
        String resultName = "CRANBERRY4_" + fullDataset.getDatasetName() + "_kVoronoi" + kVoronoi + "_pca" + pcaLength + "_simRelMinAns" + simRelMinAnswerSize + "simRelMaxAns" + simRelMaxAnswerSize + "_prefix" + prefixLength + "_learntOmegaOn_" + querySampleCount + "q__" + dataSampleCount + "o__k" + k + "_perc" + percentile + "_pCum" + pCum + "_sketches" + sketchLength + "";

        testQueries(fullDataset, pcaDataset, sketchesDataset, simRel, pivotCountForVoronoi, kVoronoi, simRelMinAnswerSize, simRelMaxAnswerSize, k, prefixLength, pcaLength, resultName, sketchLength, pCum, distIntervalsForPX, querySampleCount);
    }

    private static void testQueries(
            Dataset fullDataset,
            Dataset pcaDataset,
            Dataset sketchesDataset,
            SimRelInterface<float[]> simRel,
            int pivotCountForVoronoi,
            int voronoiK,
            int simRelMinAnswerSize,
            int simRelMaxAnswerSize,
            int k,
            int prefixLength,
            int pcaLength,
            String resultName,
            int sketchLength,
            float pCum,
            float distIntervalsForPX,
            int querySampleCount
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

        String resultNamePrefix = "Voronoi" + voronoiK + "_pCum" + pCum;
        SecondaryFilteringWithSketches sketchFiltering = SISAPChallengeEvaluator.initSecondaryFilteringWithSketches(fullDataset, sketchesDataset, resultNamePrefix, pCum, distIntervalsForPX);

        Map pcaOMap = getMapOfPrefixes(pcaDatasetMetricSpace, pcaDataset.getMetricObjectsFromDataset(), prefixLength);
        // key value map to PCA of the query objects
        Map pcaQMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(pcaDatasetMetricSpace, pcaDataset.getMetricQueryObjects(), false);

        VorSkeSimSorting alg = new VorSkeSimSorting(
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

//        CheckingOfNearestNeighbours DEVEL = new CheckingOfNearestNeighbours(new FSNearestNeighboursStorageImpl(), fullDataset.getDatasetName(), fullDataset.getQuerySetName());

        FSQueryExecutionStatsStoreImpl statsStorage = new FSQueryExecutionStatsStoreImpl(fullDataset.getDatasetName(), fullDataset.getQuerySetName(), k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), resultName, null);

        for (int i = 0; i < fullQueries.size(); i++) {
            Object query = fullQueries.get(i);
            Object qId = fullMetricSpace.getIDOfMetricObject(query);
            Object pcaQData = pcaQMap.get(qId);
//            Set<String> ANSWER = DEVEL.getIDsOfNNForQuery(qId.toString(), k);
            Set<String> ANSWER = null;
            results[i] = alg.completeKnnSearch(fullMetricSpace, query, k, null, pcaDatasetMetricSpace, pcaQData, ANSWER);

            long[] earlyStopsPerCoords = (long[]) alg.getSimRelStatsOfLastExecutedQuery();
            String earlyStopsPerCoordsString = DataTypeConvertor.longToString(earlyStopsPerCoords, ",");
            if (STORE_RESULTS && !LEARN_SIMREL) {
                statsStorage.storeStatsForQuery(qId, alg.getDistCompsForQuery(qId), alg.getTimeOfQuery(qId), earlyStopsPerCoordsString);
            } else {
                System.out.println(earlyStopsPerCoordsString);
            }
            LOG.log(Level.INFO, "Processed query {0}", new Object[]{i + 1});

            if (i == 499) {
                break;
            }
            if (LEARN_SIMREL && i == querySampleCount - 1) {
                float[][] ret = ((SimRelEuclideanPCAForLearning) simRel).getDiffWhenWrong(FSSimRelThresholdsTOmegaStorage.PERCENTILES);
                FSSimRelThresholdsTOmegaStorage simRelStorage = new FSSimRelThresholdsTOmegaStorage(querySampleCount, pcaLength, simRelMinAnswerSize, pivotCountForVoronoi, voronoiK);
                simRelStorage.store(ret, pcaDataset.getDatasetName());
                return;
            }
        }
//        LOG.log(Level.INFO, "Storing statistics of queries");
//        statsStorage.storeStatsForQueries(alg.getDistCompsPerQueries(), alg.getTimesPerQueries());
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

        int counter = 0;
        boolean add = false;
        while (metricObjectsFromDataset.hasNext()) {
            List<Object> batch = Tools.getObjectsFromIterator(metricObjectsFromDataset, 1000000);
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
        if (LEARN_SIMREL) {
            SimRelEuclideanPCAForLearning simRelLearn = new SimRelEuclideanPCAForLearning(pcaLength);
            return simRelLearn;
        }
        FSSimRelThresholdsTOmegaStorage simRelStorage = new FSSimRelThresholdsTOmegaStorage(querySampleCount, pcaLength, kPCA, pivotsCount, dataSampleCount);
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

}
