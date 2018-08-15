package io.renderapps.balizinha.ui.profile;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.util.PhotoHelper;

import static io.renderapps.balizinha.util.Constants.REF_PLAYERS;

/**
 * Class displays a users profile
 */

public class UserProfileActivity extends AppCompatActivity {

    // properties
    public static String USER_ID = "uid";
    private String uid;

    private ImageView photo;
    private TextView defaultImage;

    private TextView name;
    private TextView city;
    private TextView description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // fetch bundle from intent
        uid = getIntent().getStringExtra(USER_ID);
        if (uid == null || uid.isEmpty()) {
            onBackPressed();
            return;
        }

        // toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle("Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // views
        photo = findViewById(R.id.user_photo);
        defaultImage = findViewById(R.id.default_img);
        name = findViewById(R.id.user_name);
        city = findViewById(R.id.user_city);
        description = findViewById(R.id.user_description);

        loadUserProfile();
        loadPhoto();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    public void loadUserProfile(){
        FirebaseDatabase.getInstance().getReference().child(REF_PLAYERS)
                .child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    final Player player = dataSnapshot.getValue(Player.class);
                    if (player == null) {
                        onBackPressed();
                        return;
                    }

                    if (player.getName() != null && !player.getName().isEmpty()) {
                        name.setText(player.getName());
                        defaultImage.setText(String.valueOf(player.getName().charAt(0)));
                    } else {
                        name.setText("-");
                        defaultImage.setText("-");
                    }

                    if (player.getCity() != null && !player.getCity().isEmpty())
                        city.setText(player.getCity());
                    else
                        city.setText("N/A");

                    if (player.getInfo() != null && !player.getInfo().isEmpty())
                        description.setText(player.getInfo());
                    else
                        description.setText("");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    void loadPhoto(){
        final Context mContext = this;
        FirebaseStorage.getInstance().getReference().child("images/player").child(uid).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                if (uri != null){
                    defaultImage.setVisibility(View.GONE);
                    photo.setVisibility(View.VISIBLE);
                    PhotoHelper.glideImage(mContext, photo, uri.toString(), R.drawable.ic_default_photo);
                } else {
                    setDefaultImage();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                setDefaultImage();
            }
        });
    }

    private void setDefaultImage(){
        photo.setVisibility(View.GONE);
        defaultImage.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
    }
}
