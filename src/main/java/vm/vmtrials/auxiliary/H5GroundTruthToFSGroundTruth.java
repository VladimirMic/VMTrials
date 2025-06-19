package vm.vmtrials.auxiliary;

import io.jhdf.HdfFile;
import io.jhdf.api.Dataset;
import io.jhdf.api.Node;
import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.fs.searchSpaceImpl.FSSearchSpaceImpl;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.search.algorithm.impl.GroundTruthEvaluator;

/**
 *
 * @author xmic
 */
public class H5GroundTruthToFSGroundTruth {

    public static final Logger LOG = Logger.getLogger(H5GroundTruthToFSGroundTruth.class.getName());

    public static void main(String[] args) {
        String groundTruthName = "laion2B-en-private-gold-standard-v2-30M-F64-IEEE754";
        String path = "h:\\Similarity_search\\Result\\ground_truth\\" + groundTruthName + ".h5";
        Iterator[] it = parseH5GroundTruth(path);
        TreeSet<Map.Entry<Comparable, Float>>[] results = GroundTruthEvaluator.initKNNResultSets(10000);
        List<Object> queries = new ArrayList<>();
        for (int i = 0; it[0].hasNext() && it[1].hasNext(); i++) {
            AbstractMap.SimpleEntry<String, int[]> nextIDs = (AbstractMap.SimpleEntry<String, int[]>) it[0].next();
            AbstractMap.SimpleEntry<String, float[]> nextDist = (AbstractMap.SimpleEntry<String, float[]>) it[1].next();
            String qID = nextDist.getKey();
            qID = "Q" + qID;
            queries.add(new AbstractMap.SimpleEntry<>(qID, null));
            int[] ids = nextIDs.getValue();
            float[] dists = nextDist.getValue();
            TreeSet<Map.Entry<Comparable, Float>> queryResult = results[i];
            for (int j = 0; j < ids.length; j++) {
                Integer nnID = ids[j];
                Float dist = dists[j];
                queryResult.add(new AbstractMap.SimpleEntry<>(nnID, dist));
            }
        }
        FSNearestNeighboursStorageImpl resultsStorage = new FSNearestNeighboursStorageImpl();
        FSSearchSpaceImpl metricSpace = new FSSearchSpaceImpl(null);
        resultsStorage.storeQueryResults(metricSpace, queries, results, null, groundTruthName, groundTruthName, "ground_truth");
    }

    private static Iterator[] parseH5GroundTruth(String path) {
        File f = new File(path);
        HdfFile hdfFile = new HdfFile(f.toPath());
        Iterator<Node> iterator = hdfFile.iterator();
        H5FileRowIntIterator ids = null;
        H5FileRowFloatIterator dists = null;
        for (int i = 0; iterator.hasNext(); i++) {
            Node node = iterator.next();
            String name = node.getName();
            LOG.log(Level.INFO, "Returning data from the dataset (group) {0} in the file {1}", new Object[]{name, f.getName()});
            Dataset dataset = hdfFile.getDatasetByPath(name);
            if (i == 0) {
                ids = new H5FileRowIntIterator(hdfFile, dataset);
            } else {
                dists = new H5FileRowFloatIterator(hdfFile, dataset);
            }
        }
        return new Iterator[]{ids, dists};
    }

    private static class H5FileRowIntIterator extends H5FileRowIterator<int[]> {

        private H5FileRowIntIterator(HdfFile hdfFile, Dataset dataset) {
            super(hdfFile, dataset, "", -1);
        }

    }

    private static class H5FileRowFloatIterator extends H5FileRowIterator<float[]> {

        private H5FileRowFloatIterator(HdfFile hdfFile, Dataset dataset) {
            super(hdfFile, dataset, "", -1);
        }

    }

    private static class H5FileRowIterator<T> implements Iterator<AbstractMap.SimpleEntry<String, T>> {

        protected AbstractMap.SimpleEntry<String, T> nextObject;
        protected AbstractMap.SimpleEntry<String, T> currentObject;

        private final HdfFile hdfFile;
        private final Dataset dataset;
        private final int maxCount;
        private final int[] vectorDimensions;
        private final String prefixFoIDs;
        private final long[] counter;

        private H5FileRowIterator(HdfFile hdfFile, Dataset dataset, String prefix, int maxCount) {
            if (maxCount < 0) {
                maxCount = Integer.MAX_VALUE;
            }
            this.hdfFile = hdfFile;
            this.dataset = dataset;
            int[] storageDimensions = dataset.getDimensions();
            this.maxCount = Math.min(maxCount, storageDimensions[0]);
            this.vectorDimensions = new int[]{1, storageDimensions[1]};
            this.prefixFoIDs = prefix;
            counter = new long[]{0, 0};
            nextObject = nextStreamObject();
        }

        @Override
        public boolean hasNext() {
            boolean ret = nextObject != null;
            if (!ret) {
                hdfFile.close();
            }
            return ret;
        }

        @Override
        public AbstractMap.SimpleEntry<String, T> next() {
            if (nextObject == null) {
                throw new NoSuchElementException("No more objects in the stream");
            }
            currentObject = nextObject;
            nextObject = nextStreamObject();
            return currentObject;
        }

        private AbstractMap.SimpleEntry<String, T> nextStreamObject() {
            if (counter[0] >= maxCount) {
                return null;
            }
            T[] dataBuffer = (T[]) dataset.getData(counter, vectorDimensions);
            String id = prefixFoIDs + (counter[0] + 1);
            AbstractMap.SimpleEntry<String, T> entry = new AbstractMap.SimpleEntry<>(id, dataBuffer[0]);
            counter[0]++;
            return entry;
        }
    }

}
