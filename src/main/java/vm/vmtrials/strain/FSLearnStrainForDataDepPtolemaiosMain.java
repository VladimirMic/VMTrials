/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.strain;

import java.util.List;
import java.util.logging.Logger;
import vm.fs.dataset.FSDatasetInstances;
import vm.fs.main.search.perform.FSKNNQueriesSeqScanWithFilteringMain;
import vm.fs.store.auxiliaryForDistBounding.FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.distance.DistanceFunctionInterface;
import vm.searchSpace.distance.bounding.twopivots.impl.DataDependentPtolemaicFiltering;
import vm.searchSpace.distance.storedPrecomputedDistances.AbstractPrecomputedDistancesMatrixSerializator;

/**
 *
 * @author Vlada
 */
public class FSLearnStrainForDataDepPtolemaiosMain {

    public static final Integer SAMPLE_SET_SIZE = -1;
    public static final Integer SAMPLE_QUERY_SET_SIZE = 1000;
    public static final Logger LOG = Logger.getLogger(FSLearnStrainForDataDepPtolemaiosMain.class.getName());

    public static void main(String[] args) {
        Dataset[] datasets = new Dataset[]{
            //            new FSDatasetInstanceSingularizator.LAION_10M_PCA256Dataset()
            new FSDatasetInstances.Faiss_Clip_100M_PCA256_Candidates()
        };

        for (Dataset dataset : datasets) {
            int pivotCount = dataset.getRecommendedNumberOfPivotsForFiltering();
            run(dataset, pivotCount);
        }
    }

    private static void run(Dataset dataset, int pivotCount) {
        AbstractSearchSpace metricSpace = dataset.getSearchSpace();
        DistanceFunctionInterface df = dataset.getDistanceFunction();
        List pivots = dataset.getPivots(pivotCount);
        DataDependentPtolemaicFiltering filter = initDataDepPtolemaicFilter(pivots, dataset);

        List<Object> queriesSamples = dataset.getQueryObjects(SAMPLE_QUERY_SET_SIZE);

        FSKNNQueriesSeqScanWithFilteringMain.initPODists(dataset, pivotCount, dataset.getRecommendedNumberOfPivotsForFiltering(), pivots);
        float[][] poDists = FSKNNQueriesSeqScanWithFilteringMain.getPoDists();
        AbstractPrecomputedDistancesMatrixSerializator pd = FSKNNQueriesSeqScanWithFilteringMain.getPd();

        for (int i = 0; i < 3; i++) {
            KNNSearchWithPtolemaicFilteringLearnSkittle alg = new KNNSearchWithPtolemaicFilteringLearnSkittle(metricSpace, filter, pivots, poDists, pd.getRowHeaders(), df);
            LearnStrainForDataDepPtolemaios evaluator = new LearnStrainForDataDepPtolemaios(alg, dataset, queriesSamples, pivotCount);
            evaluator.learn();
        }
    }

    private static DataDependentPtolemaicFiltering initDataDepPtolemaicFilter(List pivots, Dataset dataset) {
        int pivotCount = pivots.size();
        DataDependentPtolemaicFiltering dataDependentPtolemaicFiltering = FSPtolemyInequalityWithLimitedAnglesCoefsStorageImpl.getLearnedInstance(
                "",
                dataset,
                pivotCount
        );
        return dataDependentPtolemaicFiltering;
    }

}
