/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.auxiliary.datasetPartitioning;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;
import vm.fs.dataset.FSDatasetInstances;
import vm.fs.store.partitioning.FSVoronoiPartitioningStorage;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.ToolsSpaceDomain;
import vm.searchSpace.distance.AbstractDistanceFunction;

/**
 *
 * @author xmic
 */
public class PrintRealCandSetSizes {

    private static final Logger LOG = Logger.getLogger(PrintRealCandSetSizes.class.getName());

    public static void main(String[] args) {
        Dataset[] datasets = new Dataset[]{
            new FSDatasetInstances.LAION_100M_Dataset(true)
        };
        for (Dataset dataset : datasets) {
            run(dataset);
        }
    }

    private static <T> void run(Dataset<T> dataset) {
        int k = 1000000;
        int pivotCount = 20000;
        FSVoronoiPartitioningStorage storage = new FSVoronoiPartitioningStorage();
        AbstractSearchSpace metricSpace = dataset.getSearchSpace();
        AbstractDistanceFunction df = dataset.getDistanceFunction();
        Map<Comparable, T> queries = ToolsSpaceDomain.getSearchObjectsAsIdDataMap(metricSpace, dataset.getQueryObjects());
        Map<Comparable, T> pivots = ToolsSpaceDomain.getSearchObjectsAsIdDataMap(metricSpace, dataset.getPivots(-1));

        Map<Comparable, Collection<Comparable>> voronoiPartitioning = storage.load(dataset.getDatasetName(), pivotCount);
        for (Map.Entry<Comparable, T> fullQuery : queries.entrySet()) {
            printNumberOfCandidates(k, fullQuery, voronoiPartitioning, df, pivots);
        }
    }

    private static <T> void printNumberOfCandidates(int k, Map.Entry<Comparable, T> fullQuery, Map<Comparable, Collection<Comparable>> voronoiPartitioning, AbstractDistanceFunction df, Map pivots) {
        String qID = (String) fullQuery.getKey();
        Object qData = fullQuery.getValue();
        Comparable[] pivotPermutation = ToolsSpaceDomain.getPivotIDsPermutation(df, pivots, qData, -1, null);
        // go cells by cell until all kNN are found
        int cellsCount = 0;
        int cellsTotalSize = 0;
        int cellSizeWithNext = 0;
        for (Comparable idOfClosestPivotToQ : pivotPermutation) {
            Collection<Comparable> cell = voronoiPartitioning.get(idOfClosestPivotToQ);
            if (cell == null) {
//                LOG.log(Level.WARNING, "Empty Voronoi cell for pivot {0}", idOfClosestPivotToQ);
                continue;
            }
            cellsCount++;
            cellsTotalSize = cellSizeWithNext;
            cellSizeWithNext += cell.size();
            if (cellSizeWithNext > k) {
                break;
            }
        }
        System.out.println("qID;" + qID + ";cellsCount;" + cellsCount + ";cellsTotalSize;" + cellsTotalSize);
    }

}
