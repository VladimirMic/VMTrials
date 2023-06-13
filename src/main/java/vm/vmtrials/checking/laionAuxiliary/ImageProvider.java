package vm.vmtrials.checking.laionAuxiliary;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.fs.FSGlobal;
import vm.fs.dataset.FSDatasetInstanceSingularizator;
import vm.metricSpace.Dataset;

/**
 *
 * @author xmic
 */
public class ImageProvider {

    public static void main(String[] args) {
        Dataset dataset = new FSDatasetInstanceSingularizator.LAION_10M_Dataset();
        ImageProvider imageProvider = new ImageProvider(dataset);
        String url = imageProvider.getURLForImage("10");
        System.out.println(url);
        imageProvider.openURL(url);
    }

    private final List<String>[] csv;

    public ImageProvider(Dataset dataset) {
        File f = null;
        String datasetName = dataset.getDatasetName();
        switch (datasetName) {
            case "laion2B-en-clip768v2-n=10M.h5":
                f = new File(FSGlobal.DATASET_METADATA, "meta-10M.tsv");
                break;
            case "laion2B-en-clip768v2-n=30M.h5":
                f = new File(FSGlobal.DATASET_METADATA, "meta-30M.tsv");
                break;
            case "laion2B-en-clip768v2-n=100M.h5":
                f = new File(FSGlobal.DATASET_METADATA, "meta-100M.tsv");
                break;
            default:
                break;
        }
        if (f == null) {
            throw new IllegalArgumentException("We have the images just for 10M, 30? and 100M LAION dataset.");
        }
        csv = Tools.parseCsv(f.getAbsolutePath(), 4, "\t", false);
    }

    public String getURLForImage(String id) {
        int idx = Integer.parseInt(id);
        return csv[3].get(idx);
    }

    public void openURL(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (IOException ex) {
            Logger.getLogger(ImageProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
