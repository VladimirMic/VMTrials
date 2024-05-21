/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.auxiliary;

import java.util.List;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.metricSpace.Dataset;
import vm.metricSpace.AbstractMetricSpacesStorage;

/**
 *
 * @author Vlada
 */
public class SelectAndStoreObjectsFromDatasetToAnotherDataset {

    public static void main(String[] args) {
        int k = 1000;
        Dataset dataProvider = new FSDatasetInstanceSingularizator.DeCAF100M_PCA256Dataset();
        List objects = dataProvider.getSampleOfDataset(k);

//        Map dataMap = dataProvider.getKeyValueStorage();
//
//        List objectsToStore = new ArrayList();
//        for (Object o : objects) {
//            Object oID = providersMetricSpace.getIDOfMetricObject(o);
//            Object oData = dataMap.get(oID);
//            AbstractMap.SimpleEntry<Object, Object> simpleEntry = new AbstractMap.SimpleEntry<>(oID, oData);
//            objectsToStore.add(simpleEntry);
//        }

        AbstractMetricSpacesStorage storage = dataProvider.getMetricSpacesStorage();
        storage.storeQueryObjects(objects, "decaf_100m_PCA256_first_objects_" + k);
//        storage.storePivots(objectsToStore, "laion2B-en-clip768v2-n=100M.h5_PCA32_20000");

    }
}
