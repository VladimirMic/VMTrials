/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.tmp;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;

/**
 *
 * @author Vlada
 */
public class CheckPresenceOfQueries {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstanceSingularizator.DeCAF20M_PCA256Dataset();
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        List queries = dataset.getQueryObjects();

        Iterator it = dataset.getMetricObjectsFromDataset();

        Set qIDs = ToolsMetricDomain.getIDs(queries.iterator(), metricSpace);
        for (int i = 0; it.hasNext(); i++) {
            Object o = it.next();
            Object oID = metricSpace.getIDOfMetricObject(o);
            if (qIDs.contains(oID)) {
                Logger.getLogger(CheckPresenceOfQueries.class.getName()).log(Level.WARNING, "The set contains ID of the query {0}", oID.toString());
            }
            if (i % 100000 == 1) {
                Logger.getLogger(CheckPresenceOfQueries.class.getName()).log(Level.INFO, "Checked {0} objects from the dataset", (i - 1));
            }
        }
    }
}
