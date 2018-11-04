package io.renderapps.balizinha.service;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;

import io.renderapps.balizinha.BuildConfig;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Event;
import io.renderapps.balizinha.ui.event.EventDetailsActivity;
import io.renderapps.balizinha.ui.league.LeagueActivity;

public class ShareService {

    public static void showShareDialog(final AppCompatActivity activity, String shareUrl){
        if (!isValidContext(activity)) return;

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(true);
        builder.setTitle("Share");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1);
        arrayAdapter.add("Send to Contacts");

        builder.setAdapter(arrayAdapter, (dialog, which) -> {
            shareWithContacts(activity, shareUrl);
            dialog.dismiss();
        });

        builder.create().show();
    }

    private static void shareWithContacts(AppCompatActivity activity, String url) {
        if (!isValidContext(activity)) return;

        final String message = (activity instanceof EventDetailsActivity) ?
                activity.getString(R.string.invitation_message_event).concat(" ").concat(url) :
                activity.getString(R.string.invitation_message_league).concat(" ").concat(url);

        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setData(Uri.parse("smsto:"));
            intent.putExtra(Intent.EXTRA_TEXT, message);
            intent.setType("text/plain");
            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(intent);
            }
        } catch (ActivityNotFoundException e){
            e.printStackTrace();
        }
    }

    public static void parseUrl(AppCompatActivity activity, Task<PendingDynamicLinkData> task){
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getLink() != null){
            final Uri url = task.getResult().getLink();

            final String type = url.getQueryParameter("type");
            final String id = url.getQueryParameter("id");
            switch (type){
                case "events":
                    Intent eventIntent = new Intent(activity, EventDetailsActivity.class);
                    eventIntent.putExtra(EventDetailsActivity.EVENT_ID, id);
                    if (isValidContext(activity)) activity.startActivity(eventIntent);
                    activity.finish();
                    break;
                case "leagues":
                    Intent leagueIntent = new Intent(activity, LeagueActivity.class);
                    leagueIntent.putExtra(LeagueActivity.EXTRA_LEAGUE_ID, id);
                    if (isValidContext(activity)) activity.startActivity(leagueIntent);
                    activity.finish();
                    break;
            }
        }
    }

    private static boolean isValidContext(AppCompatActivity activity){
        return !(activity.isDestroyed() || activity.isFinishing());
    }
}
