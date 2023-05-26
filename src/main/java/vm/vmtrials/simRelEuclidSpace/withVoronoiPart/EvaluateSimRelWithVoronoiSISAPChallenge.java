package vm.vmtrials.simRelEuclidSpace.withVoronoiPart;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.h2.mvstore.MVStore;
import vm.datatools.DataTypeConvertor;
import vm.datatools.Tools;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.fs.store.queryResults.FSQueryExecutionStatsStoreImpl;
import vm.fs.store.queryResults.recallEvaluation.FSRecallOfCandidateSetsStorageImpl;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.objTransforms.perform.PCAMetricObjectTransformer;
import vm.objTransforms.storeLearned.SVDStoreInterface;
import vm.queryResults.QueryNearestNeighboursStoreInterface;
import vm.queryResults.errorOnDistEvaluation.ErrorOnDistEvaluator;
import vm.queryResults.recallEvaluation.RecallOfCandsSetsEvaluator;
import vm.search.SearchingAlgorithm;
import vm.search.impl.SimRelSeqScanKNNCandSetThenFullDistEval;
import vm.simRel.impl.SimRelEuclideanPCAImplForTesting;
import vm.simRel.impl.learn.SimRelEuclideanPCALearn;
import vm.vmmvstore.VMMVStorage;
import vm.vmtrials.simRelEuclidSpace.EvaluateSimRelSISAPKNN;

/**
 *
 * @author Vlada
 */
public class EvaluateSimRelWithVoronoiSISAPChallenge {

    private static final Logger LOG = Logger.getLogger(EvaluateSimRelSISAPKNN.class.getName());

    public static final Boolean STORE_RESULTS = true;
    public static final Boolean FULL_RERANK = true;
    public static final Boolean INVOLVE_OBJS_UNKNOWN_RELATION = true;

    public static void main(String[] args) {
        Dataset[] fullDatasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_10M_Dataset(),
            new FSDatasetInstanceSingularizator.LAION_100M_Dataset(),};
        Dataset[] pcaDatasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_10M_PCA96Dataset(),
            new FSDatasetInstanceSingularizator.LAION_100M_PCA96Dataset()
        };

        for (int i = 0; i < fullDatasets.length; i++) {
            run(fullDatasets[i], pcaDatasets[i]);
        }
    }

    private static void run(Dataset fullDataset, Dataset pcaDataset) {

        /* kNN queries - the result set size */
        int k = 10;
        /* the length of the shortened vectors */
        int pcaLength = 96;
        /*  prefix of the shortened vectors used by the simRel */
        int prefixLength = 24;
        /* the name of the PCA-shortened dataset */
        int kPCA = 250;
        /* number of query objects to learn t(\Omega) thresholds. We use different objects than the pivots tested. */
        int querySampleCount = 1000;
        /* size of the data sample to learn t(\Omega) thresholds, SISAP: 100K */
        int dataSampleCount = 1000000;
        /* percentile - defined in the paper. Defines the precision of the simRel */
        float percentile = 0.85f;

//        /* learn thresholds t(\Omega) */
        float[] learnedErrors = learnTOmegaThresholds(pcaDataset, querySampleCount, dataSampleCount, pcaLength, prefixLength, kPCA, percentile);
        // TEST QUERIES
        SimRelEuclideanPCAImplForTesting simRel = new SimRelEuclideanPCAImplForTesting(learnedErrors, prefixLength);
        String resultName = "simRel_" + fullDataset.getDatasetName() + "_kPCA_" + kPCA + "__invUnk_" + INVOLVE_OBJS_UNKNOWN_RELATION + "__re_" + FULL_RERANK + "__PCA" + pcaLength + "_pref" + prefixLength + "_tOn__queries" + querySampleCount + "_sample" + dataSampleCount + "_k" + k + "_perc" + percentile;
        /* Storage to store the results of the kNN queries */
        QueryNearestNeighboursStoreInterface resultsStorage = new FSNearestNeighboursStorageImpl();
        /* Storage to store the stats about the kNN queries */

        Map<FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME, String> fileNameData = new HashMap<>();
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.ground_truth_name, fullDataset.getDatasetName());
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.ground_truth_query_set_name, fullDataset.getDatasetName());
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.ground_truth_nn_count, Integer.toString(k));
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.cand_set_name, pcaDataset.getDatasetName());
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.cand_set_query_set_name, pcaDataset.getDatasetName());
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.storing_result_name, resultName);
        FSQueryExecutionStatsStoreImpl statsStorage = new FSQueryExecutionStatsStoreImpl(fileNameData);

        List pcaQueryObjects = pcaDataset.getMetricQueryObjects();
        Map<Object, Object> pcaQueryObjectsMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(fullDataset.getMetricSpace(), pcaQueryObjects, false);

        testQueries(fullDataset, pcaDataset, simRel, pcaQueryObjectsMap, prefixLength, kPCA, k, resultsStorage, resultName, statsStorage, fileNameData);
