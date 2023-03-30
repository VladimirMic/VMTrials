package vm.vmtrials.simRelEuclidSpace;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import vm.datatools.DataTypeConvertor;
import vm.datatools.Tools;
import vm.fs.metricSpaceImpl.FSMetricSpaceImpl;
import vm.fs.metricSpaceImpl.FSMetricSpacesStorage;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.fs.store.queryResults.FSQueryExecutionStatsStoreImpl;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.MetricSpacesStorageInterface;
import vm.metricSpace.dataToStringConvertors.impl.FloatVectorConvertor;
import vm.queryResults.QueryExecutionStatsStoreInterface;
import vm.queryResults.QueryNearestNeighboursStoreInterface;
import vm.search.SearchingAlgorithm;
import vm.search.impl.SimRelSeqScanKNNCandSetThenFullDistEval;
import vm.simRel.impl.SimRelEuclideanPCAImplForTesting;
import vm.simRel.impl.learn.SimRelEuclideanPCALearn;

/**
 *
 * @author Vlada
 */
public class EvaluateSimRelKNNWithPrinting {

    public static final Boolean STORE_RESULTS = true;
    public static final Boolean FULL_RERANK = true;
    public static final Boolean INVOLVE_OBJS_UNKNOWN_RELATION = true;

    public static void main(String[] args) throws FileNotFoundException {
        String fullDatasetName = "decaf_1m";
        String fullQuerySetName = fullDatasetName;
        /* kNN queries - the result set size */
        int k = 30;
        /* the length of the shortened vectors */
        int pcaLength = 256;
        /*  prefix of the shortened vectors used by the simRel */
        int prefixLength = 24;
        /* the name of the PCA-shortened dataset */
        String pcaDatasetName = "decaf_1m_PCA" + pcaLength;
        /* query objects for the testing shortened by the PCA */
        String pcaQuerySetName = pcaDatasetName;
        /* super set size selected using the PCA vectors. Since PCA is approximation itself, we propose to select more than 5 objects using the PCA-shortened vectors, and then refine them */
        int kPCA = 100;
        /* number of query objects to learn t(\Omega) thresholds. We use different objects than the pivots tested. */
        int querySampleCount = 100;
        /* size of the data sample to learn t(\Omega) thresholds */
        int dataSampleCount = 100000;
        /* percentile - defined in the paper. Defines the precision of the simRel */
        float percentile = 0.85f;
//        String output = "h:\\Skola\\2022\\PCA\\simRel\\SimRelPCAStats\\trenovani_" + querySampleCount + "q_" + dataSampleCount + "d\\" + datasetName + ".csv";
//        System.setOut(new PrintStream(output));

        /* definition of the processed metric space */
        AbstractMetricSpace metricSpace = new FSMetricSpaceImpl<>();
        /* storage definition */
        MetricSpacesStorageInterface metricSpacesStorage = new FSMetricSpacesStorage<>(metricSpace, new FloatVectorConvertor());
        /* learn thresholds t(\Omega) */
        float[] learnedErrors = learnSimRelUncertainThresholdsEuclid(metricSpace, metricSpacesStorage, pcaDatasetName, querySampleCount, dataSampleCount, pcaLength, prefixLength, kPCA, percentile);

        // TEST QUERIES
        SimRelEuclideanPCAImplForTesting simRel = new SimRelEuclideanPCAImplForTesting(learnedErrors, prefixLength);
//        String resultName = "pure_simRel_PCA" + pcaLength + "_decideUsingFirst" + prefixLength + "_learnErrorsOn__queries" + querySampleCount + "_dataSamples" + dataSampleCount + "_kSearching" + k + "_percentile" + percentile;
        String resultName = "simRel__SISAP_verification_PAPER6_kPCA" + kPCA + "_involveUnknownRelation_" + INVOLVE_OBJS_UNKNOWN_RELATION + "__rerank_" + FULL_RERANK + "__PCA" + pcaLength + "_decideUsingFirst" + prefixLength + "_learnToleranceOn__queries" + querySampleCount + "_dataSamples" + dataSampleCount + "_kSearching" + k + "_percentile" + percentile;
//        String resultName = "pure_checkSingleX_deleteMany_simRel_PCA" + pcaLength + "_decideUsingFirst" + prefixLength + "_learnErrorsOn__queries" + querySampleCount + "_dataSamples" + dataSampleCount + "_kSearching" + k + "_percentile" + percentile;
        /* Storage to store the results of the kNN queries */
        QueryNearestNeighboursStoreInterface resultsStorage = new FSNearestNeighboursStorageImpl();
        /* Storage to store the stats about the kNN queries */

        Map<FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME, String> fileNameData = new HashMap<>();
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.ground_truth_name, fullDatasetName);
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.ground_truth_query_set_name, fullQuerySetName);
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.ground_truth_nn_count, Integer.toString(k));
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.cand_set_name, pcaDatasetName);
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.cand_set_query_set_name, pcaQuerySetName);
        fileNameData.put(FSQueryExecutionStatsStoreImpl.DATA_NAMES_IN_FILE_NAME.storing_result_name, resultName);
        QueryExecutionStatsStoreInterface statsStorage = new FSQueryExecutionStatsStoreImpl(fileNameData);

        testQueries(metricSpace, metricSpacesStorage, simRel, INVOLVE_OBJS_UNKNOWN_RELATION, fullQuerySetName, pcaQuerySetName, fullDatasetName, pcaDatasetName, kPCA, k, resultsStorage, resultName, statsStorage);
    }

    private static float[] learnSimRelUncertainThresholdsEuclid(AbstractMetricSpace metricSpace, MetricSpacesStorageInterface metricSpacesStorage, String pcaDatasetName, int querySampleCount, int dataSampleCount, int pcaLength, int prefixLength, int kPCA, float percentileWrong) {
        List<Object> querySamples = metricSpacesStorage.getPivots(pcaDatasetName, querySampleCount);
        List<Object> sampleOfDataset = metricSpacesStorage.getSampleOfDataset(pcaDatasetName, dataSampleCount);

        SimRelEuclideanPCALearn simRelLearn = new SimRelEuclideanPCALearn(pcaLength);
        SearchingAlgorithm alg = new SimRelSeqScanKNNCandSetThenFullDistEval(simRelLearn, kPCA, metricSpace.getDistanceFunctionForDataset(pcaDatasetName));

        simRelLearn.resetLearning(pcaLength);
        for (Object queryObj : querySamples) {
            simRelLearn.resetCounters(pcaLength);
            alg.candSetKnnSearch(metricSpace, queryObj, kPCA, sampleOfDataset.iterator());
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

    private static void testQueries(AbstractMetricSpace metricSpace, MetricSpacesStorageInterface metricSpacesStorage, SimRelEuclideanPCAImplForTesting simRel, boolean involveObjWithUnknownRelation, String fullQuerySetName, String pcaQuerySetName, String fullDatasetName, String pcaDatasetName, int kPCA, int k, QueryNearestNeighboursStoreInterface resultsStorage, String resultName, QueryExecutionStatsStoreInterface statsStorage) {
        List<Object> pcaData = Tools.getObjectsFromIterator(metricSpacesStorage.getObjectsFromDataset(pcaDatasetName));
        Iterator<Object> fullDatasetIterator = metricSpacesStorage.getObjectsFromDataset(fullDatasetName);
        Map<Object, Object> mapOfAllFullObjects = null;
        if (FULL_RERANK) {
            mapOfAllFullObjects = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, fullDatasetIterator, true);
        }
        List<Object> fullQueries = metricSpacesStorage.getQueryObjects(fullQuerySetName);
        Map<Object, Object> pcaQueries = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, metricSpacesStorage.getQueryObjects(pcaQuerySetName), false);
        SimRelSeqScanKNNCandSetThenFullDistEval alg = new SimRelSeqScanKNNCandSetThenFullDistEval<>(simRel, kPCA, metricSpace.getDistanceFunctionForDataset(fullDatasetName), involveObjWithUnknownRelation);
        for (int i = 0; i < fullQueries.size(); i++) {
            long time = - System.currentTimeMillis();
            Object fullQueryObj = fullQueries.get(i);
            Object queryObjId = metricSpace.getIDOfMetricObject(fullQueryObj);
            Object pcaQueryObj = pcaQueries.get(queryObjId);
            List<Object> candSetObjIDs = alg.candSetKnnSearch(metricSpace, pcaQueryObj, kPCA, pcaData.iterator());
            TreeSet<Map.Entry<Object, Float>> rerankCandidateSet = alg.rerankCandidateSet(metricSpace, fullQueryObj, k, fullDatasetName, mapOfAllFullObjects, candSetObjIDs);
            time += System.currentTimeMillis();
            alg.incTime(queryObjId, time);
            if (STORE_RESULTS) {
                resultsStorage.storeQueryResult(queryObjId, rerankCandidateSet, fullDatasetName, fullQuerySetName, resultName);
            }
            int[] earlyStopsPerCoords = (int[]) alg.getSimRelStatsOfLastExecutedQuery();
            String earlyStopsPerCoordsString = DataTypeConvertor.intsToString(earlyStopsPerCoords, ";");
            if (STORE_RESULTS) {
                statsStorage.storeStatsForQuery(queryObjId, alg.getDistCompsForQuery(queryObjId), alg.getTimeOfQuery(queryObjId), earlyStopsPerCoordsString);
            } else {
                System.out.println(earlyStopsPerCoordsString);
            }
        }
    }

}
