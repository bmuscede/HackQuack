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
 * Created by bmuscede on 09/03/18.
 */

public abstract class GoogleSolver {
    protected final static String GOOGLE = "http://www.google.com/search?q=";
    protected final static String CHARSET = "UTF-8";
    protected final static String USER_AGENT = "HqCheap 1.0";

    protected int count(String string, String substring){
        int count = 0;
        int idx = 0;

        while ((idx = string.indexOf(substring, idx)) != -1){
            idx++;
            count++;
        }

        return count;
    }

    protected boolean isAllZero(List<Integer> results){
        for (int val : results){
            if (val != 0) return false;
        }

        return true;
    }

    protected int getLargestIndex(List<Integer> list){
        int largest = 0;

        for (int i = 1; i < list.size(); i++){
            if (list.get(i) > list.get(largest)) largest = i;
        }

        return largest;
    }

    protected int getSmallestIndex(List<Integer> list){
        int smallest = 0;

        for (int i = 1; i < list.size(); i++){
            if (list.get(i) < list.get(smallest)) smallest = i;
        }

        return smallest;
    }
}
