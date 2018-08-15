package io.renderapps.balizinha.ui.profile;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.service.FirebaseService;
import io.renderapps.balizinha.ui.main.MainActivity;
import io.renderapps.balizinha.util.CircleTransform;
import io.renderapps.balizinha.util.PhotoHelper;

import static io.renderapps.balizinha.util.Constants.PERMISSION_CAMERA;
import static io.renderapps.balizinha.util.Constants.PERMISSION_GALLERY;
import static io.renderapps.balizinha.util.Constants.REF_PLAYERS;
import static io.renderapps.balizinha.util.Constants.REQUEST_CAMERA;
import static io.renderapps.balizinha.util.Constants.REQUEST_IMAGE;

/**
 * Class creates user profile or updates
 */
public class SetupProfileActivity extends AppCompatActivity implements View.OnClickListener {

    private final static int MEGABYTE = 1000000;

    // views
    @BindView(R.id.user_photo) ImageView profilePhoto;
    @BindView(R.id.user_name) EditText nameField;
    @BindView(R.id.user_city) EditText locationField;
    @BindView(R.id.user_description) EditText descriptionField;

    // on-click
    @OnClick(R.id.user_photo) void onUploadPhoto(){
        uploadPhoto();
    }

    // optional views
    private ProgressBar updateProgressbar;
    private FrameLayout progressView;
    private CircularProgressButton saveButton;

    // properties
    public static String EXTRA_PROFILE_UPDATE = "update_account"; // indicates profile update

    private boolean isUpdating = false;
    private boolean didSetPhoto = false;
    private boolean allowBackNavigation = true;

    private Player player;
    private byte[] mBytes;
    private PhotoHelper photoHelper;

    // firebase
    private FirebaseUser firebaseUser;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // bundle
        Bundle extras = getIntent().getExtras();
        if (extras != null)
            isUpdating = extras.getBoolean(EXTRA_PROFILE_UPDATE);

        // layout based on setup or update
        int view = (isUpdating) ? R.layout.activity_update_profile : R.layout.activity_setup_profile;
        setContentView(view);
        ButterKnife.bind(this);

        // firebase
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        databaseRef.child(REF_PLAYERS).child(firebaseUser.getUid()).keepSynced(true);

