/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.skittle;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.auxiliaryForDistBounding.FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl;
import vm.fs.store.precomputedDists.FSPrecomputedDistancesMatrixLoaderImpl;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.twopivots.impl.DataDependentGeneralisedPtolemaicFiltering;
import vm.metricSpace.distance.storedPrecomputedDistances.AbstractPrecomputedDistancesMatrixLoader;
import vm.search.algorithm.impl.KNNSearchWithPtolemaicFiltering;

/**
 *
 * @author Vlada
 */
public class FSLearnSkittleForDataDepPtolemaiosMain {

    public static final Integer SAMPLE_SET_SIZE = -1;
    public static final Integer SAMPLE_QUERY_SET_SIZE = 100;
    public static final Logger LOG = Logger.getLogger(FSLearnSkittleForDataDepPtolemaiosMain.class.getName());

    public static void main(String[] args) {
        int pivotCount = KNNSearchWithPtolemaicFiltering.LB_COUNT;
        Dataset[] datasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_10M_PCA256Dataset()
        };

        for (Dataset dataset : datasets) {
            run(dataset, pivotCount);
        }
    }

    private static void run(Dataset dataset, int pivotCount) {
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        DistanceFunctionInterface df = dataset.getDistanceFunction();
        List pivots = dataset.getPivots(pivotCount);
        DataDependentGeneralisedPtolemaicFiltering filter = initDataDepPtolemaicFilter(pivots, dataset);

        Iterator<Object> sampleObjects = dataset.getMetricObjectsFromDataset(SAMPLE_SET_SIZE);
        List<Object> queriesSamples = dataset.getSampleOfDataset(SAMPLE_QUERY_SET_SIZE);

        AbstractPrecomputedDistancesMatrixLoader pd = new FSPrecomputedDistancesMatrixLoaderImpl();
//        AbstractPrecomputedDistancesMatrixLoader pd = ToolsMetricDomain.evaluateMatrixOfDistances(sampleObjects, pivots, metricSpace, df);
        float[][] poDists = pd.loadPrecomPivotsToObjectsDists(dataset, pivotCount);

        KNNSearchWithPtolemaicFilteringLearnSkittle alg = new KNNSearchWithPtolemaicFilteringLearnSkittle(metricSpace, filter, pivots, poDists, pd.getRowHeaders(), pd.getColumnHeaders(), df);

        LearnSkittleForDataDepPtolemaios evaluator = new LearnSkittleForDataDepPtolemaios(alg, dataset.getDatasetName(),dataset.getQuerySetName(), metricSpace, queriesSamples, sampleObjects, df);
        evaluator.learn();

    }

    private static DataDependentGeneralisedPtolemaicFiltering initDataDepPtolemaicFilter(List pivots, Dataset dataset) {
        int pivotCount = pivots.size();
        DataDependentGeneralisedPtolemaicFiltering dataDependentPtolemaicFiltering = FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl.getLearnedInstance(
                "",
                dataset,
                pivotCount
        );
        return dataDependentPtolemaicFiltering;
    }

}
