package vm.vmtrials.checking;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import vm.fs.dataset.FSDatasetInstances;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.ToolsSpaceDomain;
import vm.searchSpace.distance.AbstractDistanceFunction;

/**
 *
 * @author Vlada
 */
public class PrintSomeDists {

    public static void main(String[] args) throws FileNotFoundException {
        Dataset dataset = new FSDatasetInstances.MOCAP10FPS();
        AbstractSearchSpace searchSpace = dataset.getSearchSpace();
        AbstractDistanceFunction df = dataset.getDistanceFunction();
        List pivots = dataset.getPivots(dataset.getRecommendedNumberOfPivotsForFiltering());
        Map pivotsMap = ToolsSpaceDomain.getSearchObjectsAsIdDataMap(searchSpace, pivots);
        Iterator it = dataset.getSearchObjectsFromDataset();
        System.setOut(new PrintStream("h:\\Similarity_search\\Dataset\\temp.csv"));
        Object next = it.next();
        Object oID = searchSpace.getIDOfObject(next);
        Object oData = searchSpace.getDataOfObject(next);
        Set ids = pivotsMap.keySet();
        for (Object id : ids) {
            System.out.print(";" + id);
        }
        System.out.println();
        System.out.print(oID + ";");
        for (Object id : ids) {
            Object pivotData = pivotsMap.get(id);
            float distance = df.getDistance(oData, pivotData);
            System.out.print(distance + ";");
        }

    }
}
