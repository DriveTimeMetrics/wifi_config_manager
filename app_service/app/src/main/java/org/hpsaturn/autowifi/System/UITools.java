package org.hpsaturn.autowifi.System;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.provider.Settings.Secure;
import android.support.v4.content.ContextCompat;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.hpsaturn.autowifi.Config;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class UITools {

    private static final boolean DEBUG = Config.DEBUG;
    public static final String TAG = UITools.class.getSimpleName();

    public static void showToast(Context context, int i) {
        int duration = Toast.LENGTH_LONG;
        Toast.makeText(context, i, duration).show();
    }

    public static void showToast(Context context, CharSequence msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static void hideKeyboard(Activity act) {
        act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    public static void setButtonTintBackground(Context ctx, Button mButtonSMS, int tintColor) {
        mButtonSMS.getBackground().setColorFilter(ContextCompat.getColor(ctx, tintColor), PorterDuff.Mode.MULTIPLY);
    }

//	VALIDATORS

    public static boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isDocumentValid(CharSequence name) {
        Pattern pattern = Pattern.compile("^[A-Za-z0-9]+");
        Matcher matcher = pattern.matcher(name);
        return matcher.matches();

    }

    public static boolean isNameValid(CharSequence name) {
        Pattern pattern = Pattern.compile("[^0-9()*^|\\.,:;\"&@$~+_-]+");
        Matcher matcher = pattern.matcher(name);
        return matcher.matches();

    }

    public static boolean isPhoneValid(CharSequence phone) {
        return android.util.Patterns.PHONE.matcher(phone).matches();
    }

    public static boolean isAddressValid(CharSequence address) {
        //TODO
        return true;
    }

    public static boolean isValidCarNumber(String carnum) {

        Pattern pattern = Pattern.compile("^([a-zA-Z]{2,3}\\d{3,4})$");
        Matcher matcher = pattern.matcher(carnum);
        return matcher.matches();

    }

    public static InputFilter[] getNameValidator() {

        InputFilter filter = new InputFilter() {

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (source instanceof SpannableStringBuilder) {
                    SpannableStringBuilder sourceAsSpannableBuilder = (SpannableStringBuilder) source;
                    for (int i = end - 1; i >= start; i--) {
                        char currentChar = source.charAt(i);
                        if (!Character.isLetter(currentChar) && !Character.isSpaceChar(currentChar)) {
                            sourceAsSpannableBuilder.delete(i, i + 1);
                        }
                    }
                    return source;
                } else {
                    StringBuilder filteredStringBuilder = new StringBuilder();
                    for (int i = 0; i < end; i++) {
                        char currentChar = source.charAt(i);
                        if (Character.isLetter(currentChar) || Character.isSpaceChar(currentChar)) {
                            filteredStringBuilder.append(currentChar);
                        }
                    }
                    return filteredStringBuilder.toString();
                }
            }
        };

        return new InputFilter[]{filter};

    }

    public static String sha1(String s) {

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        digest.reset();
        byte[] data = digest.digest(s.getBytes());
        return String.format("%0" + (data.length * 2) + "X", new BigInteger(1, data));

    }


    public static int getVersionCode(Context ctx) {
        try {
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode;

        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static String getVersionName(Context ctx) {
        try {
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;

        } catch (NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getAndroidDeviceId(Context ctx) {
        return Secure.getString(ctx.getContentResolver(), Secure.ANDROID_ID);
    }


    public static void share(Context ctx, String msg) {

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, msg);
        ctx.startActivity(Intent.createChooser(sharingIntent, "Share using"));

    }

    public static void cancelNotification(Context ctx, int notifyId) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancel(notifyId);
    }

    public static void cancelNotification(Context ctx, String tag) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancel(tag, 0);
    }

    public static void clearAllNotifications(Context ctx) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancelAll();
    }

    public static void sendEmail(Context ctx, String email, String subject) {

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        ctx.startActivity(Intent.createChooser(emailIntent, "Send email..."));

    }


    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static String loadDataFromAsset(Context ctx, String file) {

        String tContents = "";

        try {
            InputStream stream = ctx.getAssets().open(file);

            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            tContents = new String(buffer);
        } catch (IOException e) {
            if (DEBUG) Log.e(TAG, "loadDataFromAsset FAILED: " + e.getCause());
        }

        return tContents;

    }

    private void showLoader(ProgressDialog loader) {

        if (loader != null) {
            try {
                if (!loader.isShowing()) loader.show();
            } catch (Exception e) {
                if (DEBUG) Log.d(TAG, "LOADER Exception:");
                if (DEBUG) e.printStackTrace();
            }
        }

    }

    private void dismissLoader(ProgressDialog loader) {

        if (loader != null) {
            try {
                if (loader.isShowing()) loader.dismiss();
                loader = null;
            } catch (Exception e) {
                if (DEBUG) Log.d(TAG, "LOADER Exception:");
                if (DEBUG) e.printStackTrace();
            }

        }

    }

}
