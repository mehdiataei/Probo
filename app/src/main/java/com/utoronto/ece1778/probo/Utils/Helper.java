package com.utoronto.ece1778.probo.Utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class Helper {
    public static void hideKeyboard(Context context, View currentFocus) {
        if (currentFocus == null) {
            return;
        }

        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(
                Activity.INPUT_METHOD_SERVICE
        );

        inputMethodManager.hideSoftInputFromWindow(
                currentFocus.getWindowToken(),
                0
        );
    }

    public static void vibrate(Context context, int ms) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(ms);
        }
    }
}
