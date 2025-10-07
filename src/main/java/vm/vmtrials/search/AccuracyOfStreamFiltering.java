/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.search;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.JFreeChart;
import vm.datatools.Tools;
import vm.fs.dataset.FSDatasetInstances;
import vm.fs.main.search.perform.FSKNNQueriesSeqScanWithFilteringMain;
import vm.plot.impl.LinesOrPointsPlotter;
import vm.search.algorithm.SearchingAlgorithm;
import vm.search.algorithm.impl.GroundTruthEvaluator;
import vm.searchSpace.AbstractSearchSpace;
import vm.searchSpace.Dataset;
import vm.searchSpace.distance.DistanceFunctionInterface;
import vm.searchSpace.distance.bounding.BoundsOnDistanceEstimation;

/**
 *
 * @author Vlada
 */
public class AccuracyOfStreamFiltering {

    public static void main(String[] args) {
//        run(new FSDatasetInstances.DeCAFDataset());
//        run(new FSDatasetInstances.MOCAP10FPS());
        run(new FSDatasetInstances.MOCAP30FPS());
    }

    private static void run(Dataset dataToFilter) {
        List pivots = dataToFilter.getPivots(dataToFilter.getRecommendedNumberOfPivotsForFiltering());
        int batchSize = dataToFilter.getPrecomputedDatasetSize() / 200;
        int k = GroundTruthEvaluator.K_IMPLICIT_FOR_QUERIES;
        BoundsOnDistanceEstimation[] filters = FSKNNQueriesSeqScanWithFilteringMain.initTestedFilters(null, pivots, dataToFilter, k);
        SearchingAlgorithm alg1 = FSKNNQueriesSeqScanWithFilteringMain.initAlg(filters[1], dataToFilter, dataToFilter.getSearchSpace(), pivots, dataToFilter.getDistanceFunction(), null);
        SearchingAlgorithm alg5 = FSKNNQueriesSeqScanWithFilteringMain.initAlg(filters[5], dataToFilter, dataToFilter.getSearchSpace(), pivots, dataToFilter.getDistanceFunction(), null);
        run(dataToFilter, k, pivots.size(), batchSize, alg1, alg5);
    }

    private static <T> void run(Dataset<T> dataToFilter, int k, int pivotCount, int batchSize, SearchingAlgorithm... algs) {
        String[] tracesNames = new String[algs.length];
        for (int i = 0; i < tracesNames.length; i++) {
            tracesNames[i] = algs[i].getResultName();
        }

        List queries = dataToFilter.getQueryObjects();
        Iterator objects = dataToFilter.getSearchObjectsFromDataset();
        AbstractSearchSpace<T> searchSpace = dataToFilter.getSearchSpace();
        DistanceFunctionInterface<T> df = searchSpace.getDistanceFunction();
        SortedMap<Float, Float>[] mapOfAvgAccuracyForAlgs = new TreeMap[algs.length];
        TreeSet<Map.Entry<Comparable, Float>>[][] currAnswers = new TreeSet[algs.length][queries.size()];
        List<Object> batch = Tools.getObjectsFromIterator(objects, k);
        int oCounter = batch.size();
        for (int i = 0; i < algs.length; i++) {
            mapOfAvgAccuracyForAlgs[i] = new TreeMap<>();
            currAnswers[i] = algs[i].completeKnnFilteringWithQuerySet(searchSpace, queries, k, batch.iterator(), (Object[]) null);
        }
        TreeSet<Map.Entry<Comparable, Float>>[] preciseAnswers = SearchingAlgorithm.initKNNResultSets(queries.size());
        computePartialGroundTruthAndAccuracy(oCounter, mapOfAvgAccuracyForAlgs, currAnswers, preciseAnswers, batch, searchSpace, df, queries, k);
        while (objects.hasNext()) {
            batch = Tools.getObjectsFromIterator(objects, batchSize);
            oCounter += batch.size();
            for (int i = 0; i < algs.length; i++) {
                currAnswers[i] = algs[i].completeKnnFilteringWithQuerySet(searchSpace, queries, k, batch.iterator(), (Object[]) currAnswers[i]);
            }
            computePartialGroundTruthAndAccuracy(oCounter, mapOfAvgAccuracyForAlgs, currAnswers, preciseAnswers, batch, searchSpace, df, queries, k);
            if (oCounter % 10 == 0) {
                Logger.getLogger(AccuracyOfStreamFiltering.class.getName()).log(Level.INFO, "Processed {0} stream objects", oCounter);
            }
        }
        LinesOrPointsPlotter plotter = new LinesOrPointsPlotter(true);
        plotter.setYBounds(0.95f, 1.0f);
        JFreeChart plot = plotter.createPlot("", "# processed objects", "Avg. accuracy", tracesNames, null, mapOfAvgAccuracyForAlgs);
        plotter.storePlotPDF("h:\\Similarity_search\\Plots\\2025_PtolemaiosLimited\\2025_09_Stream_Accuracy_" + dataToFilter.getDatasetName() + "_" + pivotCount + "pivots", plot);
    }

