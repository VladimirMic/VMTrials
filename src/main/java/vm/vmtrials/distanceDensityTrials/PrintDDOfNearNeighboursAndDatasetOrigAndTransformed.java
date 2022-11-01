package vm.vmtrials.distanceDensityTrials;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeSet;
import vm.db.metricSpaceImpl.DBMetricSpaceImpl;
import vm.db.metricSpaceImpl.DBMetricSpacesStorage;
import vm.queryResults.GroundTruthEvaluator;
import vm.metricSpace.MetricDomainTools;
import vm.metricSpace.MetricSpacesStorageInterface;
import vm.metricSpace.dataToStringConvertors.impl.FloatVectorConvertor;
import vm.metricSpace.distance.DistanceFunction;
import vm.metricSpace.AbstractMetricSpace;

/**
 *
 * @author Vlada
 */
public class PrintDDOfNearNeighboursAndDatasetOrigAndTransformed {

    public static void main(String[] args) throws SQLException {
        String datasetName;
        datasetName = "decaf_1m";
        float distInterval = 2f;
        String transformedDatasetName;
        transformedDatasetName = "decaf_1m_PCA4";
        float transformedDistInterval = 1.0f;

        AbstractMetricSpace metricSpace = new DBMetricSpaceImpl<>();
        DistanceFunction distanceFunction = metricSpace.getDistanceFunctionForDataset(datasetName);
        MetricSpacesStorageInterface metricSpacesStorage = new DBMetricSpacesStorage<>(metricSpace, new FloatVectorConvertor());

//      getHistogramsForRandomAndNearestNeighbours
        int objCount = 100 * 1000;//100,000
        int distCount = 1000 * 1000;//1000,000
        int queriesCount = 1;//1000
        int k = 100;
        List<Object[]> idsOfRandomPairs = new ArrayList<>();
        List<Object[]> idsOfNNPairs = new ArrayList<>();
        SortedMap<Float, Float> ddRandomSample = PrintDDOfDataset.createDDOfRandomSample(metricSpace, metricSpacesStorage, distanceFunction, datasetName, objCount, distCount, distInterval, idsOfRandomPairs);
        SortedMap<Float, Float> ddOfNNSample = createDDOfNNSample(metricSpace, metricSpacesStorage, distanceFunction, datasetName, queriesCount, objCount, k, distInterval, idsOfNNPairs);
//      print
        printDDOfRandomAndNearNeighbours(datasetName, distInterval, ddRandomSample, ddOfNNSample);

//      find the same pairs in the transformed dataset and print corresponding distances
        List<Object> transformedObjects = metricSpacesStorage.getSampleOfDataset(transformedDatasetName, -1);
        Map<Object, Object> metricObjectsAsIdObjectMap = MetricDomainTools.getMetricObjectsAsIdObjectMap(metricSpace, transformedObjects);
        DistanceFunction distanceFunctionForTransformedDataset = metricSpace.getDistanceFunctionForDataset(transformedDatasetName);
        SortedMap<Float, Float> ddRandomSampleTransformed = evaluateDDForPairs(metricSpace, distanceFunctionForTransformedDataset, idsOfRandomPairs, metricObjectsAsIdObjectMap, transformedDistInterval);
        SortedMap<Float, Float> ddOfNNSampleTransformed = evaluateDDForPairs(metricSpace, distanceFunctionForTransformedDataset, idsOfNNPairs, metricObjectsAsIdObjectMap, transformedDistInterval);

//      print
        printDDOfRandomAndNearNeighbours(transformedDatasetName, transformedDistInterval, ddRandomSampleTransformed, ddOfNNSampleTransformed);
    }

    private static SortedMap<Float, Float> createDDOfNNSample(AbstractMetricSpace metricSpace, MetricSpacesStorageInterface metricSpacesStorage, DistanceFunction distanceFunction, String datasetName, int queryObjCount, int sampleCount, int k, float distInterval, List<Object[]> idsOfNNPairs) {
        List<Object> queryObjects = metricSpacesStorage.getSampleOfDataset(datasetName, queryObjCount);
        List<Object> metricObjects = metricSpacesStorage.getSampleOfDataset(datasetName, sampleCount + queryObjCount);
        for (int i = 0; i < queryObjCount; i++) {
            metricObjects.remove(0);
        }
        GroundTruthEvaluator gte = new GroundTruthEvaluator(metricSpace, distanceFunction, queryObjects, k, null);
        TreeSet<Map.Entry<Object, Float>>[] groundTruth = gte.processIteratorInParallel(metricObjects.iterator());
        List<Float> distances = new ArrayList<>();
        for (int i = 0; i < groundTruth.length; i++) {
            TreeSet<Map.Entry<Object, Float>> evaluatedQuery = groundTruth[i];
            Object queryObjID = metricSpace.getIDOfMetricObject(queryObjects.get(i));
            for (Map.Entry<Object, Float> entry : evaluatedQuery) {
                Object idOfMetricObject = entry.getKey();
                idsOfNNPairs.add(new Object[]{queryObjID, idOfMetricObject});
                distances.add(entry.getValue());
            }
        }
        return MetricDomainTools.createDistanceDensityPlot(distances, distInterval);
    }

    private static SortedMap<Float, Float> evaluateDDForPairs(AbstractMetricSpace metricSpace, DistanceFunction distanceFunction, List<Object[]> idsPairs, Map<Object, Object> metricObjects, float distInterval) {
        List<Float> distances = new ArrayList<>();
        for (Object[] idsPair : idsPairs) {
            Object o1 = metricObjects.get(idsPair[0]);
            Object o2 = metricObjects.get(idsPair[1]);
            o1 = metricSpace.getMetricObjectData(o1);
            o2 = metricSpace.getMetricObjectData(o2);
            float distance = distanceFunction.getDistance(o1, o2);
            distances.add(distance);
        }
        return MetricDomainTools.createDistanceDensityPlot(distances, distInterval);
    }

    private static void printDDOfRandomAndNearNeighbours(String datasetName, float distInterval, SortedMap<Float, Float> ddRandomSample, SortedMap<Float, Float> ddOfNNSample) {
        System.out.println(datasetName);
        System.out.println("Distance;Density of random sample; Density of distances to near neighbours");
        for (float dist = 0; true; dist += distInterval) {
            float rand = ddRandomSample.containsKey(dist) ? ddRandomSample.get(dist) : 0;
            float nn = ddOfNNSample.containsKey(dist) ? ddOfNNSample.get(dist) : 0;
            System.out.println(dist + ";" + rand + ";" + nn);
            if (dist > ddOfNNSample.lastKey() && dist > ddRandomSample.lastKey()) {
                break;
            }
        }
    }
}
