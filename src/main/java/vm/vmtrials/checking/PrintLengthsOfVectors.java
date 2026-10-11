package vm.vmtrials.checking;

import java.util.Iterator;
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
        int count = -1;
        Dataset dataset = new FSDatasetInstances.WIKI_BGE_M3_SISAP26();
        AbstractSearchSpace<float[]> metricSpace = dataset.getSearchSpace();
        Iterator objs = dataset.getSearchObjectsFromDataset(count);
        int counter = 0;
        while (objs.hasNext()) {
            Object obj = objs.next();
            Comparable oID = metricSpace.getIDOfObject(obj);
            float[] oData = metricSpace.getDataOfObject(obj);
            double lengthOfVector = Tools.getLengthOfVector(oData);
            System.out.println(oID.toString() + ";" + lengthOfVector);
            counter++;
        }
        System.out.println("Count: " + counter);
    }

}
