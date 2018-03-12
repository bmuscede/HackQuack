package com.uwaterloo.bmuscede.solver;

import java.util.List;

/**
 * Created by bmuscede on 12/03/18.
 */

public interface Solver {
    int answerQuestion(boolean isNotQ, String question, List<String> answers);
}
