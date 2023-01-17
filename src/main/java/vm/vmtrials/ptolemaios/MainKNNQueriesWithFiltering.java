/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vm.vmtrials.ptolemaios;

import java.sql.SQLException;
import vm.db.DBDatasetInstance;
import vm.metricspace.Dataset;
import vm.metricspace.distance.bounding.twopivots.TwoPivotsFiltering;
import vm.metricspace.distance.bounding.twopivots.impl.PtolemaiosFiltering;
import vm.metricspace.distance.bounding.twopivots.impl.PtolemaiosFilteringWithLimitedAngles;

/**
 *
 * @author Vlada
 */
public class MainKNNQueriesWithFiltering {

    public static void main(String[] args) throws SQLException {
        Dataset dataset = new DBDatasetInstance.DeCAFDataset();
        String pathToHulls = "h:\\Skola\\2022\\Ptolemaions_limited\\EFgetBD\\Hulls\\DeCAF_convex_hulls__tetrahedrons_100000__RATIO_OF_OUTLIERS_TO_CUT0.01__PIVOT_PAIRS_128.csv";
        TwoPivotsFiltering filter = new PtolemaiosFiltering();
        TwoPivotsFiltering filterBetter = new PtolemaiosFilteringWithLimitedAngles(pathToHulls);
        
    }
}
