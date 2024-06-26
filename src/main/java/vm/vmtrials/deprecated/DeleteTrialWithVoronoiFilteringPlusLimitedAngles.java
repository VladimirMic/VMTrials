package vm.vmtrials.deprecated;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.metricSpace.ToolsMetricDomain;
import vm.metricSpace.distance.DistanceFunctionInterface;
import vm.metricSpace.distance.bounding.onepivot.AbstractOnePivotFilter;
import vm.search.algorithm.SearchingAlgorithm;
import vm.metricSpace.datasetPartitioning.StorageDatasetPartitionsInterface;

/**
 *
 * @author Vlada
 * @param <T> type of data used in the distance function
 */
/**
 *
 * @author Vlada
 */
@Deprecated
public class DeleteTrialWithVoronoiFilteringPlusLimitedAngles<T> extends SearchingAlgorithm<T> {

    private final Logger LOG = Logger.getLogger(DeleteTrialWithVoronoiFilteringPlusLimitedAngles.class.getName());

    private final Map<Comparable, T> pivotsMap;
    private final DistanceFunctionInterface<T> df;
    private final Map<Comparable, TreeSet<Comparable>> voronoiPartitioning;

    private final AbstractOnePivotFilter filter;
    private final Map delete;

    public DeleteTrialWithVoronoiFilteringPlusLimitedAngles(Dataset<T> dataset, StorageDatasetPartitionsInterface voronoiPartitioningStorage, int pivotCountUsedForTheVoronoiPartitioning) {
        this(dataset, voronoiPartitioningStorage, 0, null);
    }

    public DeleteTrialWithVoronoiFilteringPlusLimitedAngles(Dataset<T> dataset, StorageDatasetPartitionsInterface voronoiPartitioningStorage, int pivotCountUsedForTheVoronoiPartitioning, AbstractOnePivotFilter filter) {
        List pivots = dataset.getPivots(-1);
        pivotsMap = ToolsMetricDomain.getMetricObjectsAsIdDataMap(dataset.getMetricSpace(), pivots);
        df = dataset.getDistanceFunction();
        voronoiPartitioning = voronoiPartitioningStorage.load(dataset.getDatasetName(), pivotCountUsedForTheVoronoiPartitioning);
        this.filter = filter;
        delete = dataset.getKeyValueStorage();
    }

    @Override
    public TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams) {
        throw new UnsupportedOperationException("Not supported and will not be.");
    }

    /**
     *
     * @param metricSpace
     * @param fullQueryObj
     * @param k
     * @param objects ignored!
     * @return
     */
    @Override
    public List<Comparable> candSetKnnSearch(AbstractMetricSpace<T> metricSpace, Object fullQueryObj, int k, Iterator<Object> objects, Object... additionalParams) {
        T qData = metricSpace.getDataOfMetricObject(fullQueryObj);
        TreeSet<Map.Entry<Comparable, Float>> pivotPerm = ToolsMetricDomain.getPivotIDsPermutationWithDists(df, pivotsMap, qData, -1);
        Iterator<Map.Entry<Comparable, Float>> it = pivotPerm.iterator();
        List<Comparable> ret = new ArrayList<>();
        int idxOfNext = 0;
        TreeSet<Comparable> nextCell = null;
        while (it.hasNext() && (nextCell == null || ret.size() + nextCell.size() < k) && idxOfNext < voronoiPartitioning.size() - 1) {
            Map.Entry<Comparable, Float> nextPIndex = it.next();
            if (nextCell != null) {
                if (filter == null) {
                    ret.addAll(nextCell);
                } else {
                    for (Map.Entry<Comparable, Float> entry : pivotPerm) {
                        Object pID = entry.getKey();
                        T pData = (T) pivotsMap.get(pID);
                        float dQP = entry.getValue();
                        for (Object oID : nextCell) {
                            T oData = (T) delete.get(oID);
                            float dOP = df.getDistance(pData, oData);
                            float lb = filter.lowerBound(dQP, dOP, pID);
                            float ub = filter.upperBound(dQP, dOP, pID);
                            if (df.getDistance(qData, oData) / lb < 2) {
                                String s = "";
                            }

                        }

                    }
                }
            }
            nextCell = voronoiPartitioning.get(nextPIndex.getKey());
            idxOfNext++;
        }
        LOG.log(Level.FINE, "Returning the cand set with {0} objects. It is made of {1} cells", new Object[]{ret.size(), idxOfNext});
        return ret;
    }

    public int getNumberOfPivots() {
        return pivotsMap.size();
    }

    @Override
    public String getResultName() {
        return "Deprecated";
    }

}
