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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import tq.apps.obg.R;
import tq.apps.obg.databinding.ActivityTqBinding;
import tq.apps.obg.db.DBHelper;
import tq.apps.obg.domain.EmblemDBList;
import tq.apps.obg.domain.EmblemVO;
import tq.apps.obg.domain.PersonDBList;
import tq.apps.obg.domain.PersonVO;
import tq.apps.obg.domain.TileDBList;
import tq.apps.obg.domain.TileVO;
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

public class TQActivity extends AppCompatActivity implements View.OnClickListener{
    ActivityTqBinding mBinding;
    private int levelNum;
    private DBHelper dbHelper;
    private int quizLife = 3;
    private UserServiceInterface mServiceInterface;
    private boolean isPlayerQuiz;
    CountDownTimer mCountDown;
    Handler mProgressHandler;
    int quizCount;
    ProgressBar quizProg= null;
    Timer timer = null;
    TimerTask timerTask = null;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("quizRe") != null) {
                quizReadyListener();
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
        mServiceInterface = UserApplication.getInstance().getServiceInterface();
        dbHelper = DBHelper.getInstance(this);
        Intent intent = getIntent();
        String str = intent.getStringExtra("quizKinds");
        if (str.equals("player")) {
            isPlayerQuiz = true;
        } else {
            isPlayerQuiz = false;
        }
        quizCount = 150;
        quizProg = mBinding.quizTimer;
        mBinding.contents1.setOnClickListener(this);
        mBinding.contents2.setOnClickListener(this);
        mBinding.contents3.setOnClickListener(this);
        mBinding.contents4.setOnClickListener(this);
        mBinding.testGogo.setOnClickListener(this);
        quizReadyListener();
        registerBroadcast();
        mProgressHandler = new Handler(){
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (quizCount <= 0) {
                    quizGameOverListener();
                } else {
                    System.out.println("quizCount :: "+quizCount);
                    quizCount--;
                    quizProg.incrementProgressBy(-1);
                    mProgressHandler.sendEmptyMessageDelayed(0, 100);
                }
            }
        };
    }


    private void setNextLevelFragment() {
        startTimerThread();
        if (levelNum != 9) {
            levelNum = mServiceInterface.getQuizLevel();
        }
        mBinding.buttonLayout.setVisibility(View.GONE);
        Fragment fragment = null;
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
            fragment = new GameOverFragment();
        }
        FragmentManager fm = getFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(R.id.level_fragment, fragment);
        fragmentTransaction.commit();
        setBtnContents();
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.contents1:
                checkAnswer(mBinding.contents1.getText().toString());
                break;
            case R.id.contents2:
                checkAnswer(mBinding.contents2.getText().toString());
                break;
            case R.id.contents3:
                checkAnswer(mBinding.contents3.getText().toString());
                break;
            case R.id.contents4:
                checkAnswer(mBinding.contents4.getText().toString());
                break;
            case R.id.test_gogo:
                setNextLevelFragment();
                break;
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

    public void checkAnswer(String answerStr) {
        if(mServiceInterface.isAnswer(answerStr, mServiceInterface.getIsPlayerQuiz())){
            setNextLevelFragment();
        } else {
            Toast.makeText(getApplicationContext(), "땡!!!!!!!!!", Toast.LENGTH_SHORT).show();
            setQuizLife();

        }
        mBinding.quizScoreText.setText(String.valueOf(mServiceInterface.getQuizScore()));
    }
    public void setQuizLife() {
        quizLife--;
        if (quizLife == 2) {
            mBinding.quizLife1.setVisibility(View.GONE);
            setNextLevelFragment();
        } else if (quizLife == 1) {
            mBinding.quizLife2.setVisibility(View.GONE);
            setNextLevelFragment();
        } else {
            mBinding.quizLife3.setVisibility(View.GONE);
            quizGameOverListener();
        }
    }
    public void registerBroadcast(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(BroadcastActions.BUTTON_VISIABLE);
        registerReceiver(mBroadcastReceiver, filter);
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
    private void buttonVisible() {

        mBinding.buttonLayout.setVisibility(View.VISIBLE);

    }

    private void startTimerThread() {
        /*timerTask = new TimerTask() {
            @Override
            public void run() {
                decreaseBar();
            }
        };
        timer = new Timer();
        timer.schedule(timerTask, 0 ,1000);*/
        /*mCountDown = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long l) {
                quizCount++;
                quizProg.setProgress((int)quizCount*100/(5000/1000));
            }

            @Override
            public void onFinish() {
                quizCount++;
                quizProg.setProgress(100);
            }
        };
        mCountDown.start();*/
        mProgressHandler.removeMessages(0);
        quizCount = 150;
        quizProg.setMax(quizCount);
        quizProg.setProgress(quizCount);
        mProgressHandler.sendEmptyMessage(0);

    }

    private void decreaseBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                quizCount = quizProg.getProgress();
                if (quizCount > 0) {
                    quizCount-=1;
                } else if (quizCount == 0) {
                    timer.cancel();
                    Thread.interrupted();
                }
                quizProg.setProgress(quizCount);
            }
        });
    }
    private void quizGameOverListener() {
        System.out.println("Game Over");
        levelNum = 9;
        setNextLevelFragment();
        mBinding.tqTopLayout.setVisibility(View.GONE);
        mBinding.tqBottomLayout.setVisibility(View.GONE);
    }

    private void quizReadyListener() {
        levelNum = 0;
        mBinding.tqTopLayout.setVisibility(View.VISIBLE);
        mBinding.tqBottomLayout.setVisibility(View.VISIBLE);
        mServiceInterface.setTileImageList();
        mServiceInterface.setIsPlayerQuiz(isPlayerQuiz);
        setNextLevelFragment();
    }
}
