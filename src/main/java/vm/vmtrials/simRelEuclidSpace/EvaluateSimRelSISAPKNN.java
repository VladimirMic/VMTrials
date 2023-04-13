package vm.vmtrials.simRelEuclidSpace;

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
import vm.fs.store.dataTransforms.FSSVDStorageImpl;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.fs.store.queryResults.FSQueryExecutionStatsStoreImpl;
import vm.fs.store.queryResults.recallEvaluation.FSRecallOfCandidateSetsStorageImpl;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.objTransforms.perform.PCAMetricObjectTransformer;
import vm.objTransforms.storeLearned.SVDStoreInterface;
import vm.queryResults.QueryNearestNeighboursStoreInterface;
import vm.queryResults.errorOnDistEvaluation.ErrorOnDistEvaluator;
import vm.queryResults.recallEvaluation.RecallOfCandsSetsEvaluator;
import vm.search.SearchingAlgorithm;
import vm.search.impl.SimRelSeqScanKNNCandSetThenFullDistEval;
import vm.simRel.impl.SimRelEuclideanPCAImplForTesting;
import vm.simRel.impl.learn.SimRelEuclideanPCALearn;
import vm.vmmapdb.MapDBFile;

/**
 *
 * @author Vlada
 */
public class EvaluateSimRelSISAPKNN {

    private static final Logger LOG = Logger.getLogger(EvaluateSimRelSISAPKNN.class.getName());

    public static final Boolean STORE_RESULTS = true;
    public static final Boolean FULL_RERANK = true;
    public static final Boolean INVOLVE_OBJS_UNKNOWN_RELATION = true;

    public static void main(String[] args) {
        Dataset fullDataset = new FSDatasetInstanceSingularizator.DeCAFDataset();
        Dataset pcaDataset = new FSDatasetInstanceSingularizator.DeCAF_PCA256Dataset();
        run(fullDataset, pcaDataset);
    }

