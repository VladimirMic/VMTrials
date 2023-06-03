package vm.vmtrials.auxiliary;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import vm.datatools.DataTypeConvertor;
import vm.fs.metricSpaceImpl.FSMetricSpaceImpl;
import vm.fs.metricSpaceImpl.FSMetricSpacesStorage;
import vm.math.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.MetricSpacesStorageInterface;
import vm.metricSpace.dataToStringConvertors.impl.FloatVectorConvertor;

/**
 *
 * @author Vlada
 */
public class PrintVectorCoordinatesStats {

    public static void main(String[] args) throws SQLException, FileNotFoundException {
        String datasetName = "sift_1m_PCA8";
        String output = "h:\\Skola\\2022\\PCA\\simRel\\Coorinates_states__dataset_" + datasetName + ".csv";
        System.setOut(new PrintStream(output));

        AbstractMetricSpace<float[]> metricSpace = new FSMetricSpaceImpl<>();
        MetricSpacesStorageInterface metricSpacesStorage = new FSMetricSpacesStorage<>(metricSpace, new FloatVectorConvertor());

        List<Object> metricObjects = metricSpacesStorage.getSampleOfDataset(datasetName, -1);
        float[][] matrix = ToolsMetricDomain.transformMetricObjectsToTransposedVectorMatrix(metricSpace, metricObjects);
        // rows are coordinates, columns are samples
        printStatsForCoordinates(matrix);
    }

    private static void printStatsForCoordinates(float[][] vectors) {
        for (int i = 0; i < vectors.length; i++) {
            float[] vector = vectors[i];
            printStatsForCoordinate(vector, i + 1);
        }
    }

    private static void printStatsForCoordinate(float[] vec, int i) {
        // stats: minimum, kvartil, median, prumer, maximum
        Arrays.sort(vec);
        int quartile = vec.length / 4;
        double[] values = DataTypeConvertor.floatsToDoubles(vec);
        double variance = Tools.getVariance(values);
        double mean = Tools.getMean(values);
        System.out.print(i + ";");
        System.out.print(vec[0] + ";");
        System.out.print(vec[quartile] + ";");
        System.out.print(vec[2 * quartile] + ";");
        System.out.print(vec[3 * quartile] + ";");
        System.out.print(vec[vec.length - 1] + ";");
        System.out.print(mean + ";");
        System.out.print(variance);
        System.out.println("");
//        for (double value : values) {
//            System.out.println(value);
//        }
//        System.exit(0);
    }

}
