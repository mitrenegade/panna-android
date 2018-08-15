package io.renderapps.balizinha.ui.league;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.luizgrp.sectionedrecyclerviewadapter.Section;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.League;
import io.renderapps.balizinha.service.CloudService;
import io.renderapps.balizinha.ui.account.AccountActivity;

import static io.renderapps.balizinha.util.Constants.REF_LEAGUES;


public class LeagueFragment extends Fragment {

    private final static int USER_LEAGUES = 0;
    private final static int LEAGUES = 1;

    private final static String USER_LEAGUES_TAG = "user_league_tag";
    private final static String LEAGUES_TAG = "leagues_tag";

    private ArrayList<League> userLeagues;
    private ArrayList<String> userLeagueIds;
    private ArrayList<League> otherLeagues;

    // sections
    SectionedRecyclerViewAdapter sectionAdapter;
    LeagueSection userLeagueSection;
    LeagueSection leagueSection;

    @BindView(R.id.leagues_recycler) RecyclerView leaguesRecycler;

    public LeagueFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userLeagues = new ArrayList<>();
        userLeagueIds = new ArrayList<>();
        otherLeagues = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.fragment_league, container, false);
        ButterKnife.bind(this, root);
        setHasOptionsMenu(true);

        // toolbar
        Toolbar toolbar = root.findViewById(R.id.league_toolbar);
        toolbar.setTitle(R.string.title_league);
        if (getActivity() != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

            final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null){
                actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        setupRecyclers();

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        userLeagueIds.clear();
        userLeagues.clear();
        otherLeagues.clear();

        userLeagueSection.setState(Section.State.LOADING);
        leagueSection.setState(Section.State.LOADING);

        if (sectionAdapter != null)
            sectionAdapter.notifyDataSetChanged();

        fetchUserLeagues();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            launchAccount();
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclers(){
        if (getActivity() == null) return;

        sectionAdapter = new SectionedRecyclerViewAdapter();
        userLeagueSection = new LeagueSection(getActivity(), sectionAdapter, "Your Leagues", userLeagues);
        leagueSection = new LeagueSection(getActivity(), sectionAdapter, "Other Leagues", otherLeagues);

        userLeagueSection.setHasFooter(false);
        leagueSection.setHasFooter(false);

        sectionAdapter.addSection(USER_LEAGUES_TAG, userLeagueSection);
        sectionAdapter.addSection(LEAGUES_TAG, leagueSection);

        leaguesRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        leaguesRecycler.setAdapter(sectionAdapter);
    }

    private void updateAdapter(final int league, final int index){
        if (getActivity() == null || getActivity().isDestroyed() || getActivity().isFinishing())
            return;

        if (isAdded()){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (league == USER_LEAGUES){
                        sectionAdapter.notifyItemInsertedInSection(USER_LEAGUES_TAG, index);
                    } else {
                        sectionAdapter.notifyItemInsertedInSection(LEAGUES_TAG, index);
                    }
                }
            });
        }
    }

    void launchAccount(){
        if (getActivity() != null && !getActivity().isDestroyed() && !getActivity().isFinishing()) {
            getActivity().startActivity(new Intent(getActivity(), AccountActivity.class));
        }
    }

    void setSectionLoaded(){
        if (getActivity() != null && !getActivity().isDestroyed() && !getActivity().isFinishing()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    userLeagueSection.setState(Section.State.LOADED);
                    leagueSection.setState(Section.State.LOADED);
                    sectionAdapter.notifyDataSetChanged();
                }
            });
        }
    }


    /**************************************************************************************************
     * Leagues
     *************************************************************************************************/

    private void fetchAllLeagues(){
        FirebaseDatabase.getInstance().getReference(REF_LEAGUES).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        if (snapshot.exists() && snapshot.getValue() != null) {
                            League league = snapshot.getValue(League.class);
                            if (league == null) return;

                            final String leagueId = snapshot.getKey();
                            league.setId(leagueId);

                            final int index = userLeagueIds.indexOf(leagueId);
                            if (index > -1) {
                                userLeagues.add(league);
                                updateAdapter(USER_LEAGUES, userLeagues.size() - 1);
                            } else {
                                otherLeagues.add(league);
                                updateAdapter(LEAGUES, otherLeagues.size() - 1);
                            }
                        }
                    }
                }

                setSectionLoaded();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void fetchUserLeagues(){
        final String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        new CloudService(new CloudService.ProgressListener() {
            @Override
            public void onStringResponse(String response) {
                if (response == null || response.isEmpty()){
                    setSectionLoaded();
                    return;
                }

                // time out
                // todo: add network error status, retry functionality
                if (response.equals("timeout")){
                    setSectionLoaded();
                    return;
                }

                try {
                    JSONObject jsonObject = new JSONObject(response);

                    if (!jsonObject.isNull("result")) {

                        JSONObject resultsObj = jsonObject.getJSONObject("result");

                        Iterator<String> ids = resultsObj.keys();

                        while (ids.hasNext()) {
                            String leagueId = ids.next();
                            String status = resultsObj.getString(leagueId);

                            if (!status.equals("none")) {
                                userLeagueIds.add(leagueId);
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    setSectionLoaded();
                    return;
                }

                fetchAllLeagues();
            }
        }).getLeaguesForPlayer(uid);
    }
}
