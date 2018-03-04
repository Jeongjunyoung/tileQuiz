package tq.apps.obg.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import tq.apps.obg.animation.Rotate3DAnimation;
import tq.apps.obg.db.DBHelper;
import tq.apps.obg.domain.EmblemVO;
import tq.apps.obg.domain.PersonVO;
import tq.apps.obg.domain.TileVO;

/**
 * Created by d1jun on 2018-02-23.
 */

public class UserService extends Service {
    private final IBinder mBinder = new UserServiceBinder();
    private static final int[] levelArr = {0, 1, 1, 2, 2, 2};
    private int clickedNum = 0;
    //private boolean isFront = true;
    private LinearLayout firstView, secondView;
    private Bitmap firstResId, secondResId;
    private int DURATION = 220;
    private float centerX;
    private float centerY;
    private DBHelper dbHelper;
    private List<TileVO> mTileImageList;
    private List<PersonVO> mPersonImageList;
    private List<EmblemVO> mEmblemImageList;
    private PersonVO mAnswerVO;
    private EmblemVO mAnswerMVO;
    private int quizIndex = 0;
    private int quizLevel = 0;
    private int arrIndex = 0;
    private int quizScore = 0;
    private int levelCount = 0;
    private int quizButtonLevel, quizButtonLevelIndex;
    private boolean isPlayerQuiz;
    Handler mHandler = new Handler();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class UserServiceBinder extends Binder {
        public UserService getService() {
            return UserService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = DBHelper.getInstance(getApplicationContext());
        System.out.println("UserService");
        setData();
    }

    private void setData() {
        mTileImageList = dbHelper.selectTielData();
        mPersonImageList = dbHelper.selectPersonData();
        mEmblemImageList = dbHelper.selectEmblemData();
        setmTileImageList();
        setmPersonImageList();
        setmEmblemImageList();
    }

    public List<TileVO> getTileImages() {
        List<TileVO> list = dbHelper.selectTielData();
        long seed = System.nanoTime();
        Collections.shuffle(list, new Random(seed));
        return list;
    }

    public void checkedSameImage(LinearLayout view, Bitmap resId) {
        clickedNum++;
        if (clickedNum == 1) {
            firstView = view;
            firstResId = resId;
        } else if (clickedNum == 2) {
            secondView = view;
            secondResId = resId;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isSameResId(firstResId, secondResId)) {
                        System.out.println("같음");
                        quizButtonLevelIndex += 1;
                        firstView.setBackgroundColor(Color.parseColor("#00000000"));
                        firstView.setEnabled(false);
                        secondView.setBackgroundColor(Color.parseColor("#00000000"));
                        secondView.setEnabled(false);
                        firstView.getChildAt(0).setVisibility(View.GONE);
                        secondView.getChildAt(0).setVisibility(View.GONE);
                        if (quizButtonLevelIndex == quizButtonLevel) {
                            sendBroadcast(new Intent(BroadcastActions.BUTTON_VISIABLE));
                        }
                    } else {
                        System.out.println("다름");
                        applyRotation(0f, 90f, 180f, 0f, firstView, false);
                        applyRotation(0f, 90f, 180f, 0f, secondView, false);
                    }

                }
            }, 290);
            clickedNum = 0;

        }

    }

    public boolean isSameResId(Bitmap num1, Bitmap num2) {
        if (num1.equals(num2)) {
            return true;
        }
        return false;
    }

    public Bitmap getBitMap(ImageView iv) {
        Drawable drawable = iv.getDrawable();
        return ((BitmapDrawable) drawable).getBitmap();
    }

    public void applyRotation(float start, float mid, float end, float depth, LinearLayout layout, boolean isFront) {
        FrameLayout frameLayout = (FrameLayout) layout.getChildAt(0);
        centerX = frameLayout.getWidth() / 2.0f;
        centerY = frameLayout.getHeight() / 2.0f;
        Rotate3DAnimation rot = new Rotate3DAnimation(start, mid, centerX, centerY, depth, true);
        rot.setDuration(DURATION);
        rot.setAnimationListener(new UserService.DisplayNextView(mid, end, depth, layout, isFront));
        frameLayout.startAnimation(rot);
    }

    public class DisplayNextView implements Animation.AnimationListener {
        private float mid;
        private float end;
        private float depth;
        private LinearLayout mLayout;
        private FrameLayout mFrameLayout;
        private boolean isFront;

        public DisplayNextView(float mid, float end, float depth, LinearLayout layout, boolean b) {
            this.mid = mid;
            this.end = end;
            this.depth = depth;
            this.mLayout = layout;
            this.mFrameLayout = (FrameLayout) layout.getChildAt(0);
            this.isFront = b;
        }

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mFrameLayout.post(new Runnable() {
                @Override
                public void run() {
                    if (isFront) {
                        mFrameLayout.getChildAt(0).setVisibility(View.GONE);
                        mFrameLayout.getChildAt(1).setVisibility(View.VISIBLE);
                        mFrameLayout.setEnabled(false);
                        checkedSameImage(mLayout, getBitMap((ImageView) mFrameLayout.getChildAt(1)));
                    } else {
                        mFrameLayout.setEnabled(true);
                        mFrameLayout.getChildAt(0).setVisibility(View.VISIBLE);
                        mFrameLayout.getChildAt(1).setVisibility(View.GONE);
                    }
                    Rotate3DAnimation rot = new Rotate3DAnimation(mid, end, centerX, centerY, depth, false);
                    rot.setDuration(DURATION);
                    rot.setInterpolator(new AccelerateInterpolator());
                    mFrameLayout.startAnimation(rot);
                }
            });
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    public List<String> getEmblemContentsArr() {
        List<String> strArr = new ArrayList<>();
        setmEmblemVO();
        List<EmblemVO> vo = dbHelper.selectEmblemContents(mAnswerMVO.getE_league());
        Collections.shuffle(vo, new Random(getSeed()));
        strArr.add(mAnswerMVO.getE_name());
        boolean isOverlap;
        int voIndex = 0;
        EmblemVO pVo = null;
        for (int i = 1; i < 4; i++) {
            pVo = vo.get(voIndex++);
            isOverlap = false;
            for (int j = 0; j < strArr.size(); j++) {
                if (strArr.get(j).trim().equals(pVo.getE_name().trim())) {
                    i--;
                    isOverlap = true;
                }
            }
            if (!isOverlap) {
                strArr.add(pVo.getE_name());
            }
        }
        Collections.shuffle(strArr, new Random(getSeed()));
        return strArr;
    }

    public List<String> getContentsArr() {
        List<String> strArr = new ArrayList<>();
        setmPersonVO();
        List<PersonVO> vo = dbHelper.selectContentsData(mAnswerVO.getP_job());
        Collections.shuffle(vo, new Random(getSeed()));
        strArr.add(mAnswerVO.getP_name());
        boolean isOverlap;
        int voIndex = 0;
        PersonVO pVo = null;
        for (int i = 1; i < 4; i++) {
            pVo = vo.get(voIndex++);
            isOverlap = false;
            for (int j = 0; j < strArr.size(); j++) {
                if (strArr.get(j).trim().equals(pVo.getP_name().trim())) {
                    i--;
                    isOverlap = true;
                }
            }
            if (!isOverlap) {
                strArr.add(pVo.getP_name());
            }
        }
        Collections.shuffle(strArr, new Random(getSeed()));
        return strArr;
    }

    public void setmTileImageList() {
        Collections.shuffle(mTileImageList, new Random(getSeed()));
    }

    public void setmPersonImageList() {
        Collections.shuffle(mPersonImageList, new Random(getSeed()));
    }

    public void setmEmblemImageList() {
        Collections.shuffle(mEmblemImageList, new Random(getSeed()));
    }

    public List<Integer> getmTileImageList(int level) {
        quizButtonLevel = level / 2;
        System.out.println(quizButtonLevel + "::AA:");
        quizButtonLevelIndex = 0;
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < level; i++) {
            list.add(mTileImageList.get(i).gettile_res_id());
            list.add(mTileImageList.get(i).gettile_res_id());
        }
        Collections.shuffle(list, new Random(getSeed()));
        setmTileImageList();
        setQuizLevel();
        return list;
    }

    public void setmPersonVO() {
        mAnswerVO = mPersonImageList.get(quizIndex);
    }

    public void setmEmblemVO() {
        mAnswerMVO = mEmblemImageList.get(quizIndex);
    }

    public PersonVO getmPersonVO() {
        return mAnswerVO;
    }

    public PersonVO getmPersonImageList() {
        quizIndex++;
        return mAnswerVO;
    }

    public EmblemVO getmEmblemImageList() {
        quizIndex++;
        return mAnswerMVO;
    }

    public int getQiuzIndex() {
        return quizIndex;
    }

    public long getSeed() {
        return System.nanoTime();
    }

    public int getQuizLevel() {
        return quizLevel;
    }

    public void setQuizLevel() {
        if (arrIndex < levelArr.length) {
            quizLevel = levelArr[arrIndex++];
        } else {
            if (levelCount < 5) {
                levelCount++;
            } else {
                quizLevel++;
                levelCount = 0;
            }
            System.out.println("quizLevel : " + quizLevel);
            System.out.println("Level 3!!!!!!!!!!!");
        }
    }

    public boolean isAnswer(String answer, boolean isPlayerQuiz) {
        if (isPlayerQuiz) {
            if (answer.equals(mAnswerVO.getP_name())) {
                setQuizScore(150);
                return true;
            }
        } else {
            if (answer.equals(mAnswerMVO.getE_name())) {
                setQuizScore(150);
                return true;
            }
        }
        return false;
    }

    public void setQuizScore(int score) {
        quizScore += score;
    }

    public int getQuizScore() {
        return quizScore;
    }

    public void setIsPlayerQuiz(boolean quiz) {
        if (quiz) {
            isPlayerQuiz = true;
        } else {
            isPlayerQuiz = false;
        }
    }

    public boolean getIsPlayerQuiz() {
        return isPlayerQuiz;
    }


    //Tile Hint
    public void applyRotationHint(float start, float mid, float end, float depth, FrameLayout frameLayout) {
        centerX = frameLayout.getWidth() / 2.0f;
        centerY = frameLayout.getHeight() / 2.0f;
        Rotate3DAnimation rot = new Rotate3DAnimation(start, mid, centerX, centerY, depth, true);
        rot.setDuration(DURATION);
        rot.setAnimationListener(new DisplayNextViewHint(mid, end, depth, frameLayout));
        frameLayout.startAnimation(rot);
    }

    public class DisplayNextViewHint implements Animation.AnimationListener {
        private float mid;
        private float end;
        private float depth;
        private FrameLayout mFrameLayout;
        private boolean isFrontHint;
        private int isEnd;

        public DisplayNextViewHint(float mid, float end, float depth, FrameLayout frameLayout) {
            this.mid = mid;
            this.end = end;
            this.depth = depth;
            this.mFrameLayout = frameLayout;
            this.isFrontHint = true;
            this.isEnd = 0;
        }

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mFrameLayout.post(new Runnable() {
                @Override
                public void run() {
                    mFrameLayout.getChildAt(0).setVisibility(View.GONE);
                    mFrameLayout.getChildAt(1).setVisibility(View.VISIBLE);
                    mFrameLayout.setEnabled(false);
                    Rotate3DAnimation rot = new Rotate3DAnimation(mid, end, centerX, centerY, depth, false);
                    rot.setDuration(DURATION);
                    rot.setInterpolator(new AccelerateInterpolator());
                    mFrameLayout.startAnimation(rot);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            applyRotationHintBack(180f, 270f, 360f, 0f, mFrameLayout);
                        }
                    }, 170);
                }
            });

        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }
    public void applyRotationHintBack(float start, float mid, float end, float depth, FrameLayout frameLayout) {
        centerX = frameLayout.getWidth() / 2.0f;
        centerY = frameLayout.getHeight() / 2.0f;
        Rotate3DAnimation rot = new Rotate3DAnimation(start, mid, centerX, centerY, depth, true);
        rot.setDuration(DURATION);
        rot.setAnimationListener(new DisplayNextViewHintBack(mid, end, depth, frameLayout));
        frameLayout.startAnimation(rot);
    }

    public class DisplayNextViewHintBack implements Animation.AnimationListener {
        private float mid;
        private float end;
        private float depth;
        private FrameLayout mFrameLayout;

        public DisplayNextViewHintBack(float mid, float end, float depth, FrameLayout frameLayout) {
            this.mid = mid;
            this.end = end;
            this.depth = depth;
            this.mFrameLayout = frameLayout;
        }

        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mFrameLayout.post(new Runnable() {
                @Override
                public void run() {
                    mFrameLayout.getChildAt(0).setVisibility(View.VISIBLE);
                    mFrameLayout.getChildAt(1).setVisibility(View.GONE);
                    mFrameLayout.setEnabled(true);
                    Rotate3DAnimation rot = new Rotate3DAnimation(mid, end, centerX, centerY, depth, false);
                    rot.setDuration(DURATION);
                    rot.setInterpolator(new AccelerateInterpolator());
                    mFrameLayout.startAnimation(rot);
                }
            });

        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }
}