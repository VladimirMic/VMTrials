/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.tripleFiltering_Challenge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.fs.FSGlobal;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.main.objTransforms.apply.FSApplyPCAMain;
import vm.fs.main.search.filtering.learning.LearnSecondaryFilteringWithGHPSketchesMain;
import vm.fs.metricSpaceImpl.FSMetricSpaceImpl;
import vm.fs.metricSpaceImpl.FSMetricSpacesStorage;
import vm.fs.store.dataTransforms.FSGHPSketchesPivotPairsStorageImpl;
import vm.fs.store.dataTransforms.FSSVDStorageImpl;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.fs.store.queryResults.FSQueryExecutionStatsStoreImpl;
import vm.fs.store.partitioning.FSVoronoiPartitioningStorage;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.AbstractMetricSpacesStorage;
import vm.metricSpace.MainMemoryDatasetCache;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.dataToStringConvertors.SingularisedConvertors;
import vm.metricSpace.datasetPartitioning.impl.VoronoiPartitioning;
import vm.objTransforms.MetricObjectTransformerInterface;
import vm.objTransforms.MetricObjectsParallelTransformerImpl;
import vm.objTransforms.objectToSketchTransformators.AbstractObjectToSketchTransformator;
import vm.objTransforms.objectToSketchTransformators.SketchingGHP;
import vm.objTransforms.perform.PCAPrefixMetricObjectTransformer;
import vm.objTransforms.perform.TransformDataToGHPSketches;
import vm.objTransforms.storeLearned.GHPSketchingPivotPairsStoreInterface;
import vm.search.algorithm.impl.multiFiltering.CranberryAlgorithm;
import static vm.vmtrials.tripleFiltering_Challenge.SISAPChallengeAlgBuilder.MAX_DATASET_SIZE_TO_CACHE;
import static vm.vmtrials.tripleFiltering_Challenge.SISAPChallengeAlgBuilder.MIN_DATASET_SIZE_TO_CACHE;

/**
 *
 * @author Vlada
 */
public class Main {

    public static final Logger LOG = Logger.getLogger(Main.class.getName());
    public static final Integer SKETCH_LENGTH = 512;
    public static final String FILE_WITH_T_OMEGA_THRESHOLDS = "laion2B-en-clip768v2-n=10M.h5_PCA256_q100voronoiP20000_voronoiK200101_pcaLength256_kPCA750.csv";

    private static SISAPChallengeAlgBuilder algBuilder = null;

    private static Boolean makeAllSteps;

