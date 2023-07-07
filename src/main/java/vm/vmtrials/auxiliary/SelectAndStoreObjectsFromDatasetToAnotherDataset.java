/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.auxiliary;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.AbstractMetricSpacesStorage;

/**
 *
 * @author Vlada
 */
public class SelectAndStoreObjectsFromDatasetToAnotherDataset {

    public static void main(String[] args) {
        Dataset idsProvider = new FSDatasetInstanceSingularizator.LAION_100M_Dataset();
        List pivots = idsProvider.getPivots(-1);
        AbstractMetricSpace providersMetricSpace = idsProvider.getMetricSpace();

        Dataset dataProvider = new FSDatasetInstanceSingularizator.LAION_100M_PCA32Dataset();
        Map dataMap = dataProvider.getKeyValueStorage();

        List objectsToStore = new ArrayList();
        for (Object pivot : pivots) {
            Object pID = providersMetricSpace.getIDOfMetricObject(pivot);
            Object data = dataMap.get(pID);
            AbstractMap.SimpleEntry<Object, Object> simpleEntry = new AbstractMap.SimpleEntry<>(pID, data);
            objectsToStore.add(simpleEntry);
        }

        AbstractMetricSpacesStorage storage = dataProvider.getMetricSpacesStorage();
        storage.storePivots(objectsToStore, "laion2B-en-clip768v2-n=100M.h5_PCA32_20000");

    }
}
