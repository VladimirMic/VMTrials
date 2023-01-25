package vm.vmtrials.ptolemaios;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import vm.db.dataset.DBDatasetInstanceSingularizator;
import vm.fs.metricspace.distance.PrecomputedDistancesImpl;
import vm.metricspace.AbstractMetricSpace;
import vm.metricspace.Dataset;
import vm.metricspace.MetricSpacesStorageInterface;
import vm.metricspace.distance.bounding.twopivots.TwoPivotsFiltering;
import vm.metricspace.distance.bounding.twopivots.impl.PtolemaiosFilteringWithLimitedAngles;
import vm.search.impl.KNNSearchWithTwoPivotFiltering;
import vm.metricspace.distance.DistanceFunctionInterface;
import vm.metricspace.distance.PrecomputedDistancesInterface;

/**
 *
 * @author Vlada
 */
public class MainKNNQueriesWithFiltering {

    public static void main(String[] args) throws SQLException {
        Dataset dataset = new DBDatasetInstanceSingularizator.DeCAFDataset();
        String pathToHulls = "h:\\Skola\\2022\\Ptolemaions_limited\\EFgetBD\\Hulls\\" + dataset.getDatasetName() + "___tetrahedrons_100000__ratio_of_outliers_to_cut_0.01__pivot_pairs_128.csv";
        int k = 100;

        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        DistanceFunctionInterface df = dataset.getDistanceFunction();
        MetricSpacesStorageInterface storage = dataset.getMetricSpacesStorage();
        int pivotCount = 512; // also in the name of the precomputed file!
        PrecomputedDistancesInterface pd = new PrecomputedDistancesImpl();
        List<String> columnHeaders = new ArrayList<>();
        List<String> rowHeaders = new ArrayList<>();
        float[][] poDists = pd.loadPrecomPivotsToObjectsDists(dataset.getDatasetName(), dataset.getDatasetName(), pivotCount, columnHeaders, rowHeaders);
        List queries = dataset.getMetricQueryObjectsForTheSameDataset();
        List pivots = dataset.getPivotsForTheSameDataset(pivotCount);
        float[][] pivotPivotDists = metricSpace.getDistanceMap(dataset.getDatasetName(), pivots, pivots);

        TwoPivotsFiltering filter = new PtolemaiosFilteringWithLimitedAngles(pathToHulls);

        KNNSearchWithTwoPivotFiltering alg = new KNNSearchWithTwoPivotFiltering(metricSpace, filter, pivots, poDists, pivotPivotDists, df);
        for (Object query : queries) {
            TreeSet<Map.Entry<Object, Float>> result = alg.completeKnnSearch(metricSpace, query, k, dataset.getMetricObjectsFromDataset());
            String s = "";
        }
    }
}