/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.checking;

import java.util.Iterator;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;

/**
 *
 * @author Vlada
 */
public class PrintIDs {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstanceSingularizator.DeCAF100M_PCA256Dataset();
        Iterator it = dataset.getMetricObjectsFromDataset();
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        String oIdPrev = null;
        for (int i = 0; i < 1000 && it.hasNext(); i++) {
            Object o = it.next();
            String id = metricSpace.getIDOfMetricObject(o).toString();
            System.out.print(id);
            if (oIdPrev == null) {
                System.out.println();
            } else {
                System.out.println(";" + id.compareTo(oIdPrev));
            }
            oIdPrev = id;
        }
    }
}
