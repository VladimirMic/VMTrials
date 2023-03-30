package vm.vmtrials.simRelEuclidSpace.is;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import static java.util.Locale.filter;
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
import vm.metricSpace.ToolsMetricDomain;
import vm.objTransforms.storeLearned.SVDStoreInterface;
import vm.queryResults.QueryNearestNeighboursStoreInterface;
import vm.queryResults.recallEvaluation.RecallOfCandsSetsEvaluator;
import vm.search.SearchingAlgorithm;
import vm.search.impl.RefineCandidateSetWithPCASimRel;
import vm.search.impl.SimRelSeqScanKNNCandSetThenFullDistEval;
import vm.simRel.SimRelInterface;
import vm.simRel.impl.SimRelEuclideanPCAImpl;
import vm.simRel.impl.SimRelEuclideanPCAImplForTesting;
import vm.simRel.impl.learn.SimRelEuclideanPCALearn;

/**
 *
 * @author Vlada
 */
public class EvaluateKNNQueriesWithSimRel {

    private static final Logger LOG = Logger.getLogger(EvaluateKNNQueriesWithSimRel.class.getName());

    public static final Boolean STORE_RESULTS = true;
    public static final Boolean INVOLVE_OBJS_UNKNOWN_RELATION = true;

    public static final Integer TESTED_DATASET_SIZE = -1;

    public static void main(String[] args) {
        FSDatasetInstanceSingularizator.DeCAFDataset fullDataset = new FSDatasetInstanceSingularizator.DeCAFDataset();
        FSDatasetInstanceSingularizator.DeCAF_PCA256Dataset pcaDataset = new FSDatasetInstanceSingularizator.DeCAF_PCA256Dataset();
        run(fullDataset, pcaDataset);
    }

