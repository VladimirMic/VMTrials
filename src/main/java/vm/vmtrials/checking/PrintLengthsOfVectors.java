package vm.vmtrials.checking;

import java.util.List;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.mathtools.Tools;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;

/**
 *
 * @author Vlada
 */
public class PrintLengthsOfVectors {

    public static void main(String[] args) {
        int count = 100;
        Dataset dataset = new FSDatasetInstanceSingularizator.DeCAFDataset();
        AbstractMetricSpace<float[]> metricSpace = dataset.getMetricSpace();
        List objs = dataset.getSampleOfDataset(count);
        for (Object obj : objs) {
            Comparable oID = metricSpace.getIDOfMetricObject(obj);
            float[] oData = metricSpace.getDataOfMetricObject(obj);
            double lengthOfVector = Tools.getLengthOfVector(oData);
            System.out.println(oID.toString() + ";" + lengthOfVector);
        }
    }

}
