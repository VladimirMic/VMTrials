package vm.vmtrials.hdf5;

import io.jhdf.HdfFile;
import io.jhdf.api.Dataset;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Paths;
import vm.datatools.Tools;

/**
 *
 * @author Vlada
 */
public class Reader {

    public static void main(String[] args) throws FileNotFoundException {
        String path = "h:\\Similarity_search\\Dataset\\Query\\public-queries-10k-clip768v2.h5";
        System.setOut(new PrintStream("h:\\Similarity_search\\Dataset\\Query\\public-queries-10k-clip768v2.h5_CHECK.csv"));
        long[] sliceOffset = new long[]{-1, 0};
        int[] sliceDimensions = new int[]{1, -1};

        try (HdfFile hdfFile = new HdfFile(Paths.get(path))) {
            Dataset dataset = hdfFile.getDatasetByPath("emb");

            int[] dimensions = dataset.getDimensions();
            sliceDimensions[1] = dimensions[1];
            for (int i = 0; i < dimensions[0]; i++) {
                sliceOffset[0] = i;
                float[][] dataBuffer = (float[][]) dataset.getData(sliceOffset, sliceDimensions);
                Tools.printArray(dataBuffer[0], ";", true, System.out);
            }
        }
    }
}
