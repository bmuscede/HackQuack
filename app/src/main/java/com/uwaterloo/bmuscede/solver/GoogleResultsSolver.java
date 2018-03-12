package com.uwaterloo.bmuscede.solver;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bmuscede on 12/03/18.
 */

public class GoogleResultsSolver extends GoogleSolver implements Solver {
    public int answerQuestion(boolean notQ, String question, List<String> answers){
        List<Integer> results = new ArrayList<Integer>();

        //Connects to Google.
        Document doc;
        String html = "";
        try {
            doc = Jsoup.connect(GOOGLE + URLEncoder.encode(question, CHARSET))
                    .userAgent(USER_AGENT).get();
            html = doc.toString().toLowerCase();
        } catch (Exception e){
            return -1;
        }

        //Checks for the biggest number of hits.
        for (String ans : answers){
            results.add(count(html, ans));
        }

        //Determines if all results are zero.
        if (isAllZero(results)){
            return -1;
        }

        //Gets the largest index (or smallest).
        if (!notQ){
            return getLargestIndex(results);
        } else {

        }
        return getSmallestIndex(results);
    }
}
