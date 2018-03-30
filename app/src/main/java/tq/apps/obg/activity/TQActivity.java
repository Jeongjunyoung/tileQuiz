package tq.apps.obg.activity;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tq.apps.obg.R;
import tq.apps.obg.databinding.ActivityTqBinding;
import tq.apps.obg.db.DBHelper;
import tq.apps.obg.fragment.GameOverFragment;
import tq.apps.obg.fragment.Level0Fragment;
import tq.apps.obg.fragment.Level1Fragment;
import tq.apps.obg.fragment.Level2Fragment;
import tq.apps.obg.fragment.Level3Fragment;
import tq.apps.obg.fragment.Level4Fragment;
import tq.apps.obg.fragment.Level5Fragment;
import tq.apps.obg.service.BroadcastActions;
import tq.apps.obg.service.UserApplication;
import tq.apps.obg.service.UserServiceInterface;

public class TQActivity extends AppCompatActivity implements View.OnClickListener, Animation.AnimationListener{
    private ActivityTqBinding mBinding;
    private static final int RC_LEADERBOARD_UI = 0;
    private int levelNum, hintNum;
    private DBHelper dbHelper;
    private int quizLife = 3;
    private UserServiceInterface mServiceInterface;
    private InterstitialAd mInterstitialAd;
    private boolean isPlayerQuiz;
    private Handler mProgressHandler;
    private int quizCount;
    private RoundCornerProgressBar quizProg;
    private Fragment fragment = null;
    private long mQuizScore;
    private Handler mHandler = new Handler();
    private AnimationDrawable aDrawable;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;
    private String kindStr;
    private long recordScore;
    private Animation trueAnim;
    private Animation falseAnim;
    private Animation btnLayoutAnim;
    private boolean isGameOver;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("quizRe") != null) {
                quizReadyListener();
            } else if (intent.getStringExtra("tileScore") != null) {
                updateScore();
            } else {
                buttonVisible();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_tq);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        initView();

    }

    @SuppressLint("HandlerLeak")
    private void initView() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        mBinding.tqAdView.loadAd(adRequest);
        setFrontAds();
        mServiceInterface = UserApplication.getInstance().getServiceInterface();
        database = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        myRef = database.getReference("saving-data/user/"+mUser.getUid());
        mBinding.startBtn.setBackgroundResource(R.drawable.start_btn_anim);
        aDrawable = (AnimationDrawable) mBinding.startBtn.getBackground();
        dbHelper = DBHelper.getInstance(this);
        Intent intent = getIntent();
        String str = intent.getStringExtra("quizKinds");
        if (str.equals("player")) {
            isPlayerQuiz = true;
            kindStr = "player_score";
        } else {
            isPlayerQuiz = false;
            kindStr = "emblem_score";
        }
        mServiceInterface.setIsPlayerQuiz(isPlayerQuiz);
        quizCount = 175;
        quizProg = mBinding.quizTimer;
        hintNum = mServiceInterface.getHintNum();
        quizProg.setProgressColor(Color.parseColor("#b7e4b6"));
        quizProg.setProgressBackgroundColor(Color.parseColor("#3e483d"));
        mBinding.contents1.setOnClickListener(this);
        mBinding.contents2.setOnClickListener(this);
        mBinding.contents3.setOnClickListener(this);
        mBinding.contents4.setOnClickListener(this);
        mBinding.startBtnClick.setOnClickListener(this);
        mBinding.startBtn.setOnClickListener(this);
        mBinding.btnViewHint.setOnClickListener(this);
        trueAnim = AnimationUtils.loadAnimation(this, R.anim.shake_btn_true);
        falseAnim = AnimationUtils.loadAnimation(this, R.anim.shake_btn_false);
        btnLayoutAnim = AnimationUtils.loadAnimation(this, R.anim.btn_layout_anim);
        btnLayoutAnim.setAnimationListener(this);
        mProgressHandler = new Handler();
        //mBinding.pauseBtn.setOnClickListener(this);
        //mBinding.pauseCloseBtn.setOnClickListener(this);
        //quizReadyListener();
        registerBroadcast();
        hintNum = mServiceInterface.getHintNum();
        mBinding.tqHintText.setText(String.valueOf(hintNum));
        handlerProgressBar();
        setRecordTextView();
    }


    private void setNextLevelFragment() {
        isGameOver = false;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBinding.contents1.setBackgroundResource(R.drawable.btn_background);
                mBinding.contents2.setBackgroundResource(R.drawable.btn_background);
                mBinding.contents3.setBackgroundResource(R.drawable.btn_background);
                mBinding.contents4.setBackgroundResource(R.drawable.btn_background);
                if (levelNum != 9) {
                    startTimerThread();
                    levelNum = mServiceInterface.getQuizLevel();
                }
                mBinding.buttonLayout.setVisibility(View.GONE);
                if (levelNum == 1) {
                    fragment = new Level1Fragment();
                } else if (levelNum ==2) {
                    fragment = new Level2Fragment();
                } else if (levelNum == 3) {
                    fragment = new Level3Fragment();
                } else if (levelNum == 0) {
                    fragment = new Level0Fragment();
                } else if (levelNum == 4) {
                    fragment = new Level4Fragment();
                } else if (levelNum == 5) {
                    fragment = new Level5Fragment();
                } else if (levelNum == 9) {
                    mBinding.btnViewHint.setEnabled(false);
                    mBinding.tqBottomLayout.setVisibility(View.GONE);
                    mBinding.progressbarLayout.setVisibility(View.GONE);
                    fragment = new GameOverFragment();
                }
                FragmentManager fm = getFragmentManager();
                FragmentTransaction fragmentTransaction = fm.beginTransaction();
                fragmentTransaction.replace(R.id.level_fragment, fragment).commit();
                setBtnContents();
                btnSetEnable(true);
            }
        }, 600);
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.contents1:
                checkAnswer(mBinding.contents1.getText().toString(), mBinding.contents1);
                break;
            case R.id.contents2:
                checkAnswer(mBinding.contents2.getText().toString(), mBinding.contents2);
                break;
            case R.id.contents3:
                checkAnswer(mBinding.contents3.getText().toString(), mBinding.contents3);
                break;
            case R.id.contents4:
                checkAnswer(mBinding.contents4.getText().toString(), mBinding.contents4);
                break;
            case R.id.start_btn_click:
                mBinding.levelFragment.setVisibility(View.VISIBLE);
                mBinding.startBtnLayout.setVisibility(View.GONE);
                setNextLevelFragment();
                break;
            case R.id.start_btn:
                mBinding.levelFragment.setVisibility(View.VISIBLE);
                mBinding.startBtnLayout.setVisibility(View.GONE);
                setNextLevelFragment();
                break;
            case R.id.btn_view_hint:
                hintNum -= 1;
                if (hintNum >= 0) {
                    mServiceInterface.viewHintListener(null);
                    mBinding.tqHintText.setText(String.valueOf(hintNum));
                }
                break;
            /*case R.id.pause_btn:
                //onPause();
                break;
            case R.id.pause_close_btn:
                mBinding.pauseLayout.setVisibility(View.GONE);
                if(!isGameOver) {
                    mProgressHandler.sendEmptyMessage(0);
                }
                break;*/
        }
    }

    public void setBtnContents() {
        List<String> strList = null;
        if (isPlayerQuiz) {
            strList = mServiceInterface.getContentsArr();
        } else {
            strList = mServiceInterface.getEmblemContentsArr();
        }
        mBinding.contents1.setText(strList.get(0).toString());
        mBinding.contents2.setText(strList.get(1).toString());
        mBinding.contents3.setText(strList.get(2).toString());
        mBinding.contents4.setText(strList.get(3).toString());
    }

    public void checkAnswer(String answerStr, Button button) {
        btnSetEnable(false);
        if(mServiceInterface.isAnswer(answerStr, mServiceInterface.getIsPlayerQuiz())){
            button.startAnimation(trueAnim);
            button.setBackgroundResource(R.drawable.btn_background_true);
        } else {
            //오답 선택시 이벤트
            button.startAnimation(falseAnim);
            button.setBackgroundResource(R.drawable.btn_background_false);
            setQuizLife();
        }
        updateScore();
        setNextLevelFragment();
    }
    public void setQuizLife() {
        quizLife--;
        if (quizLife == 2) {
            mBinding.quizLife1.setImageResource(R.drawable.life_none);
        } else if (quizLife == 1) {
            mBinding.quizLife2.setImageResource(R.drawable.life_none);
        } else {
            mBinding.quizLife3.setImageResource(R.drawable.life_none);
            quizGameOverListener();
        }
    }
    public void registerBroadcast(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BroadcastActions.BUTTON_VISIABLE);
        filter.addAction(BroadcastActions.QUIZ_RESTART);
        filter.addAction(BroadcastActions.TILE_SCORE);
        registerReceiver(mBroadcastReceiver, filter);
    }

    private void buttonVisible() {
        mBinding.buttonLayout.setVisibility(View.VISIBLE);
        mBinding.buttonLayout.startAnimation(btnLayoutAnim);

    }

    private void startTimerThread() {
        mProgressHandler.removeMessages(0);
        quizCount = 175;
        quizProg.setMax(quizCount);
        quizProg.setProgress(quizCount);
        mProgressHandler.sendEmptyMessage(0);
    }
    //Game Over 리스너
    @SuppressLint("RestrictedApi")
    private void quizGameOverListener() {
        isGameOver = true;
        //mProgressHandler.removeMessages(0);
        levelNum = 9;
        if (isPlayerQuiz) {
            Games.getLeaderboardsClient(this, GoogleSignIn.getLastSignedInAccount(this)).
                    submitScore(getString(R.string.leaderboard_player_score), mQuizScore);
        } else {
            Games.getLeaderboardsClient(this, GoogleSignIn.getLastSignedInAccount(this)).
                    submitScore(getString(R.string.leaderboard_emblem_score), mQuizScore);
        }
        showFrontAds();
        updateRecordScore();
        showLeaderboard();
        setNextLevelFragment();
    }
    //Quiz Ready 리스너
    private void quizReadyListener() {
        levelNum = 0;
        quizCount = 175;
        quizLife = 3;
        quizProg.setProgress(quizCount);
        mBinding.quizLife1.setBackgroundResource(R.drawable.icon_life);
        mBinding.quizLife2.setBackgroundResource(R.drawable.icon_life);
        mBinding.quizLife3.setBackgroundResource(R.drawable.icon_life);
        mServiceInterface.setQuizScore(0);
        mBinding.quizScoreText.setText("0");
        mBinding.btnViewHint.setEnabled(true);
        mBinding.tqBottomLayout.setVisibility(View.VISIBLE);
        mBinding.progressbarLayout.setVisibility(View.VISIBLE);
        mServiceInterface.setTileImageList();
        mServiceInterface.setIsPlayerQuiz(isPlayerQuiz);
        FragmentManager fm = getFragmentManager();
        setRecordTextView();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        if (fragment != null) {
            fragmentTransaction.remove(fragment).commit();
        }
        setNextLevelFragment();
    }
    public void unregisterBroadcast(){
        unregisterReceiver(mBroadcastReceiver);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProgressHandler.removeMessages(0);
        unregisterBroadcast();
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            aDrawable.start();
        } else {
            aDrawable.stop();
        }
    }
    //리더보드 View
    @SuppressLint("RestrictedApi")
    private void showLeaderboard() {
        if (isPlayerQuiz) {
            Games.getLeaderboardsClient(this, GoogleSignIn.getLastSignedInAccount(this))
                    .getLeaderboardIntent(getString(R.string.leaderboard_player_score))
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            startActivityForResult(intent, RC_LEADERBOARD_UI);
                        }
                    });
        } else {
            Games.getLeaderboardsClient(this, GoogleSignIn.getLastSignedInAccount(this))
                    .getLeaderboardIntent(getString(R.string.leaderboard_emblem_score))
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            startActivityForResult(intent, RC_LEADERBOARD_UI);
                        }
                    });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(TQActivity.this, FrontActivity.class);
        startActivity(intent);
        finish();
    }

    /*@Override
    protected void onPause() {
        System.out.println("ppppppppp");
        *//*mProgressHandler.removeMessages(0);*//*
        super.onPause();
    }

    @Override
    protected void onPostResume() {
        System.out.println("rrrrrrrrrr");
        if (!isGameOver) {
            System.out.println("if rrrrrrrrrrrrr");
            //mProgressHandler.sendEmptyMessage(0);
        }
        super.onPostResume();
    }*/

    private void btnSetEnable(boolean isTrue) {
        mBinding.contents1.setEnabled(isTrue);
        mBinding.contents2.setEnabled(isTrue);
        mBinding.contents3.setEnabled(isTrue);
        mBinding.contents4.setEnabled(isTrue);
    }

    private void updateScore() {
        mQuizScore = (long) mServiceInterface.getQuizScore();
        mBinding.quizScoreText.setText(String.valueOf(mQuizScore));
    }
    private void setRecordTextView() {
        myRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getKey().equals(kindStr)) {
                    mBinding.quizRecordText.setText(dataSnapshot.getValue().toString());
                    recordScore = (long) Integer.parseInt(dataSnapshot.getValue().toString());
                }
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void updateRecordScore() {
        if (mQuizScore > recordScore) {
            myRef.child(kindStr).setValue(mQuizScore);
            mServiceInterface.setNewScore(true);
        } else {
            mServiceInterface.setNewScore(false);
        }
    }

    private void setFrontAds() {
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.front_ads_unit_id));
        final AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        mInterstitialAd.loadAd(adRequest);
        mInterstitialAd.setAdListener(new AdListener(){
            @Override
            public void onAdClosed() {
                AdRequest adRequestClosed = new AdRequest.Builder()
                        .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                        .build();
                mInterstitialAd.loadAd(adRequestClosed);
            }
        });
    }
    private void showFrontAds() {
        mServiceInterface.setFrontAdsCount(1);
        int frontAdsCount = mServiceInterface.getFrontAdsCount();
        if (frontAdsCount > 2 && mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
            mServiceInterface.setFrontAdsCount(0);
        }
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {
        mBinding.buttonLayout.startAnimation(falseAnim);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    @SuppressLint("HandlerLeak")
    private void handlerProgressBar() {
        mProgressHandler = new Handler() {
            public void handleMessage(Message msg) {
//                isGameOver = true;
                super.handleMessage(msg);
                if (quizCount <= 0) {
                    isGameOver = true;
                    quizGameOverListener();
                } else {
                    quizCount -= 0.5;
                    quizProg.setProgress(quizCount);
                    mProgressHandler.sendEmptyMessageDelayed(0, 100);
                }
            }
        };
    }

    /*public class AsyncTaskTimer extends TimerTask{
        @Override
        public void run() {
        }
        int secTime;
        protected void onPreExecute() {
            mBinding.quizTimer.setMax(quizCount);
            super.onPreExecute();
        }
        @Override
        protected Long doInBackground(String... params) {
            //int durationTime = Integer.parseInt(params[0]);

            secTime = 200;
            while (secTime >= 0) {
                publishProgress(secTime);
                if(secTime == 0){
                    System.out.println("End.. End.. End..");
                    break;
                }
                secTime--;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
            quizProg.setProgress(progress[0]);
        }

        protected void onPostExecute(Long result) {
            super.onPostExecute(result);
        }
    }*/
}