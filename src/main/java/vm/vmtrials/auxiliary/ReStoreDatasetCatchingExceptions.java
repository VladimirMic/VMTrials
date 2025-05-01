/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.auxiliary;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.fs.dataset.FSDatasetInstances;
import vm.metricSpace.AbstractMetricSpacesStorage;
import vm.metricSpace.Dataset;

/**
 *
 * @author xmic
 */
public class ReStoreDatasetCatchingExceptions {

    public static final Integer BATCH_SIZE = 500000;

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstances.RandomDataset10Uniform();
        Iterator it = dataset.getMetricObjectsFromDataset();
        AbstractMetricSpacesStorage storage = dataset.getMetricSpacesStorage();
        String newDatasetName = dataset.getDatasetName() + "_restored";
        int count = 0;
        while (it.hasNext()) {
            List<Object> batch = Tools.getObjectsFromIterator(it, BATCH_SIZE);
            storage.storeObjectsToDataset(batch.iterator(), -1, newDatasetName);
            count += batch.size();
            Logger.getLogger(ReStoreDatasetCatchingExceptions.class.getName()).log(Level.INFO, "Restored {0} objects", count);
        }
    }
}
