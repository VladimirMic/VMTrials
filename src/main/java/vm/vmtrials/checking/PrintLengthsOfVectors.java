package vm.vmtrials.checking;

import java.util.List;
import vm.fs.dataset.FSDatasetInstances;
import vm.mathtools.Tools;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;

/**
 *
 * @author Vlada
 */
public class PrintLengthsOfVectors {

    public static void main(String[] args) {
        int count = 100;
        Dataset dataset = new FSDatasetInstances.DeCAFDataset();
        AbstractSearchSpace<float[]> metricSpace = dataset.getSearchSpace();
        List objs = dataset.getSampleOfDataset(count);
        for (Object obj : objs) {
            Comparable oID = metricSpace.getIDOfObject(obj);
            float[] oData = metricSpace.getDataOfObject(obj);
            double lengthOfVector = Tools.getLengthOfVector(oData);
            System.out.println(oID.toString() + ";" + lengthOfVector);
        }
    }

}