    public static void main(String[] args) {
        long buildTime = -System.currentTimeMillis();
        System.err.println("Args: ");
        for (int i = 0; i < args.length; i++) {
            System.err.print(i + ": " + args[i] + " ");
            if (i == 1 || i == 3 || i == 4) {
                System.err.print(" - In fact, I will ignore this parameter, but thank you for the information");
            }
            System.err.println();
        }
        String dataset768DimPath = args[0];
        String querySet768DimPath = args[2];
        makeAllSteps = args.length <= 5 || Boolean.parseBoolean(args[5]);
        int k = args.length <= 6 ? 10 : Integer.parseInt(args[6]);

        ImplicitH5Dataset fullDataset = createImplicitH5Dataset(dataset768DimPath, querySet768DimPath);
        MainMemoryDatasetCache pcaDataset = transformDatasetAndQueriesToPCAPreffixes(fullDataset, 256, 24);

        Dataset sketchesDataset;
        AbstractObjectToSketchTransformator sketchingTechnique;
        if (makeAllSteps) {
            sketchesDataset = buildAndStoreAlgorithm(fullDataset, makeAllSteps);
            sketchingTechnique = getSketchingTechnique(fullDataset);
        } else {
            sketchingTechnique = getSketchingTechnique(fullDataset);
            sketchesDataset = createImplicitSketchesDataset(sketchingTechnique, fullDataset.getDatasetName(), SKETCH_LENGTH, 0.5f);
        }
        System.gc();
        if (algBuilder == null) {
            algBuilder = initAlgorithm(fullDataset, pcaDataset, sketchesDataset, sketchingTechnique, k);
        }
        buildTime += System.currentTimeMillis();
        System.gc();
        List fullQueries = fullDataset.getMetricQueryObjects();
        List pcaQueries = pcaDataset.getMetricQueryObjects();

        AbstractMetricSpace pcaDatasetMetricSpace = pcaDataset.getMetricSpace();

        // key value map to PCA of the query objects
        Map pcaQMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(pcaDatasetMetricSpace, pcaQueries, false);

        AbstractMetricSpace<float[]> fullMetricSpace = fullDataset.getMetricSpace();

        CranberryAlgorithm cranberryAlg = algBuilder.getCranberryAlg();
        fullDataset.unloadPivots();
        pcaDataset.unloadPivots();
        pcaDataset.unloadQueries();
        pcaDataset.unloadDataObjets();
        if (sketchesDataset instanceof MainMemoryDatasetCache) {
            MainMemoryDatasetCache m = (MainMemoryDatasetCache) sketchesDataset;
            m.unloadPivots();
            m.unloadQueries();
            m.unloadDataObjets();
        }
        System.gc();
        vm.javatools.Tools.sleepSeconds(10);

        long queryTime = -System.currentTimeMillis();
        TreeSet[] results = cranberryAlg.completeKnnFilteringWithQuerySet(fullMetricSpace, fullQueries, k, null, pcaDatasetMetricSpace, pcaQMap);

        queryTime += System.currentTimeMillis();
        algBuilder.shutDownThreadPool();

        LOG.log(Level.INFO, "Storing results of queries: buildtime: {0}, querytime: {1}", new Object[]{buildTime, queryTime});
        FSNearestNeighboursStorageImpl resultsStorage = new FSNearestNeighboursStorageImpl(false);
        resultsStorage.storeQueryResults(pcaDatasetMetricSpace, fullQueries, results, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), "");
        Map<String, Object> ret = new HashMap<>();
        ret.put("buildtime", buildTime / 1000f);
        ret.put("querytime", queryTime / 1000f);
        try {
            String mapAsCSVString = Tools.mapAsCSVString(ret, ";", ":");
            mapAsCSVString = mapAsCSVString.substring(0, mapAsCSVString.length() - 1);
            String name = fullDataset.getDatasetName() + "_" + fullDataset.getQuerySetName() + "_run_params.csv";
            File output = new File(FSGlobal.RESULT_FOLDER, name);
            output = FSGlobal.checkFileExistence(output);
            System.setOut(new PrintStream(output));
            System.out.println(mapAsCSVString);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        LOG.log(Level.INFO, "Storing statistics of queries");
        String resultName = "CRANBERRY_COS_FINAL_PAR_" + CranberryAlgorithm.QUERIES_PARALELISM + "_" + cranberryAlg.getMaxDistComps() + "maxDists_" + fullDataset.getDatasetName();
        FSQueryExecutionStatsStoreImpl statsStorage = new FSQueryExecutionStatsStoreImpl(fullDataset.getDatasetName(), fullDataset.getQuerySetName(), k, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), resultName, null);
        statsStorage.storeStatsForQueries(cranberryAlg.getDistCompsPerQueries(), cranberryAlg.getTimesPerQueries(), cranberryAlg.getSimRelsPerQueries());
        statsStorage.save();

