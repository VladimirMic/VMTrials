package vm.vmtrials.checking.laionAuxiliary;

import java.util.Iterator;
import java.util.Map;
import vm.fs.dataset.FSDatasetInstances;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;

/**
 *
 * @author Vlada
 */
public class H5FileAsKeyValueStorage {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstances.LAION_100M_Dataset(true);
        Map map = dataset.getKeyValueStorage();
        Iterator it = dataset.getMetricObjectsFromDataset();
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        while (it.hasNext()) {
            Object next = it.next();
            Comparable id = metricSpace.getIDOfMetricObject(next);
            Object check = map.get(id);
            String s = "";
        }
    }
}
