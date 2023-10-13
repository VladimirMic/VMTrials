package vm.vmtrials.checking.laionAuxiliary;



import java.util.Iterator;
import java.util.Map;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;

/**
 *
 * @author Vlada
 */
public class H5FileAsKeyValueStorage {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstanceSingularizator.LAION_100M_Dataset(true);
        Map map = dataset.getKeyValueStorage();
        Iterator it = dataset.getMetricObjectsFromDataset();
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        while (it.hasNext()) {
            Object next = it.next();
            Object id = metricSpace.getIDOfMetricObject(next);
            Object check = map.get(id);
            String s = "";
        }
    }
}
