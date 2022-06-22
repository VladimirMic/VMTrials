package vm.vmtrials.simRelEuclidSpace;

import vm.trials.deprecated.PureSimRelSequentialScanKNN;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import vm.datatools.DataTypeConvertor;
import vm.datatools.Tools;
import vm.db.main.queryResults.DBQueryExecutionStatsStoreImpl;
import vm.db.metricSpaceImpl.DBMetricSpaceImpl;
import vm.db.metricSpaceImpl.DBMetricSpacesStorage;
import vm.db.store.queryResults.DBNearestNeighboursStorageImpl;
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

    public static void main(String[] args) throws SQLException, FileNotFoundException {
        int pcaLength = 256;
        int prefixLength = 32;
        String datasetName = "decaf_1m_PCA" + pcaLength;
        int k = 100;
        int querySampleCount = 100;
        int dataSampleCount = 100000;
        float percentile = 0.95f;
//        String output = "h:\\Skola\\2022\\PCA\\simRel\\SimRelPCAStats\\trenovani_" + querySampleCount + "q_" + dataSampleCount + "d\\" + datasetName + ".csv";
//        System.setOut(new PrintStream(output));

        DBMetricSpaceImpl metricSpace = new DBMetricSpaceImpl<>();
        MetricSpacesStorageInterface metricSpacesStorage = new DBMetricSpacesStorage<>(metricSpace, new FloatVectorConvertor());
        float[] learnedErrors = new float[0];
        learnedErrors = learnSimRelUncertainThresholdsEuclid(metricSpace, metricSpacesStorage, datasetName, querySampleCount, dataSampleCount, pcaLength, k, percentile, true);

// TEST QUERIES
        SimRelEuclideanPCAImpl simRel = new SimRelEuclideanPCAImpl(learnedErrors, prefixLength);
//        String resultName = "pure_simRel_PCA" + pcaLength + "_decideUsingFirst" + prefixLength + "_learnErrorsOn__queries" + querySampleCount + "_dataSamples" + dataSampleCount + "_kSearching" + k + "_percentile" + percentile;
        String resultName = "pure_deleteMany_simRel_PCA" + pcaLength + "_decideUsingFirst" + prefixLength + "_learnDecreasingErrorsOn__queries" + querySampleCount + "_dataSamples" + dataSampleCount + "_kSearching" + k + "_percentile" + percentile;
//        String resultName = "pure_deleteMany_simRel_PCA" + pcaLength + "_decideUsingFirst" + prefixLength + "_learnErrorsOn__queries" + querySampleCount + "_dataSamples" + dataSampleCount + "_kSearching" + k + "_percentile" + percentile;
        QueryNearestNeighboursStoreInterface resultsStorage = new DBNearestNeighboursStorageImpl();

        QueryExecutionStatsStoreInterface statsStorage = new DBQueryExecutionStatsStoreImpl(datasetName, datasetName, k, datasetName, datasetName, resultName, null);
        testQueries(metricSpace, metricSpacesStorage, simRel, datasetName, datasetName, k, resultsStorage, resultName, statsStorage);
    }

    private static float[] learnSimRelUncertainThresholdsEuclid(DBMetricSpaceImpl metricSpace, MetricSpacesStorageInterface metricSpacesStorage, String datasetName, int querySampleCount, int dataSampleCount, int pcaLength, int k, float percentileWrong) {
        return learnSimRelUncertainThresholdsEuclid(metricSpace, metricSpacesStorage, datasetName, querySampleCount, dataSampleCount, pcaLength, k, percentileWrong, false);
    }

    private static float[] learnSimRelUncertainThresholdsEuclid(DBMetricSpaceImpl metricSpace, MetricSpacesStorageInterface metricSpacesStorage, String datasetName, int querySampleCount, int dataSampleCount, int pcaLength, int k, float percentileWrong, boolean makeDecreasing) {
        List<Object> querySamples = metricSpacesStorage.getMetricPivots(datasetName, querySampleCount);
        List<Object> sampleOfDataset = metricSpacesStorage.getSampleOfDataset(datasetName, dataSampleCount);

        SimRelEuclideanPCALearn simRelLearn = new SimRelEuclideanPCALearn();
        SimRelSeqScanKNNJustLastObjCheckThenFullDistEval alg = new SimRelSeqScanKNNJustLastObjCheckThenFullDistEval(simRelLearn, metricSpace.getDistanceFunctionForDataset(datasetName));

        simRelLearn.resetLearning(pcaLength);
        for (Object queryObj : querySamples) {
            simRelLearn.resetCounters(pcaLength);
            alg.knnSearch(metricSpace, queryObj, k, sampleOfDataset.iterator());
//            int[] errorsPerCoord = simRelLearn.getErrorsPerCoord();
//            int comparisonCounter = simRelLearn.getSimRelCounter();
//            for (int i = 0; i < pcaLength; i++) {
//                System.out.print(i + ";" + (errorsPerCoord[i] / (float) comparisonCounter));
//            }
//            System.out.println();
        }
        float[] ret = simRelLearn.getDiffWhenWrong(percentileWrong);
        if (makeDecreasing) {
            float max = 0;
            for (int i = ret.length - 1; i >= 0; i--) {
                max = Math.max(max, ret[i]);
                ret[i] = max;
            }
        }
        return ret;
    }

    private static void testQueries(DBMetricSpaceImpl metricSpace, MetricSpacesStorageInterface metricSpacesStorage, SimRelEuclideanPCAImpl simRel, String querySetName, String datasetName, int k, QueryNearestNeighboursStoreInterface resultsStorage, String resultName, QueryExecutionStatsStoreInterface statsStorage) {
        Iterator<Object> dataIt = metricSpacesStorage.getMetricObjectsFromDataset(datasetName);
        List<Object> data = Tools.getObjectsFromIterator(dataIt);
        List<Object> queries = metricSpacesStorage.getMetricQueryObjects(querySetName);
//        SimRelSeqScanKNNJustLastObjCheckThenFullDistEval alg = new SimRelSeqScanKNNJustLastObjCheckThenFullDistEval(simRel, metricSpace.getDistanceFunctionForDataset(datasetName));
        SimRelSeqScanKNNCandSetThenFullDistEval alg = new SimRelSeqScanKNNCandSetThenFullDistEval<>(simRel, metricSpace.getDistanceFunctionForDataset(datasetName));
        for (int i = 0; i < queries.size(); i++) {
            Object queryObj = queries.get(i);
            Object queryObjId = metricSpace.getIDOfMetricObject(queryObj);
            TreeSet<Map.Entry<Object, Float>> result = alg.knnSearch(metricSpace, queryObj, k, data.iterator());
            resultsStorage.storeQueryResult(queryObjId, result, datasetName, datasetName, resultName);
            int[] earlyStopsPerCoords = (int[]) alg.getSimRelStatsOfLastExecutedQuery();
            String earlyStopsPerCoordsString = DataTypeConvertor.intsToString(earlyStopsPerCoords, ";");
            statsStorage.storeStatsForQuery(queryObjId, alg.getDistCompsOfLastExecutedQuery(), alg.getTimeOfLastExecutedQuery(), earlyStopsPerCoordsString);
        }
    }

}