    private static <T> void computePartialGroundTruthAndAccuracy(int oCounter, SortedMap<Float, Float>[] mapOfAvgAccuracyForAlgs, TreeSet<Map.Entry<Comparable, Float>>[][] currAnswers, TreeSet<Map.Entry<Comparable, Float>>[] preciseAnswers, List<Object> batch, AbstractSearchSpace<T> searchSpace, DistanceFunctionInterface df, List queries, int k) {
// computeGroundTruth;
        for (int qCounter = 0; qCounter < queries.size(); qCounter++) {
            Object q = queries.get(qCounter);
            T qData = searchSpace.getDataOfObject(q);
            float radius = SearchingAlgorithm.adjustAndReturnSearchRadiusAfterAddingOne(preciseAnswers[qCounter], k, Float.MAX_VALUE);
            for (Object o : batch) {
                Comparable oID = searchSpace.getIDOfObject(o);
                T oData = searchSpace.getDataOfObject(o);
                float distance = df.getDistance(qData, oData);
                if (distance < radius) {
                    preciseAnswers[qCounter].add(new AbstractMap.SimpleEntry<>(oID, distance));
                    radius = SearchingAlgorithm.adjustAndReturnSearchRadiusAfterAddingOne(preciseAnswers[qCounter], k, Float.MAX_VALUE);
                }
            }
        }
        for (int algIndex = 0; algIndex < mapOfAvgAccuracyForAlgs.length; algIndex++) {
            SortedMap<Float, Float> mapOfAvgAccuracyForAlg = mapOfAvgAccuracyForAlgs[algIndex];
            computeAvgAccuracyAfterProcessingBatchObjects(mapOfAvgAccuracyForAlg, preciseAnswers, currAnswers[algIndex], oCounter, k);
        }
    }

    private static void computeAvgAccuracyAfterProcessingBatchObjects(SortedMap<Float, Float> mapOfAvgAccuracyForAlg, TreeSet<Map.Entry<Comparable, Float>>[] preciseAnswers, TreeSet<Map.Entry<Comparable, Float>>[] currAnswer, int oCounter, int k) {
        int hits = 0;
        for (int queryIndex = 0; queryIndex < preciseAnswers.length; queryIndex++) {
            Iterator<Map.Entry<Comparable, Float>> precise = preciseAnswers[queryIndex].iterator();
            Iterator<Map.Entry<Comparable, Float>> approx = currAnswer[queryIndex].iterator();
            float nextPrecise, nextApprox;
            recall:
            while (precise.hasNext()) {
                nextPrecise = precise.next().getValue();
                nextApprox = approx.next().getValue();
                while (nextPrecise < nextApprox) {
                    if (precise.hasNext()) {
                        nextPrecise = precise.next().getValue();
                    } else {
                        break recall;
                    }
                }
                hits++;
            }
        }
        float avgRecall = ((float) hits) / preciseAnswers.length / k;
        mapOfAvgAccuracyForAlg.put((float) oCounter, avgRecall);
    }

}
