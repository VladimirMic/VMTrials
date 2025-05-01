/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.checking.pdbe;

import java.util.Iterator;
import vm.fs.dataset.FSDatasetInstances;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.distance.DistanceFunctionInterface;

/**
 *
 * @author xmic
 */
public class CheckQScoreDistComp {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstances.PDBePtoteinChainsDataset();
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        Iterator it = dataset.getMetricObjectsFromDataset();
        Object o1 = it.next();
        Object o2 = it.next();
        DistanceFunctionInterface df = dataset.getDistanceFunction();
        float distance = df.getDistance(o1, o2);
        System.out.print("The distance of " + metricSpace.getIDOfMetricObject(o1) + " and " + metricSpace.getIDOfMetricObject(o2) + " is " + distance);
    }
}
