package io.renderapps.balizinha.util;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.ui.profile.SetupProfileActivity;

/**
 * Created by joel on 7/23/18.
 */

public class DialogHelper {

    public static void showSuccessfulJoin(final AppCompatActivity activity){
        if (!isValidContext(activity)) return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(activity.getString(R.string.success_join_title));
                builder.setMessage(activity.getString(R.string.success_join));
                builder.setCancelable(false);
                builder.setNegativeButton(
                        "Close",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                builder.create().show();
            }
        });
    }

    public static void showPaymentRequiredDialog(final AppCompatActivity activity){
        if (!isValidContext(activity)) return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                builder.create().show();
            }
        });
    }

    public static void showAddNameDialog(final AppCompatActivity activity){
        if (!isValidContext(activity)) return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(activity.getString(R.string.add_name_title));
                builder.setMessage(activity.getString(R.string.add_name));
                builder.setCancelable(false);

                builder.setPositiveButton(
                        "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent profileIntent = new Intent(activity, SetupProfileActivity.class);
                                profileIntent.putExtra(SetupProfileActivity.EXTRA_PROFILE_UPDATE, true);
                                activity.startActivity(profileIntent);
                                activity.overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                            }
                        });

                builder.setNegativeButton(
                        "Not now",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                builder.create().show();
            }
        });
    }

    private static boolean isValidContext(AppCompatActivity activity){
        return !(activity.isDestroyed() || activity.isFinishing());
    }
}
