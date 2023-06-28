/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.hdf5;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.DataTypeConvertor;
import vm.datatools.Tools;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.filtering.FSSimRelThresholdsTOmegaStorage;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.fs.store.queryResults.FSQueryExecutionStatsStoreImpl;
import vm.fs.store.queryResults.recallEvaluation.FSRecallOfCandidateSetsStorageImpl;
import vm.fs.store.voronoiPartitioning.FSVoronoiPartitioningStorage;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.queryResults.QueryNearestNeighboursStoreInterface;
import vm.queryResults.errorOnDistEvaluation.ErrorOnDistEvaluator;
import vm.queryResults.recallEvaluation.RecallOfCandsSetsEvaluator;
import vm.search.impl.VoronoiPartitionsCandSetIdentifier;
import vm.simRel.impl.SimRelEuclideanPCAImpl;
import vm.simRel.impl.SimRelEuclideanPCAImplForTesting;
import vm.vmtrials.deprecated.SimRelInstantRefinement;
import vm.vmtrials.tripleFiltering_Challenge.EvaluateVorSkeSimMain;

/**
 *
 * @author Vlada
 */
public class EvaluateSimRelInstantWithVoronoi {

    private static final Logger LOG = Logger.getLogger(EvaluateSimRelInstantWithVoronoi.class.getName());

    public static final Boolean STORE_RESULTS = true;
    public static final Boolean FULL_RERANK = true;

    public static void main(String[] args) {
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

        for (int i = 0; i < fullDatasets.length; i++) {
            run(fullDatasets[i], pcaDatasets[i]);
            System.exit(0);
        }
    }

