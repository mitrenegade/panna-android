package io.renderapps.balizinha.util;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.reactivex.annotations.Nullable;
import io.renderapps.balizinha.AppController;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.ui.login.LoginActivity;
import io.renderapps.balizinha.ui.main.MainActivity;
import io.renderapps.balizinha.ui.profile.SetupProfileActivity;

import static io.renderapps.balizinha.BuildConfig.APPLICATION_ID;
import static io.renderapps.balizinha.util.CommonUtils.isValidContext;

/**
 * Created by joel on 7/23/18.
 */

public class DialogHelper {


    @Nullable
    public static AlertDialog createJoinDialog(AppCompatActivity activity){
        if (!isValidContext(activity)) return null;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);

        View v = LayoutInflater.from(activity).inflate(R.layout.dialog_progress, null);
        ((TextView)v.findViewById(R.id.progress_text)).setText("Joining game...");
        builder.setView(v);

        return builder.create();
    }

    public static void showLoginDialog(final AppCompatActivity activity){
        if (!isValidContext(activity)) return;

        new AlertDialog.Builder(activity)
                .setTitle("Log In")
                .setMessage("You must be logged in to view an event.")
                .setPositiveButton("Log In", (dialog, i) -> {
                    dialog.dismiss();
                    activity.startActivity(new Intent(activity, LoginActivity.class));
                    activity.finish();
                })
                .setNegativeButton("Cancel", (dialog, i) -> {
                    activity.finish();
                    dialog.dismiss();
                })
                .create()
                .show();

    }

    public static void showDeviceSettingsDialog(final AppCompatActivity activity){
        if (!isValidContext(activity)) return;

        activity.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Permission denied");
            builder.setMessage(activity.getString(R.string.permission_settings));
            builder.setPositiveButton(
                    "Settings", (dialog, id) -> {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", APPLICATION_ID, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                    });
            builder.setNegativeButton(
                    "Cancel",
                    (dialog, id) -> dialog.cancel());

            builder.create().show();
        });
    }

    public static void showSuccessfulJoin(final AppCompatActivity activity){
        if (!isValidContext(activity)) return;

        activity.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(activity.getString(R.string.success_join_title));
            builder.setMessage(activity.getString(R.string.success_join));
            builder.setCancelable(false);
            builder.setNegativeButton(
                    "Close",
                    (dialog, id) -> dialog.cancel());

            builder.create().show();
        });
    }

    public static void showPaymentMethodRequiredDialog(final AppCompatActivity activity){
        if (!isValidContext(activity)) return;

        activity.runOnUiThread(() -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(activity.getString(R.string.no_payment_method));
            builder.setCancelable(false);

            // Get the layout inflater
            LayoutInflater inflater = activity.getLayoutInflater();
            View view = inflater.inflate(R.layout.dialog_layout_payment, null);
            ((TextView)view.findViewById(R.id.payment_details)).setText(R.string.event_fee);
            ((ImageView)view.findViewById(R.id.payment)).setImageResource(R.drawable.ic_credit_card);
            builder.setView(view)
                    // Add action buttons
                    .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
                    .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.cancel());

            builder.create().show();
        });
    }

    public static void showAddNameDialog(final AppCompatActivity activity){
        if (!isValidContext(activity)) return;

        activity.runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(activity.getString(R.string.add_name_title));
            builder.setMessage(activity.getString(R.string.add_name));
            builder.setCancelable(false);

            builder.setPositiveButton(
                    "OK",
                    (dialog, id) -> {
                        Intent profileIntent = new Intent(activity, SetupProfileActivity.class);
                        profileIntent.putExtra(SetupProfileActivity.EXTRA_PROFILE_UPDATE, true);
                        activity.startActivity(profileIntent);
                        activity.overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                    });

            builder.setNegativeButton(
                    "Not now",
                    (dialog, id) -> dialog.cancel());

            builder.create().show();
        });
    }

    public static Dialog showProgressDialog(final AppCompatActivity activity, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);

        View v = activity.getLayoutInflater().inflate(R.layout.dialog_progress, null);
        builder.setView(v);
        ((TextView) v.findViewById(R.id.progress_text)).setText(message);
        return builder.create();
    }

    public static void showUpdateAvailable(final MainActivity activity, String updateVersion){
        if (!isValidContext(activity)) return;

        final boolean showUpdate = ((AppController)activity.getApplication()).getDataManager().getShowPlaystoreUpdate();
        final long elapsedTime = ((AppController)activity.getApplication()).getDataManager().getUpdateElapsedTime();

        // user has checked to never see updates again
        if (!showUpdate)
            return;

        // show updates in only 12hr intervals
        long differenceInMillis = System.currentTimeMillis() - elapsedTime;
        long differenceInHours = TimeUnit.MILLISECONDS.toHours(differenceInMillis);
        if (differenceInHours < 12)
            return;

        // no need to show update if user has latest version
        if (updateVersion.equals(Constants.APP_VERSION))
            return;

        final String updateMsg = "There is a newer version " + updateVersion
                + " of Balizinha available in the Play Store.";

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle("Update Available");
        builder.setCancelable(false);

        // set view
        View v = activity.getLayoutInflater().inflate(R.layout.dialog_app_update, null);
        builder.setView(v);
        final CheckBox hideUpdates = v.findViewById(R.id.update_checkbox);
        ((TextView)v.findViewById(R.id.update_message)).setText(updateMsg);
        // response
        builder.setPositiveButton(
                "Open in Play Store",
                (dialog, id) -> {
                    openPlayStore(activity);
                    ((AppController)activity.getApplication()).getDataManager().setUpdateElapsedTime(new Date().getTime());
                    dialog.dismiss();
                });

        builder.setNegativeButton(
                "Later",
                (dialog, id) -> {
                    dialog.cancel();
                    if (hideUpdates.isChecked()){
                        // write to user preferences
                        ((AppController)activity.getApplication()).getDataManager().setShowPlaystoreUpdate(false);
                        dialog.dismiss();
                    } else {
                        ((AppController)activity.getApplication()).getDataManager().setUpdateElapsedTime(new Date().getTime());
                        dialog.dismiss();
                    }
                });

        if (activity.isDestroyed() || activity.isFinishing())
            return;
        builder.create().show();
    }

    private static void openPlayStore(MainActivity activity){
        final String appPackageName = activity.getPackageName();
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException e) {
            // open in default browser if play store app not installed
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }
}
