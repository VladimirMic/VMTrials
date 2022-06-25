package vm.vmtrials.simRelEuclidSpace;

import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import vm.datatools.DataTypeConvertor;
import vm.datatools.Tools;
import vm.db.main.queryResults.DBQueryExecutionStatsStoreImpl;
import vm.db.metricSpaceImpl.DBMetricSpaceImpl;
import vm.db.metricSpaceImpl.DBMetricSpacesStorage;
import vm.db.store.queryResults.DBNearestNeighboursStorageImpl;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.MetricDomainTools;
import vm.metricSpace.MetricSpacesStorageInterface;
import vm.metricSpace.dataToStringConvertors.impl.FloatVectorConvertor;
import vm.queryResults.QueryExecutionStatsStoreInterface;
import vm.queryResults.QueryNearestNeighboursStoreInterface;
import vm.search.impl.SimRelSeqScanKNNCandSetThenFullDistEval;
import vm.search.impl.SimRelSeqScanKNNJustLastObjCheckThenFullDistEval;
import vm.simRel.impl.SimRelEuclideanPCAImpl;
import vm.simRel.impl.learn.SimRelEuclideanPCALearn;

/**
 *
 * @author Vlada
 */
public class EvaluateSimRelKNNWithPrinting {

    public static final Boolean STORE_RESULTS = true;

    public static void main(String[] args) throws SQLException, FileNotFoundException {
        String fullDatasetName = "decaf_1m";
        String fullQuerySetName = fullDatasetName;
        int k = 30;
        int pcaLength = 256;
        int prefixLength = 24;
        String pcaDatasetName = "decaf_1m_PCA" + pcaLength;
        String pcaQuerySetName = pcaDatasetName;
        int kPCA = 100;
        int querySampleCount = 100;
        int dataSampleCount = 100000;
        float percentile = 0.9f;
//        String output = "h:\\Skola\\2022\\PCA\\simRel\\SimRelPCAStats\\trenovani_" + querySampleCount + "q_" + dataSampleCount + "d\\" + datasetName + ".csv";
//        System.setOut(new PrintStream(output));

        AbstractMetricSpace metricSpace = new DBMetricSpaceImpl<>();
        MetricSpacesStorageInterface metricSpacesStorage = new DBMetricSpacesStorage<>(metricSpace, new FloatVectorConvertor());
        float[] learnedErrors = new float[0];
        learnedErrors = learnSimRelUncertainThresholdsEuclid(metricSpace, metricSpacesStorage, pcaDatasetName, querySampleCount, dataSampleCount, pcaLength, kPCA, percentile);

// TEST QUERIES
        SimRelEuclideanPCAImpl simRel = new SimRelEuclideanPCAImpl(learnedErrors, prefixLength);
//        String resultName = "pure_simRel_PCA" + pcaLength + "_decideUsingFirst" + prefixLength + "_learnErrorsOn__queries" + querySampleCount + "_dataSamples" + dataSampleCount + "_kSearching" + k + "_percentile" + percentile;
        String resultName = "PAPER_properReranking_deleteMany_simRel_PCA" + pcaLength + "_decideUsingFirst" + prefixLength + "_learnDecreasingErrorsOn__queries" + querySampleCount + "_dataSamples" + dataSampleCount + "_kSearching" + k + "_percentile" + percentile;
//        String resultName = "pure_checkSingleX_deleteMany_simRel_PCA" + pcaLength + "_decideUsingFirst" + prefixLength + "_learnErrorsOn__queries" + querySampleCount + "_dataSamples" + dataSampleCount + "_kSearching" + k + "_percentile" + percentile;
        QueryNearestNeighboursStoreInterface resultsStorage = new DBNearestNeighboursStorageImpl();

        QueryExecutionStatsStoreInterface statsStorage = new DBQueryExecutionStatsStoreImpl(fullDatasetName, fullQuerySetName, k, pcaDatasetName, pcaQuerySetName, resultName, null);
        testQueries(metricSpace, metricSpacesStorage, simRel, fullQuerySetName, pcaQuerySetName, fullDatasetName, pcaDatasetName, kPCA, k, resultsStorage, resultName, statsStorage);
    }

