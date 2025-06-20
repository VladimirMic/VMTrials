package vm.vmtrials.tripleFiltering_Challenge;

import java.util.AbstractMap;
import java.util.Map;
import java.util.TreeSet;
import vm.fs.store.filtering.FSSecondaryFilteringWithSketchesStorage;
import vm.fs.store.partitioning.FSVoronoiPartitioningStorage;
import vm.objTransforms.objectToSketchTransformators.AbstractObjectToSketchTransformator;
import vm.search.algorithm.impl.VoronoiPartitionsCandSetIdentifier;
import vm.search.algorithm.impl.multiFiltering.CranberryAlgorithm;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.distance.bounding.nopivot.impl.SecondaryFilteringWithSketches;
import vm.searchSpace.distance.bounding.nopivot.learning.LearningSecondaryFilteringWithSketches;
import vm.searchSpace.distance.bounding.nopivot.storeLearned.SecondaryFilteringWithSketchesStoreInterface;
import vm.simRel.SimRelInterface;

/**
 *
 * @author Vlada
 */
public class SISAPChallengeAlgBuilder {

    public static final Integer MIN_DATASET_SIZE_TO_CACHE = 1000000; // milion
    public static final Integer MAX_DATASET_SIZE_TO_CACHE = 110000000; // 50 millions

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
    private final AbstractSearchSpace fullMetricSpace;
    private final AbstractSearchSpace pcaDatasetMetricSpace;

    /**
     *
     *
     * @param fullDataset
     * @param pcaDataset
     * @param sketchesDataset
     * @param sketchingTechnique for 100M dataset
     * @param k set 10 for the sisap challenge
     * @param tOmegaStresholdsFileNameVoluntary if the file has a specific name
     * different from the automatically derived
     */
    public SISAPChallengeAlgBuilder(Dataset fullDataset, Dataset pcaDataset, Dataset sketchesDataset, AbstractObjectToSketchTransformator sketchingTechnique, int k, String tOmegaStresholdsFileNameVoluntary) {
        algSketchFiltering = initSecondaryFilteringWithSketches(fullDataset, sketchesDataset, "", pCum, distIntervalsForPX);
        int datasetSize = algSketchFiltering.getNumberOfSketches();
        int pivotsUsedForTheVoronoi = EvaluateCRANBERRYMain.getPivotCountForVoronoi(datasetSize);
        System.gc();
        algVoronoi = new VoronoiPartitionsCandSetIdentifier(fullDataset, new FSVoronoiPartitioningStorage(), pivotsUsedForTheVoronoi);
        int voronoiK = EvaluateCRANBERRYMain.getVoronoiK(datasetSize);
        int kPCA = EvaluateCRANBERRYMain.getMinSimRelAnswerSize(datasetSize);
        System.gc();
        algSimRelFiltering = EvaluateCRANBERRYMain.initSimRel(querySampleCount, pcaLength, kPCA, voronoiK, pcaDataset.getDatasetName(), percentile, prefixLength, null, tOmegaStresholdsFileNameVoluntary);
        this.k = k;

        fullMetricSpace = fullDataset.getSearchSpace();
        pcaDatasetMetricSpace = pcaDataset.getSearchSpace();
        System.gc();
        Map pcaOMap = EvaluateCRANBERRYMain.getMapOfPrefixes(pcaDatasetMetricSpace, pcaDataset.getSearchObjectsFromDataset(), prefixLength);

        cranberryAlg = new CranberryAlgorithm<>(
                algVoronoi,
                voronoiK,
                algSketchFiltering,
                sketchingTechnique,
                sketchesDataset.getSearchSpace(),
                algSimRelFiltering,
                kPCA,
                pcaOMap,
                fullDataset.getKeyValueStorage(),
                algSketchFiltering.getNumberOfSketches(),
                fullDataset.getDistanceFunction()
        );
        System.gc();
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
