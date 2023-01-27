package vm.vmtrials.ptolemaios;

import java.util.List;
import java.util.TreeSet;
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
        Dataset dataset = new FSDatasetInstanceSingularizator.RandomDataset20Uniform();
//        String pathToHulls = "h:\\Skola\\2022\\Ptolemaions_limited\\EFgetBD\\Hulls\\" + dataset.getDatasetName() + "___tetrahedrons_100000__ratio_of_outliers_to_cut_0.01__pivot_pairs_128.csv";
        int k = 100;

        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        DistanceFunctionInterface df = dataset.getDistanceFunction();
        int pivotCount = 512;
        PrecomputedDistancesLoader pd = new PrecomputedDistancesLoaderImpl();
        float[][] poDists = pd.loadPrecomPivotsToObjectsDists(dataset.getDatasetName(), dataset.getDatasetName(), pivotCount);
        List queries = dataset.getMetricQueryObjectsForTheSameDataset();
        List pivots = dataset.getPivotsForTheSameDataset(pivotCount);
        float[][] pivotPivotDists = metricSpace.getDistanceMap(df, pivots, pivots);

//        TwoPivotsFiltering filter = new PtolemaiosFilteringWithLimitedAngles(pathToHulls);
        TwoPivotsFiltering filter = new PtolemaiosFiltering();
        KNNSearchWithTwoPivotFiltering alg = new KNNSearchWithTwoPivotFiltering(metricSpace, filter, pivots, poDists, pd.getRowHeaders(), pd.getColumnHeaders(), pivotPivotDists, df);
        TreeSet[] results = alg.completeKnnSearchOfQuerySet(metricSpace, queries, k, dataset.getMetricObjectsFromDataset());
        FSQueryExecutionStatsStoreImpl statsStorage = new FSQueryExecutionStatsStoreImpl(dataset.getDatasetName(), dataset.getDatasetName(), k, dataset.getDatasetName(), dataset.getDatasetName(), filter.getNameOfBounds(), null);
        statsStorage.storeStatsForQueries(alg.getDistCompsPerQueries(), alg.getTimesPerQueries());
        statsStorage.saveFile();

        FSNearestNeighboursStorageImpl resultsStorage = new FSNearestNeighboursStorageImpl();
        resultsStorage.storeQueryResults(queries, results, dataset.getDatasetName(), dataset.getDatasetName(), filter.getNameOfBounds());
    }

}