        if (isUpdating)
            setupUpdateProfile();
        else
            setupNewProfile();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_account, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.save:
                if (!updateProgressbar.isShown())
                    updateAndSave();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.save_button){
            if (progressView != null) {
                if (!progressView.isShown()) {
                    updateAndSave();
                    return;
                }
            }
            updateAndSave();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (saveButton != null)
            saveButton.dispose();
    }

    @Override
    public void onBackPressed() {
        if (!isUpdating) return;
        // prevent back navigation when profile is updating
        if (!allowBackNavigation) return;

        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
    }

    /**************************************************************************************************
     * Setup Profile
     *************************************************************************************************/

    void setupUpdateProfile(){
        updateProgressbar = findViewById(R.id.update_progressbar);
        progressView = findViewById(R.id.progress_view);

        Toolbar toolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.profile);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // hide keyboard
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        fetchAccount();
        loadPhoto();
    }

    void setupNewProfile(){
        saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(this);
    }

    /**************************************************************************************************
     * Action handlers
     *************************************************************************************************/

    private void updateAndSave(){
        enableEditing(false);
        if (!validForm()){
            enableEditing(true);
            return;
        }

        if (isUpdating){
            updateProgressbar.setVisibility(View.VISIBLE);
        } else {
            if (!didSetPhoto) {
                showDialog();
                return;
            }
            saveButton.startAnimation();
        }

        updateAccount();
    }

    public boolean validForm() {
        String name = nameField.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            nameField.setError("Required");
            return false;
        } else
            nameField.setError(null);
        return true;
    }

    public void enableEditing(boolean isEnabled){
        nameField.setEnabled(isEnabled);
        locationField.setEnabled(isEnabled);
        descriptionField.setEnabled(isEnabled);
        profilePhoto.setEnabled(isEnabled);
        allowBackNavigation = isEnabled;
    }

    private Map<String, Object> createChildUpdates(){
        Map<String, Object> childUpdates = new HashMap<>();
        final String uid = firebaseUser.getUid();
        String name = nameField.getText().toString().trim();
        String city = locationField.getText().toString().trim();
        String info = descriptionField.getText().toString().trim();

        // add children
        childUpdates.put("/players/" + uid + "/name", name);
        if (!city.isEmpty())
            childUpdates.put("/players/" + uid + "/city", city);
        if (!info.isEmpty())
            childUpdates.put("/players/" + uid + "/info", info);

        return childUpdates;
    }

    public void launchMainActivity(){
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_check);
        saveButton.doneLoadingAnimation(android.R.color.white, bm);

        final Context context = this;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(context, MainActivity.class));
                finish();
            }
        }, 1000);
    }

    /******************************************************************************
     * Firebase
     *****************************************************************************/

    public void updateAccount(){
        final Context mContext = this;

        databaseRef.updateChildren(createChildUpdates()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    if (mBytes != null && mBytes.length > 0)
                        FirebaseService.startActionUploadPhoto(mContext, firebaseUser.getUid(),
                                mBytes);

                    if (!isDestroyed() && !isFinishing()){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isUpdating) {
                                    Toast.makeText(mContext, "Profile updated", Toast.LENGTH_SHORT).show();
                                    allowBackNavigation = true;
                                    onBackPressed();
                                } else {
                                    launchMainActivity();
                                }
                            }
                        });
                    }
                } else {
                    Toast.makeText(mContext, getString(R.string.db_error), Toast.LENGTH_LONG).show();
                    enableEditing(true);

                    if (!isDestroyed() && !isFinishing()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isUpdating) {
                                    updateProgressbar.setVisibility(View.GONE);
                                } else
                                    saveButton.revertAnimation();
                            }
                        });
                    }
                }
            }
        });
    }


    public void fetchAccount(){
        databaseRef.child(REF_PLAYERS).child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    player = dataSnapshot.getValue(Player.class);
                    if (player == null)
                        onBackPressed();

                    if (!isDestroyed() && !isFinishing()){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (player.getName() != null) {
                                    nameField.setText(player.getName());
                                    nameField.setSelection(nameField.getText().length());
                                }
                                if (player.getInfo() != null)
                                    descriptionField.setText(player.getInfo());
                                if (player.getCity() != null)
                                    locationField.setText(player.getCity());

                                // hide progress view
                                progressView.setVisibility(View.GONE);
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    void loadPhoto(){
        final Context mContext = this;
        FirebaseStorage.getInstance().getReference()
                .child("images/player").child(firebaseUser.getUid()).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                if (uri != null){
                    PhotoHelper.glideImage(mContext, profilePhoto, uri.toString(), R.drawable.ic_default_photo);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
    }

    /******************************************************************************
     * Photo handlers
     *****************************************************************************/

    void uploadPhoto(){
        if (photoHelper != null)
            photoHelper.showCaptureOptions();
        else
            photoHelper = new PhotoHelper(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode){
                case PERMISSION_CAMERA:
                    photoHelper.cameraIntent();
                    break;
                case PERMISSION_GALLERY:
                    photoHelper.galleryIntent();
                    break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK){
            switch (requestCode){
                case REQUEST_IMAGE:
                    photoHelper.onSelectFromGalleryResult(data);
                    break;
                case REQUEST_CAMERA:
                    photoHelper.onCaptureImageResult(data);
                    break;
            }
        }
    }

    public void onAddPhoto(byte[] bytes){
        final Context mContext = this;

        // load photo, resize under 1MB if possible
        RequestOptions myOptions = new RequestOptions()
                .centerInside()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .override(500, 500);

        if (!isDestroyed() && !isFinishing()) {
            Glide.with(this)
                    .asBitmap()
                    .apply(myOptions)
                    .load(bytes)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                            if (resource.getByteCount() >= MEGABYTE){
                                Toast.makeText(mContext, "Image size is too large, please upload a smaller one.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                RequestOptions uploadOptions = new RequestOptions()
                                        .fitCenter()
                                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                        .transform(new CircleTransform(mContext));

                                Glide.with(mContext)
                                        .load(resource)
                                        .apply(uploadOptions)
                                        .into(profilePhoto);


                                byte[] res = getImageAsBytes(resource);
                                if (res != null){
                                    mBytes = res;
                                    didSetPhoto = true;
                                }
                            }
                        }
                    });
        }
    }

    byte[] getImageAsBytes(Bitmap bmp){
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, bao);
        return bao.toByteArray();
    }

    public void showDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.set_photo_msg));
        builder.setCancelable(false);

        builder.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        enableEditing(true);
                        uploadPhoto();
                    }
                });

        builder.setNegativeButton(
                "Not now",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        didSetPhoto = true;
                        updateAndSave();
                    }
                });

        builder.create().show();
    }
}
