package vm.vmtrials.deprecated.simrel;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.search.algorithm.SearchingAlgorithm;
import vm.searchSpace.AbstractSearchSpace;
import vm.simRel.SimRelInterface;
import vm.simRel.impl.SimRelEuclideanPCAImplForTesting;

/**
 *
 * @author Vlada
 * @param <T>
 */
@Deprecated
public class PureSimRelSequentialScanKNN<T> extends SearchingAlgorithm<T> {

    private final Logger LOG = Logger.getLogger(PureSimRelSequentialScanKNN.class.getName());
    private final SimRelInterface<T> simRelFunc;

    private int simRelEvalCounter;

    public PureSimRelSequentialScanKNN(SimRelInterface<T> simRelFunc) {
        this.simRelFunc = simRelFunc;
    }

    @Override
    public TreeSet<Map.Entry<Comparable, Float>> completeKnnSearch(AbstractSearchSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... paramsToExtractDataFromMetricObject) {
        if (simRelFunc instanceof SimRelEuclideanPCAImplForTesting) {
            SimRelEuclideanPCAImplForTesting euclid = (SimRelEuclideanPCAImplForTesting) simRelFunc;
            euclid.resetEarlyStopsOnCoordsCounts();
        }
        T queryObjectData = metricSpace.getDataOfObject(queryObject);
        List<Comparable> ret = new ArrayList<>();
        Map<Comparable, T> retData = new HashMap<>();
        simRelEvalCounter = 0;
        for (int i = 1; objects.hasNext(); i++) {
            Object metricObject = objects.next();
            Comparable idOfMetricObject = metricSpace.getIDOfObject(metricObject);
            T metricObjectData = metricSpace.getDataOfObject(metricObject);
            addOToAnswer(k, queryObjectData, metricObjectData, idOfMetricObject, ret, retData);
            if (i % 1000000 == 0) {
                LOG.log(Level.INFO, "Processed {0} objects, evaluated {1} distances", new Object[]{i, simRelEvalCounter});
            }
        }
        LOG.log(Level.INFO, "distancesCounter;" + simRelEvalCounter);
        return transformListToAbstractDists(ret);
    }

    public Object getSimRelStatsOfLastExecutedQuery() {
        if (simRelFunc instanceof SimRelEuclideanPCAImplForTesting) {
            SimRelEuclideanPCAImplForTesting euclid = (SimRelEuclideanPCAImplForTesting) simRelFunc;
            return euclid.getEarlyStopsOnCoordsCounts();
        }
        return null;
    }

    private void addOToAnswer(int k, T queryObjectData, T oData, Comparable idOfO, List<Comparable> ret, Map<Comparable, T> retData) {
        if (ret.isEmpty()) {
            ret.add(idOfO);
            retData.put(idOfO, oData);
            return;
        }
        boolean add = false;
        for (int i = ret.size() - 1; i >= 0; i--) {
            T oLastData = retData.get(ret.get(i));
            simRelEvalCounter++;
            short moreSimilar = simRelFunc.getMoreSimilar(queryObjectData, oLastData, oData);
            if (moreSimilar == 1) {
                if (i < k - 1) {
                    ret.add(i + 1, idOfO);
                    retData.put(idOfO, oData);
                    if (ret.size() == k + 1) {
                        retData.remove(ret.get(k));
                        ret.remove(k);
                    }
                }
                return;
            }
            if (moreSimilar == 2) {
                add = true;
            }
        }
        if (add) {
            ret.add(0, idOfO);
            retData.put(idOfO, oData);
            if (ret.size() == k + 1) {
                retData.remove(ret.get(k));
                ret.remove(k);
            }
        }
    }

    private TreeSet<Entry<Comparable, Float>> transformListToAbstractDists(List<Comparable> list) {
        TreeSet<Map.Entry<Comparable, Float>> ret = new TreeSet<>(new Tools.MapByFloatValueComparator());
        for (int i = 0; i < list.size(); i++) {
            ret.add(new AbstractMap.SimpleEntry<>(list.get(i), (float) i));
        }
        return ret;
    }

    @Override
    public List<Comparable> candSetKnnSearch(AbstractSearchSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... additionalParams) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getResultName() {
        return "Deprecated";
    }

}
