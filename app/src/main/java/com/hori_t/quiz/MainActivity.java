package com.hori_t.quiz;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import com.hori_t.quiz_lib.Question.AnswerChoice;
import com.hori_t.quiz_lib.Question.Question;
import com.hori_t.quiz_lib.QuizException;
import com.hori_t.quiz_lib.QuizGuestManager;
import com.hori_t.quiz_lib.QuizHostManager;
import com.hori_t.quiz_lib.QuizPlayer;
import com.sky.hori_t.util.Log;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements QuizEventListener, ActivityCompat.OnRequestPermissionsResultCallback {
    @Nullable
    private QuizHostManager mHostManager;
    @NonNull
    private final QuizHostManager.Callback mHostCallback = new QuizHostManager.Callback() {
        @Override
        public void onWaitingStarted(@NonNull QuizPlayer player) {
        }

        @Override
        public void onPlayerChanged(@NonNull ArrayList<QuizPlayer> playerList) {
            changePlayerList(playerList);
        }

        @Override
        public void onQuestion(@NonNull Question question) {
            _onQuestion(question);
        }

        @Override
        public void onPlayerAnswered(int playerNum) {
            mQuizFragment.onPlayerAnswered(playerNum);
        }

        @Override
        public void onCorrectAnswer(boolean isCorrect, @NonNull AnswerChoice answer) {
            _onCorrectAnswer(isCorrect, answer);
        }

        @Override
        public void onPlayerDisconnected(int playerNum) {
            _onPlayerDisconnected(playerNum);
        }

        @Override
        public void onResult(@NonNull ArrayList<QuizPlayer> result) {
            showResult(result);
        }

        @Override
        public void onStopped(@Nullable QuizException exception) {
            if (null != exception) {
                changeEvent(Event.CONFIRMED_RESULT, null);
                showErrorDialog(getString(R.string.host_error_message));
            }
        }
    };
    @Nullable
    private QuizGuestManager mGuestManager;
    @NonNull
    private final QuizGuestManager.Callback mGuestCallback = new QuizGuestManager.Callback() {
        @Override
        public void onJoined(@NonNull QuizPlayer player) {
        }

        @Override
        public void onPlayerChanged(@NonNull ArrayList<QuizPlayer> playerList) {
            changePlayerList(playerList);
        }

        @Override
        public void onQuestion(@NonNull Question question) {
            _onQuestion(question);
        }

        @Override
        public void onPlayerAnswered(int playerNum) {
            mQuizFragment.onPlayerAnswered(playerNum);
        }

        @Override
        public void onCorrectAnswer(boolean isCorrect, @NonNull AnswerChoice answer) {
            _onCorrectAnswer(isCorrect, answer);
        }

        @Override
        public void onPlayerDisconnected(int playerNum) {
            _onPlayerDisconnected(playerNum);
        }

        @Override
        public void onResult(@NonNull ArrayList<QuizPlayer> result) {
            showResult(result);
        }

        @Override
        public void onStopped(@Nullable QuizException exception) {
            if (null != exception) {
                changeEvent(Event.CONFIRMED_RESULT, null);
                showErrorDialog(getString(R.string.guest_error_message));
            }
        }
    };
    @Nullable
    private PlayerType mType;
    @Nullable
    private Handler mHandler;
    @Nullable
    private AlertDialog mDialog;
    @Nullable
    private WaitingMemberFragment mWaitingMemberFragment;
    @Nullable
    private QuizFragment mQuizFragment;
    @Nullable
    private SoundPool mSoundPool;
    private int mResultSoundId;
    private int mCorrectSoundId;
    private int mIncorrectSoundId;

    private static final int REQUEST_PERMISSION_CODE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (null == savedInstanceState) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.container, FlashFragment.newInstance());
            fragmentTransaction.commit();
        }

        mHandler = new Handler(getMainLooper());

        // Permissionチェック
        if (Build.VERSION_CODES.M <= Build.VERSION.SDK_INT) {
            int permissionCheck = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[] {
                                Manifest.permission.ACCESS_COARSE_LOCATION },
                        REQUEST_PERMISSION_CODE);
            } else {
                mHandler.postDelayed(() -> changeEvent(Event.FLASH_SHOWED, null),
                        1000);
            }
        } else {
            mHandler.postDelayed(() -> changeEvent(Event.FLASH_SHOWED, null),
                    1000);
        }

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mSoundPool = new SoundPool.Builder()
                .setMaxStreams(2)
                .build();
        mResultSoundId = mSoundPool.load(this, R.raw.trumpet, 0);
        mCorrectSoundId = mSoundPool.load(this, R.raw.correct, 0);
        mIncorrectSoundId = mSoundPool.load(this, R.raw.incorrect1, 0);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (REQUEST_PERMISSION_CODE != requestCode) {
            return;
        }
        if ((grantResults.length == 0)
                || (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Log.d("[Permission] Refused.");
            showPermissionDialog();
        } else {
            Log.d("[Permission] Granted.");
            mHandler.postDelayed(() -> changeEvent(Event.FLASH_SHOWED, null),
                    1000);
        }
    }

    @Override
    public void changeEvent(@NonNull Event event, @Nullable Bundle bundle) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        // 戻るボタンを押した際に戻るポイントを設定する(この場合replace()に設定した処理の前まで戻る)
        switch (event) {
            case FLASH_SHOWED: {
                fragmentTransaction.replace(R.id.container, StartFragment.newInstance());
                break;
            }
            case START:
                checkBluetoothOn();
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.replace(R.id.container, SelectFragment.newInstance());
                break;
            case SELECT_HOST: {
                mType = PlayerType.Host;
                mHostManager = new QuizHostManager(
                        this, Looper.getMainLooper(), mHostCallback);
                mHostManager.startWaiting();

                bundle = new Bundle();
                bundle.putInt("PlayerType", PlayerType.Host.getInt());
                mWaitingMemberFragment = WaitingMemberFragment.newInstance();
                mWaitingMemberFragment.setArguments(bundle);

                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.replace(R.id.container, mWaitingMemberFragment);
                break;
            }
            case SELECT_GUEST: {
                mType = PlayerType.Guest;
                mGuestManager = new QuizGuestManager(
                        this, Looper.getMainLooper(), mGuestCallback);
                mGuestManager.discoverHost();

                bundle = new Bundle();
                bundle.putInt("PlayerType", PlayerType.Guest.getInt());
                mWaitingMemberFragment = WaitingMemberFragment.newInstance();
                mWaitingMemberFragment.setArguments(bundle);

                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.replace(R.id.container, mWaitingMemberFragment);
                break;
            }
            case GATHER_MEMBER:
                if (null == bundle) {
                    bundle = new Bundle();
                }
                ArrayList<QuizPlayer> playerList = (mType == PlayerType.Guest) ?
                        mGuestManager.getAllPlayerInfo() : mHostManager.getAllPlayerInfo();
                setMe(playerList);
                bundle.putSerializable("PlayerList", playerList);
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                mQuizFragment = QuizFragment.newInstance();
                mQuizFragment.setArguments(bundle);
                fragmentTransaction.replace(R.id.container, mQuizFragment);
                if (mType == PlayerType.Host) {
                    mHostManager.startQuiz(Constants.QUESTION_NUM);
                }
                break;
            case CONFIRMED_RESULT:
                if (PlayerType.Host == mType) {
                    mHostManager.stop();
                    mHostManager = null;
                } else {
                    mGuestManager.stop();
                    mGuestManager = null;
                }
                mType = null;
                fragmentTransaction.replace(R.id.container, StartFragment.newInstance());
                break;
        }
        fragmentTransaction.commit();
    }

    private void _onQuestion(@NonNull Question question) {
        if (null != mDialog) {
            mDialog.dismiss();
            mDialog = null;
        }
        if ((PlayerType.Guest == mType) && (null == mQuizFragment)) {
            Bundle bundle = new Bundle();
            bundle.putString("QuizText", question.getQuestionText());
            changeEvent(Event.GATHER_MEMBER, bundle);
            return;
        }
        ArrayList<QuizPlayer> playerList = (PlayerType.Host == mType) ?
                mHostManager.getAllPlayerInfo() : mGuestManager.getAllPlayerInfo();
        setMe(playerList);
        mQuizFragment.onQuestion(playerList, question.getQuestionText());
    }

    @Override
    public void answer(@NonNull AnswerChoice choice) {
        Log.d("[Answer] " + choice);
        if (PlayerType.Host == mType) {
            mHostManager.answer(choice);
        } else if (PlayerType.Guest == mType) {
            mGuestManager.answer(choice);
        }
    }

    private void _onCorrectAnswer(boolean isCorrect, @NonNull AnswerChoice choice) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        int soundId;
        if (isCorrect) {
            builder.setMessage(R.string.correct);
            soundId = mCorrectSoundId;
        } else {
            builder.setMessage(getString(R.string.incorrect) + choice);
            soundId = mIncorrectSoundId;
        }
        mDialog = builder.setCancelable(false).create();
        mDialog.show();
        playSound(soundId, false);
    }

    private void changePlayerList(@NonNull ArrayList<QuizPlayer> playerList) {
        setMe(playerList);
        mWaitingMemberFragment.changePlayerList(playerList);
    }

    private void _onPlayerDisconnected(int playerNum) {
        if (null == mQuizFragment) {
            return;
        }
        mQuizFragment.onPlayerDisconnected(playerNum);
    }

    private void showResult(@NonNull ArrayList<QuizPlayer> playerList) {
        mDialog.dismiss();
        setMe(playerList);
        // Bundleに結果のリストをセット
        Bundle bundle = new Bundle();
        bundle.putSerializable("ResultList", playerList);
        ResultFragment resultFragment = ResultFragment.newInstance();
        resultFragment.setArguments(bundle);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fragmentTransaction.replace(R.id.container, resultFragment);
        fragmentTransaction.commit();
        playSound(mResultSoundId, false);
    }

    private void playSound(int soundID, boolean isLoop) {
        Log.vMethodIn();
        int loop = isLoop ? -1 : 0;
        mSoundPool.play(soundID, 1.0f, 1.0f, 0, loop, 1.0f);
        Log.vMethodOut();
    }

    private void setMe(@NonNull ArrayList<QuizPlayer> playerList) {
        QuizPlayer me = (PlayerType.Host == mType) ?
                mHostManager.getMyPlayerInfo() :mGuestManager.getMyPlayerInfo();
        for (QuizPlayer player : playerList) {
            if (me.getId().equals(player.getId())) {
                player.setMe();
                break;
            }
        }
    }

    private void showErrorDialog(@NonNull String message) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.error))
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok), null)
                .setCancelable(false)
                .show();

        Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        button.setOnClickListener(v -> dialog.dismiss());
    }

    private void showIncompatibleBleDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(R.string.ble_incompatible_message)
                .setNeutralButton(R.string.ok, (dialog, which) -> finishAndRemoveTask())
                .setCancelable(false)
                .show();
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(R.string.permission_error_message)
                .setNeutralButton(R.string.ok, (dialog, which) -> finishAndRemoveTask())
                .setCancelable(false)
                .show();
    }

    private void checkBluetoothOn() {
        Log.vMethodIn();
        BluetoothManager bluetoothManager =
                (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        if (null == adapter) {
            showIncompatibleBleDialog();
            Log.d("This device does not support Bluetooth.");
            return;
        }
        if (!adapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        Log.vMethodOut();
    }
}
