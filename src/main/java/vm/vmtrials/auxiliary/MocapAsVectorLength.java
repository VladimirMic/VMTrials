/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.auxiliary;

import java.util.Iterator;
import java.util.List;
import vm.fs.dataset.FSDatasetInstances;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;

/**
 *
 * @author xmic
 */
public class MocapAsVectorLength {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstances.MOCAP10FPS();
        printAverageLength(dataset);
        dataset = new FSDatasetInstances.MOCAP30FPS();
        printAverageLength(dataset);

    }

    private static void printAverageLength(Dataset dataset) {
        long sum = 0;
        int movements = 0;
        Iterator obj = dataset.getMetricObjectsFromDataset();
        AbstractMetricSpace metricSpace = dataset.getMetricSpace();
        while (obj.hasNext()) {
            Object next = obj.next();
            List<float[][]> data = (List) metricSpace.getDataOfMetricObject(next);
            long poses = data.size();
            if (poses > 0) {
                float[][] pose = data.get(0);
                if (pose.length > 0) {
                    sum += poses * pose.length * pose[0].length;
                    movements++;
                }
            }
        }
        float avg = sum / (float) movements;
        System.out.println(avg);
    }
}
