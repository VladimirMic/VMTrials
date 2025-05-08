/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.papers.weightingqueries2025;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import vm.datatools.DataTypeConvertor;
import vm.datatools.Tools;

/**
 *
 * @author au734419
 */
public class DBAnswers {

    public static final Logger LOG = Logger.getLogger(DBAnswers.class.getName());

    /**
     * User_id mapped to triplet id with answer Object can be either integer or
     * list
     */
    private final Map<String, Map<ImageTriplet, Object>> answersOfUsers = new TreeMap<>();
    private int size;

    public void registerUser(String userId) {
        if (!answersOfUsers.containsKey(userId)) {
            answersOfUsers.put(userId, new TreeMap<>());
        }
    }

    public void addAnswer(String userId, ImageTriplet triplet, int answer) {
        size++;
        registerUser(userId);
        Map<ImageTriplet, Object> map = answersOfUsers.get(userId);
        if (map.containsKey(triplet)) {
            Object present = map.get(triplet);
            List<Integer> newAnswers;
            if (present instanceof List) {
                newAnswers = (List<Integer>) present;
            } else {
                newAnswers = new ArrayList<>();
                newAnswers.add((Integer) present);
                map.put(triplet, newAnswers);
            }
            newAnswers.add(answer);
        } else {
            map.put(triplet, answer);
        }
    }

    public Set<String> getUserIDs() {
        return Collections.unmodifiableSet(answersOfUsers.keySet());
    }

    public int getUserCount() {
        return answersOfUsers.keySet().size();
    }

    public Map<ImageTriplet, List<Integer>> getAsTripletsToTheirAssessments() {
        Map<ImageTriplet, List<Integer>> ret = new TreeMap<>();
        for (Map.Entry<String, Map<ImageTriplet, Object>> entry : answersOfUsers.entrySet()) {
            Map<ImageTriplet, Object> tripletAnswers = entry.getValue();
            for (Map.Entry<ImageTriplet, Object> tripletAnswer : tripletAnswers.entrySet()) {
                ImageTriplet imageTriplet = tripletAnswer.getKey();
                Object answers = tripletAnswer.getValue();
                if (!ret.containsKey(imageTriplet)) {
                    ret.put(imageTriplet, new ArrayList<>());
                }
                List<Integer> list = ret.get(imageTriplet);
                if (answers instanceof List cast) {
                    list.addAll(cast);
                } else {
                    list.add((Integer) answers);
                }
            }
        }
        return ret;
    }

    public int size() {
        return size;
    }

    public SortedSet<AbstractMap.SimpleEntry<ImageTriplet, float[]>> getTripletsSortedByMean(Map<ImageTriplet, List<Integer>> tripletsToTheirAssessments) {
        SortedSet<AbstractMap.SimpleEntry<ImageTriplet, float[]>> ret = new TreeSet<>(new Tools.MapByFloatArrayValueComparator<>());
        for (Map.Entry<ImageTriplet, List<Integer>> entry : tripletsToTheirAssessments.entrySet()) {
            List<Integer> answers = entry.getValue();
            int[] ints = DataTypeConvertor.integerListToInts(answers);
            float mean = (float) vm.mathtools.Tools.getMean(ints);
            float median = (float) vm.mathtools.Tools.getMedian(ints);
            float variance = (float) vm.mathtools.Tools.getVariance(ints);
            AbstractMap.SimpleEntry entryToAdd = new AbstractMap.SimpleEntry(entry.getKey(), new float[]{mean, variance, median});
            ret.add(entryToAdd);
        }
        return ret;
    }
}
