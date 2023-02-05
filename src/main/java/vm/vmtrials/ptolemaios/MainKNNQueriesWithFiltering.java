package vm.vmtrials.ptolemaios;

import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.metricspace.distance.precomputedDistances.PrecomputedDistancesLoaderImpl;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.fs.store.queryResults.FSQueryExecutionStatsStoreImpl;
import vm.fs.store.queryResults.recallEvaluation.FSRecallOfCandidateSetsStorageImpl;
import vm.metricspace.AbstractMetricSpace;
import vm.metricspace.Dataset;
import vm.metricspace.distance.DistanceFunctionInterface;
import vm.metricspace.distance.PrecomputedDistancesLoader;
import vm.metricspace.distance.bounding.twopivots.TwoPivotsFiltering;
import vm.metricspace.distance.bounding.twopivots.impl.FourPointBasedFiltering;
import vm.queryResults.recallEvaluation.RecallOfCandsSetsEvaluator;
import vm.search.SearchingAlgorithm;
import vm.search.impl.KNNSearchWithTwoPivotFiltering;

/**
 *
 * @author Vlada
 */
public class MainKNNQueriesWithFiltering {

    private static final Logger LOG = Logger.getLogger(MainKNNQueriesWithFiltering.class.getName());

    public static void main(String[] args) {

//        String pathToHulls = "h:\\Skola\\2022\\Ptolemaions_limited\\EFgetBD\\Hulls\\" + dataset.getDatasetName() + "___tetrahedrons_100000__ratio_of_outliers_to_cut_0.01__pivot_pairs_128.csv";
//        run(new FSDatasetInstanceSingularizator.SIFTdataset());
//        System.gc();
//        run(new FSDatasetInstanceSingularizator.MPEG7dataset());
//        System.gc();
        run(new FSDatasetInstanceSingularizator.RandomDataset20Uniform());
        System.gc();
//        run(new FSDatasetInstanceSingularizator.DeCAFDataset());
    }

    private static void run(Dataset dataset) {
        int k = 100;
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        DistanceFunctionInterface df = dataset.getDistanceFunction();
        int pivotCount = 256;
        PrecomputedDistancesLoader pd = new PrecomputedDistancesLoaderImpl();
        float[][] poDists = pd.loadPrecomPivotsToObjectsDists(dataset.getDatasetName(), dataset.getDatasetName(), pivotCount);
        List queries = dataset.getMetricQueryObjectsForTheSameDataset();
        List pivots = dataset.getPivotsForTheSameDataset(pivotCount);
        float[][] pivotPivotDists = metricSpace.getDistanceMap(df, pivots, pivots);

//        TwoPivotsFiltering filter = new PtolemaiosFilteringWithLimitedAngles(pivotCount + "_pivots", pathToHulls);
        TwoPivotsFiltering filter = new FourPointBasedFiltering(pivotCount + "_pivots");
//        TriangleInequality filter = new TriangleInequality(pivotCount + "_pivots");
        SearchingAlgorithm alg = new KNNSearchWithTwoPivotFiltering(metricSpace, filter, pivots, poDists, pd.getRowHeaders(), pd.getColumnHeaders(), pivotPivotDists, df);
//        SearchingAlgorithm alg = new KNNSearchWithOnePivotFiltering(metricSpace, filter, pivots, poDists, pd.getRowHeaders(), pd.getColumnHeaders(), df);
        TreeSet[] results = alg.completeKnnSearchOfQuerySet(metricSpace, queries, k, dataset.getMetricObjectsFromDataset());

        LOG.log(Level.INFO, "Storing statistics of queries");
        FSQueryExecutionStatsStoreImpl statsStorage = new FSQueryExecutionStatsStoreImpl(dataset.getDatasetName(), dataset.getDatasetName(), k, dataset.getDatasetName(), dataset.getDatasetName(), filter.getTechFullName(), null);
        statsStorage.storeStatsForQueries(alg.getDistCompsPerQueries(), alg.getTimesPerQueries());
        statsStorage.saveFile();

        LOG.log(Level.INFO, "Storing results of queries");
        FSNearestNeighboursStorageImpl resultsStorage = new FSNearestNeighboursStorageImpl();
        resultsStorage.storeQueryResults(metricSpace, queries, results, dataset.getDatasetName(), dataset.getDatasetName(), filter.getTechFullName());

        LOG.log(Level.INFO, "Evaluating accuracy of queries");
        FSRecallOfCandidateSetsStorageImpl recallStorage = new FSRecallOfCandidateSetsStorageImpl(dataset.getDatasetName(), dataset.getDatasetName(), k, dataset.getDatasetName(), dataset.getDatasetName(), filter.getTechFullName(), null);
        RecallOfCandsSetsEvaluator evaluator = new RecallOfCandsSetsEvaluator(new FSNearestNeighboursStorageImpl(), recallStorage);
        evaluator.evaluateAndStoreRecallsOfQueries(dataset.getDatasetName(), dataset.getDatasetName(), k, dataset.getDatasetName(), dataset.getDatasetName(), filter.getTechFullName(), k);
        recallStorage.saveFile();
    }

}
