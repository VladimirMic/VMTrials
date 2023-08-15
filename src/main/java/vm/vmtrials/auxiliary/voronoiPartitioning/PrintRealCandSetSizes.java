/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.auxiliary.voronoiPartitioning;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
 * @author xmic
 */
public class PrintRealCandSetSizes {

    private static final Logger LOG = Logger.getLogger(PrintRealCandSetSizes.class.getName());

    public static void main(String[] args) {
        Dataset[] datasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_100M_Dataset()
        };
        for (Dataset dataset : datasets) {
            run(dataset);
        }
    }

    private static void run(Dataset dataset) {
        int k = 1000000;
        int pivotCount = 20000;
        FSVoronoiPartitioningStorage storage = new FSVoronoiPartitioningStorage();
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        DistanceFunctionInterface df = dataset.getDistanceFunction();
        Map<Object, Object> queries = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, dataset.getMetricQueryObjects(), true);
        Map pivots = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, dataset.getPivots(-1), true);

        Map<Object, TreeSet<Object>> voronoiPartitioning = storage.load(dataset.getDatasetName(), pivotCount);
        for (Map.Entry<Object, Object> fullQuery : queries.entrySet()) {
            printNumberOfCandidates(k, fullQuery, voronoiPartitioning, dataset.getDatasetName(), df, pivots);
        }
    }

    private static void printNumberOfCandidates(int k, Map.Entry<Object, Object> fullQuery, Map<Object, TreeSet<Object>> voronoiPartitioning, String datasetName, DistanceFunctionInterface df, Map pivots) {
        String qID = (String) fullQuery.getKey();
        Object qData = fullQuery.getValue();
        Object[] pivotPermutation = ToolsMetricDomain.getPivotIDsPermutation(df, pivots, qData, -1, null);
        // go cells by cell until all kNN are found
        int cellsCount = 0;
        int cellsTotalSize = 0;
        int cellSizeWithNext = 0;
        for (Object idOfClosestPivotToQ : pivotPermutation) {
            TreeSet<Object> cell = voronoiPartitioning.get(idOfClosestPivotToQ);
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
