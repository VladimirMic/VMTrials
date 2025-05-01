package vm.vmtrials.simRelEuclidSpace;

import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import vm.datatools.Tools;
import vm.fs.dataset.FSDatasetInstances;
import vm.fs.store.queryResults.FSNearestNeighboursStorageImpl;
import vm.metricSpace.Dataset;

/**
 *
 * @author xmic
 */
public class VisualiseImagesWithSimRelStats {

    public static void main(String[] args) throws IOException, InterruptedException {
        int columns = 28;
        List<String>[] csvWithSimRelStats = Tools.parseCsv("c:\\Data\\2022\\query_obj_id,additional_stats.csv", columns, true);
        FSNearestNeighboursStorageImpl groundTruthStorage = new FSNearestNeighboursStorageImpl();

        Dataset decaf = new FSDatasetInstances.DeCAFDataset();
        Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> groundTruth = groundTruthStorage.getGroundTruthForDataset(decaf.getDatasetName(), decaf.getDatasetName());

        List<String> ids = csvWithSimRelStats[0];
        List<String> filterByFirst = csvWithSimRelStats[1];
        List<String> simRelZero = csvWithSimRelStats[25];
        List<String> simRels = csvWithSimRelStats[26];
        System.setOut(new PrintStream("c:\\Data\\2022\\html.html"));
        System.out.println("<html>");
        System.out.println("<head/>");
        System.out.println("<body>");
        DecimalFormat idFormat = new DecimalFormat("0000000000");
        DecimalFormat df = new DecimalFormat("0,000");

        System.out.println("<table>");

        for (int i = 0; i < 10; i++) {
            printLine(ids, groundTruth, i, filterByFirst, simRelZero, simRels, idFormat, df);
        }
        System.out.println("<tr>");
        System.out.println("<td colspan=11>");
        System.out.println("<hr/>");
        System.out.println("<hr/>");
        System.out.println("<hr/>");
        System.out.println("</td>");
        System.out.println("</tr>");
        System.out.println("<hr/>");
        for (int i = 0; i < 10; i++) {
            int idx = ids.size() - 1 - i;
            printLine(ids, groundTruth, idx, filterByFirst, simRelZero, simRels, idFormat, df);
        }
        System.out.println("</table>");
        System.out.println("</body>");
        System.out.println("</html>");
    }

    public static String formatFloat(float f, int multiple) {
        f = f * multiple;
        f = ((int) (f * 10)) / (float) 10;
        return Float.toString(f);
    }

    private static void printLine(List<String> ids, Map<Comparable, TreeSet<Map.Entry<Comparable, Float>>> groundTruth, int i, List<String> filterByFirst, List<String> simRelZero, List<String> simRels, DecimalFormat idFormat, DecimalFormat df) {
        String id = ids.get(i);
        id = idFormat.format(Integer.parseInt(id));
        int byFirst = Integer.parseInt(filterByFirst.get(i));
        int zero = Integer.parseInt(simRelZero.get(i));
        int total = Integer.parseInt(simRels.get(i));

        String zeroRelative = formatFloat((float) zero / total, 100);
        String byFirstRelative = formatFloat((float) byFirst / total, 100);

        String url = "https://disa.fi.muni.cz/profimedia/images/" + id;
        System.out.println("<tr>");
        System.out.println("<td>" + (i + 1) + "</td>");
        System.out.println("<td style='padding-right: 70px;'>");
        System.out.println("<img src='" + url + "'></img>");
        System.out.println("<br/>");
        System.out.println("By first: " + df.format(byFirst) + " (" + byFirstRelative + " %)");
        System.out.println("<br/>");
        System.out.println("Zero: " + df.format(zero) + " (" + zeroRelative + " %)");
        System.out.println("<br/>");
        System.out.println("SimRels: " + df.format(total));
        System.out.println("</td>");
        Iterator<Map.Entry<Comparable, Float>> nns = groundTruth.get(id).iterator();
        for (int j = 0; j < 10; j++) {
            Map.Entry<Comparable, Float> nn = nns.next();
            String nnID = nn.getKey().toString();
            String dist = formatFloat(nn.getValue(), 1);
            url = "https://disa.fi.muni.cz/profimedia/images/" + nnID;
            System.out.println("<td>");
            System.out.println("<img src='" + url + "'></img>");
            System.out.println("<br/>");
            System.out.println("Dist: " + dist);
            System.out.println("</td>");
        }
        System.out.println("</tr>");
    }

}
