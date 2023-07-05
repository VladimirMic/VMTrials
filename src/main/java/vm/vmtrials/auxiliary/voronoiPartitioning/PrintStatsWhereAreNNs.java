package vm.vmtrials.auxiliary.voronoiPartitioning;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.fs.store.voronoiPartitioning.FSVoronoiPartitioningStorage;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author Vlada
 */
public class PrintStatsWhereAreNNs {

    private static final Logger LOG = Logger.getLogger(PrintStatsWhereAreNNs.class.getName());

    public static void main(String[] args) {
        Dataset[] datasets = new Dataset[]{
            //                        new FSDatasetInstanceSingularizator.LAION_100k_Dataset(),
            //            new FSDatasetInstanceSingularizator.LAION_300k_Dataset(),
                        new FSDatasetInstanceSingularizator.LAION_10M_Dataset(),
//                        new FSDatasetInstanceSingularizator.LAION_30M_Dataset(),
//            new FSDatasetInstanceSingularizator.LAION_100M_Dataset()
        };
        for (Dataset dataset : datasets) {
            run(dataset);
        }
    }

    private static void run(Dataset dataset) {
        int k = 10;
        int pivotCount = 20000;
        FSVoronoiPartitioningStorage storage = new FSVoronoiPartitioningStorage();

        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        DistanceFunctionInterface df = dataset.getDistanceFunction();
        //
        Map<Object, Object> pivots = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, dataset.getPivots(pivotCount), true);
        Map<Object, Object> queries = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, dataset.getMetricQueryObjects(), true);
        Map<String, TreeSet<Map.Entry<Object, Float>>> gt = new FSNearestNeighboursStorageImpl().getGroundTruthForDataset(dataset.getDatasetName(), dataset.getQuerySetName());
        //
        Map<Object, TreeSet<Object>> voronoiPartitioning = storage.load(dataset.getDatasetName(), pivotCount);
        for (int limitToFind = 10; limitToFind > 0; limitToFind--) {
            performForLimit(limitToFind, pivots, queries, gt, voronoiPartitioning, storage, dataset.getDatasetName(), pivotCount, df, k);
        }
    }

    private static Map<Object, Boolean> createMapOfBooleaValues(TreeSet<Map.Entry<Object, Float>> gtQueryResult, int k, boolean value) {
        Map<Object, Boolean> ret = new HashMap<>();
        Iterator<Map.Entry<Object, Float>> it = gtQueryResult.iterator();
        for (int i = 0; it.hasNext() && i < k; i++) {
            Map.Entry<Object, Float> nn = it.next();
            ret.put(nn.getKey(), value);
        }
        return ret;
    }

    private static boolean trueAtLeast(Map<Object, Boolean> mapOfCoveredNNs, int limit) {
        int count = 0;
        for (Map.Entry<Object, Boolean> entry : mapOfCoveredNNs.entrySet()) {
            if (entry.getValue()) {
                count++;
            }
        }
        return count >= limit;
    }

    private static void performForLimit(int limitToFind, Map<Object, Object> pivots, Map<Object, Object> queries, Map<String, TreeSet<Map.Entry<Object, Float>>> gt, Map<Object, TreeSet<Object>> voronoiPartitioning, FSVoronoiPartitioningStorage storage, String datasetName, int pivotCount, DistanceFunctionInterface df, int k) {
        LOG.log(Level.INFO, "Evaluation for limit {0}", limitToFind);
        File file = storage.getFileForFSVoronoiStorage(datasetName, pivotCount, false);
        String name = file.getName() + "whereAre" + limitToFind + "outOf" + k + "closest.csv";
        try {
            System.setOut(new PrintStream(new File(file.getParentFile(), name)));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PrintCellsSizes.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (Map.Entry<String, TreeSet<Map.Entry<Object, Float>>> gtForQuery : gt.entrySet()) {
            String qID = gtForQuery.getKey();
            Object qData = queries.get(qID);
            Object[] pivotPermutation = ToolsMetricDomain.getPivotIDsPermutation(df, pivots, qData, -1);
            TreeSet<Map.Entry<Object, Float>> gtQueryResult = gtForQuery.getValue();
            // go cells by cell until all kNN are found
            int cellsCount = 0;
            int cellsTotalSize = 0;
            Map<Object, Boolean> mapOfCoveredNNs = createMapOfBooleaValues(gtQueryResult, k, false);
            for (Object idOfClosestPivotToQ : pivotPermutation) {
                TreeSet<Object> cell = voronoiPartitioning.get(idOfClosestPivotToQ);
                if (cell == null) {
                    LOG.log(Level.WARNING, "Empty Voronoi cell for pivot {0}", idOfClosestPivotToQ);
                    continue;
                }
                cellsCount++;
                cellsTotalSize += cell.size();
                Map<Object, Boolean> copyMapOfCoveredNNs = new HashMap<>(mapOfCoveredNNs);
                for (Map.Entry<Object, Boolean> nn : mapOfCoveredNNs.entrySet()) {
                    if (!nn.getValue() && cell.contains(nn.getKey())) {
                        copyMapOfCoveredNNs.put(nn.getKey(), true);
                    }
                }
                mapOfCoveredNNs = copyMapOfCoveredNNs;
                if (trueAtLeast(mapOfCoveredNNs, limitToFind)) {
                    break;
                }
            }
            System.out.print("qID;" + qID + ";cellsCount;" + cellsCount + ";cellsTotalSize;" + cellsTotalSize + ";pivotPermutation;");

            for (int i = 0; i < cellsCount; i++) {
                System.out.print(pivotPermutation[i].toString() + ";");
            }
            System.out.println();
        }
    }

}
