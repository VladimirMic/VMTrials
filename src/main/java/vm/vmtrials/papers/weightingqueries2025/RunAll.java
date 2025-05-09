/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.papers.weightingqueries2025;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.JFreeChart;
import vm.plot.impl.BoxPlotXCategoriesPlotter;
import static vm.vmtrials.papers.weightingqueries2025.DataParser.IGNORE_SIDES;

/**
 *
 * @author au734419
 */
public class RunAll {

    public static final Set<String> TRIPLETS_IDS_FOR_PLOT = new TreeSet<>();

    static {
//        TRIPLETS_IDS_FOR_PLOT.add("69");
//        TRIPLETS_IDS_FOR_PLOT.add("134");
//        TRIPLETS_IDS_FOR_PLOT.add("63");
//        TRIPLETS_IDS_FOR_PLOT.add("37");
//////        TRIPLETS_IDS_FOR_PLOT.add("108");
//////        TRIPLETS_IDS_FOR_PLOT.add("146");
//////        TRIPLETS_IDS_FOR_PLOT.add("2");
//////        TRIPLETS_IDS_FOR_PLOT.add("78");
//////        TRIPLETS_IDS_FOR_PLOT.add("20");
//////        TRIPLETS_IDS_FOR_PLOT.add("4");
////        TRIPLETS_IDS_FOR_PLOT.add("64");
////        TRIPLETS_IDS_FOR_PLOT.add("73");
////        TRIPLETS_IDS_FOR_PLOT.add("138");
//        TRIPLETS_IDS_FOR_PLOT.add("121");
//        TRIPLETS_IDS_FOR_PLOT.add("136");
    }
    public static final Logger LOG = Logger.getLogger(RunAll.class.getName());

    public static void main(String[] args) {
        DataParser parser = new DataParser();
        DBAnswers dbAnswers = parser.getDbAnswers();
        Map<ImageTriplet, List<Integer>> tripletsToTheirAssessments = dbAnswers.getAsTripletsToTheirAssessments();
        createBoxPlotWithVariances(tripletsToTheirAssessments, dbAnswers);
    }

    private static void createBoxPlotWithVariances(Map<ImageTriplet, List<Integer>> tripletsToTheirAssessments, DBAnswers dbAnswers) {
        // preparing
        SortedSet<AbstractMap.SimpleEntry<ImageTriplet, float[]>> sortedByMeans = dbAnswers.getTripletsSortedByMean(tripletsToTheirAssessments);
        int size = TRIPLETS_IDS_FOR_PLOT.isEmpty() ? sortedByMeans.size() : TRIPLETS_IDS_FOR_PLOT.size();
        String[] groupsNames = new String[size];
        List<Float>[] values = new List[size];
        int i = 0;
        for (AbstractMap.SimpleEntry<ImageTriplet, float[]> next : sortedByMeans) {
            ImageTriplet key = next.getKey();
            if (TRIPLETS_IDS_FOR_PLOT.isEmpty() || TRIPLETS_IDS_FOR_PLOT.contains(key.toString())) {
                float[] value = next.getValue();
                LOG.log(Level.INFO, "Triplet: {0}: Mean: {1}, Variance: {2}, STD: {3}, Median: {4}, IQD: {5}, count: {6}", new Object[]{key.toString(), value[0], value[1], value[2], value[3], value[4], value[5]});
                groupsNames[i] = key.toString();
                List list = tripletsToTheirAssessments.get(key);
                values[i] = list;
                i++;
            }
        }
        // plotting
        BoxPlotXCategoriesPlotter plotter = new BoxPlotXCategoriesPlotter();
        int max = IGNORE_SIDES ? 50 : 100;
        plotter.setYBounds(0, max);
        plotter.setYStep(5d);
        plotter.setVerticalXLabels(true);
        float scale = TRIPLETS_IDS_FOR_PLOT.isEmpty() ? 0.3f : 1f;
        plotter.setBoxWidthScale(scale);
        JFreeChart plot = plotter.createPlot("", "Query (Triplet ID)", "Crowd-Sourced Assessment", null, null, groupsNames, values);
        String path = DataParser.PATH_TO_TRIPLETS + "_AnswersDistribution" + TRIPLETS_IDS_FOR_PLOT.size();
        if (TRIPLETS_IDS_FOR_PLOT.isEmpty()) {
            plotter.storePlotPDF(path, plot, plotter.IMPLICIT_WIDTH * 5, (int) (plotter.IMPLICIT_HEIGHT));
            plotter.storePlotPNG(path, plot, plotter.IMPLICIT_WIDTH * 5, (int) (plotter.IMPLICIT_HEIGHT));
        } else {
            plotter.storePlotPDF(path, plot);
            plotter.storePlotPNG(path, plot);
        }
    }
}
