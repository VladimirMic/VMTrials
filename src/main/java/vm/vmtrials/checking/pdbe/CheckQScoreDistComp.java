/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.checking.pdbe;

import java.util.Iterator;
import vm.fs.dataset.FSDatasetInstances;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.distance.AbstractDistanceFunction;

/**
 *
 * @author xmic
 */
public class CheckQScoreDistComp {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstances.PDBePtoteinChainsDataset();
        AbstractSearchSpace metricSpace = dataset.getSearchSpace();
        Iterator it = dataset.getSearchObjectsFromDataset();
        Object o1 = it.next();
        Object o2 = it.next();
        AbstractDistanceFunction df = dataset.getDistanceFunction();
        float distance = df.getDistance(o1, o2);
        System.out.print("The distance of " + metricSpace.getIDOfObject(o1) + " and " + metricSpace.getIDOfObject(o2) + " is " + distance);
    }
}