    private static float[] learnSimRelUncertainThresholdsEuclid(AbstractMetricSpace metricSpace, MetricSpacesStorageInterface metricSpacesStorage, String pcaDatasetName, int querySampleCount, int dataSampleCount, int pcaLength, int kPCA, float percentileWrong) {
        List<Object> querySamples = metricSpacesStorage.getMetricPivots(pcaDatasetName, querySampleCount);
        List<Object> sampleOfDataset = metricSpacesStorage.getSampleOfDataset(pcaDatasetName, dataSampleCount);

        SimRelEuclideanPCALearn simRelLearn = new SimRelEuclideanPCALearn();
        SimRelSeqScanKNNJustLastObjCheckThenFullDistEval alg = new SimRelSeqScanKNNJustLastObjCheckThenFullDistEval(simRelLearn, metricSpace.getDistanceFunctionForDataset(pcaDatasetName));

        simRelLearn.resetLearning(pcaLength);
        for (Object queryObj : querySamples) {
            simRelLearn.resetCounters(pcaLength);
            alg.completeKnnSearch(metricSpace, queryObj, kPCA, sampleOfDataset.iterator());
//            int[] errorsPerCoord = simRelLearn.getErrorsPerCoord();
//            int comparisonCounter = simRelLearn.getSimRelCounter();
//            for (int i = 0; i < pcaLength; i++) {
//                System.out.print(i + ";" + (errorsPerCoord[i] / (float) comparisonCounter));
//            }
//            System.out.println();
        }
        float[] ret = simRelLearn.getDiffWhenWrong(percentileWrong);
        return ret;
    }

    private static void testQueries(AbstractMetricSpace metricSpace, MetricSpacesStorageInterface metricSpacesStorage, SimRelEuclideanPCAImpl simRel, String fullQuerySetName, String pcaQuerySetName, String fullDatasetName, String pcaDatasetName, int kPCA, int k, QueryNearestNeighboursStoreInterface resultsStorage, String resultName, QueryExecutionStatsStoreInterface statsStorage) {
        List<Object> pcaData = Tools.getObjectsFromIterator(metricSpacesStorage.getMetricObjectsFromDataset(pcaDatasetName));
        List<Object> fullData = Tools.getObjectsFromIterator(metricSpacesStorage.getMetricObjectsFromDataset(fullDatasetName));
        List<Object> fullQueries = metricSpacesStorage.getMetricQueryObjects(fullQuerySetName);
        Map<Object, Object> pcaQueries = MetricDomainTools.getMetricObjectsAsIdObjectMap(metricSpace, metricSpacesStorage.getMetricQueryObjects(pcaQuerySetName));
        SimRelSeqScanKNNCandSetThenFullDistEval alg = new SimRelSeqScanKNNCandSetThenFullDistEval<>(simRel, kPCA, metricSpace.getDistanceFunctionForDataset(fullDatasetName));
        for (int i = 0; i < fullQueries.size(); i++) {
            Object fullQueryObj = fullQueries.get(i);
            Object queryObjId = metricSpace.getIDOfMetricObject(fullQueryObj);
            Object pcaQueryObj = pcaQueries.get(queryObjId);
            Set<Object> candSetObjIDs = alg.candSetKnnSearch(metricSpace, pcaQueryObj, kPCA, pcaData.iterator());
            Iterator<Object> fullDataIt = fullData.iterator();
            TreeSet<Map.Entry<Object, Float>> rerankCandidateSet = alg.rerankCandidateSet(metricSpace, fullQueryObj, k, fullDatasetName, fullDataIt, candSetObjIDs);
            if (STORE_RESULTS) {
                resultsStorage.storeQueryResult(queryObjId, rerankCandidateSet, fullDatasetName, fullQuerySetName, resultName);
            }
            int[] earlyStopsPerCoords = (int[]) alg.getSimRelStatsOfLastExecutedQuery();
            String earlyStopsPerCoordsString = DataTypeConvertor.intsToString(earlyStopsPerCoords, ";");
            if (STORE_RESULTS) {
                statsStorage.storeStatsForQuery(queryObjId, alg.getDistCompsOfLastExecutedQuery(), alg.getTimeOfLastExecutedQuery(), earlyStopsPerCoordsString);
            } else {
                System.out.println(earlyStopsPerCoordsString);
            }
        }
    }

}
