/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.deprecated.main;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.fs.dataset.FSDatasetInstances;
import vm.fs.store.dataTransforms.FSGHPSketchesPivotPairsStorageImpl;
import vm.fs.store.filtering.FSSecondaryFilteringWithSketchesStorage;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.fs.store.queryResults.FSQueryExecutionStatsStoreImpl;
import vm.fs.store.queryResults.recallEvaluation.FSRecallOfCandidateSetsStorageImpl;
import vm.fs.store.partitioning.FSVoronoiPartitioningStorage;
import vm.objTransforms.objectToSketchTransformators.AbstractObjectToSketchTransformator;
import vm.objTransforms.objectToSketchTransformators.SketchingGHP;
import vm.queryResults.recallEvaluation.RecallOfCandsSetsEvaluator;
import vm.search.algorithm.SearchingAlgorithm;
import vm.search.algorithm.impl.KNNSearchWithSketchSecondaryFiltering;
import vm.search.algorithm.impl.VoronoiPartitionsCandSetIdentifier;
import vm.objTransforms.storeLearned.PivotPairsStoreInterface;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.datasetPartitioning.StorageDatasetPartitionsInterface;
import vm.searchSpace.distance.DistanceFunctionInterface;
import vm.searchSpace.distance.bounding.nopivot.impl.SecondaryFilteringWithSketches;
import vm.searchSpace.distance.bounding.nopivot.learning.LearningSecondaryFilteringWithSketches;
import vm.searchSpace.distance.bounding.nopivot.storeLearned.SecondaryFilteringWithSketchesStoreInterface;

/**
 *
 * @author Vlada
 */
@Deprecated // very slow on big data
public class FSVoronoiPlusSecondaryWithSketches {

    private static final Logger LOG = Logger.getLogger(FSVoronoiPlusSecondaryWithSketches.class.getName());

    public static void main(String[] args) {
        float pCum = 0.5f;
        int sketchLength = 256;
        int pivotCount = 2048;
        Dataset[] fullDatasets = new Dataset[]{
            new FSDatasetInstances.LAION_10M_Dataset(true),
            new FSDatasetInstances.LAION_30M_Dataset(true),
            new FSDatasetInstances.LAION_100M_Dataset(true)
        };
        Dataset[] sketchesDatasets = new Dataset[]{
            new FSDatasetInstances.LAION_10M_GHP_50_256Dataset(true),
            new FSDatasetInstances.LAION_30M_GHP_50_256Dataset(true),
            new FSDatasetInstances.LAION_100M_GHP_50_256Dataset(true)
        };
        float[] distIntervalsForPX = new float[]{
            0.004f,
            0.004f,
            0.004f
        };
        int[] voronoiK = new int[]{
            400000,
            1000000,
            3000000
        };
        for (int i = 2; i < sketchesDatasets.length; i++) {
            Dataset fullDataset = fullDatasets[i];
            Dataset sketchesDataset = sketchesDatasets[i];
            float distIntervalForPX = distIntervalsForPX[i];
            run(fullDataset, sketchesDataset, distIntervalForPX, pCum, sketchLength, pivotCount, voronoiK[i]);
        }
    }

    private static void run(Dataset fullDataset, Dataset sketchesDataset, float distIntervalForPX, float pCum, int sketchLength, int pivotCountUsedForVoronoiLearning, int voronoiK) {
        int k = 10;
        AbstractSearchSpace metricSpace = fullDataset.getSearchSpace();
        DistanceFunctionInterface df = fullDataset.getDistanceFunction();
        StorageDatasetPartitionsInterface voronoiPartitioningStorage = new FSVoronoiPartitioningStorage();
        SearchingAlgorithm voronoi = new VoronoiPartitionsCandSetIdentifier(fullDataset, voronoiPartitioningStorage, pivotCountUsedForVoronoiLearning);

        PivotPairsStoreInterface storageOfPivotPairs = new FSGHPSketchesPivotPairsStorageImpl();
        List pivots = fullDataset.getPivots(-1);

        AbstractObjectToSketchTransformator sketchingTechnique = new SketchingGHP(df, metricSpace, pivots, fullDataset.getDatasetName(), 0.5f, sketchLength, storageOfPivotPairs);
        SecondaryFilteringWithSketchesStoreInterface secondaryFilteringStorage = new FSSecondaryFilteringWithSketchesStorage();
        SecondaryFilteringWithSketches filter = new SecondaryFilteringWithSketches("Voronoi" + voronoiK + "_pCum_" + pCum + "_sketchLength_" + sketchLength, fullDataset.getDatasetName(), sketchesDataset, secondaryFilteringStorage, pCum, LearningSecondaryFilteringWithSketches.SKETCHES_SAMPLE_COUNT_FOR_IDIM_PX, LearningSecondaryFilteringWithSketches.DISTS_COMPS_FOR_SK_IDIM_AND_PX, distIntervalForPX);

        SearchingAlgorithm secondaryWithSketches = new KNNSearchWithSketchSecondaryFiltering(fullDataset, filter, sketchingTechnique);

        List queries = fullDataset.getQueryObjects();

        Map keyValueStorage = fullDataset.getKeyValueStorage();
//        Map keyValueStorage = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(metricSpace, fullDataset.getMetricObjectsFromDataset(), true);

        TreeSet[] results = new TreeSet[queries.size()];
        for (int i = 0; i < queries.size(); i++) {
            Object query = queries.get(i);
            List candSetKnnSearch = voronoi.candSetKnnSearch(metricSpace, query, voronoiK, null);
            results[i] = secondaryWithSketches.completeKnnSearch(metricSpace, query, k, candSetKnnSearch.iterator(), keyValueStorage);
        }
        LOG.log(Level.INFO, "Storing statistics of queries");
        FSQueryExecutionStatsStoreImpl statsStorage = new FSQueryExecutionStatsStoreImpl(fullDataset.getDatasetName(), fullDataset.getQuerySetName(), k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), filter.getTechFullName(), null);
        statsStorage.storeStatsForQueries(secondaryWithSketches.getDistCompsPerQueries(), secondaryWithSketches.getTimesPerQueries(), secondaryWithSketches.getAdditionalStats());
        statsStorage.save();

        LOG.log(Level.INFO, "Storing results of queries");
        FSNearestNeighboursStorageImpl resultsStorage = new FSNearestNeighboursStorageImpl();
        resultsStorage.storeQueryResults(metricSpace, queries, results, k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), filter.getTechFullName());

        LOG.log(Level.INFO, "Evaluating accuracy of queries");
        FSRecallOfCandidateSetsStorageImpl recallStorage = new FSRecallOfCandidateSetsStorageImpl(fullDataset.getDatasetName(), fullDataset.getQuerySetName(), k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), filter.getTechFullName(), null);
        RecallOfCandsSetsEvaluator evaluator = new RecallOfCandsSetsEvaluator(new FSNearestNeighboursStorageImpl(), recallStorage);
        evaluator.evaluateAndStoreRecallsOfQueries(fullDataset.getDatasetName(), fullDataset.getQuerySetName(), k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), filter.getTechFullName(), k);
        recallStorage.save();
    }

}
