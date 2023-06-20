/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.tripleFiltering_Challenge;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.main.search.filtering.learning.LearnSecondaryFilteringWithGHPSketchesMain;
import vm.fs.metricSpaceImpl.FSMetricSpaceImpl;
import vm.fs.metricSpaceImpl.FSMetricSpacesStorage;
import vm.fs.store.dataTransforms.FSGHPSketchesPivotPairsStorageImpl;
import vm.fs.store.filtering.FSSimRelThresholdsTOmegaStorage;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.fs.store.voronoiPartitioning.FSVoronoiPartitioningStorage;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.MetricSpacesStorageInterface;
import vm.metricSpace.dataToStringConvertors.SingularisedConvertors;
import vm.metricSpace.voronoiPartitioning.VoronoiPartitioning;
import vm.objTransforms.objectToSketchTransformators.AbstractObjectToSketchTransformator;
import vm.objTransforms.perform.TransformDataToGHPSketches;
import vm.objTransforms.storeLearned.GHPSketchingPivotPairsStoreInterface;
import vm.simRel.impl.learn.ThresholdsTOmegaEvaluator;
import vm.simRel.impl.learn.storeLearnt.SimRelEuclidThresholdsTOmegaStorage;

/**
 *
 * @author Vlada
 */
public class Main {

    public static final Logger LOG = Logger.getLogger(Main.class.getName());
    public static final Integer SKETCH_LENGTH = 256;

    private static SISAPChallengeEvaluator algorithm = null;

