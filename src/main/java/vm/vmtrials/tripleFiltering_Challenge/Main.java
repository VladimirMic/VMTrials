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
import vm.fs.store.voronoiPartitioning.FSVoronoiPartitioningStorage;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.AbstractMetricSpacesStorage;
import vm.metricSpace.MainMemoryDatasetChache;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.dataToStringConvertors.SingularisedConvertors;
import vm.metricSpace.voronoiPartitioning.VoronoiPartitioning;
import vm.objTransforms.MetricObjectTransformerInterface;
import vm.objTransforms.MetricObjectsParallelTransformerImpl;
import vm.objTransforms.objectToSketchTransformators.AbstractObjectToSketchTransformator;
import vm.objTransforms.objectToSketchTransformators.SketchingGHP;
import vm.objTransforms.perform.PCAPrefixMetricObjectTransformer;
import vm.objTransforms.perform.TransformDataToGHPSketches;
import vm.objTransforms.storeLearned.GHPSketchingPivotPairsStoreInterface;
import vm.search.impl.multiFiltering.CranberryAlgorithm;

/**
 *
 * @author Vlada
 */
public class Main {

    public static final Logger LOG = Logger.getLogger(Main.class.getName());
    public static final Integer SKETCH_LENGTH = 512;

    private static SISAPChallengeAlgBuilder algBuilder = null;

    private static Boolean makeAllSteps;

    public static void main(String[] args) {
        long buildTime = -System.currentTimeMillis();
        System.err.println("Args: ");
        for (int i = 0; i < args.length; i++) {
            System.err.println(i + ": " + args[i] + " ");
            if (i == 1 || i == 3 || i == 4) {
                System.err.println("In fact, I will igonre this parameter, sorry for confusion");
            }
        }
        String dataset768DimPath = args[0];
        String querySet768DimPath = args[2];
        makeAllSteps = args.length <= 5 || Boolean.parseBoolean(args[5]);
        int k = args.length <= 6 ? 10 : Integer.parseInt(args[6]);

        Dataset fullDataset = createImplicitH5Dataset(dataset768DimPath, querySet768DimPath);
        Dataset pcaDataset = transformDatasetAndQueriesToPCAPreffixes(fullDataset, 256, 24);

        Dataset sketchesDataset;
        AbstractObjectToSketchTransformator sketchingTechnique;
        if (makeAllSteps) {
            sketchesDataset = buildAndStoreAlgorithm(fullDataset, makeAllSteps);
            sketchingTechnique = getSketchingTechnique(fullDataset);
        } else {
            sketchingTechnique = getSketchingTechnique(fullDataset);
            sketchesDataset = createImplicitSketchesDataset(sketchingTechnique, fullDataset.getDatasetName(), SKETCH_LENGTH, 0.5f);
        }

        if (algBuilder == null) {
            algBuilder = initAlgorithm(fullDataset, pcaDataset, sketchesDataset, sketchingTechnique, k);
        }
        buildTime += System.currentTimeMillis();
        List fullQueries = fullDataset.getMetricQueryObjects();
        List pcaQueries = pcaDataset.getMetricQueryObjects();

        AbstractMetricSpace pcaDatasetMetricSpace = pcaDataset.getMetricSpace();

        // key value map to PCA of the query objects
        Map pcaQMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(pcaDatasetMetricSpace, pcaQueries, false);

        AbstractMetricSpace<float[]> fullMetricSpace = fullDataset.getMetricSpace();

        CranberryAlgorithm cranberryAlg = algBuilder.getCranberryAlg();
        System.gc();
        vm.javatools.Tools.sleepSeconds(5);

        long queryTime = -System.currentTimeMillis();
        TreeSet[] results = cranberryAlg.completeKnnSearchOfQuerySet(fullMetricSpace, fullQueries, k, null, pcaDatasetMetricSpace, pcaQMap);

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
    }

    private static SISAPChallengeAlgBuilder initAlgorithm(Dataset fullDataset, Dataset pcaDataset, Dataset sketchesDataset, AbstractObjectToSketchTransformator sketchingTechnique, int k) {
        LOG.log(Level.INFO, "Initializing algorithm");

        int pivotsUsedForTheVoronoi = getPivotCount();
        int kPCA = getPCAK();
        String fileWithTOmegaThresholds = "laion2B-en-clip768v2-n=30M.h5_PCA256_q200voronoiP20000_voronoiK600000_pcaLength256_kPCA100.csv";
        SISAPChallengeAlgBuilder ret = new SISAPChallengeAlgBuilder(fullDataset, pcaDataset, sketchesDataset, sketchingTechnique, kPCA, k, pivotsUsedForTheVoronoi, fileWithTOmegaThresholds);
        Logger.getLogger(Main.class.getName()).log(Level.INFO, "Algorithm initialised");
        return ret;
    }

    /**
     * *************************************************
     * Init params for datasets given by their size ****
     * *************************************************
     */
    private static int getPivotCount() {
        return 20000;
    }

    private static int getPCAK() {
        return 100;
    }

