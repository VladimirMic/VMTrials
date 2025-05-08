/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vm.vmtrials.papers.weightingqueries2025;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import vm.datatools.Tools;

/**
 *
 * @author au734419
 */
public class DataParser {

    public static final Logger LOG = Logger.getLogger(DataParser.class.getName());

    public static final String PATH_TO_TRIPLETS = "c:\\Data\\Dansko\\Markéta_data\\triplets.csv";
    public static final String PATH_TO_ANSWERS = "c:\\Data\\Dansko\\Markéta_data\\answers_from_users.csv";
    /**
     * triplet_id to triplet
     */
    private final Map<Integer, ImageTriplet> triplets = new TreeMap<>();
    private final DBAnswers dbAnswers = new DBAnswers();

    public DataParser() {
        parse();
    }

    public Map<Integer, ImageTriplet> getTriplets() {
        return Collections.unmodifiableMap(triplets);
    }

    public DBAnswers getDbAnswers() {
        return dbAnswers;
    }

    private void parse() {
        List<String[]> tripletsString = Tools.parseCsvRowOriented(PATH_TO_TRIPLETS, ",");
        for (int i = 1; i < tripletsString.size(); i++) {
            String[] tripletString = tripletsString.get(i);
            int id = Integer.parseInt(tripletString[0]);
            ImageTriplet triplet = new ImageTriplet(id, tripletString[1], tripletString[2], tripletString[4]);
            triplets.put(id, triplet);
        }
        LOG.log(Level.INFO, "Found {0} triplets of images that migh be assessed", triplets.size());
        tripletsString = Tools.parseCsvRowOriented(PATH_TO_ANSWERS, ";");
        for (int i = 1; i < tripletsString.size(); i++) {
            String[] tripletString = tripletsString.get(i);
            int tripletId = Integer.parseInt(tripletString[1]);
            ImageTriplet triplet = triplets.get(tripletId);
            dbAnswers.addAnswer(tripletString[0], triplet, Integer.parseInt(tripletString[2]));
        }
        LOG.log(Level.INFO, "Found {0} answers from {1} respondents", new Object[]{dbAnswers.size(), dbAnswers.getUserCount()});
    }

}
