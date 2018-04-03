package io.renderapps.balizinha.util;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DatabaseReference;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.activity.AttendeesActivity;
import io.renderapps.balizinha.activity.EventDetailsActivity;
import io.renderapps.balizinha.activity.MainActivity;
import io.renderapps.balizinha.activity.SetupProfileActivity;

/**
 * General purpose helpers
 */

public class GeneralHelpers {

    public static void syncEndpoints(DatabaseReference ref, String uid){
        ref.child(Constants.REF_PLAYERS).child(uid).keepSynced(true);
        ref.child("userEvents").child(uid).keepSynced(true);
        ref.child("stripe_customers").child(uid).keepSynced(true);
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public static void showSuccessfulJoin(Context mContext){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.success_join_title));
        builder.setMessage(mContext.getString(R.string.success_join));
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

    public static void showPaymentRequiredDialog(Context mContext){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.no_payment_method));
        builder.setCancelable(false);

        // Get the layout inflater
        LayoutInflater inflater = null;
        Activity activity = null;

        if (mContext instanceof MainActivity) {
            activity = ((MainActivity)mContext);
            inflater = activity.getLayoutInflater();
        } else if (mContext instanceof EventDetailsActivity){
            activity = ((EventDetailsActivity)mContext);
            inflater = activity.getLayoutInflater();
        }

        if (inflater == null)
            return;
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

        if (!activity.isDestroyed() && !activity.isFinishing())
            builder.create().show();
    }

    public static void showAddNameDialog(Context mContext){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getString(R.string.add_name_title));
        builder.setMessage(mContext.getString(R.string.add_name));
        builder.setCancelable(false);

        Activity activity = null;
        if (mContext instanceof MainActivity)
            activity = ((MainActivity)mContext);
        if (mContext instanceof EventDetailsActivity)
            activity = ((EventDetailsActivity)mContext);
        if (activity == null)
            return;

        final Activity finalActivity = activity;
        builder.setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent profileIntent = new Intent(finalActivity, SetupProfileActivity.class);
                        profileIntent.putExtra(SetupProfileActivity.EXTRA_PROFILE_UPDATE, true);
                        finalActivity.startActivity(profileIntent);
                        finalActivity.overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
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



    private static boolean isValidContextForGlide(final Context context) {
        if (context == null) {
            return false;
        }
        if (context instanceof SetupProfileActivity) {
            final SetupProfileActivity activity = (SetupProfileActivity) context;
            if (activity.isDestroyed() || activity.isFinishing()) {
                return false;
            }
        }

        if (context instanceof AttendeesActivity) {
            final AttendeesActivity activity = (AttendeesActivity) context;
            if (activity.isDestroyed() || activity.isFinishing()) {
                return false;
            }
        }

        if (context instanceof EventDetailsActivity) {
            final EventDetailsActivity activity = (EventDetailsActivity) context;
            if (activity.isDestroyed() || activity.isFinishing()) {
                return false;
            }
        }

        if (context instanceof MainActivity) {
            final MainActivity activity = (MainActivity) context;
            if (activity.isDestroyed() || activity.isFinishing()) {
                return false;
            }
        }
        return true;
    }

    public static void glideImageWithBytes(Context context, ImageView imageView, byte[] bytes,
                                           int placeHolder){
        RequestOptions myOptions = new RequestOptions()
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(new CircleTransform(context))
                .placeholder(placeHolder);

        // load photo
        if (isValidContextForGlide(context)) {
            Glide.with(context)
                    .asBitmap()
                    .apply(myOptions)
                    .load(bytes)
                    .into(imageView);
        }
    }

    public static void glideImage(Context context, ImageView imageView, String url, int placeHolder){
        RequestOptions myOptions = new RequestOptions()
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(new CircleTransform(context))
                .placeholder(placeHolder);

        // load photo
        if (isValidContextForGlide(context)) {
            Glide.with(context)
                    .asBitmap()
                    .apply(myOptions)
                    .load(url)
                    .into(imageView);
        }
    }
}
