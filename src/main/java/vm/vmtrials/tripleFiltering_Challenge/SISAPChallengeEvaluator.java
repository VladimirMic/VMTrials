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
import vm.search.impl.multiFiltering.VorSkeSim;
import vm.simRel.impl.SimRelEuclideanPCAImpl;
import static vm.vmtrials.tripleFiltering_Challenge.EvaluateVorSkeSimMain.initSimRel;

/**
 *
 * @author Vlada
 */
public class SISAPChallengeEvaluator {

    private final int sketchLength = 512;
    private final float pCum = 0.7f;

    /*  prefix of the shortened vectors used by the simRel */
    private final int prefixLength = 24;
    /*  prefix of the shortened vectors used by the simRel */
    private final int pcaLength = 96;
    /* number of query objects to learn t(\Omega) thresholds in the simRel. We use different objects than the queries tested. */
    private final int querySampleCount = 100;
    /* size of the data sample to learn t(\Omega) thresholds, SISAP: 100K */
    private final int dataSampleCount = 100000;

    private final int pivotsUsedForTheVoronoi = 2048;
    /* percentile - defined in the paper. Defines the precision of the simRel */
    private final float percentile = 0.9f;

    private final float distIntervalsForPX = 0.004f;

    private final VoronoiPartitionsCandSetIdentifier ALG_VORONOI;
    private final SecondaryFilteringWithSketches SKETCH_FILTERING;
    private final SimRelEuclideanPCAImpl SIM_REL_FILTER;
    private final Integer K;

    private final VorSkeSim ALGORITHM;
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
     */
    public SISAPChallengeEvaluator(Dataset fullDataset, Dataset pcaDataset, Dataset sketchesDataset, int voronoiK, int kPCA, int k) {
        String resultNamePrefix = "Voronoi" + voronoiK + "_pCum" + pCum;
        ALG_VORONOI = new VoronoiPartitionsCandSetIdentifier(fullDataset, new FSVoronoiPartitioningStorage(), pivotsUsedForTheVoronoi);
        EvaluateVorSkeSimMain.initSimRel(querySampleCount, pcaLength, kPCA, dataSampleCount, pcaDataset.getDatasetName(), percentile, prefixLength);
        SKETCH_FILTERING = EvaluateVorSkeSimMain.initSecondaryFilteringWithSketches(fullDataset, sketchesDataset, resultNamePrefix, pCum, distIntervalsForPX);
        K = k;

        fullMetricSpace = fullDataset.getMetricSpace();
        pcaDatasetMetricSpace = pcaDataset.getMetricSpace();
        Map pcaOMap = EvaluateVorSkeSimMain.getMapOfPrefixes(pcaDatasetMetricSpace, pcaDataset.getMetricObjectsFromDataset(), prefixLength);

        // sketching technique to transform query object to sketch
        GHPSketchingPivotPairsStoreInterface storageOfPivotPairs = new FSGHPSketchesPivotPairsStorageImpl();

        List pivots = fullDataset.getPivots(-1);
        AbstractObjectToSketchTransformator sketchingTechnique = new SketchingGHP(fullDataset.getDistanceFunction(), fullDataset.getMetricSpace(), pivots, false, fullDataset.getDatasetName(), 0.5f, sketchLength, storageOfPivotPairs);

        SIM_REL_FILTER = initSimRel(querySampleCount, pcaLength, kPCA, dataSampleCount, pcaDataset.getDatasetName(), percentile, prefixLength);

        ALGORITHM = new VorSkeSim(
                ALG_VORONOI,
                voronoiK,
                SKETCH_FILTERING,
                sketchingTechnique,
                sketchesDataset.getMetricSpace(),
                SIM_REL_FILTER, kPCA, pcaOMap, fullDataset.getKeyValueStorage(), fullDataset.getDistanceFunction());

    }

    public TreeSet<Map.Entry<Object, Float>> evaluatekNNQuery(String queryObjID, float[] qVectorData, float[] pcaQDataPreffixOrFull) {
        AbstractMap.SimpleEntry<String, float[]> query = new AbstractMap.SimpleEntry<>(queryObjID, qVectorData);
        AbstractMap.SimpleEntry<String, float[]> queryPCA = new AbstractMap.SimpleEntry<>(queryObjID, pcaQDataPreffixOrFull);

        TreeSet ret = ALGORITHM.completeKnnSearch(fullMetricSpace, query, K, null, pcaDatasetMetricSpace, queryPCA);
        return ret;
    }

}
