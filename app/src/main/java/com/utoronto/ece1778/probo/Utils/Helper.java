package com.utoronto.ece1778.probo.Utils;

import android.app.Activity;
import android.content.Context;
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
}
