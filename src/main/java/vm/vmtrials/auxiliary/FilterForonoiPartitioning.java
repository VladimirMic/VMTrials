/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.auxiliary;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.voronoiPartitioning.FSVoronoiPartitioningStorage;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;

/**
 *
 * @author Vlada
 */
public class FilterForonoiPartitioning {

    private static final Logger LOG = Logger.getLogger(FilterForonoiPartitioning.class.getName());

    public static void main(String[] args) {
        Dataset partitioninedDataset = new FSDatasetInstanceSingularizator.LAION_100M_Dataset();
        Dataset targetDataset = new FSDatasetInstanceSingularizator.LAION_10M_Dataset();
        int pivotCount = 20000;
        String newDatasetName = targetDataset.getDatasetName();

        Set<Object> idsToPreserve = loadKeyOfObjects(targetDataset);

        FSVoronoiPartitioningStorage voronoiPartitioningStorage = new FSVoronoiPartitioningStorage();
        Map<Object, TreeSet<Object>> vp = voronoiPartitioningStorage.load(partitioninedDataset.getDatasetName(), pivotCount);
        Map<Object, SortedSet<Object>> ret = new HashMap<>();
        for (Map.Entry<Object, TreeSet<Object>> cell : vp.entrySet()) {
            Object pivotID = cell.getKey();
            TreeSet<Object> newCell = new TreeSet<>();
            TreeSet<Object> idsInCell = cell.getValue();
            for (Object id : idsInCell) {
                if (idsToPreserve.contains(id)) {
                    newCell.add(id);
                }
            }
            LOG.log(Level.INFO, "The cell with the pivot {0} is going to contain {1} objects out of {2} in the original cell", new Object[]{pivotID, newCell.size(), idsInCell.size()});
            ret.put(pivotID, newCell);
        }
        voronoiPartitioningStorage.store(ret, newDatasetName, pivotCount);
    }

    private static Set<Object> loadKeyOfObjects(Dataset dataset) {
        Iterator it = dataset.getMetricObjectsFromDataset();
        return ToolsMetricDomain.getIDs(it, dataset.getMetricSpace());
    }
}