    private static void run(Dataset fullDataset, Dataset pcaDataset) {
        /* kNN queries - the result set size */
        int k = 30;
        /* the length of the shortened vectors */
        int pcaLength = 256;
        /*  prefix of the shortened vectors used by the simRel */
        int prefixLength = 24;
        /* the name of the PCA-shortened dataset */
        String pcaDatasetName = "decaf_1m_PCA" + pcaLength;
        /* super set size selected using the PCA vectors. Since PCA is approximation itself, we propose to select more than 5 objects using the PCA-shortened vectors, and then refine them */
        int kPCA = 100;
        /* number of query objects to learn t(\Omega) thresholds. We use different objects than the pivots tested. */
        int querySampleCount = 100;
        /* size of the data sample to learn t(\Omega) thresholds */
        int dataSampleCount = 100000;
        /* percentile - defined in the paper. Defines the precision of the simRel */
        float percentile = 0.85f;

//        /* learn thresholds t(\Omega) */
        float[] learnedErrors = learnTOmegaThresholds(fullDataset, pcaDataset, querySampleCount, dataSampleCount, pcaLength, prefixLength, kPCA, percentile);
        SVDStoreInterface svdStorage = new FSSVDStorageImpl(fullDataset.getDatasetName(), 100000, false);
        PCAMetricObjectTransformer pcaTransformer = initPCA(pcaDataset.getMetricSpace(), svdStorage, pcaLength, prefixLength);
        // TEST QUERIES
        SimRelEuclideanPCAImplForTesting simRel = new SimRelEuclideanPCAImplForTesting(learnedErrors, prefixLength);
//        String resultName = "pure_simRel_PCA" + pcaLength + "_decideUsingFirst" + prefixLength + "_learnErrorsOn__queries" + querySampleCount + "_dataSamples" + dataSampleCount + "_kSearching" + k + "_percentile" + percentile;
        String resultName = "simRel_SISAP_for_IS_SSD_kPCA" + kPCA + "_involveUnknownRelation_" + INVOLVE_OBJS_UNKNOWN_RELATION + "__rerank_" + FULL_RERANK + "__PCA" + pcaLength + "_decideUsingFirst" + prefixLength + "_learnToleranceOn__queries" + querySampleCount + "_dataSamples" + dataSampleCount + "_kSearching" + k + "_percentile" + percentile;
//        String resultName = "pure_checkSingleX_deleteMany_simRel_PCA" + pcaLength + "_decideUsingFirst" + prefixLength + "_learnErrorsOn__queries" + querySampleCount + "_dataSamples" + dataSampleCount + "_kSearching" + k + "_percentile" + percentile;
        /* Storage to store the results of the kNN queries */
        QueryNearestNeighboursStoreInterface resultsStorage = new FSNearestNeighboursStorageImpl();
        /* Storage to store the stats about the kNN queries */

        Map<FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME, String> fileNameData = new HashMap<>();
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.ground_truth_name, fullDataset.getDatasetName());
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.ground_truth_query_set_name, fullDataset.getDatasetName());
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.ground_truth_nn_count, Integer.toString(k));
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.cand_set_name, pcaDatasetName);
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.cand_set_query_set_name, pcaDatasetName);
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.storing_result_name, resultName);
        FSQueryExecutionStatsStoreImpl statsStorage = new FSQueryExecutionStatsStoreImpl(fileNameData);

        testQueries(fullDataset, pcaDataset, simRel, pcaTransformer, prefixLength, kPCA, k, resultsStorage, resultName, statsStorage, fileNameData);
    }

    private static float[] learnTOmegaThresholds(Dataset fullDataset, Dataset pcaDataset, int querySampleCount, int dataSampleCount, int pcaLength, int prefixLength, int kPCA, float percentileWrong) {
        List<Object> querySamples = pcaDataset.getPivotsForTheSameDataset(querySampleCount);
        List<Object> sampleOfDataset = pcaDataset.getSampleOfDataset(dataSampleCount);

        SimRelEuclideanPCALearn simRelLearn = new SimRelEuclideanPCALearn(pcaLength);
        SearchingAlgorithm alg = new SimRelSeqScanKNNCandSetThenFullDistEval(simRelLearn, kPCA, pcaDataset.getDistanceFunction());

        simRelLearn.resetLearning(pcaLength);
        for (Object queryObj : querySamples) {
            simRelLearn.resetCounters(pcaLength);
            alg.candSetKnnSearch(fullDataset.getMetricSpace(), queryObj, kPCA, sampleOfDataset.iterator());
        }
        float[] ret = simRelLearn.getDiffWhenWrong(percentileWrong, prefixLength);
        return ret;
    }

    private static void testQueries(Dataset fullDataset, Dataset pcaDataset, SimRelEuclideanPCAImplForTesting simRel, PCAMetricObjectTransformer pcaTransformer, int prefixLength, int kPCA, int k, QueryNearestNeighboursStoreInterface resultsStorage, String resultName, FSQueryExecutionStatsStoreImpl statsStorage, Map<FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME, String> fileNameDataForRecallStorage) {
        List<Object> pcaData = Tools.getObjectsFromIterator(pcaDataset.getMetricObjectsFromDataset());
        Map<Object, Object> mapOfAllFullObjects = null;
        if (FULL_RERANK) {
//            Iterator<Object> fullDatasetIterator = fullDataset.getMetricObjectsFromDataset();
//            mapOfAllFullObjects = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(fullDataset.getMetricSpace(), fullDatasetIterator, true);
            MapDBFile mapDB = new MapDBFile(fullDataset.getMetricSpace(), fullDataset.getDatasetName(), false);
            mapOfAllFullObjects = mapDB.getStorage();
        }
        List<Object> fullQueries = fullDataset.getMetricQueryObjectsForTheSameDataset();
        SimRelSeqScanKNNCandSetThenFullDistEval alg = new SimRelSeqScanKNNCandSetThenFullDistEval(simRel, kPCA, fullDataset.getDistanceFunction(), INVOLVE_OBJS_UNKNOWN_RELATION);
        AbstractMetricSpace metricSpaceOfFullDataset = fullDataset.getMetricSpace();
        for (int i = 0; i < fullQueries.size(); i++) {
            long time = -System.currentTimeMillis();
            Object fullQueryObj = fullQueries.get(i);
            Object queryObjId = metricSpaceOfFullDataset.getIDOfMetricObject(fullQueryObj);
            AbstractMap.SimpleEntry<Object, float[]> pcaQueryObj = (AbstractMap.SimpleEntry<Object, float[]>) pcaTransformer.transformMetricObject(fullQueryObj, prefixLength);

            List<Object> candSetObjIDs = alg.candSetKnnSearch(metricSpaceOfFullDataset, pcaQueryObj, kPCA, pcaData.iterator());
            TreeSet<Map.Entry<Object, Float>> rerankCandidateSet = alg.rerankCandidateSet(metricSpaceOfFullDataset, fullQueryObj, k, fullDataset.getDatasetName(), mapOfAllFullObjects, candSetObjIDs);
            time += System.currentTimeMillis();
            alg.incTime(queryObjId, time);
            if (STORE_RESULTS) {
                resultsStorage.storeQueryResult(queryObjId, rerankCandidateSet, fullDataset.getDatasetName(), fullDataset.getDatasetName(), resultName);
            }
            int[] earlyStopsPerCoords = (int[]) alg.getSimRelStatsOfLastExecutedQuery();
            String earlyStopsPerCoordsString = DataTypeConvertor.intsToString(earlyStopsPerCoords, ",");
            if (STORE_RESULTS) {
                statsStorage.storeStatsForQuery(queryObjId, alg.getDistCompsForQuery(queryObjId), alg.getTimeOfQuery(queryObjId), earlyStopsPerCoordsString);
            } else {
                System.out.println(earlyStopsPerCoordsString);
            }
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

    private static PCAMetricObjectTransformer initPCA(AbstractMetricSpace<float[]> metricSpace, SVDStoreInterface svdStorage, int pcaFullLength, int pcaPreffixLength) {
        LOG.log(Level.INFO, "Start loading instance of the PCA with length {0}", pcaPreffixLength);
        float[][] vtMatrixFull = svdStorage.getVTMatrix();
        float[][] vtMatrix = Tools.shrinkMatrix(vtMatrixFull, pcaFullLength, vtMatrixFull[0].length);
        return new PCAMetricObjectTransformer(vtMatrix, svdStorage.getMeansOverColumns(), metricSpace);
    }

}
