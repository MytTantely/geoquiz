package com.bignerdranch.android.geoquiz;

public class Question {
    private int mTextResId;
    private boolean mAnswerTrue;

    public boolean isAnswerTrue() {
        return mAnswerTrue;
    }

    public void setAnswerTrue(boolean answerTrue) {
        mAnswerTrue = answerTrue;
    }

    public Question(int textResId, boolean answerTrue){
        mAnswerTrue = answerTrue;
        mTextResId = textResId;
    }

    public int getTextResId() {
        return mTextResId;
    }

    public void setTextResId(int textResId) {
        mTextResId = textResId;
    }
}