//        testQueries(fullDataset, pcaDataset, null, null, prefixLength, kPCA, k, resultsStorage, resultName, statsStorage, fileNameData);
    }

    private static float[] learnTOmegaThresholds(Dataset pcaDataset, int querySampleCount, int dataSampleCount, int pcaLength, int prefixLength, int kPCA, float percentileWrong) {
        List<Object> querySamples = pcaDataset.getPivots(querySampleCount);
        List<Object> sampleOfDataset = pcaDataset.getSampleOfDataset(dataSampleCount);

        SimRelEuclideanPCALearn simRelLearn = new SimRelEuclideanPCALearn(pcaLength);
        SearchingAlgorithm alg = new SimRelSeqScanKNNCandSetThenFullDistEval(simRelLearn, kPCA, pcaDataset.getDistanceFunction());

        simRelLearn.resetLearning(pcaLength);
        for (int i = 0; i < querySamples.size(); i++) {
            Object queryObj = querySamples.get(i);
            simRelLearn.resetCounters(pcaLength);
            alg.candSetKnnSearch(pcaDataset.getMetricSpace(), queryObj, kPCA, sampleOfDataset.iterator());
            LOG.log(Level.INFO, "Learning tresholds with the query obj {0}", new Object[]{i + 1});
        }
        float[] ret = simRelLearn.getDiffWhenWrong(percentileWrong, prefixLength);
        return ret;
    }

    private static void testQueries(Dataset fullDataset, Dataset pcaDataset, SimRelEuclideanPCAImplForTesting simRel, Map<Object, Object> pcaQuerybjectsMap, int prefixLength, int kPCA, int k, QueryNearestNeighboursStoreInterface resultsStorage, String resultName, FSQueryExecutionStatsStoreImpl statsStorage, Map<FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME, String> fileNameDataForRecallStorage) {
        List<Object> pcaData = Tools.getObjectsFromIterator(pcaDataset.getMetricObjectsFromDataset());
        Map<Object, Object> mapOfAllFullObjects = null;
//        if (FULL_RERANK) {
////            Iterator<Object> fullDatasetIterator = fullDataset.getMetricObjectsFromDataset();
////            mapOfAllFullObjects = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(fullDataset.getMetricSpace(), fullDatasetIterator, true);
////            MapDBFile mapDB = new MapDBFile(fullDataset.getMetricSpace(), fullDataset.getDatasetName(), false);
////            mapOfAllFullObjects = mapDB.getStorage();
//            MVStore storage = VMMVStorage.openStorage(fullDataset.getDatasetName());
//            mapOfAllFullObjects = VMMVStorage.getStoredMap(storage);
//        }
        List<Object> fullQueries = fullDataset.getMetricQueryObjects();
        SimRelSeqScanKNNCandSetThenFullDistEval alg = new SimRelSeqScanKNNCandSetThenFullDistEval(simRel, kPCA, fullDataset.getDistanceFunction(), INVOLVE_OBJS_UNKNOWN_RELATION);
        AbstractMetricSpace metricSpaceOfFullDataset = fullDataset.getMetricSpace();
        AbstractMetricSpace pcaDatasetMetricSpace = pcaDataset.getMetricSpace();
        for (int i = 0; i < fullQueries.size(); i++) {
            long time = -System.currentTimeMillis();
            Object fullQueryObj = fullQueries.get(i);
            Object queryObjId = metricSpaceOfFullDataset.getIDOfMetricObject(fullQueryObj);
            AbstractMap.SimpleEntry<Object, float[]> pcaQueryObj = (AbstractMap.SimpleEntry<Object, float[]>) pcaQuerybjectsMap.get(fullQueryObj);

            List<Object> candSetObjIDs = alg.candSetKnnSearch(pcaDatasetMetricSpace, pcaQueryObj, kPCA, pcaData.iterator());
            TreeSet<Map.Entry<Object, Float>> rerankCandidateSet = alg.rerankCandidateSet(metricSpaceOfFullDataset, fullQueryObj, k, fullDataset.getDatasetName(), mapOfAllFullObjects, candSetObjIDs);
            time += System.currentTimeMillis();
            alg.incTime(queryObjId, time);
            if (STORE_RESULTS) {
                resultsStorage.storeQueryResult(queryObjId, rerankCandidateSet, fullDataset.getDatasetName(), fullDataset.getDatasetName(), resultName);
            }
            long[] earlyStopsPerCoords = (long[]) alg.getSimRelStatsOfLastExecutedQuery();
            String earlyStopsPerCoordsString = DataTypeConvertor.longToString(earlyStopsPerCoords, ",");
            if (STORE_RESULTS) {
                statsStorage.storeStatsForQuery(queryObjId, alg.getDistCompsForQuery(queryObjId), alg.getTimeOfQuery(queryObjId), earlyStopsPerCoordsString);
            } else {
                System.out.println(earlyStopsPerCoordsString);
            }
            LOG.log(Level.INFO, "Processed query {0}", new Object[]{i + 1});
        }
        if (STORE_RESULTS) {
            statsStorage.saveFile();
            LOG.log(Level.INFO, "Evaluating accuracy of queries");
            FSRecallOfCandidateSetsStorageImpl recallStorage = new FSRecallOfCandidateSetsStorageImpl(fileNameDataForRecallStorage);
            RecallOfCandsSetsEvaluator recallEvaluator = new RecallOfCandsSetsEvaluator(resultsStorage, recallStorage);
            recallEvaluator.evaluateAndStoreRecallsOfQueries(fullDataset.getDatasetName(), fullDataset.getDatasetName(), k, fullDataset.getDatasetName(), fullDataset.getDatasetName(), resultName, k);
            recallStorage.saveFile();
            LOG.log(Level.INFO, "Evaluating error on distance");
            ErrorOnDistEvaluator eodEvaluator = new ErrorOnDistEvaluator(resultsStorage, recallStorage);
            eodEvaluator.evaluateAndStoreErrorsOnDist(fullDataset.getDatasetName(), fullDataset.getDatasetName(), k, fullDataset.getDatasetName(), fullDataset.getDatasetName(), resultName);
            recallStorage.saveFile();
        }
    }

    private static PCAMetricObjectTransformer initPCA(AbstractMetricSpace<float[]> originalMetricSpace, AbstractMetricSpace<float[]> pcaMetricSpace, SVDStoreInterface svdStorage, int pcaFullLength, int pcaPreffixLength) {
        LOG.log(Level.INFO, "Start loading instance of the PCA with length {0}", pcaPreffixLength);
        float[][] vtMatrixFull = svdStorage.getVTMatrix();
        float[][] vtMatrix = Tools.shrinkMatrix(vtMatrixFull, pcaPreffixLength, vtMatrixFull[0].length);
        return new PCAMetricObjectTransformer(vtMatrix, svdStorage.getMeansOverColumns(), originalMetricSpace, pcaMetricSpace);
    }

}
