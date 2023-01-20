package vm.vmtrials.checking;

import java.sql.SQLException;
import java.util.Iterator;
import vm.db.metricSpaceImpl.DBMetricSpaceImpl;
import vm.db.metricSpaceImpl.DBMetricSpacesStorage;
import vm.metricspace.MetricSpacesStorageInterface;
import vm.metricspace.dataToStringConvertors.impl.FloatVectorConvertor;

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

        Iterator<Object> metricObjects = metricSpacesStorage.getObjectsFromDataset(datasetName);
        int counter = 0;
        for (counter = 0; metricObjects.hasNext(); counter++) {
            metricObjects.next();
        }
        System.out.println("Number of objects in a dataset: " + datasetName + " is: " + counter);

    }
}
