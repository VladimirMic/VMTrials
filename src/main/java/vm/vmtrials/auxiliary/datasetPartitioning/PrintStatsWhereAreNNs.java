package vm.vmtrials.auxiliary.datasetPartitioning;

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
import vm.fs.store.partitioning.FSGRAPPLEPartitioningStorage;
import vm.fs.store.partitioning.FSVoronoiPartitioningStorage;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.search.algorithm.impl.GRAPPLEPartitionsCandSetIdentifier;
import vm.search.algorithm.impl.VoronoiPartitionsCandSetIdentifier;

/**
 *
 * @author Vlada
 */
public class PrintStatsWhereAreNNs {

    private static final Logger LOG = Logger.getLogger(PrintStatsWhereAreNNs.class.getName());

    public static void main(String[] args) {
        Dataset[] datasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.DeCAFDataset()
//            new FSDatasetInstanceSingularizator.MPEG7dataset(),
//            new FSDatasetInstanceSingularizator.SIFTdataset()
//                        new FSDatasetInstanceSingularizator.LAION_100k_Dataset(),
        //            new FSDatasetInstanceSingularizator.LAION_300k_Dataset(),
        //            new FSDatasetInstanceSingularizator.LAION_10M_Dataset(),
        //                        new FSDatasetInstanceSingularizator.LAION_30M_Dataset(),
        //            new FSDatasetInstanceSingularizator.LAION_100M_Dataset()
        };
        for (Dataset dataset : datasets) {
            run(dataset);
        }
    }

    private static void run(Dataset dataset) {
        int k = 10;
        int pivotCount = 256;
        FSVoronoiPartitioningStorage storage = new FSGRAPPLEPartitioningStorage();

        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        DistanceFunctionInterface df = dataset.getDistanceFunction();
        //
        Map<Object, Object> pivots = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, dataset.getPivots(pivotCount), true);
        Map<Object, Object> queries = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, dataset.getQueryObjects(), true);
        Map<String, TreeSet<Map.Entry<Object, Float>>> gt = new FSNearestNeighboursStorageImpl().getGroundTruthForDataset(dataset.getDatasetName(), dataset.getQuerySetName());
        //
        Map<Object, TreeSet<Object>> partitioning = storage.load(dataset.getDatasetName(), pivotCount);
        VoronoiPartitionsCandSetIdentifier identifier = new GRAPPLEPartitionsCandSetIdentifier(dataset, storage, pivotCount);
        for (int limitToFind = 10; limitToFind > 0; limitToFind--) {
            performForLimit(limitToFind, pivots, queries, gt, partitioning, storage, identifier, dataset.getDatasetName(), pivotCount, df, k);
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

    private static void performForLimit(int limitToFind, Map<Object, Object> pivots, Map<Object, Object> queries, Map<String, TreeSet<Map.Entry<Object, Float>>> gt, Map<Object, TreeSet<Object>> voronoiPartitioning, FSVoronoiPartitioningStorage storage, VoronoiPartitionsCandSetIdentifier identifier, String datasetName, int pivotCount, DistanceFunctionInterface df, int k) {
        LOG.log(Level.INFO, "Evaluation for limit {0}", limitToFind);
        File file = storage.getFile(datasetName, pivotCount, false);
        String name = file.getName() + "whereAre" + limitToFind + "outOf" + k + "closest.csv";
        try {
            System.setOut(new PrintStream(new File(file.getParentFile(), name)));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PrintCellsSizes.class.getName()).log(Level.SEVERE, null, ex);
        }
        int counter = 0;
        for (Map.Entry<String, TreeSet<Map.Entry<Object, Float>>> gtForQuery : gt.entrySet()) {
            counter++;
            String qID = gtForQuery.getKey();
            Object qData = queries.get(qID);
            Object[] priorityQueue = identifier.evaluateKeyOrdering(df, pivots, qData);
            TreeSet<Map.Entry<Object, Float>> gtQueryResult = gtForQuery.getValue();
            // go cells by cell until all kNN are found
            int cellsCount = 0;
            int cellsTotalSize = 0;
            Map<Object, Boolean> mapOfCoveredNNs = createMapOfBooleaValues(gtQueryResult, k, false);
            for (Object cellID : priorityQueue) {
                TreeSet<Object> cell = voronoiPartitioning.get(cellID);
                if (cell == null) {
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
            System.out.println(counter + ";qID;" + qID + ";cellsCount;" + cellsCount + ";cellsTotalSize;" + cellsTotalSize);
            System.err.println(counter + ";qID;" + qID + ";cellsCount;" + cellsCount + ";cellsTotalSize;" + cellsTotalSize);
        }
    }

}
