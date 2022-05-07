package vm.vmtrials.simRelEuclidSpace;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import vm.datatools.Tools;
import vm.db.metricSpaceImpl.DBMetricSpaceImpl;
import vm.db.metricSpaceImpl.DBMetricSpacesStorage;
import vm.metricSpace.MetricSpacesStorageInterface;
import vm.metricSpace.dataToStringConvertors.impl.FloatVectorConvertor;
import vm.metricSpace.distance.impl.L2OnFloatsArray;
import vm.simRel.SimRelSequentialScanKNN;
import vm.simRel.impl.learn.SimRelEuclideanPCALearn;

/**
 *
 * @author Vlada
 */
public class EvaluateSimRelKNNWithPrinting {

    public static void main(String[] args) throws SQLException, FileNotFoundException {
        int pcaLength = 256;
        String datasetName = "decaf_1m_PCA" + pcaLength;
        int k = 30;
//        String output = "h:\\Skola\\2022\\PCA\\simRel\\SimRelPCAStats\\" + datasetName + ".csv";
//        System.setOut(new PrintStream(output));

        DBMetricSpaceImpl<float[]> metricSpace = new DBMetricSpaceImpl<>();
        MetricSpacesStorageInterface metricSpacesStorage = new DBMetricSpacesStorage<>(metricSpace, new FloatVectorConvertor());

        List<Object> querySamples = metricSpacesStorage.getMetricPivots(datasetName, 100);
        List<Object> sampleOfDataset = metricSpacesStorage.getSampleOfDataset(datasetName, 100000);

        SimRelEuclideanPCALearn simRelEuclideanPCAImpl = new SimRelEuclideanPCALearn();
        SimRelSequentialScanKNN alg = new SimRelSequentialScanKNN(simRelEuclideanPCAImpl, new L2OnFloatsArray());

        simRelEuclideanPCAImpl.resetLearning(pcaLength);
        float[] maxDiffWhenWrong = null;
        for (Object queryObj : querySamples) {
            simRelEuclideanPCAImpl.resetCounters(pcaLength);
            TreeSet<Map.Entry<Object, Float>> knnSearch = alg.knnSearch(k, queryObj, sampleOfDataset.iterator(), metricSpace);
            int[] errorsPerCoord = simRelEuclideanPCAImpl.getErrorsPerCoord();
            int comparisonCounter = simRelEuclideanPCAImpl.getComparisonCounter();
            maxDiffWhenWrong = simRelEuclideanPCAImpl.getMaxDiffWhenWrong();
            for (int i = 0; i < pcaLength; i++) {
                System.out.print(i + ";" + comparisonCounter + ";" + (errorsPerCoord[i] / (float) comparisonCounter) + ";" + maxDiffWhenWrong[i] + ";");
            }
            System.out.println();
        }
        Tools.printArray(maxDiffWhenWrong);
    }

}
