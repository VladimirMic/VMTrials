/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.checking;

import java.util.Iterator;
import vm.fs.dataset.FSDatasetInstances;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;

/**
 *
 * @author Vlada
 */
public class PrintIDs {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstances.LAION_10M_PCA256Dataset();
        run(dataset, true);
    }

    private static <T> void run(Dataset<T> dataset, boolean fromKeyValueStorage) {
        Iterator it;
        if (fromKeyValueStorage) {
            it = dataset.getSearchObjectsFromDatasetKeyValueStorage();
        } else {
            it = dataset.getSearchObjectsFromDataset();
        }
        AbstractSearchSpace metricSpace = dataset.getSearchSpace();
        Comparable oIdPrev = null;
        for (int i = 0; it.hasNext(); i++) {
            Object o = it.next();
            Comparable id = metricSpace.getIDOfObject(o);
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
