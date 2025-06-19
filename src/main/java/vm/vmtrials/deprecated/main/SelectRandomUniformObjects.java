package vm.vmtrials.deprecated.main;

import java.util.List;
import vm.datatools.Tools;
import vm.fs.dataset.FSDatasetInstances;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.AbstractSearchSpacesStorage;
import vm.searchSpace.Dataset;
import vm.searchSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
@Deprecated // use FSSelectRandomQueryObjectsAndPivotsFromDatasetMain instead
public class SelectRandomUniformObjects {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstances.Yahoo100M_Dataset();
        int count = 5000;
        List list = Tools.randomUniform(dataset.getSearchObjectsFromDataset(), 102050000, count);
        DistanceFunctionInterface df = dataset.getDistanceFunction();
        AbstractSearchSpace metricSpace = dataset.getSearchSpace();
        for (int i = 0; i < list.size() - 1; i++) {
            Object o1 = list.get(i);
            Object o1Data = metricSpace.getDataOfObject(o1);
            for (int j = i + 1; j < list.size(); j++) {
                Object o2 = list.get(j);
                Object o2Data = metricSpace.getDataOfObject(o2);
                float distance = df.getDistance(o1Data, o2Data);
                if (distance == 0f) {
                    System.out.println("Zero distance between pivots " + i + " and " + j);
                }
            }
        }
        AbstractSearchSpacesStorage storage = dataset.getSearchSpacesStorage();
        storage.storePivots(list, dataset.getDatasetName() + "_" + count + "pivots");
        storage.storeObjectsToDataset(list.iterator(), -1, dataset.getDatasetName() + "_random_sample_" + count + ".gz");
    }

}