    private static void run(Dataset fullDataset, Dataset pcaDataset) {
        /* the length of the shortened vectors */
        int pcaLength = 256;
        /*  prefix of the shortened vectors used by the simRel */
        int prefixLength = 24;

        /* kNN queries - the result set size */
        int k = 30;
        /* super set size selected using the PCA vectors. Since PCA is approximation itself, we propose to select more than 5 objects using the PCA-shortened vectors, and then refine them */
        int kPCA = 100;
        /* number of query objects to learn t(\Omega) thresholds. We use different objects than the pivots tested. */
        int querySampleCount = 500;
        /* size of the data sample to learn t(\Omega) thresholds */
        int dataSampleCount = 100000;
        /* percentile - defined in the paper. Defines the precision of the simRel */
        float percentile = 0.85f;

        /* learn thresholds t(\Omega) */
        float[] learnedErrors = learnTOmegaThresholds(fullDataset, pcaDataset, querySampleCount, dataSampleCount, pcaLength, prefixLength, kPCA, percentile);
        SVDStoreInterface svdStorage = new FSSVDStorageImpl(fullDataset.getDatasetName(), 100000, false);

        // TEST QUERIES
        SimRelEuclideanPCAImpl simRel = new SimRelEuclideanPCAImplForTesting(learnedErrors, prefixLength);

        String resultName = "simRel_IS__kPCA" + kPCA + "_involveUnknownRelation_" + INVOLVE_OBJS_UNKNOWN_RELATION + "__PCA" + pcaLength + "_decideUsingFirst" + prefixLength + "_learnToleranceOn__queries" + querySampleCount + "_dataSamples" + dataSampleCount + "_kSearching" + k + "_percentile" + percentile;
        /* Storage to store the results of the kNN queries */
        QueryNearestNeighboursStoreInterface resultsStorage = new FSNearestNeighboursStorageImpl();
        /* Storage to store the stats about the kNN queries */
        Map<FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME, String> fileNameData = new HashMap<>();
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.ground_truth_name, fullDataset.getDatasetName());
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.ground_truth_query_set_name, fullDataset.getDatasetName());
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.ground_truth_nn_count, Integer.toString(k));
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.cand_set_name, pcaDataset.getDatasetName());
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.storing_result_name, resultName);
        FSQueryExecutionStatsStoreImpl statsStorage = new FSQueryExecutionStatsStoreImpl(fileNameData);
        RefineCandidateSetWithPCASimRel alg = new RefineCandidateSetWithPCASimRel(fullDataset.getMetricSpace(), fullDataset.getDistanceFunction(), simRel, svdStorage, pcaDataset.getMetricObjectsFromDataset(TESTED_DATASET_SIZE), prefixLength, pcaLength);

        testQueries(alg, fullDataset, simRel, INVOLVE_OBJS_UNKNOWN_RELATION, kPCA, k, resultsStorage, resultName, statsStorage, fileNameData);
    }

    private static float[] learnTOmegaThresholds(Dataset fullDataset, Dataset pcaDataset, int querySampleCount, int dataSampleCount, int pcaLength, int prefixLength, int kPCA, float percentileWrong) {
        List<Object> querySamples = pcaDataset.getPivotsForTheSameDataset(querySampleCount);
        List<Object> sampleOfDataset = pcaDataset.getSampleOfDataset(dataSampleCount);

        SimRelEuclideanPCALearn simRelLearn = new SimRelEuclideanPCALearn(pcaLength);
        SearchingAlgorithm alg = new SimRelSeqScanKNNCandSetThenFullDistEval(simRelLearn, kPCA, pcaDataset.getDistanceFunction());

        for (Object queryObj : querySamples) {
            simRelLearn.resetCounters(pcaLength);
            alg.candSetKnnSearch(fullDataset.getMetricSpace(), queryObj, kPCA, sampleOfDataset.iterator());
//            int[] errorsPerCoord = simRelLearn.getErrorsPerCoord();
//            int comparisonCounter = simRelLearn.getSimRelCounter();
//            for (int i = 0; i < pcaLength; i++) {
//                System.out.print(i + ";" + (errorsPerCoord[i] / (float) comparisonCounter));
//            }
//            System.out.println();
        }
        float[] ret = simRelLearn.getDiffWhenWrong(percentileWrong, prefixLength);
        return ret;
    }

    private static void testQueries(RefineCandidateSetWithPCASimRel alg, Dataset fullDataset, SimRelInterface simRel, boolean involveObjWithUnknownRelation, int kPCA, int k, QueryNearestNeighboursStoreInterface resultsStorage, String resultName, FSQueryExecutionStatsStoreImpl statsStorage, Map<FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME, String> fileNameDataForRecallStorage) {
        List<Object> fullQueries = fullDataset.getMetricQueryObjectsForTheSameDataset();
        AbstractMetricSpace metricSpace = fullDataset.getMetricSpace();
        Iterator fullDatasetIterator = fullDataset.getMetricObjectsFromDataset(TESTED_DATASET_SIZE);
        Map<Object, Object> mapOfAllFullObjects = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(fullDataset.getMetricSpace(), fullDatasetIterator, true);
        for (int i = 0; i < fullQueries.size(); i++) {
            Object fullQueryObj = fullQueries.get(i);
            Object queryObjId = metricSpace.getIDOfMetricObject(fullQueryObj);
            TreeSet<Map.Entry<Object, Float>> completeKnnSearch = alg.completeKnnSearch(metricSpace, fullQueryObj, k, mapOfAllFullObjects, kPCA, involveObjWithUnknownRelation);
            if (STORE_RESULTS) {
                resultsStorage.storeQueryResult(queryObjId, completeKnnSearch, fullDataset.getDatasetName(), fullDataset.getDatasetName(), resultName);
            }
            int[] earlyStopsPerCoords = (int[]) getSimRelStatsOfLastExecutedQuery(simRel);
            String earlyStopsPerCoordsString = DataTypeConvertor.intsToString(earlyStopsPerCoords, ",");
            int objCheckedCount = alg.getAndResetObjCheckedCount();
            if (STORE_RESULTS) {
                statsStorage.storeStatsForQuery(queryObjId, alg.getDistCompsForQuery(queryObjId), alg.getTimeOfQuery(queryObjId), earlyStopsPerCoordsString, objCheckedCount);
            } else {
                System.out.println(earlyStopsPerCoordsString);
            }
        }
        statsStorage.saveFile();

        LOG.log(Level.INFO, "Evaluating accuracy of queries");
        FSRecallOfCandidateSetsStorageImpl recallStorage = new FSRecallOfCandidateSetsStorageImpl(fileNameDataForRecallStorage);
        RecallOfCandsSetsEvaluator evaluator = new RecallOfCandsSetsEvaluator(resultsStorage, recallStorage);
        evaluator.evaluateAndStoreRecallsOfQueries(fullDataset.getDatasetName(), fullDataset.getDatasetName(), k, fullDataset.getDatasetName(), fullDataset.getDatasetName(), resultName, k);
        recallStorage.saveFile();
    }

    public static Object getSimRelStatsOfLastExecutedQuery(SimRelInterface simRelFunc) {
        if (simRelFunc instanceof SimRelEuclideanPCAImplForTesting) {
            SimRelEuclideanPCAImplForTesting euclid = (SimRelEuclideanPCAImplForTesting) simRelFunc;
            return euclid.getEarlyStopsOnCoordsCounts();
        }
        return null;
    }

}
