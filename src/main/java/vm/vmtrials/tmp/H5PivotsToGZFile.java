package vm.vmtrials.tmp;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import vm.fs.dataset.FSDatasetInstances;
import vm.fs.searchSpaceImpl.FSSearchSpacesStorage;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.data.toStringConvertors.SingularisedConvertors;

/**
 *
 * @author Vlada
 */
public class H5PivotsToGZFile {

    public static void main(String[] args) {
        Dataset[] datasets = new Dataset[]{
            new FSDatasetInstances.LAION_100M_PCA96Dataset(),
            //
            new FSDatasetInstances.LAION_100M_PCA32Dataset()
        };

        Dataset fullDataset = new FSDatasetInstances.LAION_100M_Dataset(true);
        for (Dataset pcaDataset : datasets) {
            run(fullDataset, pcaDataset);
        }

    }

    private static void run(Dataset fullDataset, Dataset pcaDataset) {
        List pivots = fullDataset.getPivots(-1);
        AbstractSearchSpace metricSpace = fullDataset.getSearchSpace();
        Map map = pcaDataset.getKeyValueStorage();
        List pcaPivots = new ArrayList();
        for (Object pivot : pivots) {
            Comparable pId = metricSpace.getIDOfObject(pivot);
            System.out.println(pId.toString());
            float[] data = (float[]) map.get(pId);
            AbstractMap.SimpleEntry<String, float[]> entry = new AbstractMap.SimpleEntry(pId.toString(), data);
            pcaPivots.add(entry);
        }
        System.out.println("size " + pcaPivots.size());;
        FSSearchSpacesStorage storage = new FSSearchSpacesStorage<>(pcaDataset.getSearchSpace(), SingularisedConvertors.FLOAT_VECTOR_SPACE);
        storage.storePivots(pcaPivots, pcaDataset.getDatasetName());
    }
}
