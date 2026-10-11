/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.checking;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import vm.datatools.Tools;
import vm.fs.dataset.FSDatasetInstances;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;

/**
 *
 * @author Vlada
 */
public class PrintIDs {

    public static void main(String[] args) {
        run(new FSDatasetInstances.MOCAP10FPS(), false);
    }

    private static <T> void run(Dataset<T> dataset, boolean fromKeyValueStorage) {
        Iterator it;
        if (fromKeyValueStorage) {
            it = dataset.getSearchObjectsFromDatasetKeyValueStorage();
        } else {
            it = dataset.getPivots(64).iterator();
        }
        AbstractSearchSpace metricSpace = dataset.getSearchSpace();
        Comparable oIdPrev = null;
        SortedSet<Comparable> set = new TreeSet<>();
        for (int i = 0; it.hasNext(); i++) {
            Object o = it.next();
            Comparable id = metricSpace.getIDOfObject(o);
            set.add(id);
            System.out.print(i + ": " + id);
            if (oIdPrev == null) {
                System.out.println();
            } else {
                System.out.println(";" + id.compareTo(oIdPrev));
            }
            oIdPrev = id;
        }
        Tools.printCollection(set, "\n");
    }
}
