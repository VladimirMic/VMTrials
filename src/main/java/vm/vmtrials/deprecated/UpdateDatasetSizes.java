package vm.vmtrials.deprecated;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import vm.db.DBGlobal;
import vm.db.metricSpaceImpl.DBMetricSpaceImpl;
import vm.db.metricSpaceImpl.DBMetricSpacesStorage;
import vm.metricSpace.dataToStringConvertors.SingularisedConvertors;
import vm.metricSpace.AbstractMetricSpace;

/**
 *
 * @author Vlada
 */
@Deprecated
public class UpdateDatasetSizes {

    public static void main(String[] args) throws SQLException {
        AbstractMetricSpace metricSpace = new DBMetricSpaceImpl();
        DBMetricSpacesStorage dbMetricSpacesStorage = new DBMetricSpacesStorage(metricSpace, SingularisedConvertors.FLOAT_VECTOR_SPACE);
        Statement st = DBGlobal.getSingularConnectionFromPredefinedIniFile().createStatement();

//        updateAll(dbMetricSpacesStorage, st);
        updateDatasetSize("decaf_1m_PCA2387", dbMetricSpacesStorage, st);
    }

    private static void updateDatasetSize(String datasetName, DBMetricSpacesStorage dbMetricSpacesStorage, Statement st) throws SQLException {
        int numberOfObjectsInDataset = dbMetricSpacesStorage.reevaluatetNumberOfObjectsInDataset(datasetName);
        String sql = "UPDATE dataset SET obj_count=" + numberOfObjectsInDataset + " WHERE name='" + datasetName + "'";
        System.out.println(datasetName + ": " + numberOfObjectsInDataset + " metric objects");
        st.execute(sql);
    }

    @Deprecated
    private static void updateAll(DBMetricSpacesStorage dbMetricSpacesStorage, Statement st) throws SQLException {
        String sql = "SELECT name FROM dataset";
        ResultSet rs = st.executeQuery(sql);
        while (true) {
            rs.next();
            String datasetName = rs.getString("name");
            updateDatasetSize(datasetName, dbMetricSpacesStorage, st);
        }
    }
}
