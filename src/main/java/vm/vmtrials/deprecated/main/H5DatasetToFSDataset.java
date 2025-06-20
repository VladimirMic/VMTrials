/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.deprecated.main;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.fs.dataset.FSDatasetInstances;
import vm.searchSpace.AbstractSearchSpacesStorage;
import vm.searchSpace.AbstractSearchSpacesStorage.OBJECT_TYPE;
import static vm.searchSpace.AbstractSearchSpacesStorage.OBJECT_TYPE.DATASET_OBJECT;
import static vm.searchSpace.AbstractSearchSpacesStorage.OBJECT_TYPE.PIVOT_OBJECT;
import static vm.searchSpace.AbstractSearchSpacesStorage.OBJECT_TYPE.QUERY_OBJECT;
import vm.searchSpace.Dataset;

/**
 *
 * @author Vlada
 */
@Deprecated //no reason
public class H5DatasetToFSDataset {

    public static final Logger LOG = Logger.getLogger(H5DatasetToFSDataset.class.getName());

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstances.LAION_30M_Dataset(true);
        AbstractSearchSpacesStorage searchSpacesStorage = dataset.getSearchSpacesStorage();
        Iterator it = dataset.getPivots(-1).iterator();
        processData(it, OBJECT_TYPE.PIVOT_OBJECT, dataset.getPivotSetName(), searchSpacesStorage);
        it = dataset.getQueryObjects().iterator();
        processData(it, OBJECT_TYPE.QUERY_OBJECT, dataset.getQuerySetName(), searchSpacesStorage);
        it = dataset.getSearchObjectsFromDataset();
        processData(it, OBJECT_TYPE.DATASET_OBJECT, dataset.getDatasetName(), searchSpacesStorage);
    }

    private static void processData(Iterator it, OBJECT_TYPE objectType, String setName, AbstractSearchSpacesStorage searchSpacesStorage) {
        String derivedName = setName + "_fs";
        switch (objectType) {
            case PIVOT_OBJECT: {
                List<Object> list = Tools.getObjectsFromIterator(it);
                searchSpacesStorage.storePivots(list, derivedName);
                LOG.log(Level.INFO, "Stored {0} pivots", list.size());
                break;
            }
            case QUERY_OBJECT: {
                List<Object> list = Tools.getObjectsFromIterator(it);
                searchSpacesStorage.storeQueryObjects(list, derivedName);
                LOG.log(Level.INFO, "Stored {0} query objects", list.size());
                break;
            }
            case DATASET_OBJECT: {
                int counter = 0;
                while (it.hasNext()) {
                    List batch = Tools.getObjectsFromIterator(it, 1000000);
                    counter += batch.size();
                    searchSpacesStorage.storeObjectsToDataset(batch.iterator(), -1, derivedName);
                    LOG.log(Level.INFO, "Stored {0} data objects", counter);
                }
                break;
            }
        }
    }
}
