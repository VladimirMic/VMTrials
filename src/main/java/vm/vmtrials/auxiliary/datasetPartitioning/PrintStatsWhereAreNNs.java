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
import vm.fs.dataset.FSDatasetInstances;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.fs.store.partitioning.FSGRAPPLEPartitioningStorage;
import vm.fs.store.partitioning.FSVoronoiPartitioningStorage;
import vm.search.algorithm.impl.GRAPPLEPartitionsCandSetIdentifier;
import vm.search.algorithm.impl.VoronoiPartitionsCandSetIdentifier;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.ToolsSpaceDomain;
import vm.searchSpace.distance.AbstractDistanceFunction;

/**
 *
 * @author Vlada
 */
public class PrintStatsWhereAreNNs {

    private static final Logger LOG = Logger.getLogger(PrintStatsWhereAreNNs.class.getName());

    public static void main(String[] args) {
        Dataset[] datasets = new Dataset[]{
            new FSDatasetInstances.DeCAFDataset()
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

    private static <T> void run(Dataset<T> dataset) {
        int k = 10;
        int pivotCount = 256;
        FSVoronoiPartitioningStorage storage = new FSGRAPPLEPartitioningStorage();

        AbstractSearchSpace metricSpace = dataset.getSearchSpace();
        AbstractDistanceFunction df = dataset.getDistanceFunction();
        //
        Map<Comparable, T> pivots = ToolsSpaceDomain.getSearchObjectsAsIdDataMap(metricSpace, dataset.getPivots(pivotCount));
        Map<Comparable, T> queries = ToolsSpaceDomain.getSearchObjectsAsIdDataMap(metricSpace, dataset.getQueryObjects());
        Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> gt = new FSNearestNeighboursStorageImpl().getGroundTruthForDataset(dataset.getDatasetName(), dataset.getQuerySetName());
        //
        Map<Comparable, TreeSet<Comparable>> partitioning = storage.loadAsTreeSets(dataset.getDatasetName(), pivotCount);
        VoronoiPartitionsCandSetIdentifier identifier = new GRAPPLEPartitionsCandSetIdentifier(dataset, storage, pivotCount);
        for (int limitToFind = 10; limitToFind > 0; limitToFind--) {
            performForLimit(limitToFind, pivots, queries, gt, partitioning, storage, identifier, dataset.getDatasetName(), pivotCount, df, k);
        }
    }

    private static Map<Object, Boolean> createMapOfBooleaValues(TreeSet<Map.Entry<Comparable, Float>> gtQueryResult, int k, boolean value) {
        Map<Object, Boolean> ret = new HashMap<>();
        Iterator<Map.Entry<Comparable, Float>> it = gtQueryResult.iterator();
        for (int i = 0; it.hasNext() && i < k; i++) {
            Map.Entry<Comparable, Float> nn = it.next();
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

    private static <T> void performForLimit(int limitToFind, Map<Comparable, T> pivots, Map<Comparable, T> queries, Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> gt, Map<Comparable, TreeSet<Comparable>> voronoiPartitioning, FSVoronoiPartitioningStorage storage, VoronoiPartitionsCandSetIdentifier identifier, String datasetName, int pivotCount, AbstractDistanceFunction df, int k) {
        LOG.log(Level.INFO, "Evaluation for limit {0}", limitToFind);
        File file = storage.getFile(datasetName, pivotCount, false);
        String name = file.getName() + "whereAre" + limitToFind + "outOf" + k + "closest.csv";
        try {
            System.setOut(new PrintStream(new File(file.getParentFile(), name)));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PrintCellsSizes.class.getName()).log(Level.SEVERE, null, ex);
        }
        int counter = 0;
        for (Map.Entry<Comparable, TreeSet<Map.Entry<Comparable, Float>>> gtForQuery : gt.entrySet()) {
            counter++;
            Comparable qID = gtForQuery.getKey();
            Object qData = queries.get(qID);
            Object[] priorityQueue = identifier.evaluateKeyOrdering(df, pivots, qData);
            TreeSet<Map.Entry<Comparable, Float>> gtQueryResult = gtForQuery.getValue();
            // go cells by cell until all kNN are found
            int cellsCount = 0;
            int cellsTotalSize = 0;
            Map<Object, Boolean> mapOfCoveredNNs = createMapOfBooleaValues(gtQueryResult, k, false);
            for (Object cellID : priorityQueue) {
                TreeSet<Comparable> cell = voronoiPartitioning.get(cellID);
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
