/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.checking;

import java.util.Iterator;
import java.util.Map;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.metricSpaceImpl.FSMetricSpaceImpl;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;

/**
 *
 * @author Vlada
 */
public class PrintIDs {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstanceSingularizator.LAION_10M_PCA256Dataset();
        run(dataset, true);
    }

    private static <T> void run(Dataset<T> dataset, boolean fromKeyValueStorage) {
        Iterator it;
        if (fromKeyValueStorage) {
            it = dataset.getMetricObjectsFromDatasetKeyValueStorage();
        } else {
            it = dataset.getMetricObjectsFromDataset();
        }
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        Comparable oIdPrev = null;
        for (int i = 0; it.hasNext(); i++) {
            Object o = it.next();
            Comparable id = metricSpace.getIDOfMetricObject(o);
            System.out.print(i + ": " + id);
            if (oIdPrev == null) {
                System.out.println();
            } else {
                System.out.println(";" + id.compareTo(oIdPrev));
            }
            oIdPrev = id;
        }
    }
}
