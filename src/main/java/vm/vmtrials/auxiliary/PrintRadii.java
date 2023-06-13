package vm.vmtrials.auxiliary;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.metricSpace.Dataset;
import vm.queryResults.QueryNearestNeighboursStoreInterface;

/**
 *
 * @author xmic
 */
public class PrintRadii {

    public static void main(String[] args) {
        int k = 30;
        Dataset groundTruthDataset = new FSDatasetInstanceSingularizator.DeCAFDataset();
        QueryNearestNeighboursStoreInterface resultsStorage = new FSNearestNeighboursStorageImpl();
        Map<String, TreeSet<Map.Entry<Object, Float>>> groundTruthForDataset = resultsStorage.getGroundTruthForDataset(groundTruthDataset.getDatasetName(), groundTruthDataset.getDatasetName());

        Set<String> queryIDs = groundTruthForDataset.keySet();
        for (String queryID : queryIDs) {
            Iterator<Map.Entry<Object, Float>> nns = groundTruthForDataset.get(queryID).iterator();
            for (int i = 0; i < k - 1; i++) {
                nns.next();
            }
            Float range = nns.next().getValue();
            System.out.println(queryID + ";" + range);
        }

    }
}