        LOG.log(Level.INFO, "Storing results of queries");
        FSNearestNeighboursStorageImpl resultsStorageGZ = new FSNearestNeighboursStorageImpl();
        resultsStorageGZ.storeQueryResults(fullMetricSpace, fullQueries, results, fullDataset.getDatasetName(), fullDataset.getQuerySetName(), resultName);
    }

    private static SISAPChallengeAlgBuilder initAlgorithm(Dataset fullDataset, Dataset pcaDataset, Dataset sketchesDataset, AbstractObjectToSketchTransformator sketchingTechnique, int k) {
        LOG.log(Level.INFO, "Initializing algorithm");
        SISAPChallengeAlgBuilder ret = new SISAPChallengeAlgBuilder(fullDataset, pcaDataset, sketchesDataset, sketchingTechnique, k, FILE_WITH_T_OMEGA_THRESHOLDS);
        Logger.getLogger(Main.class.getName()).log(Level.INFO, "Algorithm initialised");
        return ret;
    }

    /**
     * *************************************************
     * Build indexes and create auxiliary files ********
     * *************************************************
     */
    private static Dataset buildAndStoreAlgorithm(Dataset fullDataset, boolean makeAllSteps) {
        LOG.log(Level.INFO, "\nStarting the sketching transformation with the predefined sketching technique");
        MainMemoryDatasetCache sketchesDataset = new MainMemoryDatasetCache(new FSMetricSpaceImpl());
        AbstractObjectToSketchTransformator sketchingTechnique = createSketches(fullDataset, sketchesDataset);
        String name = sketchingTechnique.getNameOfTransformedSetOfObjects(fullDataset.getDatasetName(), SKETCH_LENGTH, 0.5f);
        sketchesDataset.setName(name);
        System.gc();
        LOG.log(Level.INFO, "\nStarting the learn of the Secondary filtering with sketches");
        if (makeAllSteps) {
            learnSketchMapping(fullDataset, sketchesDataset, 0.004f, SKETCH_LENGTH, 2f);
        }
        System.gc();
        if (makeAllSteps) {
            LOG.log(Level.INFO, "\nStarting the Voronoi partitioning");
            createAndStoreVoronoiPartitioning(fullDataset, sketchesDataset.getDatasetSize());
            System.gc();
        }
        System.gc();
        LOG.log(Level.INFO, "\nBuild finished");
        return sketchesDataset;

    }

    private static void createAndStoreVoronoiPartitioning(Dataset dataset, int datasetSize) {
        int pivotCount = EvaluateCRANBERRYMain.getPivotCountForVoronoi(datasetSize);
        List<Object> pivots = dataset.getPivots(pivotCount);
        VoronoiPartitioning vp = new VoronoiPartitioning(dataset.getMetricSpace(), dataset.getDistanceFunction(), pivots);
        FSVoronoiPartitioningStorage storage = new FSVoronoiPartitioningStorage();
        vp.partitionObjects(dataset.getMetricObjectsFromDataset(), dataset.getDatasetName(), storage, pivotCount);
    }

    private static AbstractObjectToSketchTransformator createSketches(Dataset fullDataset, MainMemoryDatasetCache resultsDataset) {
        AbstractMetricSpacesStorage storageForSketches = new FSMetricSpacesStorage(new FSMetricSpaceImpl<>(), SingularisedConvertors.LONG_VECTOR_SPACE);
        GHPSketchingPivotPairsStoreInterface storageOfPivotPairs = new FSGHPSketchesPivotPairsStorageImpl();
        TransformDataToGHPSketches evaluator = new TransformDataToGHPSketches(fullDataset, storageOfPivotPairs, storageForSketches, 0.5f, -1);
        int[] sketchesLengths = new int[]{SKETCH_LENGTH};
        String[] sketchesPairsName = new String[]{"laion2B-en-clip768v2-n=100M.h5_GHP_50_" + SKETCH_LENGTH};
        AbstractObjectToSketchTransformator ret = evaluator.createSketchesForDatasetPivotsAndQueries(sketchesLengths, sketchesPairsName, resultsDataset);
        return ret;
    }

    private static AbstractObjectToSketchTransformator getSketchingTechnique(Dataset dataset) {
        SketchingGHP ret = new SketchingGHP(dataset.getDistanceFunction(), dataset.getMetricSpace(), dataset.getPivots(-1), "laion2B-en-clip768v2-n=100M.h5_GHP_50_512", new FSGHPSketchesPivotPairsStorageImpl());
        return ret;
    }

    /**
     * *************************************************
     * Create implicit datasets - full and PCA dataset *
     * *************************************************
     */
    private static ImplicitH5Dataset createImplicitH5Dataset(String datasetPath, String querySetPath) {
        return new Main.ImplicitH5Dataset(datasetPath, querySetPath);
    }

    private static void learnSketchMapping(Dataset fullDataset, Dataset sketchesDataset, float distIntervalForpx, int sketchLength, float maxDist) {
        LearnSecondaryFilteringWithGHPSketchesMain.run(fullDataset, sketchesDataset, distIntervalForpx, sketchLength, maxDist);
    }

    private static Dataset createImplicitSketchesDataset(AbstractObjectToSketchTransformator sketchingTechnique, String fullDatasetName, int sketchLength, float balance) {
        String name = sketchingTechnique.getNameOfTransformedSetOfObjects(fullDatasetName, sketchLength, balance);
        FSDatasetInstanceSingularizator.FSHammingSpaceDataset dataset = new FSDatasetInstanceSingularizator.FSHammingSpaceDataset(name);
        return dataset;
    }

    private static MainMemoryDatasetCache transformDatasetAndQueriesToPCAPreffixes(Dataset dataset, int pcaLength, int storedPrefix) {
        String datasetUsedToLearnSVD = "laion2B-en-clip768v2-n=100M.h5";
        AbstractMetricSpace<float[]> metricSpace = dataset.getMetricSpace();
        AbstractMetricSpacesStorage metricSpacesStorage = dataset.getMetricSpacesStorage();
        int sampleSetSize = 500000;
        FSSVDStorageImpl svdStorage = new FSSVDStorageImpl(datasetUsedToLearnSVD, sampleSetSize, false);
        float[][] vtMatrixFull = svdStorage.getVTMatrix();

        float[][] vtMatrix = Tools.shrinkMatrix(vtMatrixFull, pcaLength, vtMatrixFull[0].length);

        MetricObjectTransformerInterface pca = new PCAPrefixMetricObjectTransformer(vtMatrix, svdStorage.getMeansOverColumns(), metricSpace, metricSpace, storedPrefix);
        String newName = pca.getNameOfTransformedSetOfObjects(dataset.getDatasetName());
        MainMemoryDatasetCache cachedDataset = new MainMemoryDatasetCache(metricSpace, newName);
        if (makeAllSteps) {
            LOG.log(Level.INFO, "\nTransform to the prefixes of PCA start");
            MetricObjectsParallelTransformerImpl parallelTransformerImpl = new MetricObjectsParallelTransformerImpl(pca, metricSpacesStorage, pca.getNameOfTransformedSetOfObjects(dataset.getDatasetName(), false));
            FSApplyPCAMain.transformPivots(dataset.getPivots(-1).iterator(), parallelTransformerImpl, "Pivot set with name \"" + datasetUsedToLearnSVD + "\" transformed by VT matrix of svd " + sampleSetSize + " to the length " + pcaLength, cachedDataset);
            FSApplyPCAMain.transformQueryObjects(dataset.getMetricQueryObjects().iterator(), parallelTransformerImpl, "Query set with name \"" + datasetUsedToLearnSVD + "\" transformed by VT matrix of svd " + sampleSetSize + " to the length " + pcaLength, cachedDataset);
            FSApplyPCAMain.transformDataset(dataset.getMetricObjectsFromDataset(), parallelTransformerImpl, "Dataset with name \"" + datasetUsedToLearnSVD + "\" transformed by VT matrix of svd " + sampleSetSize + " to the length " + pcaLength, cachedDataset);
            LOG.log(Level.INFO, "\nTransform to the prefixes of PCA finished");
        }
        int datasetSize = cachedDataset.getDatasetSize();
        if (datasetSize >= MIN_DATASET_SIZE_TO_CACHE && datasetSize <= MAX_DATASET_SIZE_TO_CACHE && dataset instanceof ImplicitH5Dataset) {
            ImplicitH5Dataset h5Dataset = (ImplicitH5Dataset) dataset;
            h5Dataset.loadAllDataObjetsToRam();
        }
        return cachedDataset;
    }

    private static class ImplicitH5Dataset extends FSDatasetInstanceSingularizator.H5FloatVectorDataset {

        private final File datasetFile;
        private final File querySetFile;
        private final String querySetName;
        private final MainMemoryDatasetCache cache;

        public ImplicitH5Dataset(String datasetPath, String querySetPath) {
            super(new File(datasetPath).getName());
            querySetName = new File(querySetPath).getName();
            this.datasetFile = FSGlobal.checkFileExistence(new File(datasetPath), false);
            this.querySetFile = FSGlobal.checkFileExistence(new File(querySetPath), false);
            cache = new MainMemoryDatasetCache(metricSpace, datasetName, metricSpacesStorage);
            cache.addPivots(getPivots(-1));
            cache.addQueries(getMetricQueryObjects());
        }

        @Override
        public String getQuerySetName() {
            return querySetName;
        }

        @Override
        public final List<Object> getMetricQueryObjects() {
            if (cache.queriesLoaded()) {
                return cache.getMetricQueryObjects();
            }
            FSMetricSpacesStorage storage = (FSMetricSpacesStorage) metricSpacesStorage;
            Iterator it = storage.getIteratorOfObjects(querySetFile, "Q");
            return Tools.getObjectsFromIterator(it);
        }

        @Override
        public Iterator<Object> getMetricObjectsFromDataset(Object... params) {
            if (cache.dataLoaded()) {
                return cache.getMetricObjectsFromDataset(params);
            }
            FSMetricSpacesStorage storage = (FSMetricSpacesStorage) metricSpacesStorage;
            params = Tools.concatArrays(params, new Object[]{""});
            Iterator it = storage.getIteratorOfObjects(datasetFile, params);
            return it;
        }

        @Override
        public Map<Object, Object> getKeyValueStorage() {
            if (cache.dataLoaded()) {
                return cache.getKeyValueStorage();
            }
            return super.getKeyValueStorage();
        }

        @Override
        public final List<Object> getPivots(int objLoadedCount) {
            if (cache.pivotsLoaded()) {
                return cache.getPivots(objLoadedCount);
            }
            return metricSpacesStorage.getPivots("laion2B-en-clip768v2-n=100M.h5_20000pivots.gz", objLoadedCount);
        }

        @Override
        public List<Object> getSampleOfDataset(int objCount) {
            Iterator<Object> it = getMetricObjectsFromDataset();
            return Tools.getObjectsFromIterator(it, objCount);
        }

        public void loadAllDataObjetsToRam() {
            cache.loadAllDataObjets();
        }

        public void unloadPivots() {
            cache.unloadPivots();
        }

        public void unloadQueries() {
            cache.unloadQueries();
        }

        public void unloadDataObjets() {
            cache.unloadDataObjets();
        }

    }

}