    /**
     * *************************************************
     * Build indexes and create auxiliary files ********
     * *************************************************
     */
    private static Dataset buildAndStoreAlgorithm(Dataset fullDataset, boolean makeAllSteps) {
        if (makeAllSteps) {
            LOG.log(Level.INFO, "\nStarting the Voronoi partitioning");
            createAndStoreVoronoiPartitioning(fullDataset);
            System.gc();
        }
        LOG.log(Level.INFO, "\nStarting the sketching transformation with the predefined sketching technique");
        MainMemoryDatasetChache sketchesDataset = new MainMemoryDatasetChache(new FSMetricSpaceImpl());
        AbstractObjectToSketchTransformator sketchingTechnique = createSketches(fullDataset, sketchesDataset);
        String name = sketchingTechnique.getNameOfTransformedSetOfObjects(fullDataset.getDatasetName(), SKETCH_LENGTH, 0.5f);
        sketchesDataset.setName(name);
        System.gc();
        LOG.log(Level.INFO, "\nStarting the learn of the Secondary filtering with sketches");
        if (makeAllSteps) {
            learnSketchMapping(fullDataset, sketchesDataset, 0.004f, SKETCH_LENGTH, 2f);
        }
        System.gc();
        LOG.log(Level.INFO, "\nBuild finished");
        return sketchesDataset;

    }

    private static void createAndStoreVoronoiPartitioning(Dataset dataset) {
        int pivotCount = getPivotCount();
        List<Object> pivots = dataset.getPivots(2 * pivotCount);
        VoronoiPartitioning vp = new VoronoiPartitioning(dataset.getMetricSpace(), dataset.getDistanceFunction(), pivots);
        FSVoronoiPartitioningStorage storage = new FSVoronoiPartitioningStorage();
        vp.splitByVoronoi(dataset.getMetricObjectsFromDataset(), dataset.getDatasetName(), pivotCount, storage);
    }

    private static AbstractObjectToSketchTransformator createSketches(Dataset fullDataset, MainMemoryDatasetChache resultsDataset) {
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
    private static Dataset createImplicitH5Dataset(String datasetPath, String querySetPath) {
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

    private static Dataset transformDatasetAndQueriesToPCAPreffixes(Dataset dataset, int pcaLength, int storedPrefix) {
        String datasetUsedToLearnSVD = "laion2B-en-clip768v2-n=100M.h5";
        AbstractMetricSpace<float[]> metricSpace = dataset.getMetricSpace();
        AbstractMetricSpacesStorage metricSpacesStorage = dataset.getMetricSpacesStorage();
        int sampleSetSize = 500000;
        FSSVDStorageImpl svdStorage = new FSSVDStorageImpl(datasetUsedToLearnSVD, sampleSetSize, false);
        float[][] vtMatrixFull = svdStorage.getVTMatrix();

        float[][] vtMatrix = Tools.shrinkMatrix(vtMatrixFull, pcaLength, vtMatrixFull[0].length);

        MetricObjectTransformerInterface pca = new PCAPrefixMetricObjectTransformer(vtMatrix, svdStorage.getMeansOverColumns(), metricSpace, metricSpace, storedPrefix);
        String newName = pca.getNameOfTransformedSetOfObjects(dataset.getDatasetName());
        MainMemoryDatasetChache cachedDataset = new MainMemoryDatasetChache(metricSpace, newName);
        if (makeAllSteps) {
            LOG.log(Level.INFO, "\nTransform to the prefixes of PCA start");
            MetricObjectsParallelTransformerImpl parallelTransformerImpl = new MetricObjectsParallelTransformerImpl(pca, metricSpacesStorage, pca.getNameOfTransformedSetOfObjects(dataset.getDatasetName(), false));
            FSApplyPCAMain.transformPivots(dataset.getPivots(-1).iterator(), parallelTransformerImpl, "Pivot set with name \"" + datasetUsedToLearnSVD + "\" transformed by VT matrix of svd " + sampleSetSize + " to the length " + pcaLength, cachedDataset);
            FSApplyPCAMain.transformQueryObjects(dataset.getMetricQueryObjects().iterator(), parallelTransformerImpl, "Query set with name \"" + datasetUsedToLearnSVD + "\" transformed by VT matrix of svd " + sampleSetSize + " to the length " + pcaLength, cachedDataset);
            FSApplyPCAMain.transformDataset(dataset.getMetricObjectsFromDataset(), parallelTransformerImpl, "Dataset with name \"" + datasetUsedToLearnSVD + "\" transformed by VT matrix of svd " + sampleSetSize + " to the length " + pcaLength, cachedDataset);
            LOG.log(Level.INFO, "\nTransform to the prefixes of PCA finished");
        }
        return cachedDataset;
    }

    private static class ImplicitH5Dataset extends FSDatasetInstanceSingularizator.H5FloatVectorDataset {

        private final File datasetFile;
        private final File querySetFile;
        private final String querySetName;
        private final MainMemoryDatasetChache cache;

        public ImplicitH5Dataset(String datasetPath, String querySetPath) {
            super(new File(datasetPath).getName());
            querySetName = new File(querySetPath).getName();
            this.datasetFile = FSGlobal.checkFileExistence(new File(datasetPath), false);
            this.querySetFile = FSGlobal.checkFileExistence(new File(querySetPath), false);
            cache = new MainMemoryDatasetChache(metricSpace);
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
            FSMetricSpacesStorage storage = (FSMetricSpacesStorage) metricSpacesStorage;
            params = Tools.concatArrays(params, new Object[]{""});
            Iterator it = storage.getIteratorOfObjects(datasetFile, params);
            return it;
        }

        @Override
        public final List<Object> getPivots(int objLoadedCount) {
            if (cache.pivotsLoaded()) {
                return cache.getPivots();
            }
            return metricSpacesStorage.getPivots("laion2B-en-clip768v2-n=100M.h5_20000pivots.gz", objLoadedCount);
        }

        @Override
        public List<Object> getSampleOfDataset(int objCount) {
            Iterator<Object> it = getMetricObjectsFromDataset();
            return Tools.getObjectsFromIterator(it, objCount);
        }

    }

}
