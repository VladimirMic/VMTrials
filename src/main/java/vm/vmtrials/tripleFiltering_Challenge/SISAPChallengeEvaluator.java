/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.tripleFiltering_Challenge;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import vm.fs.store.dataTransforms.FSGHPSketchesPivotPairsStorageImpl;
import vm.fs.store.voronoiPartitioning.FSVoronoiPartitioningStorage;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.distance.bounding.nopivot.impl.SecondaryFilteringWithSketches;
import vm.objTransforms.objectToSketchTransformators.AbstractObjectToSketchTransformator;
import vm.objTransforms.objectToSketchTransformators.SketchingGHP;
import vm.objTransforms.storeLearned.GHPSketchingPivotPairsStoreInterface;
import vm.search.impl.VoronoiPartitionsCandSetIdentifier;
import vm.search.impl.multiFiltering.VorSkeSimSorting;
import vm.simRel.impl.SimRelEuclideanPCAImpl;
import static vm.vmtrials.tripleFiltering_Challenge.EvaluateVorSkeSimMain.initSimRel;

/**
 *
 * @author Vlada
 */
public class SISAPChallengeEvaluator {

    private final int sketchLength = 256;
    private final float pCum = 0.5f;

    /*  prefix of the shortened vectors used by the simRel */
    private final int prefixLength = 24;
    /*  prefix of the shortened vectors used by the simRel */
    private final int pcaLength = 96;
    /* number of query objects to learn t(\Omega) thresholds in the simRel. We use different objects than the queries tested. */
    private final int querySampleCount = 100;
    /* percentile - defined in the paper. Defines the precision of the simRel */
    private final float percentile = 0.9f;

    private final float distIntervalsForPX = 0.004f;

    private final VoronoiPartitionsCandSetIdentifier algVoronoi;
    private final SecondaryFilteringWithSketches algSketchFiltering;
    private final SimRelEuclideanPCAImpl algSimRelFiltering;
    private final Integer k;

    private final VorSkeSimSorting vorSkeSimAlg;
    private final AbstractMetricSpace fullMetricSpace;
    private final AbstractMetricSpace pcaDatasetMetricSpace;

    /**
     *
     *
     * @param fullDataset
     * @param pcaDataset
     * @param sketchesDataset
     * @param voronoiK set 400 000 for 10M dataset, 1M for 30M dataset and 3M
     * for 100M dataset
     * @param kPCA set 300 for 10M dataset, 500 otherwise
     * @param k set 10 for the sisap challenge
     * @param pivotsUsedForTheVoronoi
     */
    public SISAPChallengeEvaluator(Dataset fullDataset, Dataset pcaDataset, Dataset sketchesDataset, int voronoiK, int kPCA, int k, int pivotsUsedForTheVoronoi) {
        String resultNamePrefix = "Voronoi" + voronoiK + "_pCum" + pCum;
        algVoronoi = new VoronoiPartitionsCandSetIdentifier(fullDataset, new FSVoronoiPartitioningStorage(), pivotsUsedForTheVoronoi);
        EvaluateVorSkeSimMain.initSimRel(querySampleCount, pcaLength, kPCA, voronoiK, pcaDataset.getDatasetName(), percentile, prefixLength);
        algSketchFiltering = EvaluateVorSkeSimMain.initSecondaryFilteringWithSketches(fullDataset, sketchesDataset, resultNamePrefix, pCum, distIntervalsForPX);
        this.k = k;

        fullMetricSpace = fullDataset.getMetricSpace();
        pcaDatasetMetricSpace = pcaDataset.getMetricSpace();
        Map pcaOMap = EvaluateVorSkeSimMain.getMapOfPrefixes(pcaDatasetMetricSpace, pcaDataset.getMetricObjectsFromDataset(), prefixLength);

        // sketching technique to transform query object to sketch
        GHPSketchingPivotPairsStoreInterface storageOfPivotPairs = new FSGHPSketchesPivotPairsStorageImpl();

        List pivots = fullDataset.getPivots(-1);
        AbstractObjectToSketchTransformator sketchingTechnique = new SketchingGHP(fullDataset.getDistanceFunction(), fullDataset.getMetricSpace(), pivots, false, fullDataset.getDatasetName(), 0.5f, sketchLength, storageOfPivotPairs);

        algSimRelFiltering = initSimRel(querySampleCount, pcaLength, kPCA, voronoiK, pcaDataset.getDatasetName(), percentile, prefixLength);

        vorSkeSimAlg = new VorSkeSimSorting<>(
                algVoronoi,
                voronoiK,
                algSketchFiltering,
                sketchingTechnique,
                sketchesDataset.getMetricSpace(),
                algSimRelFiltering, kPCA, Integer.MAX_VALUE, pcaOMap, fullDataset.getKeyValueStorage(), fullDataset.getDistanceFunction());

    }

    public TreeSet<Map.Entry<Object, Float>> evaluatekNNQuery(String queryObjID, float[] qVectorData, float[] pcaQDataPreffixOrFull) {
        AbstractMap.SimpleEntry<String, float[]> query = new AbstractMap.SimpleEntry<>(queryObjID, qVectorData);
        AbstractMap.SimpleEntry<String, float[]> queryPCA = new AbstractMap.SimpleEntry<>(queryObjID, pcaQDataPreffixOrFull);

        TreeSet ret = vorSkeSimAlg.completeKnnSearch(fullMetricSpace, query, k, null, pcaDatasetMetricSpace, queryPCA);
        return ret;
    }

}
