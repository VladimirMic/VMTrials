package vm.vmtrials.ptolemaios;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import vm.datatools.DataTypeConvertor;
import vm.db.dataset.DBDatasetInstanceSingularizator;
import vm.metricspace.AbstractMetricSpace;
import vm.metricspace.Dataset;
import vm.metricspace.MetricSpacesStorageInterface;
import vm.metricspace.distance.DistanceFunction;
import vm.metricspace.distance.bounding.twopivots.TwoPivotsFiltering;
import vm.metricspace.distance.bounding.twopivots.impl.PtolemaiosFilteringWithLimitedAngles;
import vm.search.impl.KNNSearchWithTwoPivotFiltering;

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
        DistanceFunction df = dataset.getDistanceFunction();
        MetricSpacesStorageInterface storage = dataset.getMetricSpacesStorage();
        int pivotCount = 512; // also in the name of the precomputed file!
        float[][] poDists = storage.loadPrecomPivotsToObjectsDists(dataset.getDatasetName(), pivotCount);
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
