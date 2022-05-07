/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vm.vmtrials.checking;

import java.sql.SQLException;
import java.util.Iterator;
import vm.db.metricSpaceImpl.DBMetricSpaceImpl;
import vm.db.metricSpaceImpl.DBMetricSpacesStorage;
import vm.metricSpace.MetricSpacesStorageInterface;
import vm.metricSpace.dataToStringConvertors.impl.FloatVectorConvertor;

/**
 *
 * @author Vlada
 */
public class GetNumberOfObjectsInDataset {

    public static void main(String[] args) throws SQLException {
        String datasetName = "sift_1m_PCA72";
//        String datasetName = "random20uniform_1m";

        DBMetricSpaceImpl metricSpace = new DBMetricSpaceImpl<>();
        MetricSpacesStorageInterface metricSpacesStorage = new DBMetricSpacesStorage<>(metricSpace, new FloatVectorConvertor());

        Iterator<Object> metricObjects = metricSpacesStorage.getMetricObjectsFromDataset(datasetName);
        int counter = 0;
        for (counter = 0; metricObjects.hasNext(); counter++) {
            metricObjects.next();
        }
        System.out.println("Number of objects in a dataset: " + datasetName + " is: " + counter);

    }
}
