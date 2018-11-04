package io.renderapps.balizinha.util;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;


import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.renderapps.balizinha.ui.event.attendees.AttendeesActivity;
import io.renderapps.balizinha.ui.event.EventDetailsActivity;
import io.renderapps.balizinha.ui.event.organize.CreateEventActivity;
import io.renderapps.balizinha.ui.event.organize.LeagueSelectorActivity;
import io.renderapps.balizinha.ui.league.LeagueActivity;
import io.renderapps.balizinha.ui.main.MainActivity;
import io.renderapps.balizinha.ui.profile.SetupProfileActivity;
import io.renderapps.balizinha.ui.profile.UserProfileActivity;

import static io.renderapps.balizinha.util.Constants.PERMISSION_CAMERA;
import static io.renderapps.balizinha.util.Constants.PERMISSION_GALLERY;
import static io.renderapps.balizinha.util.Constants.REQUEST_CAMERA;
import static io.renderapps.balizinha.util.Constants.REQUEST_IMAGE;

/**
 * Helper class to handle photo capture or selection from gallery
 * Adds photo to storage and users db
 */

public class PhotoHelper {

    private AppCompatActivity activity;
    private Context context;

    public PhotoHelper(AppCompatActivity activity) {
        this.activity = activity;
        this.context = activity;
        showCaptureOptions();
    }

    // camera or photo gallery
    public void showCaptureOptions() {
        if (context != null) {
            final CharSequence[] items = {"Take photo", "Choose from gallery"};
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Add Profile Photo");
            builder.setItems(items, (dialog, item) -> {
                if (items[item].equals("Take photo"))
                    cameraIntent();
                else if (items[item].equals("Choose from gallery"))
                    galleryIntent();
            });
            builder.show();
        }
    }

    public void galleryIntent() {
        Intent intent = new Intent();
        if (checkGalleryPermission()) {
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);//
            activity.startActivityForResult(Intent.createChooser(intent, "Select photo"), REQUEST_IMAGE);
        }
    }

    public void cameraIntent() {
        Intent intent;
        if (checkCameraPermission()) {
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            activity.startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_CAMERA);
            return false;
        }
        return true;
    }

    private boolean checkGalleryPermission() {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_GALLERY);
            return false;
        }
        return true;
    }


    /*******************************************************
     * Update storage and db
     *******************************************************/

    public void onCaptureImageResult(Intent data) {
        // encode img for storage
        if (data.getExtras() != null) {
            final Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if (thumbnail != null) {
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                byte[] byteFormat = bytes.toByteArray();
                uploadPhoto(byteFormat);
            }
        }
    }

    public void onSelectFromGalleryResult(Intent data) {
        if (data == null)
            return;
        try {
            // encode img
            final Bitmap bm = MediaStore.Images.Media.getBitmap(context.getContentResolver(),
                    data.getData());
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

            byte[] byteFormat = bytes.toByteArray();
            uploadPhoto(byteFormat);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadPhoto(byte[] bytes){
        if (activity instanceof SetupProfileActivity)
            ((SetupProfileActivity)activity).onAddPhoto(bytes);

        if (activity instanceof CreateEventActivity)
            ((CreateEventActivity)activity).onAddPhoto(bytes);
    }

    /*******************************************************
     * Glide
     *******************************************************/


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

        if (context instanceof UserProfileActivity) {
            final UserProfileActivity activity = (UserProfileActivity) context;
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

        if (context instanceof LeagueActivity) {
            final LeagueActivity activity = (LeagueActivity) context;
            if (activity.isDestroyed() || activity.isFinishing()) {
                return false;
            }
        }

        if (context instanceof MainActivity) {
            final MainActivity activity = (MainActivity) context;
            return !activity.isDestroyed() && !activity.isFinishing();
        }

        if (context instanceof LeagueSelectorActivity) {
            final LeagueSelectorActivity activity = (LeagueSelectorActivity) context;
            return !activity.isDestroyed() && !activity.isFinishing();
        }

        if (context instanceof CreateEventActivity) {
            final CreateEventActivity activity = (CreateEventActivity) context;
            return !activity.isDestroyed() && !activity.isFinishing();
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



    public static void glideLeagueLogo(Context context, ImageView imageView, String url, int placeHolder){
        RequestOptions myOptions = new RequestOptions()
                .transforms(new CenterCrop(), new RoundedCorners(12))
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
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


    public static void glideHeader(Context context, ImageView imageView, String url, int placeHolder){
        RequestOptions myOptions = new RequestOptions()
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
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

    public static void glideImageResource(Context context, ImageView imageView, int drawable){
        RequestOptions myOptions = new RequestOptions()
                .fitCenter()
                .transform(new RoundedCorners(12))
                .diskCacheStrategy(DiskCacheStrategy.NONE);

        // load photo
        if (isValidContextForGlide(context)) {
            Glide.with(context)
                    .asBitmap()
                    .apply(myOptions)
                    .load(drawable)
                    .into(imageView);
        }
    }



    public static void clearImage(Context context, ImageView imageView, int defaultImg){
        if (isValidContextForGlide(context)){
            Glide.with(context).clear(imageView);
            imageView.setImageResource(defaultImg);
        }
    }


    public static void clearImage(Context context, ImageView imageView){
        if (isValidContextForGlide(context)){
            Glide.with(context).clear(imageView);
        }
    }

    public static byte[] getImageAsBytes(Bitmap bmp){
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, bao);
        return bao.toByteArray();
    }

}