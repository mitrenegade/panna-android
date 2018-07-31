package io.renderapps.balizinha.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.adapter.AttendeesAdapter;
import io.renderapps.balizinha.model.Player;

public class AttendeesActivity extends AppCompatActivity {
    public static final String EXTRA_PLAYERS = "players";

    private RecyclerView mRecycler;
    private ArrayList<Player> playerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendees);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Attending Players");


        // get players from bundle
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey(EXTRA_PLAYERS))
            playerList = bundle.getParcelableArrayList(EXTRA_PLAYERS);

        if (playerList == null) {
            onBackPressed();
            return;
        }

        // views
        mRecycler = findViewById(R.id.recycler);
        setupRecycler();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
    }

    void setupRecycler(){
//        mRecycler.hasFixedSize();
        mRecycler.setLayoutManager(new LinearLayoutManager(this));

        Collections.sort(playerList, new Comparator<Player>(){
            public int compare(Player p1, Player p2) {
                if (p1.getName() == null)
                    p1.setName("");
                if (p2.getName() == null)
                    p2.setName("");
                return p1.getName().compareTo(p2.getName());
            }
        });

        // adapter
        AttendeesAdapter adapter = new AttendeesAdapter(this, playerList);
        mRecycler.setAdapter(adapter);
    }
}
