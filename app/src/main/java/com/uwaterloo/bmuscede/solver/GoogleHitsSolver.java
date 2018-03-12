package com.uwaterloo.bmuscede.solver;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by bmuscede on 12/03/18.
 */

public class GoogleHitsSolver extends GoogleSolver implements Solver {
    public int answerQuestion(boolean notQ, String question, List<String> answers) {
        List<Integer> results = new ArrayList<Integer>();

        //Iterates through each question.
        for (String curAns : answers) {
            try {
                results.add(getCurrentItemHits(question + " " + curAns));
            } catch (Exception e) {
                results.add(0);
            }
        }

        //Gets the largest index (or smallest).
        if (!notQ) {
            return getLargestIndex(results);
        } else {

        }
        return getSmallestIndex(results);
    }

    private int getCurrentItemHits(String query) throws Exception{
        Document doc = Jsoup.connect(GOOGLE + URLEncoder.encode(query, CHARSET))
                .userAgent(USER_AGENT).get();
        Element elm = doc.getElementById("resultStats");
        String res = elm.toString().replace(",", "");
        Matcher matcher = Pattern.compile("\\d+").matcher(res);
        matcher.find();
        return Integer.valueOf(matcher.group());
    }
}
