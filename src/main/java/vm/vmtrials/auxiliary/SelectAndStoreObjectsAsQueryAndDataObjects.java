/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.auxiliary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.fs.dataset.FSDatasetInstances;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.AbstractSearchSpacesStorage;
import vm.searchSpace.Dataset;

/**
 *
 * @author Vlada
 */
public class SelectAndStoreObjectsAsQueryAndDataObjects {

    public static void main(String[] args) {
        Dataset[] dataProviders = new Dataset[]{
            FSDatasetInstances.MOCAP10FPS_ORIG_ALL,
            FSDatasetInstances.MOCAP30FPS_ORIG_ALL
        };
        for (Dataset dataProvider : dataProviders) {
            run(dataProvider);
        }
    }

    private static void run(Dataset dataProvider) {
        List<Object> objects = Tools.getObjectsFromIterator(dataProvider.getSearchObjectsFromDataset(-1));
        store(dataProvider, 0, objects);
        store(dataProvider, 1, objects);
    }

    private static Set<Comparable> provideMapOfCorrectDatasetIDsForDataset() {
        return parse("c:\\Data\\Similarity_search\\Dataset\\Dataset\\Honza\\data-CS_train_objects_messif-lines.txt");
    }

    private static Set<Comparable> provideMapOfCorrectQueryIDsForDataset() {
        return parse("c:\\Data\\Similarity_search\\Dataset\\Dataset\\Honza\\queries-CS_test_objects_messif-lines.txt");
    }

    private static Set<Comparable> parse(String ids) {
        List<String>[] parseCsv = Tools.parseCsv(
                ids,
                1, true);
        Set<Comparable> ret = new HashSet<>(parseCsv[0]);
        Logger.getLogger(SelectAndStoreObjectsAsQueryAndDataObjects.class.getName()).log(Level.INFO, "Found {0} IDs", ret.size());
        return ret;
    }

    private static void store(Dataset dataProvider, int type, List<Object> objects) {
        Set<Comparable> set = type == 0 ? provideMapOfCorrectDatasetIDsForDataset() : provideMapOfCorrectQueryIDsForDataset();
        AbstractSearchSpace providersMetricSpace = dataProvider.getSearchSpace();

        List objectsToStore = new ArrayList();
        for (Object o : objects) {
            Comparable oID = providersMetricSpace.getIDOfObject(o);
            if (set.contains(oID)) {
                objectsToStore.add(o);
            }
        }
        AbstractSearchSpacesStorage storage = dataProvider.getSearchSpacesStorage();
        String name;
        if (type == 0) {
            name = dataProvider.getDatasetName() + "_selected.txt.gz";
            storage.storeObjectsToDataset(objectsToStore.iterator(), -1, name);
        } else {
            name = dataProvider.getQuerySetName() + "_selected.txt.gz";
            storage.storeQueryObjects(objectsToStore, name);
        }
    }

}
