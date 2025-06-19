package vm.vmtrials.tmp;

import java.util.ArrayList;
import java.util.List;
import vm.fs.dataset.FSDatasetInstances;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.AbstractSearchSpacesStorage;
import vm.searchSpace.Dataset;

/**
 *
 * @author Vlada
 */
public class RemoveSelectedObjectFromPivots {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstances.LAION_100M_Dataset(true);
        AbstractSearchSpace metricSpace = dataset.getSearchSpace();
        List pivots = dataset.getPivots(-1);
        List newPivots = new ArrayList();
        for (Object pivot : pivots) {
            Comparable pID = metricSpace.getIDOfObject(pivot);
            if (!pID.toString().equals("P96747670")) {
                newPivots.add(pivot);
            } else {
                System.out.println("Skipped pivot " + pID.toString());
            }
        }
        AbstractSearchSpacesStorage storage = dataset.getSearchSpacesStorage();
        storage.storePivots(newPivots, "laion2B-en-clip768v2-n=100M.h5_512pivots.gz_new");
    }
}
