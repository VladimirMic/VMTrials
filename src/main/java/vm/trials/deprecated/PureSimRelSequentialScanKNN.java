package vm.trials.deprecated;

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
import vm.metricSpace.AbstractMetricSpace;
import vm.search.SearchingAlgorithm;
import vm.simRel.SimRelInterface;
import vm.simRel.impl.SimRelEuclideanPCAImpl;

/**
 *
 * @author Vlada
 * @param <T>
 */
@Deprecated
public class PureSimRelSequentialScanKNN<T> implements SearchingAlgorithm<T> {

    private static final Logger LOG = Logger.getLogger(PureSimRelSequentialScanKNN.class.getName());
    private final SimRelInterface<T> simRelFunc;

    private int simRelEvalCounter;

    public PureSimRelSequentialScanKNN(SimRelInterface<T> simRelFunc) {
        this.simRelFunc = simRelFunc;
    }

    @Override
    public TreeSet<Map.Entry<Object, Float>> knnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... paramsToExtractDataFromMetricObject) {
        if (simRelFunc instanceof SimRelEuclideanPCAImpl) {
            SimRelEuclideanPCAImpl euclid = (SimRelEuclideanPCAImpl) simRelFunc;
            euclid.resetEarlyStopsOnCoordsCounts();
        }
        T queryObjectData = metricSpace.getMetricObjectData(queryObject, paramsToExtractDataFromMetricObject);
        List<Object> ret = new ArrayList<>();
        Map<Object, T> retData = new HashMap<>();
        simRelEvalCounter = 0;
        for (int i = 1; objects.hasNext(); i++) {
            Object metricObject = objects.next();
            Object idOfMetricObject = metricSpace.getIDOfMetricObject(metricObject);
            T metricObjectData = metricSpace.getMetricObjectData(metricObject, paramsToExtractDataFromMetricObject);
            addOToAnswer(k, queryObjectData, metricObjectData, idOfMetricObject, ret, retData);
            if (i % 1000000 == 0) {
                LOG.log(Level.INFO, "Processed {0} objects, evaluated {1} distances", new Object[]{i, simRelEvalCounter});
            }
        }
        LOG.log(Level.INFO, "distancesCounter;" + simRelEvalCounter);
        return transformListToAbstractDists(ret);
    }

    @Override
    public Integer getDistCompsOfLastExecutedQuery() {
        return simRelEvalCounter;
    }

    @Override
    public Integer getTimeOfLastExecutedQuery() {
        return null;
    }

    public Object getSimRelStatsOfLastExecutedQuery() {
        if (simRelFunc instanceof SimRelEuclideanPCAImpl) {
            SimRelEuclideanPCAImpl euclid = (SimRelEuclideanPCAImpl) simRelFunc;
            return euclid.getEarlyStopsOnCoordsCounts();
        }
        return null;
    }

    private void addOToAnswer(int k, T queryObjectData, T oData, Object idOfO, List<Object> ret, Map<Object, T> retData) {
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
            if (moreSimilar == -1) {
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
            if (moreSimilar == 1) {
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

    private TreeSet<Entry<Object, Float>> transformListToAbstractDists(List<Object> list) {
        TreeSet<Map.Entry<Object, Float>> ret = new TreeSet<>(new Tools.MapByValueComparator());
        for (int i = 0; i < list.size(); i++) {
            ret.add(new AbstractMap.SimpleEntry<>(list.get(i), (float) i));
        }
        return ret;
    }
}
