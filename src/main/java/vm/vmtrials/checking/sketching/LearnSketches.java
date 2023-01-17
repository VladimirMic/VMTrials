package vm.vmtrials.checking.sketching;

import java.sql.SQLException;
import vm.db.DBDatasetInstance;
import vm.metricspace.Dataset;
import vm.objTransforms.learning.LearningSketchingGHP;
import vm.objTransforms.storeLearned.GHPSketchingPivotPairsStoreInterface;

/**
 *
 * @author xmic
 */
public class LearnSketches {
// does not work!

    public static void main(String[] args) throws SQLException, InterruptedException {
        Dataset dataset = new DBDatasetInstance.DeCAFDataset();
        GHPSketchingPivotPairsStoreInterface sketchingTechStorage = new GHPSketchingPivotPairsStoreInterface();
        int sampleSize = 50000;
        int[] sketchesLengths = new int[]{64, 256};
        LearningSketchingGHP learn = new LearningSketchingGHP(dataset.getMetricSpace(), dataset.getMetricSpacesStorage(), sketchingTechStorage);
        learn.execute(dataset.getDatasetName(), dataset.getDatasetName(), sampleSize, sketchesLengths, 0.5f);
    }
}
