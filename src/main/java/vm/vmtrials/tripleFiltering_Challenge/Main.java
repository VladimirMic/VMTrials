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
import vm.metricSpace.MetricSpacesStorageInterface;
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
        }
        int param = 0;
        String dataset768DimPath = args[param++];
        param++;
        String querySet768DimPath = args[param++];
        param++;
        int datasetSize = Integer.parseInt(args[param++]);
        boolean build = args.length <= param || Boolean.parseBoolean(args[param++]);
        makeAllSteps = build;
        int k = args.length <= param ? 10 : Integer.parseInt(args[param++]);

        Dataset fullDataset = createImplicitH5Dataset(dataset768DimPath, querySet768DimPath);
        Dataset pcaDataset = transformDatasetAndQueriesToPCAPreffixes(fullDataset, 256, 24);

        Dataset sketchesDataset;
        AbstractObjectToSketchTransformator sketchingTechnique;
        if (makeAllSteps) {
            sketchesDataset = buildAndStoreAlgorithm(fullDataset, datasetSize, makeAllSteps);
            sketchingTechnique = getSketchingTechnique(fullDataset);
        } else {
            sketchingTechnique = getSketchingTechnique(fullDataset);
            sketchesDataset = createImplicitSketchesDataset(sketchingTechnique, fullDataset.getDatasetName(), SKETCH_LENGTH, 0.5f);
        }

        if (algBuilder == null) {
            algBuilder = initAlgorithm(fullDataset, pcaDataset, sketchesDataset, sketchingTechnique, datasetSize, k);
        }
        buildTime += System.currentTimeMillis();
        List fullQueries = fullDataset.getMetricQueryObjects();
        List pcaQueries = pcaDataset.getMetricQueryObjects();

        AbstractMetricSpace pcaDatasetMetricSpace = pcaDataset.getMetricSpace();

        // key value map to PCA of the query objects
        Map pcaQMap = ToolsMetricDomain.getMetricObjectsAsIdObjectMap(pcaDatasetMetricSpace, pcaQueries, false);

        AbstractMetricSpace<float[]> fullMetricSpace = fullDataset.getMetricSpace();

        CranberryAlgorithm cranberryAlg = algBuilder.getCranberryAlg();
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

    private static SISAPChallengeAlgBuilder initAlgorithm(Dataset fullDataset, Dataset pcaDataset, Dataset sketchesDataset, AbstractObjectToSketchTransformator sketchingTechnique, int datasetSize, int k) {
        LOG.log(Level.INFO, "Initializing algorithm");

        int pivotsUsedForTheVoronoi = getPivotCount(datasetSize);
        int voronoiK = getVoronoiK(datasetSize);
        int kPCA = getPCAK(datasetSize);
        String fileWithTOmegaThresholds = "laion2B-en-clip768v2-n=30M.h5_PCA256_q200voronoiP20000_voronoiK600000_pcaLength256_kPCA100.csv";
        SISAPChallengeAlgBuilder ret = new SISAPChallengeAlgBuilder(fullDataset, pcaDataset, sketchesDataset, sketchingTechnique, voronoiK, kPCA, k, pivotsUsedForTheVoronoi, fileWithTOmegaThresholds);
        Logger.getLogger(Main.class.getName()).log(Level.INFO, "Algorithm initialised");
        return ret;
    }

    /**
     * *************************************************
     * Init params for datasets given by their size ****
     * *************************************************
     */
    public static int getVoronoiK(int datasetSize) {
        switch (datasetSize) {
            case 100000:
                return 40000;
            case 300000:
                return 150000;
            case 10000000:
                return 400000;
            case 30000000:
                return 900000;
            case 100000000:
                return 1000000;
            default:
                throw new AssertionError();
        }
    }

    private static int getPivotCount(int datasetSize) {
        if (datasetSize <= 300000) {
            return 100;
        }
        return 20000;
    }

    private static int getPCAK(int datasetSize) {
        switch (datasetSize) {
            case 100000:
            case 300000:
                return 100;
            case 10000000:
                return 100;
            case 30000000:
                return 100;
            case 100000000:
                return 100;
            default:
                throw new AssertionError();
        }
    }

    /**
     * *************************************************
     * Build indexes and create auxiliary files ********
     * *************************************************
     */
    private static Dataset buildAndStoreAlgorithm(Dataset fullDataset, int datasetSize, boolean makeAllSteps) {
        if (makeAllSteps) {
            LOG.log(Level.INFO, "\nStarting the Voronoi partitioning");
            createAndStoreVoronoiPartitioning(fullDataset, datasetSize);
            System.gc();
        }
        LOG.log(Level.INFO, "\nStarting the sketching transformation with the predefined sketching technique");
        AbstractObjectToSketchTransformator sketchingTechnique = createSketches(fullDataset);
        Dataset sketchesDataset = createImplicitSketchesDataset(sketchingTechnique, fullDataset.getDatasetName(), SKETCH_LENGTH, 0.5f);
        System.gc();
        LOG.log(Level.INFO, "\nStarting the learn of the Secondary filtering with sketches");
        if (makeAllSteps) {
            learnSketchMapping(fullDataset, sketchesDataset, 0.004f, SKETCH_LENGTH, 2f);
        }
        System.gc();
        LOG.log(Level.INFO, "\nBuild finished");
        return sketchesDataset;

    }

    private static void createAndStoreVoronoiPartitioning(Dataset dataset, int datasetSize) {
        int pivotCount = getPivotCount(datasetSize);
        List<Object> pivots = dataset.getPivots(2 * pivotCount);
        VoronoiPartitioning vp = new VoronoiPartitioning(dataset.getMetricSpace(), dataset.getDistanceFunction(), pivots);
        FSVoronoiPartitioningStorage storage = new FSVoronoiPartitioningStorage();
        vp.splitByVoronoi(dataset.getMetricObjectsFromDataset(), dataset.getDatasetName(), pivotCount, storage);
    }

    private static AbstractObjectToSketchTransformator createSketches(Dataset fullDataset) {
        MetricSpacesStorageInterface storageForSketches = new FSMetricSpacesStorage(new FSMetricSpaceImpl<>(), SingularisedConvertors.LONG_VECTOR_SPACE);
        GHPSketchingPivotPairsStoreInterface storageOfPivotPairs = new FSGHPSketchesPivotPairsStorageImpl();
        TransformDataToGHPSketches evaluator = new TransformDataToGHPSketches(fullDataset, storageOfPivotPairs, storageForSketches, 0.5f, -1);
        int[] sketchesLengths = new int[]{SKETCH_LENGTH};
        String[] sketchesPairsName = new String[]{"laion2B-en-clip768v2-n=100M.h5_GHP_50_" + SKETCH_LENGTH};
        AbstractObjectToSketchTransformator ret = evaluator.createSketchesForDatasetPivotsAndQueries(sketchesLengths, sketchesPairsName);
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
        MetricSpacesStorageInterface metricSpacesStorage = dataset.getMetricSpacesStorage();
        int sampleSetSize = 500000;
        FSSVDStorageImpl svdStorage = new FSSVDStorageImpl(datasetUsedToLearnSVD, sampleSetSize, false);
        float[][] vtMatrixFull = svdStorage.getVTMatrix();

        float[][] vtMatrix = Tools.shrinkMatrix(vtMatrixFull, pcaLength, vtMatrixFull[0].length);

        MetricObjectTransformerInterface pca = new PCAPrefixMetricObjectTransformer(vtMatrix, svdStorage.getMeansOverColumns(), metricSpace, metricSpace, storedPrefix);
        if (makeAllSteps) {
            LOG.log(Level.INFO, "\nTransform to the prefixes of PCA start");
            MetricObjectsParallelTransformerImpl parallelTransformerImpl = new MetricObjectsParallelTransformerImpl(pca, metricSpacesStorage, pca.getNameOfTransformedSetOfObjects(dataset.getDatasetName(), false));
            FSApplyPCAMain.transformPivots(dataset.getPivots(-1).iterator(), parallelTransformerImpl, "Pivot set with name \"" + datasetUsedToLearnSVD + "\" transformed by VT matrix of svd " + sampleSetSize + " to the length " + pcaLength);
            FSApplyPCAMain.transformQueryObjects(dataset.getMetricQueryObjects().iterator(), parallelTransformerImpl, "Query set with name \"" + datasetUsedToLearnSVD + "\" transformed by VT matrix of svd " + sampleSetSize + " to the length " + pcaLength);
            FSApplyPCAMain.transformDataset(dataset.getMetricObjectsFromDataset(), parallelTransformerImpl, "Dataset with name \"" + datasetUsedToLearnSVD + "\" transformed by VT matrix of svd " + sampleSetSize + " to the length " + pcaLength);
            LOG.log(Level.INFO, "\nTransform to the prefixes of PCA finished");
        }
        String newName = pca.getNameOfTransformedSetOfObjects(dataset.getDatasetName());
        Dataset ret = new FSDatasetInstanceSingularizator.FSFloatVectorDataset(newName);
        return ret;
    }

    private static class ImplicitH5Dataset extends FSDatasetInstanceSingularizator.H5FloatVectorDataset {

        private final File datasetFile;
        private final File querySetFile;
        private final String querySetName;

        public ImplicitH5Dataset(String datasetPath, String querySetPath) {
            super(new File(datasetPath).getName());
            querySetName = new File(querySetPath).getName();
            this.datasetFile = new File(datasetPath);
            this.querySetFile = new File(querySetPath);
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
            return metricSpacesStorage.getPivots("laion2B-en-clip768v2-n=100M.h5_20000pivots.gz", objLoadedCount);
        }

        @Override
        public List<Object> getSampleOfDataset(int objCount) {
            Iterator<Object> it = getMetricObjectsFromDataset();
            return Tools.getObjectsFromIterator(it, objCount);
        }

    }

}
