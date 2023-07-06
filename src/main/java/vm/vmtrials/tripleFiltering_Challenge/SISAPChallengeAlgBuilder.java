/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.tripleFiltering_Challenge;

import java.util.AbstractMap;
import java.util.Map;
import java.util.TreeSet;
import vm.fs.store.filtering.FSSecondaryFilteringWithSketchesStorage;
import vm.fs.store.voronoiPartitioning.FSVoronoiPartitioningStorage;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.distance.bounding.nopivot.impl.SecondaryFilteringWithSketches;
import vm.metricSpace.distance.bounding.nopivot.learning.LearningSecondaryFilteringWithSketches;
import vm.metricSpace.distance.bounding.nopivot.storeLearned.SecondaryFilteringWithSketchesStoreInterface;
import vm.objTransforms.objectToSketchTransformators.AbstractObjectToSketchTransformator;
import vm.search.impl.VoronoiPartitionsCandSetIdentifier;
import vm.search.impl.multiFiltering.CranberryAlgorithm;
import vm.simRel.SimRelInterface;

/**
 *
 * @author Vlada
 */
public class SISAPChallengeAlgBuilder {

    public static SecondaryFilteringWithSketches initSecondaryFilteringWithSketches(Dataset fullDataset, Dataset sketchesDataset, String filterNamePrefix, float pCum, float distIntervalForPX) {
        SecondaryFilteringWithSketchesStoreInterface secondaryFilteringStorage = new FSSecondaryFilteringWithSketchesStorage();
        return new SecondaryFilteringWithSketches(filterNamePrefix, fullDataset.getDatasetName(), sketchesDataset, secondaryFilteringStorage, pCum, LearningSecondaryFilteringWithSketches.SKETCHES_SAMPLE_COUNT_FOR_IDIM_PX, LearningSecondaryFilteringWithSketches.DISTS_COMPS_FOR_SK_IDIM_AND_PX, distIntervalForPX);
    }

    private final float pCum = LearningSecondaryFilteringWithSketches.THRESHOLDS_P_CUM[0];

    /*  prefix of the shortened vectors used by the simRel */
    private final int prefixLength = 24;
    /*  prefix of the shortened vectors used by the simRel */
    private final int pcaLength = 256;
    /* number of query objects to learn t(\Omega) thresholds in the simRel. */
    private final int querySampleCount = 200;
    /* percentile - defined in the paper. Defines the precision of the simRel */
    private final float percentile = 0.99f;

    private final float distIntervalsForPX = 0.004f;

    private final VoronoiPartitionsCandSetIdentifier algVoronoi;
    private final SecondaryFilteringWithSketches algSketchFiltering;
    private final SimRelInterface<float[]> algSimRelFiltering;
    private final Integer k;

    private final CranberryAlgorithm cranberryAlg;
    private final AbstractMetricSpace fullMetricSpace;
    private final AbstractMetricSpace pcaDatasetMetricSpace;

    /**
     *
     *
     * @param fullDataset
     * @param pcaDataset
     * @param sketchesDataset
     * @param sketchingTechnique
     * @param voronoiK set 400 000 for 10M dataset, 1M for 30M dataset and 3M
     * for 100M dataset
     * @param kPCA set 300 for 10M dataset, 500 otherwise
     * @param k set 10 for the sisap challenge
     * @param pivotsUsedForTheVoronoi
     * @param tOmegaStresholdsFileNameVoluntary if the file has a specific name
     * different from the automatically derived
     */
    public SISAPChallengeAlgBuilder(Dataset fullDataset, Dataset pcaDataset, Dataset sketchesDataset, AbstractObjectToSketchTransformator sketchingTechnique, int voronoiK, int kPCA, int k, int pivotsUsedForTheVoronoi, String tOmegaStresholdsFileNameVoluntary) {
        String resultNamePrefix = "CRANBERRY_Voronoi" + voronoiK + "_pCum" + pCum;
        algVoronoi = new VoronoiPartitionsCandSetIdentifier(fullDataset, new FSVoronoiPartitioningStorage(), pivotsUsedForTheVoronoi);
        algSimRelFiltering = EvaluateCRANBERRYMain.initSimRel(querySampleCount, pcaLength, kPCA, voronoiK, pcaDataset.getDatasetName(), percentile, prefixLength, null, tOmegaStresholdsFileNameVoluntary);
        algSketchFiltering = initSecondaryFilteringWithSketches(fullDataset, sketchesDataset, resultNamePrefix, pCum, distIntervalsForPX);
        this.k = k;

        fullMetricSpace = fullDataset.getMetricSpace();
        pcaDatasetMetricSpace = pcaDataset.getMetricSpace();
        Map pcaOMap = EvaluateCRANBERRYMain.getMapOfPrefixes(pcaDatasetMetricSpace, pcaDataset.getMetricObjectsFromDataset(), prefixLength);

        cranberryAlg = new CranberryAlgorithm<>(
                algVoronoi,
                voronoiK,
                algSketchFiltering,
                sketchingTechnique,
                sketchesDataset.getMetricSpace(),
                algSimRelFiltering,
                kPCA,
                Integer.MAX_VALUE,
                pcaOMap,
                fullDataset.getKeyValueStorage(),
                algSketchFiltering.getNumberOfSketches(),
                fullDataset.getDistanceFunction());

    }

    protected TreeSet<Map.Entry<Object, Float>> evaluatekNNQuery(String queryObjID, float[] qVectorData, float[] pcaQDataPreffixOrFull) {
        AbstractMap.SimpleEntry<String, float[]> query = new AbstractMap.SimpleEntry<>(queryObjID, qVectorData);
        AbstractMap.SimpleEntry<String, float[]> queryPCA = new AbstractMap.SimpleEntry<>(queryObjID, pcaQDataPreffixOrFull);

        TreeSet ret = cranberryAlg.completeKnnSearch(fullMetricSpace, query, k, null, pcaDatasetMetricSpace, queryPCA);
        return ret;
    }

    public CranberryAlgorithm getCranberryAlg() {
        return cranberryAlg;
    }

    public void shutDownThreadPool() {
        algSketchFiltering.shutdownThreadPool();
    }

}
