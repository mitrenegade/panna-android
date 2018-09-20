package io.renderapps.balizinha.service;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;

import io.renderapps.balizinha.BuildConfig;
import io.renderapps.balizinha.R;

public class ShareService {

    public static void showShareDialog(final AppCompatActivity activity, final String eventId){
        if (!isValidContext(activity)) return;
        if (eventId == null || eventId.isEmpty()) return;

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(true);
        builder.setTitle("Share");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1);
        arrayAdapter.add("Send to Contacts");

        builder.setAdapter(arrayAdapter, (dialog, which) -> {
            ShareService.shareEvent(activity, eventId);
            dialog.dismiss();
        });

        builder.create().show();
    }

    private static void shareEvent(final AppCompatActivity activity, @NonNull final String eventId){
        final String pannaLink = BuildConfig.SHARE_LINK;
        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority(pannaLink)
                .appendPath("events")
                .appendPath(eventId);

        String eventUrl = builder.build().toString();
        FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse(eventUrl))
                .setDynamicLinkDomain(pannaLink)
                .setAndroidParameters(
                        new DynamicLink.AndroidParameters.Builder("io.renderapps.balizinha")
                                .build())
                .setIosParameters(
                        new DynamicLink.IosParameters.Builder("io.renderapps.balizinha")
                                .setAppStoreId("1198807198")
                                .build())
                .buildShortDynamicLink()
                .addOnCompleteListener(task -> {
                    if (activity.isDestroyed() || activity.isFinishing()) return;
                    if (task.isSuccessful()){
                        final String message =
                                activity.getString(R.string.invitation_message).concat(" ")
                                        .concat(task.getResult().getShortLink().toString());
                        final Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
                        sendIntent.setType("text/plain");
                        activity.startActivity(sendIntent);
                    } else {
                        Toast.makeText(activity, "Unable to create shareable link for event.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private static boolean isValidContext(AppCompatActivity activity){
        return !(activity.isDestroyed() || activity.isFinishing());
    }
}
