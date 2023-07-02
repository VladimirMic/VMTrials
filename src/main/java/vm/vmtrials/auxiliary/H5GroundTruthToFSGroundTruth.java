/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.auxiliary;

import io.jhdf.HdfFile;
import io.jhdf.api.Dataset;
import io.jhdf.api.Node;
import java.io.File;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author xmic
 */
public class H5GroundTruthToFSGroundTruth {

    public static final Logger LOG = Logger.getLogger(H5GroundTruthToFSGroundTruth.class.getName());

    public static void main(String[] args) {
        String path = "c:\\Data\\Similarity_search\\Result\\ground_truth\\laion2B-en-public-gold-standard-v2-100M-F64-IEEE754.h5";
        Iterator[] it = parseH5GroundTruth(path);
        while (it[0].hasNext() && it[1].hasNext()) {
            AbstractMap.SimpleEntry<String, int[]> nextID = (AbstractMap.SimpleEntry<String, int[]>) it[0].next();
            AbstractMap.SimpleEntry<String, float[]> nextDist = (AbstractMap.SimpleEntry<String, float[]>) it[1].next();
            String s = "";
        }
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
