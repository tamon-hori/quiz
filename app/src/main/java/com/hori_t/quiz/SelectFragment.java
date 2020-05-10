package com.hori_t.quiz;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link QuizEventListener} interface
 * to handle interaction events.
 * Use the {@link SelectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SelectFragment extends Fragment {

    private QuizEventListener mListener;

    public SelectFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SelectFragment.
     */
    public static SelectFragment newInstance() {
        return new SelectFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_select, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SoundButton peripheral = view.findViewById(R.id.peripheral);
        peripheral.setOnClickListener(v -> {
            if (Build.VERSION_CODES.M > Build.VERSION.SDK_INT) {
                new AlertDialog.Builder(getActivity().getApplicationContext())
                        .setTitle(R.string.error)
                        .setMessage(R.string.peripheral_error_message)
                        .setNeutralButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                        .setCancelable(false)
                        .show();
            } else {
                mListener.changeEvent(
                        QuizEventListener.Event.SELECT_HOST, null);
            }
        });

        Button central = view.findViewById(R.id.central);
        central.setOnClickListener(v -> mListener.changeEvent(
                QuizEventListener.Event.SELECT_GUEST, null));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof QuizEventListener) {
            mListener = (QuizEventListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement QuizEventListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
