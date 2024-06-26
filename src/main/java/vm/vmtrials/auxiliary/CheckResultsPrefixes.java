/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.auxiliary;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import vm.fs.FSGlobal;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.queryResults.QueryNearestNeighboursStoreInterface;

/**
 *
 * @author Vlada
 */
public class CheckResultsPrefixes {

    private static Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>>[] lessCandsCache;

    public static void main(String[] args) {
        String lessResultsFolder = "faiss-100M_CLIP_PCA256-IVFPQ-tr1000000-cc262144-m32-nbits8-qc1000-k50000";
        String moreResultsFolder = "faiss-100M_CLIP_PCA256-IVFPQ-tr1000000-cc262144-m32-nbits8-qc1000-k200000";

        String[] lessFiles = new File(FSGlobal.RESULT_FOLDER, lessResultsFolder).list((File file, String string) -> string.toLowerCase().endsWith(".gz"));
        String[] moreFiles = new File(FSGlobal.RESULT_FOLDER, moreResultsFolder).list((File file, String string) -> string.toLowerCase().endsWith(".gz"));

        QueryNearestNeighboursStoreInterface resultsStorage = new FSNearestNeighboursStorageImpl();
        lessCandsCache = new Map[lessFiles.length];
        for (int m = 0; m < moreFiles.length; m++) {
            System.gc();
            String more = moreFiles[m].trim().substring(0, moreFiles[m].length() - 3);
            Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> moreCands = resultsStorage.getQueryResultsForDataset(moreResultsFolder, more, "", null);

            for (int l = 0; l < lessFiles.length; l++) {
                String less = lessFiles[l].trim().substring(0, lessFiles[l].length() - 3);

                Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> lessCands = lessCandsCache[l] != null ? lessCandsCache[l] : resultsStorage.getQueryResultsForDataset(lessResultsFolder, less, "", null);
                if (lessCandsCache[l] == null) {
                    lessCandsCache[l] = lessCands;
                }
                lessFilesLoop:
                for (Comparable queryID : lessCands.keySet()) {
                    if (!moreCands.containsKey(queryID)) {
                        System.out.println("File " + more + " does not contain query " + queryID + " which is in file " + less);
                        break;
                    }
                    TreeSet<Map.Entry<Comparable, Float>> lessCandsResults = lessCands.get(queryID);
                    TreeSet<Map.Entry<Comparable, Float>> moreCandsResults = moreCands.get(queryID);
                    Set moreNN = getNNKeys(moreCandsResults);
                    for (Map.Entry<Comparable, Float> nnEntry : lessCandsResults) {
                        if (!moreNN.contains(nnEntry.getKey())) {
                            System.out.println("No Heureka. File " + more + " is not an extended list of candidates from a file " + less);
                            break lessFilesLoop;
                        }

                    }
                }
                System.out.println("Heureka! File " + more + " is a longer list of candidates from a file " + less);
            }
        }
    }

    private static Set getNNKeys(TreeSet<Map.Entry<Comparable, Float>> candsResults) {
        Set ret = new HashSet();
        for (Map.Entry<Comparable, Float> entry : candsResults) {
            ret.add(entry.getKey());
        }
        return ret;
    }
}
