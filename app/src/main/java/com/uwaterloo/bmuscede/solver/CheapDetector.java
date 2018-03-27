package com.uwaterloo.bmuscede.solver;

import android.os.AsyncTask;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

public class CheapDetector {
    public enum Type {
        GOOGLE_HITS("Google Hits",
                "Counts the number of results returned for the question paired with each answer",
                0.4, new GoogleHitsSolver()),
        GOOGLE_COUNT("Google Results Count",
                "Counts the number of occurrences for each answer in a search of the question.",
                0.6, new GoogleResultsSolver()),
        WIKIPEDIA("Wikipedia Solver",
                "Searches Wikipedia for the answer using Google's context awareness API.",
                0.0, new WikipediaSolver());

        String name;
        String desc;
        double weight;
        boolean returned;
        int calcResult;
        Solver solver;

        private Type(String name, String desc, double weight, Solver curSlv) {
            this.name = name;
            this.desc = desc;
            this.weight = weight;
            calcResult = -1;
            returned = false;
            solver = curSlv;
        }

        public void reset(){
            calcResult = -1;
            returned = false;
        }

        public String getName() { return name; }

        public String getDesc() { return desc; }

        public double getWeight() {
            return weight;
        }

        public int getCalcResult(){
            return calcResult;
        }

        public boolean getReturned(){
            return returned;
        }

        public void answerQuestion(boolean notQ, String question, List<String> answers) {
            calcResult = solver.answerQuestion(notQ, question, answers);
            returned = true;
        }
    };

    private final static int TIMEOUT_TIME = 8000;
    private final static int SLEEP_TIME = 100;

    private String question = "";
    private List<String> answers;
    private boolean notQ;
    private int result = -1;

	public CheapDetector(){ }

	public int determineAnswer(String question, List<String> answers){
	    //Clears initial result.
        result = -1;

	    //Clears all the enums.
        for (Type curType : Type.values()) {
           curType.reset();
        }

        int timePassed = 0;

	    //Cleans up the text.
        question = question.toLowerCase().trim();
        List<String> newAnswers = new ArrayList<String>();
        for (String cur : answers){
            newAnswers.add(cur.toLowerCase().trim());
        }
        answers = newAnswers;
        this.question = question;
        this.answers = answers;

        //Checks if we have a not question.
        notQ = false;
        if (question.contains("not")){
            notQ = true;
        }

        //Generates an Async Task for each solver.
        for (Type curType : Type.values()){
            QuestionLoader ld = new QuestionLoader();
            ld.execute(curType);
        }

        //Waits...
        while (true){
            if (allDone() || timePassed == TIMEOUT_TIME){
                break;
            }

            SystemClock.sleep(SLEEP_TIME);
            timePassed += SLEEP_TIME;
        }

        //Generates the final result.
        List<Integer> indicies = new ArrayList<Integer>();
        for (Type curType : Type.values()){
            //Checks for invalid results.
            if (!curType.getReturned()) continue;
            if (curType.getCalcResult() == -1) continue;

            for (int i = 0; i < curType.getWeight() * 100; i++){
                indicies.add(curType.getCalcResult());
            }
        }
        result = getMode(indicies);
        return result;
    }

    public double getConfidence(){
        double confidence = 0.0;
        for (Type curType : Type.values()){
            if (curType.getCalcResult() == result){
                confidence += (curType.getWeight() * 100);
            }
        }

        return confidence;
    }

    private boolean allDone(){
        for (Type curType : Type.values()){
            if (!curType.getReturned()) return false;
        }
        return true;
    }

    private static int getMode(List<Integer> a) {
        int maxValue = -1, maxCount = -1;

        for (int i = 0; i < a.size(); i++) {
            int count = 0;
            for (int j = 0; j < a.size(); j++) {
                if (a.get(j) == a.get(i)) count++;
            }
            if (count > maxCount) {
                maxCount = count;
                maxValue = a.get(i);
            }
        }

        return maxValue;
    }
    private class QuestionLoader extends AsyncTask<Type, Integer, Integer>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(Type... params) {
            int result = 0;
            params[0].answerQuestion(notQ, question, answers);

            return result;
        }

        // This is called from background thread but runs in UI
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        // This runs in UI when background thread finishes
        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
        }
    }
}
