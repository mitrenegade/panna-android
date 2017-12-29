package io.renderapps.balizinha.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.util.CircleTransform;

public class UserProfileActivity extends AppCompatActivity {

    // properties
    public static String USER_ID = "uid";
    private String uid;

    // views
    private FrameLayout rootView;
    private ImageView background;
    private ImageView photo;
    private TextView name;
    private TextView city;
    private TextView description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // fetch bundle from intent
        uid = getIntent().getStringExtra(USER_ID);

        // toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // views
        rootView = findViewById(R.id.rootView);
        background = findViewById(R.id.background);
        photo = findViewById(R.id.user_photo);
        name = findViewById(R.id.user_name);
        city = findViewById(R.id.user_city);
        description = findViewById(R.id.user_description);

        loadUserProfile();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    public void loadUserProfile(){
        FirebaseDatabase.getInstance().getReference().child("players")
                .child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                    final Player player = dataSnapshot.getValue(Player.class);

                    if (player.getName() != null && !player.getName().isEmpty())
                        name.setText(player.getName());
                    else
                        name.setText("-");

                    if (player.getCity() != null && !player.getCity().isEmpty())
                        city.setText(player.getCity());
                    else
                        city.setText("N/A");

                    if (player.getInfo() != null && !player.getInfo().isEmpty())
                        description.setText(player.getInfo());
                    else
                        description.setText("");

                    // photo
                    if (player.getPhotoUrl() != null && !player.getPhotoUrl().isEmpty())
                        loadPhoto(player.getPhotoUrl());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void loadPhoto(String url){
        RequestOptions myOptions = new RequestOptions()
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transform(new CircleTransform(this))
                .placeholder(R.drawable.ic_default_photo);

        // load photo
        Glide.with(this)
                .asBitmap()
                .apply(myOptions)
                .load(url)
                .into(photo);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
    }
}
