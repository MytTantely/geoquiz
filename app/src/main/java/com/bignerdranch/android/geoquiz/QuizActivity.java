package com.bignerdranch.android.geoquiz;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.couchbase.lite.BasicAuthenticator;
import com.couchbase.lite.DataSource;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.Document;
import com.couchbase.lite.Endpoint;
import com.couchbase.lite.Expression;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryBuilder;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.ReplicatorChange;
import com.couchbase.lite.ReplicatorChangeListener;
import com.couchbase.lite.ReplicatorConfiguration;
import com.couchbase.lite.ResultSet;
import com.couchbase.lite.SelectResult;
import com.couchbase.lite.URLEndpoint;

import java.net.URI;

import static android.view.Gravity.*;

public class QuizActivity extends AppCompatActivity {

    private static final String TAG = "QuizActivity";
    private Button mTrueButton;
    private Button mFalseButton;
    private Button mNextButton;
    private Button mPrevButton;
    private Button mCheatButton;
    private TextView mQuestionTextView;

    private static final String KEY_INDEX = "index";

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(KEY_INDEX, mCurrentIndex);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        if(savedInstanceState != null){
            mCurrentIndex = savedInstanceState.getInt(KEY_INDEX, 0);
        }

        mQuestionTextView = (TextView) findViewById(R.id.question_text_view);
        mQuestionTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextQuestion();
            }
        });

        mNextButton = (Button) findViewById(R.id.next_button);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextQuestion();
            }
        });

        mPrevButton = (Button) findViewById(R.id.prev_button);
        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prevQuestion();
            }
        });

        mTrueButton = (Button) findViewById(R.id.true_button);
        mTrueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendToastMessage(checkAnswer(true));

            }
        });

        mCheatButton = (Button) findViewById(R.id.cheat_button);
        mCheatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isAnswerTrue = mQuestionBank[mCurrentIndex].isAnswerTrue();
                int questionId = mQuestionBank[mCurrentIndex].getTextResId();
                Intent intent = CheatActivity.newIntent(QuizActivity.this, isAnswerTrue, questionId);

                startActivity(intent);
            }
        });


        mFalseButton = (Button) findViewById(R.id.false_button);
        mFalseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               sendToastMessage(checkAnswer(false));
            }
        });

        updateQuestion();
    }

    private void prevQuestion() {
        mCurrentIndex = (mCurrentIndex - 1) % mQuestionBank.length;
        if(mCurrentIndex < 0){
            mCurrentIndex = mQuestionBank.length - 1;
        }
        Log.i("QuizActivity", "mCurrentIndex: " + mCurrentIndex);
        updateQuestion();
    }

    private void nextQuestion() {
        mCurrentIndex = (mCurrentIndex + 1) % mQuestionBank.length;
        updateQuestion();
    }

    @Override
    public void onStart() {
        super.onStart();

        try{
            // Get the database (and create it if it doesn’t exist).
            DatabaseConfiguration config = new DatabaseConfiguration(getApplicationContext());
            Database database = new Database("mytdb", config);

            // Create a new document (i.e. a record) in the database.
            MutableDocument mutableDoc = new MutableDocument()
                    .setFloat("version", 2.0F)
                    .setString("type", "SDK");

            // Save it to the database.
            database.save(mutableDoc);

            // Update a document.
            mutableDoc = database.getDocument(mutableDoc.getId()).toMutable();
            mutableDoc.setString("language", "Java");
            database.save(mutableDoc);
            Document document = database.getDocument(mutableDoc.getId());
            // Log the document ID (generated by the database) and properties
            Log.i(TAG, "Document ID :: " + document.getId());
            Log.i(TAG, "Learning " + document.getString("language"));

            // Create a query to fetch documents of type SDK.
            Query query = QueryBuilder.select(SelectResult.all())
                    .from(DataSource.database(database))
                    .where(Expression.property("type").equalTo(Expression.string("SDK")));
            ResultSet result = query.execute();
            Log.i(TAG, "Number of rows ::  " + result.allResults().size());
//            Log.i(TAG, "Content of row [0] ::  " + result.allResults().get(0));


            // Create replicators to push and pull changes to and from the cloud.
            Endpoint targetEndpoint = new URLEndpoint(new URI("ws://localhost:4984/mytdb"));
            ReplicatorConfiguration replConfig = new ReplicatorConfiguration(database, targetEndpoint);
            replConfig.setReplicatorType(ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL);

            // Add authentication.
            replConfig.setAuthenticator(new BasicAuthenticator("Administrator", "password"));

            // Create replicator.
            Replicator replicator = new Replicator(replConfig);

            // Listen to replicator change events.
            replicator.addChangeListener(new ReplicatorChangeListener() {
                @Override
                public void changed(ReplicatorChange change) {
                    if (change.getStatus().getError() != null)
                        Log.i(TAG, "Error code ::  " + change.getStatus().getError().getCode());
                }
            });

            // Start replication.
            replicator.start();
        }catch(Throwable t){
            Log.i( TAG, t.getMessage());
        }
    }

    private Question[] mQuestionBank = new Question[] {
            new Question(R.string.question_australia, true),
            new Question(R.string.question_oceans, true),
            new Question(R.string.question_mideast, false),
            new Question(R.string.question_africa, false),
            new Question(R.string.question_americas, true),
            new Question(R.string.question_asia, true)
    };

    private int mCurrentIndex = 0;

    private void updateQuestion(){
        final int question = mQuestionBank[mCurrentIndex].getTextResId();
        mQuestionTextView.setText(question);
    }

    private void sendToastMessage(int msg){
        Toast t = Toast.makeText(QuizActivity.this,
                msg,
                Toast.LENGTH_SHORT);
        t.setGravity(TOP, 0, 0);
        t.show();
    }

    private int checkAnswer(boolean userSChoice){
        boolean correctAnswer = mQuestionBank[mCurrentIndex].isAnswerTrue();
        int responseMsgId;
        if(userSChoice == correctAnswer){
            responseMsgId = R.string.correct_toast;
        }else{
            responseMsgId = R.string.incorrect_toast;
        }

        return responseMsgId;
    }
}
