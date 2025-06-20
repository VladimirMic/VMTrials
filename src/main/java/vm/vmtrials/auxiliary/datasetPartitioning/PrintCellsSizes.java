package vm.vmtrials.auxiliary.datasetPartitioning;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.fs.dataset.FSDatasetInstances;
import vm.fs.store.partitioning.FSGRAPPLEPartitioningStorage;
import vm.fs.store.partitioning.FSVoronoiPartitioningStorage;
import vm.searchSpace.Dataset;

/**
 *
 * @author Vlada
 */
public class PrintCellsSizes {

    public static void main(String[] args) {
        Dataset[] datasets = new Dataset[]{
            new FSDatasetInstances.DeCAFDataset(),
            new FSDatasetInstances.MPEG7dataset(),
            new FSDatasetInstances.SIFTdataset(),
            //            new FSDatasetInstanceSingularizator.LAION_100k_Dataset(),
            //            new FSDatasetInstanceSingularizator.LAION_300k_Dataset(),
            new FSDatasetInstances.LAION_10M_Dataset(true)
//            new FSDatasetInstanceSingularizator.LAION_30M_Dataset(),
//            new FSDatasetInstanceSingularizator.LAION_100M_Dataset()
        };
        for (Dataset dataset : datasets) {
            run(dataset);
        }
    }

    private static void run(Dataset dataset) {
        int pivotCount = 256;
        FSVoronoiPartitioningStorage storage = new FSGRAPPLEPartitioningStorage();
        Map<Comparable, Collection<Comparable>> vp = storage.load(dataset.getDatasetName(), pivotCount);
        File file = storage.getFile(dataset.getDatasetName(), pivotCount, false);
        String name = file.getName() + "cells_stats.csv";
        try {
            System.setOut(new PrintStream(new File(file.getParentFile(), name)));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PrintCellsSizes.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (Map.Entry<Comparable, Collection<Comparable>> cell : vp.entrySet()) {
            String pivotID = cell.getKey().toString();
            Collection<Comparable> ids = cell.getValue();
            System.out.println(pivotID + ";" + ids.size());
        }
        System.out.flush();
    }

}
