package vm.vmtrials.ptolemaios;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.metricspace.distance.precomputedDistances.PrecomputedDistancesLoaderImpl;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.fs.store.queryResults.FSQueryExecutionStatsStoreImpl;
import vm.metricspace.AbstractMetricSpace;
import vm.metricspace.Dataset;
import vm.metricspace.distance.bounding.twopivots.TwoPivotsFiltering;
import vm.search.impl.KNNSearchWithTwoPivotFiltering;
import vm.metricspace.distance.DistanceFunctionInterface;
import vm.metricspace.distance.PrecomputedDistancesLoader;
import vm.metricspace.distance.bounding.twopivots.impl.PtolemaiosFiltering;

/**
 *
 * @author Vlada
 */
public class MainKNNQueriesWithFiltering {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstanceSingularizator.DeCAFDataset();
        try {
            Thread.sleep(1000 * 60 * 60 * 3);
        } catch (InterruptedException ex) {
            Logger.getLogger(MainKNNQueriesWithFiltering.class.getName()).log(Level.SEVERE, null, ex);
        }
//        String pathToHulls = "h:\\Skola\\2022\\Ptolemaions_limited\\EFgetBD\\Hulls\\" + dataset.getDatasetName() + "___tetrahedrons_100000__ratio_of_outliers_to_cut_0.01__pivot_pairs_128.csv";
        int k = 100;

        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        DistanceFunctionInterface df = dataset.getDistanceFunction();
        int pivotCount = 512;
        PrecomputedDistancesLoader pd = new PrecomputedDistancesLoaderImpl();
        List<String> columnHeaders = new ArrayList<>();
        List<String> rowHeaders = new ArrayList<>();;// kontrolovat!
        float[][] poDists = pd.loadPrecomPivotsToObjectsDists(dataset.getDatasetName(), dataset.getDatasetName(), pivotCount, columnHeaders, rowHeaders);
        List queries = dataset.getMetricQueryObjectsForTheSameDataset();
        List pivots = dataset.getPivotsForTheSameDataset(pivotCount);
        checkPivotsOrder(pivots, columnHeaders);
        float[][] pivotPivotDists = metricSpace.getDistanceMap(df, pivots, pivots);

//        TwoPivotsFiltering filter = new PtolemaiosFilteringWithLimitedAngles(pathToHulls);
        TwoPivotsFiltering filter = new PtolemaiosFiltering();
        KNNSearchWithTwoPivotFiltering alg = new KNNSearchWithTwoPivotFiltering(metricSpace, filter, pivots, poDists, rowHeaders, pivotPivotDists, df);
        String resultsName = "Ptolemaios_01";
        List<Object> queryObjectsIDs = new ArrayList<>();
        TreeSet<Map.Entry<Object, Float>>[] results = new TreeSet[queries.size()];
        FSQueryExecutionStatsStoreImpl statsStorage = new FSQueryExecutionStatsStoreImpl(dataset.getDatasetName(), dataset.getDatasetName(), k, dataset.getDatasetName(), dataset.getDatasetName(), resultsName, null);
        for (int i = 0; i < queries.size(); i++) {
            System.err.println("query: " + i);
            Object query = queries.get(i);
            Object qId = metricSpace.getIDOfMetricObject(query);
            queryObjectsIDs.add(qId);
            TreeSet<Map.Entry<Object, Float>> result = alg.completeKnnSearch(metricSpace, query, k, dataset.getMetricObjectsFromDataset());
            results[i] = result;
            Integer distCompsOfLastExecutedQuery = alg.getDistCompsOfLastExecutedQuery();
            long timeOfLastExecutedQuery = alg.getTimeOfLastExecutedQuery();
            statsStorage.storeStatsForQuery(qId, distCompsOfLastExecutedQuery, timeOfLastExecutedQuery);
        }
        FSNearestNeighboursStorageImpl resultsStorage = new FSNearestNeighboursStorageImpl();
        resultsStorage.storeQueryResults(queryObjectsIDs, results, dataset.getDatasetName(), dataset.getDatasetName(), resultsName);
        statsStorage.saveFile();
    }

    private static void checkPivotsOrder(List pivots, List<String> columnHeaders) {
        for (int i = 0; i < columnHeaders.size(); i++) {
            String p1 = pivots.get(i).toString();
            String p2 = columnHeaders.get(i);
            if (!p1.equals(p2)) {
                throw new RuntimeException("Pivot file does not match with the pivots in the file of precomputed dists");
            }
        }
    }
}
