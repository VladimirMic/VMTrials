package vm.vmtrials.tmp;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import vm.fs.dataset.FSDatasetInstances;
import vm.fs.store.partitioning.FSVoronoiPartitioningStorage;
import vm.searchSpace.Dataset;
import vm.searchSpace.datasetPartitioning.StorageDatasetPartitionsInterface;

/**
 *
 * @author Vlada
 */
public class ChangePivotIDsInVoronoiCells {

    public static void main(String[] args) {
        Dataset[] datasets = new Dataset[]{
            new FSDatasetInstances.LAION_100M_Dataset(true)
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
        Map<Comparable, Collection<Comparable>> load = storage.load(dataset.getDatasetName(), length);
        Map<Comparable, Collection<Comparable>> updated = new HashMap<>();
        Set<Map.Entry<Comparable, Collection<Comparable>>> entrySet = load.entrySet();
        for (Map.Entry<Comparable, Collection<Comparable>> entry : entrySet) {
            String key = entry.getKey().toString();
            key = key.substring(1);
            Collection<Comparable> value = entry.getValue();
            updated.put(key, value);
        }
        storage.store(updated, "N_" + dataset.getDatasetName(), null, length);
    }
}
