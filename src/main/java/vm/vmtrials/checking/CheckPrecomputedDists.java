/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.checking;

import java.io.File;
import java.util.Map;
import vm.fs.store.precomputedDists.FSPrecomputedDistancesMatrixSerializatorImpl;

/**
 *
 * @author Vlada
 */
public class CheckPrecomputedDists {

    public static void main(String[] args) {
        String s1 = "h:\\Similarity_search\\Dataset\\DistsToPivots\\sift_1m_sift_1m_256pivots.csv.gz";
        String s2 = "h:\\Similarity_search\\Dataset\\DistsToPivots\\sift_1m_sift_1m_256pivots.csv.gz_orig.gz";
        check(s1, s2);
    }

    private static void check(String s1, String s2) {
        FSPrecomputedDistancesMatrixSerializatorImpl loader = new FSPrecomputedDistancesMatrixSerializatorImpl();
        File file = new File(s1);
        float[][] pd1 = loader.loadPrecomPivotsToObjectsDists(file, null, -1).getDists();
        Map<Comparable, Integer> rowHeaders1 = loader.getRowHeaders();
        Map<Comparable, Integer> columnHeaders1 = loader.getColumnHeaders();

        file = new File(s2);
        float[][] pd2 = loader.loadPrecomPivotsToObjectsDists(file, null, -1).getDists();
        Map<Comparable, Integer> rowHeaders2 = loader.getRowHeaders();
        Map<Comparable, Integer> columnHeaders2 = loader.getColumnHeaders();

        check(pd1, rowHeaders1, columnHeaders1, pd2, rowHeaders2, columnHeaders2);
        check(pd2, rowHeaders2, columnHeaders2, pd1, rowHeaders1, columnHeaders1);
    }

    private static void check(float[][] pd1, Map<Comparable, Integer> rowHeaders1, Map<Comparable, Integer> columnHeaders1, float[][] pd2, Map<Comparable, Integer> rowHeaders2, Map<Comparable, Integer> columnHeaders2) {
        int counter = 0;
        for (Map.Entry<Comparable, Integer> row1 : rowHeaders1.entrySet()) {
            int row1Int = row1.getValue();
            if (!rowHeaders2.containsKey(row1.getKey())) {
                System.out.println("Missing row key: " + row1.getKey());
            }
            int row2Int = rowHeaders2.get(row1.getKey());
            for (Map.Entry<Comparable, Integer> column1 : columnHeaders1.entrySet()) {
                counter++;
                int column1Int = column1.getValue();
                int column2Int = columnHeaders2.get(column1.getKey());
                float diff = pd1[row1Int][column1Int] - pd2[row2Int][column2Int];
                if (diff != 0) {
                    System.out.println(counter + ": " + diff);
                } else if (counter % 1000 == 0) {
                    System.out.println(counter + ": " + diff);
                }
            }
            if (counter > 50000) {
                break;
            }
        }
    }
}
