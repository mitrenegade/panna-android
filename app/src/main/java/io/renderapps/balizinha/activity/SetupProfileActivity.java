package io.renderapps.balizinha.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.service.FirebaseService;
import io.renderapps.balizinha.util.CircleTransform;
import io.renderapps.balizinha.util.GeneralHelpers;

import static io.renderapps.balizinha.util.Constants.REF_PLAYERS;

/**
 * Class creates user profile or updates
 */
public class SetupProfileActivity extends AppCompatActivity implements View.OnClickListener {

    // views
    private ImageView profilePhoto;
    private EditText nameField;
    private EditText locationField;
    private EditText descriptionField;
    private CircularProgressButton saveButton;

    // update views
    private ProgressBar updateProgressbar;
    private FrameLayout progressView;

    // properties
    public static String EXTRA_PROFILE_UPDATE = "update_account"; // indicates profile update
    private int REQUEST_CAMERA = 100, SELECT_FILE = 101;
    private int PERMISSION_CAMERA = 102, PERMISSION_GALLERY = 103;

    private Context mContext;
    private boolean isUpdating = false;
    private boolean didSetuPhoto = false;
    private Player player;
    private byte[] mBytes;

    // firebase
    private FirebaseUser firebaseUser;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        // bundle
        Bundle extras = getIntent().getExtras();
        if (extras != null)
            isUpdating = extras.getBoolean(EXTRA_PROFILE_UPDATE);

        // layout based on setup or update
        if (isUpdating)
            setContentView(R.layout.activity_update_profile);
        else
            setContentView(R.layout.activity_setup_profile);

        // firebase
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        databaseRef.child(REF_PLAYERS).child(firebaseUser.getUid()).keepSynced(true);

        // views
        profilePhoto = findViewById(R.id.user_photo);
        profilePhoto.setOnClickListener(this);
        nameField = findViewById(R.id.user_name);
        locationField = findViewById(R.id.user_city);
        descriptionField = findViewById(R.id.user_description);

