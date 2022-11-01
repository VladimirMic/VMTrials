/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vm.vmtrials.checking.sketching;

import java.sql.SQLException;
import vm.db.DBDatasetInstance;
import vm.metricSpace.Dataset;
import vm.objTransforms.learning.LearningSketchingGHP;
import vm.objTransforms.storeLearned.GHPSketchingPivotPairsStoreInterface;

/**
 *
 * @author xmic
 */
public class LearnSketches {

    public static void main(String[] args) throws SQLException {
        Dataset dataset = new DBDatasetInstance.DeCAFDataset();
        GHPSketchingPivotPairsStoreInterface sketchingTechStorage = new GHPSketchingPivotPairsStoreInterface();
        int sampleSize = 10000;
        int[] sketchesLengths = new int[]{64, 256};
        LearningSketchingGHP learn = new LearningSketchingGHP(dataset.getMetricSpace(), dataset.getMetricSpacesStorage(), sketchingTechStorage);
        learn.execute(dataset.getDatasetName(), dataset.getDatasetName(), sampleSize, sketchesLengths, 0.5f);
        
    }
}
