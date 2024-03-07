/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.tmp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;

/**
 *
 * @author Vlada
 */
public class CheckSufixesAsCountsOfNNResults {

    public static void main(String[] args) {
        File folder = new File("h:\\Similarity_search\\Result\\ground_truth\\");
        File[] files = folder.listFiles((File file, String string) -> string.endsWith(".gz"));
        FSNearestNeighboursStorageImpl storage = new FSNearestNeighboursStorageImpl();
        Map<File, File> map = new HashMap<>();
        for (File file : files) {
            try {
                Map<String, TreeSet<Map.Entry<Object, Float>>> results = storage.getQueryResultsForDataset(file);
                String name = file.getName();
                int idx = name.lastIndexOf(".");
                String prefix = name.substring(0, idx);
                String suffix = name.substring(idx);
                Map.Entry<String, TreeSet<Map.Entry<Object, Float>>> qRes = results.entrySet().iterator().next();
                if (qRes == null) {
                    continue;
                }
                int size = qRes.getValue().size();
                if (!prefix.endsWith("_" + size)) {
                    String newName = prefix + "_" + size + suffix;
                    System.out.println("name: " + name + ", " + size);
                    System.out.println("newName: " + newName);
                    File newFile = new File(file.getParent(), newName);
                    map.put(file, newFile);
                    System.out.println("newFile: " + newFile.getAbsolutePath() + ", map size: " + map.size());
                } else {
                    System.out.println("Already correct name : " + prefix + ", size: " + size);
                }
            } catch (Throwable ex) {
                Logger.getLogger(CheckSufixesAsCountsOfNNResults.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        for (Map.Entry<File, File> entry : map.entrySet()) {
            File key = entry.getKey();
            File value = entry.getValue();
            key.renameTo(value);
            Logger.getLogger(CheckSufixesAsCountsOfNNResults.class.getName()).log(Level.INFO, "Renaming {0} to {1}", new Object[]{key.getAbsolutePath(), value.getAbsolutePath()});
        }
    }
}
