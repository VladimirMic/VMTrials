package vm.vmtrials.tmp;

import java.util.ArrayList;
import java.util.List;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.MetricSpacesStorageInterface;

/**
 *
 * @author Vlada
 */
public class RemoveSelectedObjectFromPivots {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstanceSingularizator.LAION_100M_Dataset();
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        List pivots = dataset.getPivots(-1);
        List newPivots = new ArrayList();
        for (Object pivot : pivots) {
            Object pID = metricSpace.getIDOfMetricObject(pivot);
            if (!pID.toString().equals("P96747670")) {
                newPivots.add(pivot);
            } else {
                System.out.println("Skipped pivot " + pID.toString());
            }
        }
        MetricSpacesStorageInterface storage = dataset.getMetricSpacesStorage();
        storage.storePivots(newPivots, "laion2B-en-clip768v2-n=100M.h5_512pivots.gz_new");
    }
}
