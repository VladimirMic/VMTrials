package vm.vmtrials.checking.laionAuxiliary;

import java.util.Iterator;
import java.util.Map;
import vm.fs.dataset.FSDatasetInstances;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;

/**
 *
 * @author Vlada
 */
public class H5FileAsKeyValueStorage {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstances.LAION_100M_Dataset(true);
        Map map = dataset.getKeyValueStorage();
        Iterator it = dataset.getSearchObjectsFromDataset();
        AbstractSearchSpace metricSpace = dataset.getSearchSpace();
        while (it.hasNext()) {
            Object next = it.next();
            Comparable id = metricSpace.getIDOfObject(next);
            Object check = map.get(id);
            String s = "";
        }
    }
}
