package vm.vmtrials.deprecated;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;
import vm.metricspace.AbstractMetricSpace;
import vm.search.SearchingAlgorithm;
import vm.simRel.SimRelInterface;
import vm.simRel.impl.SimRelEuclideanPCAImpl;
import vm.metricspace.distance.DistanceFunctionInterface;

/**
 * Simple filtering with the simRel function. If o cannot be filtered thanks to
 * the comparison with the last answer object, the full distance is evaluated to
 * decide if o fits the query answer.
 *
 * @author Vlada
 * @param <T> data type of the metric objects used to evaluate the distance and
 the simrel. See metricSpace.getDataOfMetricObject method
 */
@Deprecated
public class SimRelSeqScanKNNJustLastObjCheckThenFullDistEval<T> extends SearchingAlgorithm<T> {

    private static final Logger LOG = Logger.getLogger(SimRelSeqScanKNNJustLastObjCheckThenFullDistEval.class.getName());
    private final SimRelInterface<T> simRelFunc;
    private final DistanceFunctionInterface<T> fullDistanceFunction;

    private int distCounter;

    public SimRelSeqScanKNNJustLastObjCheckThenFullDistEval(SimRelInterface<T> simRelFunc, DistanceFunctionInterface<T> fullDistanceFunction) {
        this.simRelFunc = simRelFunc;
        this.fullDistanceFunction = fullDistanceFunction;
    }

    @Override
    public TreeSet<Map.Entry<Object, Float>> completeKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... paramsToExtractDataFromMetricObject) {
        if (simRelFunc instanceof SimRelEuclideanPCAImpl) {
            SimRelEuclideanPCAImpl euclid = (SimRelEuclideanPCAImpl) simRelFunc;
            euclid.resetEarlyStopsOnCoordsCounts();
        }
        T queryObjectData = metricSpace.getDataOfMetricObject(queryObject, paramsToExtractDataFromMetricObject);
        TreeSet<Map.Entry<Object, Float>> ret = new TreeSet<>(new Tools.MapByValueComparator());
        Map<Object, T> retData = new HashMap<>();
        distCounter = 0;
        for (int i = 1; objects.hasNext(); i++) {
            Object metricObject = objects.next();
            Object idOfMetricObject = metricSpace.getIDOfMetricObject(metricObject);
            T metricObjectData = metricSpace.getDataOfMetricObject(metricObject, paramsToExtractDataFromMetricObject);
            if (ret.size() < k) {
                float distance = fullDistanceFunction.getDistance(queryObjectData, metricObjectData);
                distCounter++;
                ret.add(new AbstractMap.SimpleEntry<>(idOfMetricObject, distance));
                retData.put(idOfMetricObject, metricObjectData);
            } else {
                Object lastID = ret.last().getKey();
                T lastData = retData.get(lastID); // tady dodat polomer posledniho
                short sim = simRelFunc.getMoreSimilar(queryObjectData, lastData, metricObjectData);
                if (sim != 1) { // new object is claimed to be more similar or equaly similar or with unknown relation to o_last
                    float distance = fullDistanceFunction.getDistance(queryObjectData, metricObjectData);
                    distCounter++;// tady pridavat jen pokud je mensi a dodat statistiky noveho posledniho simrelu
                    ret.add(new AbstractMap.SimpleEntry<>(idOfMetricObject, distance));
                    retData.put(idOfMetricObject, metricObjectData);
                    Map.Entry<Object, Float> last = ret.last();
                    ret.remove(last);
                    retData.remove(last.getKey());
                }
            }
            if (i % 1000000 == 0) {
                LOG.log(Level.INFO, "Processed {0} objects, evaluated {1} distances", new Object[]{i, distCounter});
            }
        }
        LOG.log(Level.INFO, "distancesCounter;" + distCounter);
        return ret;
    }

    @Override
    public Integer getDistCompsOfLastExecutedQuery() {
        return distCounter;
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

    @Override
    public List<Object> candSetKnnSearch(AbstractMetricSpace<T> metricSpace, Object queryObject, int k, Iterator<Object> objects, Object... paramsToExtractDataFromMetricObject) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
