package com.hori_t.quiz;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class SoundButton extends android.support.v7.widget.AppCompatButton {
    @Nullable
    private OnClickListener listener;
    @Nullable
    private SoundPool mSoundPool;
    private int mDecisionId;

    public SoundButton(Context context) {
        super(context);
        setSoundPool(context);
    }

    public SoundButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSoundPool(context);
    }

    public SoundButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSoundPool(context);
    }

    private void setSoundPool(Context context) {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        mSoundPool = new SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(attributes)
                .build();
        mDecisionId = mSoundPool.load(context, R.raw.decision, 1);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        this.listener = l;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            if (listener != null) {
                post(() -> {
                    listener.onClick(SoundButton.this);
                });
            }
            mSoundPool.play(
                    mDecisionId,
                    1.0f,
                    1.0f,
                    0,
                    0,
                    1.0f);
        }

        return super.dispatchTouchEvent(ev);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        super.setClickable(enabled);
    }
}
