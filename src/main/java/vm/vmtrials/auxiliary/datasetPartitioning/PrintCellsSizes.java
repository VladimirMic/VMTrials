package vm.vmtrials.auxiliary.datasetPartitioning;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.fs.store.partitioning.FSGRAPPLEPartitioningStorage;
import vm.fs.store.partitioning.FSVoronoiPartitioningStorage;
import vm.metricSpace.Dataset;

/**
 *
 * @author Vlada
 */
public class PrintCellsSizes {

    public static void main(String[] args) {
        Dataset[] datasets = new Dataset[]{
            new FSDatasetInstanceSingularizator.DeCAFDataset(),
            new FSDatasetInstanceSingularizator.MPEG7dataset(),
            new FSDatasetInstanceSingularizator.SIFTdataset(),
            //            new FSDatasetInstanceSingularizator.LAION_100k_Dataset(),
            //            new FSDatasetInstanceSingularizator.LAION_300k_Dataset(),
            new FSDatasetInstanceSingularizator.LAION_10M_Dataset()
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
        Map<Object, TreeSet<Object>> vp = storage.load(dataset.getDatasetName(), pivotCount);
        File file = storage.getFile(dataset.getDatasetName(), pivotCount, false);
        String name = file.getName() + "cells_stats.csv";
        try {
            System.setOut(new PrintStream(new File(file.getParentFile(), name)));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PrintCellsSizes.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (Map.Entry<Object, TreeSet<Object>> cell : vp.entrySet()) {
            String pivotID = cell.getKey().toString();
            TreeSet<Object> ids = cell.getValue();
            System.out.println(pivotID + ";" + ids.size());
        }
        System.out.flush();
    }

}
