package vm.vmtrials.deprecated;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import vm.metricSpace.AbstractMetricSpace;
import vm.metricSpace.Dataset;
import vm.search.SearchingAlgorithm;

/**
 *
 * @author Vlada
 * @param <T> data type for the distance evaluation
 */
@Deprecated // mixture of parameters with different semantics
public class GradualFilteringWithSeveralAlgorithms<T> extends SearchingAlgorithm<T> {

    private final SearchingAlgorithm<T>[] algs;
    private final Dataset dataset;

    public GradualFilteringWithSeveralAlgorithms(Dataset dataset, SearchingAlgorithm<T>... algs) {
        this.algs = algs;
        this.dataset = dataset;
    }

    @Override
    public TreeSet<Map.Entry<Object, Float>> completeKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams) {
        List<Object> candSetKnnSearch = candSetKnnSearch(metricSpace, queryObject, k, objects);
        TreeSet ret = rerankCandidateSet(metricSpace, queryObject, k, dataset.getDatasetName(), dataset.getKeyValueStorage(), candSetKnnSearch);
        return ret;
    }

    @Override
    public List<Object> candSetKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object ... additionalParams) {
        Iterator<Object> cands = objects;
        List<Object> candSetKnnSearch = null;
        for (SearchingAlgorithm<T> alg : algs) {
            candSetKnnSearch = alg.candSetKnnSearch(metricSpace, queryObject, k, cands);
            cands = candSetKnnSearch.iterator();
        }
        return candSetKnnSearch;
    }

}
