package com.example.jiunwei.wordguess.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.example.jiunwei.wordguess.R;

import static android.view.KeyEvent.KEYCODE_ENTER;

/**
 * Provides {@link DialogFragment} to ask for a room code.
 */
public class RoomCodeDialogFragment extends AppCompatDialogFragment {

    /**
     * Listener interface to call when dialog is answered positively.
     */
    public interface OnJoinListener {

        /**
         * Called when user wants to join the given room.
         *
         * @param roomCode Room code of the desired room.
         */
        void onJoin(String roomCode);
    }

    /** Instance of listener to call when dialog is answered positively. */
    private OnJoinListener mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnJoinListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnJoinListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.dialog_room_code, null);
        final EditText editText = view.findViewById(R.id.room_code);
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KEYCODE_ENTER) {
                    mListener.onJoin(editText.getText().toString().trim().toUpperCase());
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        builder.setTitle(R.string.enter_room_code).setView(view)
                .setPositiveButton(R.string.join_short, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onJoin(editText.getText().toString().trim().toUpperCase());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        RoomCodeDialogFragment.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }

}
