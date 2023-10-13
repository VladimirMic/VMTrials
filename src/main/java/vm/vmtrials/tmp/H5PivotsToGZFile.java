package vm.vmtrials.tmp;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.metricSpaceImpl.FSMetricSpacesStorage;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.dataToStringConvertors.SingularisedConvertors;

/**
 *
 * @author Vlada
 */
public class H5PivotsToGZFile {

    public static void main(String[] args) {
        Dataset[] datasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.LAION_100M_PCA96Dataset(),
            //
            new FSDatasetInstanceSingularizator.LAION_100M_PCA32Dataset()
        };

        Dataset fullDataset = new FSDatasetInstanceSingularizator.LAION_100M_Dataset(true);
        for (Dataset pcaDataset : datasets) {
            run(fullDataset, pcaDataset);
        }

    }

    private static void run(Dataset fullDataset, Dataset pcaDataset) {
        List pivots = fullDataset.getPivots(-1);
        AbstractMetricSpace metricSpace = fullDataset.getMetricSpace();
        Map map = pcaDataset.getKeyValueStorage();
        List pcaPivots = new ArrayList();
        for (Object pivot : pivots) {
            Object pId = metricSpace.getIDOfMetricObject(pivot);
            System.out.println(pId.toString());
            float[] data = (float[]) map.get(pId);
            AbstractMap.SimpleEntry<String, float[]> entry = new AbstractMap.SimpleEntry(pId.toString(), data);
            pcaPivots.add(entry);
        }
        System.out.println("size " + pcaPivots.size());;
        FSMetricSpacesStorage storage = new FSMetricSpacesStorage<>(pcaDataset.getMetricSpace(), SingularisedConvertors.FLOAT_VECTOR_SPACE);
        storage.storePivots(pcaPivots, pcaDataset.getDatasetName());
    }
}
