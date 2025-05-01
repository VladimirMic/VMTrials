/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.auxiliary;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.fs.dataset.FSDatasetInstances;
import vm.fs.store.partitioning.FSVoronoiPartitioningStorage;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;

/**
 *
 * @author Vlada
 */
public class FilterVoronoiPartitioning {

    private static final Logger LOG = Logger.getLogger(FilterVoronoiPartitioning.class.getName());

    public static void main(String[] args) {
        Dataset partitioninedDataset = new FSDatasetInstances.LAION_100M_Dataset(true);
        Dataset targetDataset = new FSDatasetInstances.LAION_10M_Dataset(true);
        int pivotCount = 20000;
        String newDatasetName = targetDataset.getDatasetName();

        Set<Comparable> idsToPreserve = loadKeyOfObjects(targetDataset);

        FSVoronoiPartitioningStorage voronoiPartitioningStorage = new FSVoronoiPartitioningStorage();
        Map<Comparable, TreeSet<Comparable>> vp = voronoiPartitioningStorage.loadAsTreeSets(partitioninedDataset.getDatasetName(), pivotCount);
        Map<Comparable, Collection<Comparable>> ret = new HashMap<>();
        for (Map.Entry<Comparable, TreeSet<Comparable>> cell : vp.entrySet()) {
            Comparable pivotID = cell.getKey();
            TreeSet<Comparable> newCell = new TreeSet<>();
            TreeSet<Comparable> idsInCell = cell.getValue();
            for (Comparable id : idsInCell) {
                if (idsToPreserve.contains(id)) {
                    newCell.add(id);
                }
            }
            LOG.log(Level.INFO, "The cell with the pivot {0} is going to contain {1} objects out of {2} in the original cell", new Object[]{pivotID, newCell.size(), idsInCell.size()});
            ret.put(pivotID, newCell);
        }
        voronoiPartitioningStorage.store(ret, newDatasetName, pivotCount);
    }

    private static Set<Comparable> loadKeyOfObjects(Dataset dataset) {
        Iterator it = dataset.getMetricObjectsFromDataset();
        return ToolsMetricDomain.getIDs(it, dataset.getMetricSpace());
    }
}