        if (isUpdating) {
            updateProgressbar = findViewById(R.id.update_progressbar);
            progressView = findViewById(R.id.progress_view);
            Toolbar toolbar = findViewById(R.id.profile_toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle(R.string.profile);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            saveButton = findViewById(R.id.save_button);
            saveButton.setOnClickListener(this);
        }

        if (isUpdating) {
            // hide keyboard
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            fetchAccount();
        }
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
                onUpdate();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.user_photo:
                uploadPhoto();
                break;
            case R.id.save_button:
                if (progressView != null) {
                    if (!progressView.isShown())
                        onSave();
                } else
                    onSave();
                break;
        }
    }

    public void onSave(){
        enableEditing(false);
        if (!validForm()){
            enableEditing(true);
        } else {
            if (!didSetuPhoto) {
                showDialog();
            } else {
                saveButton.startAnimation();
                databaseRef.updateChildren(createChildUpdates())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            if (mBytes != null && mBytes.length > 0)
                                FirebaseService.startActionUploadPhoto(mContext, firebaseUser.getUid(),
                                        mBytes);
                            launchMainActivity();
                        } else {
                            Toast.makeText(mContext, getString(R.string.db_error), Toast.LENGTH_LONG).show();
                            enableEditing(true);
                            saveButton.revertAnimation();
                        }
                    }
                });
            }
        }
    }

    public void onUpdate(){
        enableEditing(false);
        if (!validForm())
            enableEditing(true);
        else {
            updateProgressbar.setVisibility(View.VISIBLE);
            databaseRef.updateChildren(createChildUpdates())
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                if (mBytes != null && mBytes.length > 0)
                                    FirebaseService.startActionUploadPhoto(mContext, firebaseUser.getUid(),
                                            mBytes);
                                Toast.makeText(mContext, "Profile updated", Toast.LENGTH_LONG).show();
                                onBackPressed();
                            } else {
                                Toast.makeText(mContext, getString(R.string.db_error), Toast.LENGTH_LONG).show();
                                enableEditing(true);
                                updateProgressbar.setVisibility(View.GONE);
                            }
                        }
                    });
        }
    }

    private Map<String, Object> createChildUpdates(){
        Map<String, Object> childUpdates = new HashMap<>();
        final String uid = firebaseUser.getUid();
        String name = nameField.getText().toString().trim();
        String city = locationField.getText().toString().trim();
        String info = descriptionField.getText().toString().trim();

        // add children
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name).build();
        firebaseUser.updateProfile(profileUpdates);
        childUpdates.put("/players/" + uid + "/name", name);
        if (!city.isEmpty())
            childUpdates.put("/players/" + uid + "/city", city);
        if (!info.isEmpty())
            childUpdates.put("/players/" + uid + "/info", info);

        return childUpdates;
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
    }

    public void launchMainActivity(){
        Bitmap check = GeneralHelpers.getBitmapFromVectorDrawable(mContext, R.drawable.ic_check);
        saveButton.doneLoadingAnimation(android.R.color.white, check);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(getBaseContext(), MainActivity.class));
                finish();
            }
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (saveButton != null)
            saveButton.dispose();
    }

    /******************************************************************************
     * Firebase
     *****************************************************************************/

    public void fetchAccount(){
        databaseRef.child(REF_PLAYERS).child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    player = dataSnapshot.getValue(Player.class);
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

                    // load picture
                    if (player.getPhotoUrl() != null)
                        GeneralHelpers.glideImage(mContext, profilePhoto, player.getPhotoUrl(),
                                R.drawable.ic_default_photo);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    /******************************************************************************
     * Add photo handlers
     *****************************************************************************/

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
                        didSetuPhoto = true;
                        onSave();
                    }
                });

        builder.create().show();
    }

    public void uploadPhoto() {
        if (mContext != null) {
            final CharSequence[] items = {"Take photo", "Choose from gallery"};
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("Add Profile Photo");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    if (items[item].equals("Take photo")) {
                        cameraIntent();
                    } else if (items[item].equals("Choose from gallery")) {
                        galleryIntent();
                    }
                }
            });
            builder.show();
        }
    }

    private void galleryIntent() {
        if (checkGalleryPermission()) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);//
            startActivityForResult(Intent.createChooser(intent, "Select photo"), SELECT_FILE);
        }
    }

    private void cameraIntent() {
        if (checkCameraPermission()) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_CAMERA);
            return false;
        }
        return true;
    }

    private boolean checkGalleryPermission() {
        if (ContextCompat.checkSelfPermission(mContext,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_GALLERY);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_CAMERA) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                cameraIntent();
            }
        } else if (requestCode == PERMISSION_GALLERY){
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                galleryIntent();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data);
            else if (requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data);
        } else {
            Toast.makeText(mContext, "Cancelled", Toast.LENGTH_SHORT).show();
        }
    }

    private void onSelectFromGalleryResult(Intent data) {
        if (data != null) {
            final Bitmap bm;
            try {
                bm = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                byte[] byteFormat = bytes.toByteArray();
                taskComplete(byteFormat);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to upload photo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onCaptureImageResult(Intent data) {
        if (data.getExtras() != null) {
            final Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            if (thumbnail != null) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                byte[] byteFormat = bytes.toByteArray();
                taskComplete(byteFormat);
                return;
            }
        }
        Toast.makeText(this, "Failed to upload photo", Toast.LENGTH_SHORT).show();
    }

    void taskComplete(byte[] bytes){
        mBytes = bytes;
        didSetuPhoto = true;
        int drawable = R.drawable.ic_add_photo;
        if (isUpdating)
            if (player != null && player.getPhotoUrl() != null && !player.getPhotoUrl().isEmpty())
                drawable = R.drawable.ic_default_photo;

        GeneralHelpers.glideImageWithBytes(this, profilePhoto, mBytes,
                drawable);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
    }
}
