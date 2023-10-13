package vm.vmtrials.auxiliary;

import java.util.List;
import vm.datatools.Tools;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.AbstractMetricSpacesStorage;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
public class SelectRandomUniform {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstanceSingularizator.LAION_100M_Dataset(true);
        int count = 5000;
        List list = Tools.randomUniform(dataset.getMetricObjectsFromDataset(), 102050000, count);
        DistanceFunctionInterface df = dataset.getDistanceFunction();
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        for (int i = 0; i < list.size() - 1; i++) {
            Object o1 = list.get(i);
            Object o1Data = metricSpace.getDataOfMetricObject(o1);
            for (int j = i + 1; j < list.size(); j++) {
                Object o2 = list.get(j);
                Object o2Data = metricSpace.getDataOfMetricObject(o2);
                float distance = df.getDistance(o1Data, o2Data);
                if (distance == 0f) {
                    System.out.println("Zero distance between pivots " + i + " and " + j);
                }
            }
        }
        AbstractMetricSpacesStorage storage = dataset.getMetricSpacesStorage();
        storage.storePivots(list, dataset.getDatasetName() + "_" + count + "pivots");
        //storage.storeObjectsToDataset(list.iterator(), -1, dataset.getDatasetName() + "_random_sample_" + count + ".gz");
    }

}
