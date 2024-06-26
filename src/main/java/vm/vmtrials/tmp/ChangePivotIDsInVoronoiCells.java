package vm.vmtrials.tmp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.partitioning.FSVoronoiPartitioningStorage;
import vm.metricSpace.Dataset;
import vm.metricSpace.datasetPartitioning.StorageDatasetPartitionsInterface;

/**
 *
 * @author Vlada
 */
public class ChangePivotIDsInVoronoiCells {

    public static void main(String[] args) {
        Dataset[] datasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_100M_Dataset(true)
        };
        int[] lengths = new int[]{256, 512, 768, 1024, 1536, 2048};
        for (Dataset dataset : datasets) {
            for (int length : lengths) {
                run(dataset, length);
            }
        }
    }

    private static void run(Dataset dataset, int length) {
        StorageDatasetPartitionsInterface storage = new FSVoronoiPartitioningStorage();
        Map<Comparable, TreeSet<Comparable>> load = storage.load(dataset.getDatasetName(), length);
        Map<Comparable, SortedSet<Comparable>> updated = new HashMap<>();
        Set<Map.Entry<Comparable, TreeSet<Comparable>>> entrySet = load.entrySet();
        for (Map.Entry<Comparable, TreeSet<Comparable>> entry : entrySet) {
            String key = entry.getKey().toString();
            key = key.substring(1);
            TreeSet<Comparable> value = entry.getValue();
            updated.put(key, value);
        }
        storage.store(updated, "N_" + dataset.getDatasetName(), length);
    }
}
