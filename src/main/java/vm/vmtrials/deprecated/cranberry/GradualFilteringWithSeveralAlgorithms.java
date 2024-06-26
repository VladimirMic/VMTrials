//package vm.vmtrials.deprecated.cranberry;
//
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.TreeSet;
//import vm.metricSpace.AbstractMetricSpace;
//import vm.metricSpace.Dataset2;
//import vm.search.algorithm.SearchingAlgorithm;
//
///**
// *
// * @author Vlada
// * @param <T> data type for the distance evaluation
// */
//@Deprecated // mixture of parameters with different semantics
//public class GradualFilteringWithSeveralAlgorithms<T> extends SearchingAlgorithm<T> {
//
//    private final SearchingAlgorithm<T>[] algs;
//    private final Dataset2<T> dataset;
//
//    public GradualFilteringWithSeveralAlgorithms(Dataset2<T> dataset, SearchingAlgorithm<T>... algs) {
//        this.algs = algs;
//        this.dataset = dataset;
//    }
//
//    @Override
//    public TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams) {
//        List<Comparable> candSetKnnSearch = candSetKnnSearch(metricSpace, queryObject, k, objects);
//        TreeSet ret = rerankCandidateSet(metricSpace, queryObject, k, dataset.getDistanceFunction(), dataset.getKeyValueStorage(), candSetKnnSearch);
//        return ret;
//    }
//
//    @Override
//    public List<Comparable> candSetKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Comparable> objects, Object... additionalParams) {
//        Iterator<Comparable> cands = objects;
//        List<Comparable> candSetKnnSearch = null;
//        for (SearchingAlgorithm<T> alg : algs) {
//            candSetKnnSearch = alg.candSetKnnSearch(metricSpace, queryObject, k, cands);
//            cands = candSetKnnSearch.iterator();
//        }
//        return candSetKnnSearch;
//    }
//
//    @Override
//    public String getResultName() {
//        return "Deprecated";
//    }
//
//}