    public static void main(String[] args) {
        System.err.println("Args: ");
        for (int i = 0; i < args.length; i++) {
            System.err.println(i + ": " + args[i] + " ");
        }
        String dataset768DimPath = args[0];
        String datasetPCA96DimPath = args[1];
        String querySet768DimPath = args[2];
        String querySetPCA96DimPath = args[3];
        int datasetSize = Integer.parseInt(args[4]);
        boolean build = args.length <= 5 || Boolean.parseBoolean(args[5]);

        int k = 10;

        Dataset fullDataset = createImplicitH5Dataset(dataset768DimPath, querySet768DimPath, false);
        Dataset pcaDataset = createImplicitH5Dataset(datasetPCA96DimPath, querySetPCA96DimPath, true);

        Dataset sketchesDataset;
        if (build) {
            sketchesDataset = buildAndStoreAlgorithm(fullDataset, pcaDataset, datasetSize);
        } else {
            AbstractObjectToSketchTransformator sketchingTechnique = createSketches(fullDataset);
            sketchesDataset = createImplicitSketchesDataset(sketchingTechnique, fullDataset.getDatasetName(), SKETCH_LENGTH, 0.5f);
        }

        if (algorithm == null) {
            algorithm = initAlgorithm(fullDataset, pcaDataset, sketchesDataset, datasetSize, k);
        }

        List fullQueries = fullDataset.getMetricQueryObjects();
        List pcaQueries = pcaDataset.getMetricQueryObjects();

        AbstractMetricSpace<float[]> metricSpace = fullDataset.getMetricSpace();

        TreeSet[] results = new TreeSet[fullQueries.size()];

        for (int i = 0; i < fullQueries.size(); i++) {
            Object fullQObject = fullQueries.get(i);
            Object pcaQObject = pcaQueries.get(i);
            String fullQID = metricSpace.getIDOfMetricObject(fullQObject).toString();
            Object pcaQID = metricSpace.getIDOfMetricObject(pcaQObject);
            if (!pcaQID.equals(fullQID)) {
                throw new IllegalArgumentException(pcaQID + ", " + fullQID);
            }
            float[] fullQData = metricSpace.getDataOfMetricObject(fullQObject);
            float[] pcaQData = metricSpace.getDataOfMetricObject(pcaQObject);
            results[i] = algorithm.evaluatekNNQuery(fullQID, fullQData, pcaQData);
        }

//        LOG.log(Level.INFO, "Storing statistics of queries");
//        FSQueryExecutionStatsStoreImpl statsStorage = new FSQueryExecutionStatsStoreImpl(fullDataset.getDatasetName(), fullDataset.getQuerySetName(), k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), "SISAP_Challenge", null);
//        statsStorage.storeStatsForQueries(secondaryWithSketches.getDistCompsPerQueries(), secondaryWithSketches.getTimesPerQueries());
//        statsStorage.saveFile();
        LOG.log(Level.INFO, "Storing results of queries");
        FSNearestNeighboursStorageImpl resultsStorage = new FSNearestNeighboursStorageImpl(false);
        resultsStorage.storeQueryResults(metricSpace, fullQueries, results, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), "");

    }

    private static SISAPChallengeEvaluator initAlgorithm(Dataset fullDataset, Dataset pcaDataset, Dataset sketchesDataset, int datasetSize, int k) {
        LOG.log(Level.INFO, "Initializing algorithm");

        int pivotsUsedForTheVoronoi = getPivotCount(datasetSize);
        int voronoiK = getVoronoiK(datasetSize);
        int kPCA = getPCAK(datasetSize);
        SISAPChallengeEvaluator ret = new SISAPChallengeEvaluator(fullDataset, pcaDataset, sketchesDataset, voronoiK, kPCA, k, pivotsUsedForTheVoronoi);
        Logger.getLogger(Main.class.getName()).log(Level.INFO, "Algorithm initialised");
        return ret;
    }

    /**
     * *************************************************
     * Init params for datasets given by their size ****
     * *************************************************
     */
    public static int getVoronoiK(int size) {
        switch (size) {
            case 100000:
                return 1000;
            case 300000:
                return 1000;
            case 10000000:
                return 400000;
            case 30000000:
                return 1000000;
            case 100000000:
                return 3000000;
            default:
                throw new AssertionError();
        }
    }

    private static int getPivotCount(int size) {
        switch (size) {
            case 100000:
            case 300000:
                return 50;
            default:
                return 20000;
        }
    }

    private static int getPCAK(int size) {
        switch (size) {
            case 100000:
            case 300000:
                return 100;
            case 10000000:
                return 200;
            case 30000000:
                return 200;
            case 100000000:
                return 200;
            default:
                throw new AssertionError();
        }
    }

    /**
     * *************************************************
     * Build indexes and create auxiliary files ********
     * *************************************************
     */
    private static Dataset buildAndStoreAlgorithm(Dataset fullDataset, Dataset pcaDataset, int sizeIfDataset) {
        LOG.log(Level.INFO, "\nBuild start");
        LOG.log(Level.INFO, "\nStarting the Voronoi partitioning");
        storeVoronoiPartitioning(fullDataset, sizeIfDataset);
        System.gc();
        LOG.log(Level.INFO, "\nStarting the learn of the tOmega thresholds for the simRel");
        storeTOmegaThresholdsForSimRel(pcaDataset, sizeIfDataset);
        System.gc();
        LOG.log(Level.INFO, "\nStarting the sketching transformation with the predefined sketching technique");
        AbstractObjectToSketchTransformator sketchingTechnique = createSketches(fullDataset);
        Dataset sketchesDataset = createImplicitSketchesDataset(sketchingTechnique, fullDataset.getDatasetName(), SKETCH_LENGTH, 0.5f);
        System.gc();
        LOG.log(Level.INFO, "\nStarting the learn of the Secondary filtering with sketches");
        learnSketchMapping(fullDataset, sketchesDataset, 0.004f, SKETCH_LENGTH, 2f);
        System.gc();
        LOG.log(Level.INFO, "\nBuild finished");
        return sketchesDataset;

    }

    private static void storeVoronoiPartitioning(Dataset dataset, int sizeIfDataset) {
        int pivotCount = getPivotCount(sizeIfDataset);
        List<Object> pivots = dataset.getPivots(2 * pivotCount);
        VoronoiPartitioning vp = new VoronoiPartitioning(dataset.getMetricSpace(), dataset.getDistanceFunction(), pivots);
        FSVoronoiPartitioningStorage storage = new FSVoronoiPartitioningStorage();
        vp.splitByVoronoi(dataset.getMetricObjectsFromDataset(), dataset.getDatasetName(), pivotCount, storage);
    }

    private static void storeTOmegaThresholdsForSimRel(Dataset pcaDataset, int datasetSize) {
        int kPCA = getPCAK(datasetSize);
        /* number of query objects to learn t(\Omega) thresholds. We use different objects than the pivots tested. */
        int querySampleCount = 100;//200
        /* size of the data sample to learn t(\Omega) thresholds: SISAP: 100 000 */
        int dataSampleCount = getVoronoiK(datasetSize); // 100000 = 100K
        int pcaLength = 96;
        SimRelEuclidThresholdsTOmegaStorage simRelStorage = new FSSimRelThresholdsTOmegaStorage(querySampleCount, pcaLength, kPCA, dataSampleCount);
        ThresholdsTOmegaEvaluator evaluator = new ThresholdsTOmegaEvaluator(querySampleCount, kPCA);
        evaluator.learnTOmegaThresholds(pcaDataset, simRelStorage, dataSampleCount, pcaLength, FSSimRelThresholdsTOmegaStorage.PERCENTILES);
    }

    private static AbstractObjectToSketchTransformator createSketches(Dataset fullDataset) {
        MetricSpacesStorageInterface storageForSketches = new FSMetricSpacesStorage(new FSMetricSpaceImpl<>(), SingularisedConvertors.LONG_VECTOR_SPACE);
        GHPSketchingPivotPairsStoreInterface storageOfPivotPairs = new FSGHPSketchesPivotPairsStorageImpl();
        TransformDataToGHPSketches evaluator = new TransformDataToGHPSketches(fullDataset, storageOfPivotPairs, storageForSketches, 0.5f, -1);
        int[] sketchesLengths = new int[]{SKETCH_LENGTH};
        String[] sketchesPairsName = new String[]{"laion2B-en-clip768v2-n=1M_sample.h5_GHP_50_" + SKETCH_LENGTH};
        AbstractObjectToSketchTransformator ret = evaluator.createSketchesForDatasetPivotsAndQueries(sketchesLengths, sketchesPairsName);
        return ret;
    }

    /**
     * *************************************************
     * Create implicit datasets - full and PCA dataset *
     * *************************************************
     */
    private static Dataset createImplicitH5Dataset(String datasetPath, String querySetPath, boolean isPCA) {
        return new Main.ImplicitH5Dataset(datasetPath, querySetPath, isPCA);
    }

    private static void learnSketchMapping(Dataset fullDataset, Dataset sketchesDataset, float distIntervalForpx, int sketchLength, float maxDist) {
        LearnSecondaryFilteringWithGHPSketchesMain.run(fullDataset, sketchesDataset, distIntervalForpx, sketchLength, maxDist);
    }

    private static Dataset createImplicitSketchesDataset(AbstractObjectToSketchTransformator sketchingTechnique, String fullDatasetName, int sketchLength, float balance) {
        String name = sketchingTechnique.getNameOfTransformedSetOfObjects(fullDatasetName, sketchLength, balance);
        FSDatasetInstanceSingularizator.FSHammingSpaceDataset dataset = new FSDatasetInstanceSingularizator.FSHammingSpaceDataset(name);
        return dataset;
    }

    private static class ImplicitH5Dataset extends FSDatasetInstanceSingularizator.H5FloatVectorDataset {

        private final File datasetFile;
        private final File querySetFile;
        private final boolean isPCA;
        private final String querySetName;

        public ImplicitH5Dataset(String datasetPath, String querySetPath, boolean isPCA) {
            super(new File(datasetPath).getName());
            querySetName = new File(querySetPath).getName();
            this.datasetFile = new File(datasetPath);
            this.querySetFile = new File(querySetPath);
            this.isPCA = isPCA;
        }

        @Override
        public String getQuerySetName() {
            return querySetName;
        }

        @Override
        public List<Object> getMetricQueryObjects() {
            FSMetricSpacesStorage storage = (FSMetricSpacesStorage) metricSpacesStorage;
            Iterator it = storage.getIteratorOfObjects(querySetFile, "Q");
            return Tools.getObjectsFromIterator(it);
        }

        @Override
        public Iterator<Object> getMetricObjectsFromDataset(Object... params) {
            FSMetricSpacesStorage storage = (FSMetricSpacesStorage) metricSpacesStorage;
            params = Tools.concatArrays(params, new Object[]{""});
            Iterator it = storage.getIteratorOfObjects(datasetFile, params);
            return it;
        }

        @Override
        public List<Object> getPivots(int objLoadedCount) {
            if (isPCA) {
                return metricSpacesStorage.getPivots("laion2B-en-pca96v2-n=100M.h5", objLoadedCount); // toto funguje?? Kolik?
            }
//            return metricSpacesStorage.getPivots("laion2B-en-clip768v2-n=100M.h5_20000pivots", objLoadedCount);
            return metricSpacesStorage.getPivots("laion2B-en-clip768v2-n=100M.h5_2048pivots", objLoadedCount);
        }

        @Override
        public List<Object> getSampleOfDataset(int objCount) {
            Iterator<Object> it = getMetricObjectsFromDataset();
            return Tools.getObjectsFromIterator(it, objCount);
        }

    }

}