    private static void run(Dataset fullDataset, Dataset pcaDataset) {
        /* kNN queries - the result set size */
        int k = 10;
        /* the maximum number of candidates identified by the Voronoi partitioning*/
        int kVoronoi = 400000;
        /* the minimum size of the simRel result */
        int kPCA = 300;
        /*  prefix of the shortened vectors used by the simRel */
        int prefixLength = 32;
        /*  prefix of the shortened vectors used by the simRel */
        int pcaLength = 96;
        /* number of query objects to learn t(\Omega) thresholds. We use different objects than the pivots tested. */
        int querySampleCount = 100;
        /* size of the data sample to learn t(\Omega) thresholds, SISAP: 100K */
        int dataSampleCount = 100000;
        /* percentile - defined in the paper. Defines the precision of the simRel */
        float percentile = 0.9f;
//        Integer voronoiPivots = 2048;

//        FSSimRelThresholdsTOmegaStorage simRelStorage = new FSSimRelThresholdsTOmegaStorage(querySampleCount, pcaLength, kPCA, voronoiPivots, kVoronoi); // On Cells
        FSSimRelThresholdsTOmegaStorage simRelStorage = new FSSimRelThresholdsTOmegaStorage(querySampleCount, pcaLength, kPCA, dataSampleCount); // Global
        float[][] simRelThresholds = simRelStorage.load(pcaDataset.getDatasetName());
        int idx = simRelStorage.percentileToArrayIdx(percentile);
        float[] learnedErrors = simRelThresholds[idx];
        // TEST QUERIES
        SimRelEuclideanPCAImpl simRel = new SimRelEuclideanPCAImplForTesting(learnedErrors, prefixLength);
//        String resultName = "Voronoi_simRelOnCells_" + fullDataset.getDatasetName() + "_kVoronoi" + kVoronoi + "_kPCA" + kPCA + "_prefix" + prefixLength + "_learntOmegaOn_" + querySampleCount + "q__" + dataSampleCount + "o__k" + k + "_perc" + percentile;
        String resultName = "Voronoi_simRelInstant_" + fullDataset.getDatasetName() + "_kVoronoi" + kVoronoi + "_kPCA" + kPCA + "_prefix" + prefixLength + "_learntOmegaOn_" + querySampleCount + "q__" + dataSampleCount + "o__k" + k + "_perc" + percentile;
        /* Storage to store the results of the kNN queries */
        QueryNearestNeighboursStoreInterface resultsStorage = new FSNearestNeighboursStorageImpl();
        /* Storage to store the stats about the kNN queries */

        Map<FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME, String> fileNameData = new HashMap<>();
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.ground_truth_name, fullDataset.getDatasetName());
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.ground_truth_query_set_name, fullDataset.getQuerySetName());
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.ground_truth_nn_count, Integer.toString(k));
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.cand_set_name, pcaDataset.getDatasetName());
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.cand_set_query_set_name, pcaDataset.getQuerySetName());
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.storing_result_name, resultName);
        FSQueryExecutionStatsStoreImpl statsStorage = new FSQueryExecutionStatsStoreImpl(fileNameData);

        testQueries(fullDataset, pcaDataset, simRel, kVoronoi, kPCA, k, prefixLength, resultsStorage, resultName, statsStorage, fileNameData);
    }

    private static void testQueries(Dataset fullDataset, Dataset pcaDataset, SimRelEuclideanPCAImpl simRel, Integer kVoronoi, int kPCA, int k, int prefixLength, QueryNearestNeighboursStoreInterface resultsStorage, String resultName, FSQueryExecutionStatsStoreImpl statsStorage, Map<FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME, String> fileNameDataForRecallStorage) {
        Map<Object, Object> mapOfAllFullObjects = fullDataset.getKeyValueStorage();
        List<Object> fullQueries = fullDataset.getMetricQueryObjects();

        VoronoiPartitionsCandSetIdentifier algVoronoi = new VoronoiPartitionsCandSetIdentifier(fullDataset, new FSVoronoiPartitioningStorage(), 2048);
        SimRelInstantRefinement algSimRel = new SimRelInstantRefinement(fullDataset.getDistanceFunction(), simRel, kPCA);

        AbstractMetricSpace metricSpaceOfFullDataset = fullDataset.getMetricSpace();
        AbstractMetricSpace pcaDatasetMetricSpace = pcaDataset.getMetricSpace();

        Map pcaOMap = EvaluateVorSkeSimMain.getMapOfPrefixes(pcaDatasetMetricSpace, pcaDataset.getMetricObjectsFromDataset(), prefixLength);
        Map pcaQueriesMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(pcaDatasetMetricSpace, pcaDataset.getMetricQueryObjects(), false);

        for (int i = 0; i < fullQueries.size(); i++) {

            long time = -System.currentTimeMillis();
            Object fullQueryObj = fullQueries.get(i);
            Object queryObjId = metricSpaceOfFullDataset.getIDOfMetricObject(fullQueryObj);

            List candidatesIDs = algVoronoi.candSetKnnSearch(metricSpaceOfFullDataset, fullQueryObj, kVoronoi, null);
            List<Object> pcaOfCandidates = Tools.filterMap(pcaOMap, candidatesIDs);
            AbstractMap.SimpleEntry<Object, float[]> pcaQueryObj = (AbstractMap.SimpleEntry<Object, float[]>) pcaQueriesMap.get(queryObjId);

//            List<Object> candSetObjIDs = algSimRel.candSetKnnSearch(pcaDatasetMetricSpace, pcaQueryObj, kPCA, pcaOfCandidates.iterator());
//            TreeSet<Map.Entry<Object, Float>> rerankCandidateSet = algSimRel.rerankCandidateSet(metricSpaceOfFullDataset, fullQueryObj, k, fullDataset.getDatasetName(), mapOfAllFullObjects, candSetObjIDs);
            TreeSet<Map.Entry<Object, Float>> result = algSimRel.completeKnnSearch(
                    metricSpaceOfFullDataset,
                    fullQueryObj,
                    k,
                    pcaOfCandidates.iterator(),
                    mapOfAllFullObjects,
                    pcaDatasetMetricSpace,
                    pcaQueryObj
            );
            time += System.currentTimeMillis();
            algSimRel.incTime(queryObjId, time);
            if (STORE_RESULTS) {
                resultsStorage.storeQueryResult(queryObjId, result, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), resultName);
            }
            long[] earlyStopsPerCoords = (long[]) algSimRel.getSimRelStatsOfLastExecutedQuery();
            String earlyStopsPerCoordsString = DataTypeConvertor.longToString(earlyStopsPerCoords, ",");
            if (STORE_RESULTS) {
                statsStorage.storeStatsForQuery(queryObjId, algSimRel.getDistCompsForQuery(queryObjId), algSimRel.getTimeOfQuery(queryObjId), earlyStopsPerCoordsString);
            } else {
                System.out.println(earlyStopsPerCoordsString);
            }
            LOG.log(Level.INFO, "Processed query {0} in {1} ms", new Object[]{i + 1, time});
        }
        if (STORE_RESULTS) {
            statsStorage.saveFile();
            LOG.log(Level.INFO, "Evaluating accuracy (recall) of queries");
            FSRecallOfCandidateSetsStorageImpl recallStorage = new FSRecallOfCandidateSetsStorageImpl(fileNameDataForRecallStorage);
            RecallOfCandsSetsEvaluator recallEvaluator = new RecallOfCandsSetsEvaluator(resultsStorage, recallStorage);
            recallEvaluator.evaluateAndStoreRecallsOfQueries(fullDataset.getDatasetName(), fullDataset.getQuerySetName(), k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), resultName, k);
            recallStorage.saveFile();
            LOG.log(Level.INFO, "Evaluating error on distance");
            ErrorOnDistEvaluator eodEvaluator = new ErrorOnDistEvaluator(resultsStorage, recallStorage);
            eodEvaluator.evaluateAndStoreErrorsOnDist(fullDataset.getDatasetName(), fullDataset.getQuerySetName(), k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), resultName);
            recallStorage.saveFile();
        }
    }

}
