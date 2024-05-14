/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.skittle;

import java.util.List;
import java.util.logging.Logger;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.auxiliaryForDistBounding.FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.twopivots.impl.DataDependentGeneralisedPtolemaicFiltering;
import vm.metricSpace.distance.storedPrecomputedDistances.AbstractPrecomputedDistancesMatrixLoader;
import vm.search.algorithm.impl.GroundTruthEvaluator;
import vm.search.algorithm.impl.KNNSearchWithPtolemaicFiltering;

/**
 *
 * @author Vlada
 */
public class FSLearnSkittleForDataDepPtolemaiosMain {

    public static final Integer SAMPLE_SET_SIZE = 100000;
    public static final Integer SAMPLE_QUERY_SET_SIZE = 100;
    public static final Logger LOG = Logger.getLogger(FSLearnSkittleForDataDepPtolemaiosMain.class.getName());

    public static void main(String[] args) {
        int pivotCount = KNNSearchWithPtolemaicFiltering.LB_COUNT;
        int k = GroundTruthEvaluator.K_IMPLICIT_FOR_QUERIES;
        Dataset[] datasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_10M_PCA256Dataset()
        };

        for (Dataset dataset : datasets) {
            run(dataset, k, pivotCount);
        }
    }

    private static void run(Dataset dataset, int k, int pivotCount) {
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        DistanceFunctionInterface df = dataset.getDistanceFunction();
        List pivots = dataset.getPivots(pivotCount);
        DataDependentGeneralisedPtolemaicFiltering filter = initDataDepPtolemaicFilter(pivots, dataset, k);

        List<Object> metricObjects = dataset.getSampleOfDataset(SAMPLE_SET_SIZE + SAMPLE_QUERY_SET_SIZE);
        List<Object> sampleObjects = metricObjects.subList(0, SAMPLE_SET_SIZE);
        List<Object> queriesSamples = metricObjects.subList(SAMPLE_SET_SIZE, SAMPLE_SET_SIZE + SAMPLE_QUERY_SET_SIZE);

        AbstractPrecomputedDistancesMatrixLoader pd = ToolsMetricDomain.evaluateMatrixOfDistances(sampleObjects.iterator(), pivots, metricSpace, df);
        float[][] poDists = pd.loadPrecomPivotsToObjectsDists(null, -1);

        KNNSearchWithPtolemaicFilteringLearnSkittle alg = new KNNSearchWithPtolemaicFilteringLearnSkittle(metricSpace, filter, pivots, poDists, pd.getRowHeaders(), pd.getColumnHeaders(), df);

        LearnSkittleForDataDepPtolemaios evaluator = new LearnSkittleForDataDepPtolemaios(alg, metricSpace, queriesSamples, sampleObjects, df);
        evaluator.learn();

    }

    private static DataDependentGeneralisedPtolemaicFiltering initDataDepPtolemaicFilter(List pivots, Dataset dataset, int k) {
        int pivotCount = pivots.size();
        DataDependentGeneralisedPtolemaicFiltering dataDependentPtolemaicFiltering = FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl.getLearnedInstance(
                "",
                dataset,
                pivotCount
        );
        return dataDependentPtolemaicFiltering;
    }

}
