package vm.vmtrials.checking.sketching;

import java.sql.SQLException;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.dataTransforms.FSGHPSketchesPivotPairsStorageImpl;
import vm.metricSpace.Dataset;
import vm.objTransforms.learning.LearningSketchingGHP;
import vm.objTransforms.storeLearned.GHPSketchingPivotPairsStoreInterface;

/**
 *
 * @author xmic
 */
public class LearnSketches {
// does not work!

    public static void main(String[] args) throws SQLException, InterruptedException {
        Dataset dataset = new FSDatasetInstanceSingularizator.DeCAFDataset();
        GHPSketchingPivotPairsStoreInterface sketchingTechStorage = new FSGHPSketchesPivotPairsStorageImpl();
        int sampleSize = 100000; // 100000
        int[] sketchesLengths = new int[]{256, 192, 128, 64, 512};
        LearningSketchingGHP learn = new LearningSketchingGHP(dataset.getMetricSpace(), dataset.getMetricSpacesStorage(), sketchingTechStorage);
        learn.execute(dataset.getDatasetName(), dataset.getDatasetName(), sampleSize, sketchesLengths, 0.5f);
    }
}
