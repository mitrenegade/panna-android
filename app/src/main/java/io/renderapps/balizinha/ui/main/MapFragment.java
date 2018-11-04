package io.renderapps.balizinha.ui.main;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.renderapps.balizinha.AppController;
import io.renderapps.balizinha.R;

import io.renderapps.balizinha.model.EventJsonAdapter;
import io.renderapps.balizinha.module.RetrofitFactory;
import io.renderapps.balizinha.model.Event;
import io.renderapps.balizinha.service.EventApiService;
import io.renderapps.balizinha.service.FirebaseService;
import io.renderapps.balizinha.ui.event.custom.EventClusterRenderer;
import io.renderapps.balizinha.util.ActivityLauncher;
import io.renderapps.balizinha.util.CommonUtils;
import io.renderapps.balizinha.util.Constants;
import io.renderapps.balizinha.util.DialogHelper;
import okhttp3.ResponseBody;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static butterknife.internal.Utils.arrayOf;
import static io.renderapps.balizinha.util.Constants.FASTEST_INTERVAL;
import static io.renderapps.balizinha.util.Constants.UPDATE_INTERVAL;

public class MapFragment extends Fragment implements OnMapReadyCallback, ClusterManager.OnClusterItemClickListener<Event>, ClusterManager.OnClusterClickListener<Event> {
    private final int LOCATION_PERMISSIONS_REQUEST_CODE = 34;
    private final int DEFAULT_ZOOM_LEVEL = 11;
    private LocationCallback mLocationCallback;

    // properties
    private Unbinder unbinder;
    private RecyclerView.AdapterDataObserver adapterDataObserver;
    private MapAdapter mAdapter;
    private List<Event> currentEvents;
    private List<String> currentEventsIds;
    private List<String> userEvents;
    private BottomSheetBehavior bottomSheetBehavior;
    private float density;

    private ClusterManager<Event> mClusterManager;
    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap gMap;
    private boolean showMap = false;

    // firebase
    private FirebaseUser firebaseUser;
    private FirebaseRemoteConfig remoteConfig;
    private DatabaseReference databaseRef;
    private DatabaseReference eventsRef;
    private ChildEventListener childEventListener;
    private Query eventQuery;

    // views
    @BindView(R.id.progress_view) ProgressBar progressView;
    @BindView(R.id.list_progress_view) FrameLayout listProgressView;
    @BindView(R.id.progressbar_peak) ProgressBar peakProgress;

    @BindView(R.id.empty_view) LinearLayout emptyView;
    @BindView(R.id.list_empty_view) LinearLayout listEmptyView;
    @BindView(R.id.collapsed_empty_state) TextView peakEmptyView;

    @BindView(R.id.list_event_recycler) RecyclerView listRecyclerView;
    @BindView(R.id.event_recycler) RecyclerView recyclerView;
    @BindView(R.id.mapView) MapView mapView;
    @BindView(R.id.bottom_sheet) ConstraintLayout bottomSheet;

    @BindView(R.id.rl_list) RelativeLayout mListView;
    @BindView(R.id.cl_map) CoordinatorLayout mMapView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.fragment_map, container, false);
        setHasOptionsMenu(true);

        unbinder = ButterKnife.bind(this, root);
        mapView.onCreate(savedInstanceState);

        // toolbar
        Toolbar toolbar = root.findViewById(R.id.map_toolbar);
        toolbar.setTitle(R.string.toolbar_title_map);

        if (getActivity() != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

            final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null){
                actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        if (getActivity() instanceof MainActivity)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        // properties
        currentEventsIds = new ArrayList<>();
        currentEvents = new ArrayList<>();
        userEvents = new ArrayList<>();
        density = getActivity().getResources().getDisplayMetrics().density;

        // firebase
        databaseRef = FirebaseDatabase.getInstance().getReference();
        eventsRef = databaseRef.child("events");

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseRef.child("userEvents").child(firebaseUser.getUid()).keepSynced(true);

//        if (googleServicesAvailable()) {
//            mapView.onCreate(savedInstanceState);
//            mapView.getMapAsync(this);
//        }

//        setupBottomSheet();
//        setupRecycler();
//        fetchUserEvents();

        showMap();
        return root;
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
        if (mAdapter != null) {
            if (mAdapter.isShowingCluster())
                mAdapter.clearClusterEvents();
            else
                mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();

        if (databaseRef != null && childEventListener != null)
            databaseRef.child("userEvents").child(firebaseUser.getUid()).removeEventListener(childEventListener);

        if (mAdapter != null && adapterDataObserver != null)
            mAdapter.unregisterAdapterDataObserver(adapterDataObserver);

        unbinder.unbind();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_map, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                if (getActivity() != null){
                    if (getActivity() instanceof MainActivity)
                        ActivityLauncher.launchAccount((MainActivity) getActivity());
                }
                break;
            case R.id.action_create:
                if (getActivity() instanceof MainActivity)
                    ActivityLauncher.createGame((MainActivity) getActivity());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupBottomSheet(){
        // Bottom sheet
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setHideable(false);

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState){
                    case BottomSheetBehavior.STATE_DRAGGING:
                    case BottomSheetBehavior.STATE_EXPANDED:
                        peakProgress.setVisibility(View.GONE);
                        peakEmptyView.setVisibility(View.GONE);
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        if (mAdapter != null && mAdapter.getItemCount() == 0){
                            if (progressView.isShown()){
                                peakProgress.setVisibility(View.VISIBLE);
                            } else {
                                peakEmptyView.setVisibility(View.VISIBLE);
                            }
                        }
                        break;

                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
    }


    void updateBottomSheetHeight(){
        if (!showMap) return;

        if (mAdapter != null && mAdapter.getItemCount() > 1){
            final int height = (int) (density * 224);
            if (bottomSheetBehavior.getPeekHeight() != height)
                bottomSheetBehavior.setPeekHeight(height);
        } else {
            bottomSheetBehavior.setPeekHeight((int) (density * 132));
        }
    }

    public void setupRecycler(){
        if (!isValidContext()) return;

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        if (showMap){
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.hasFixedSize();
        } else {
            listRecyclerView.setLayoutManager(linearLayoutManager);
            listRecyclerView.hasFixedSize();
        }

        mAdapter = new MapAdapter(getActivity(), currentEvents, currentEventsIds);
        adapterDataObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                // hide empty and progress view when first item is loaded into adapter
                if (!showMap){
                    listProgressView.setVisibility(View.GONE);
                    listEmptyView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    progressView.setVisibility(View.GONE);
                    peakEmptyView.setVisibility(View.GONE);
                    peakProgress.setVisibility(View.GONE);

                    updateBottomSheetHeight();
                }
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                if (mAdapter.getItemCount() == 0) {

                    if (mAdapter.isShowingCluster()){
                        mAdapter.clearClusterEvents();
                    }

                    if (mAdapter.getItemCount() == 0)
                        showEmptyView();
                }

                updateBottomSheetHeight();
            }
        };

        mAdapter.registerAdapterDataObserver(adapterDataObserver);
        if (showMap) {
            recyclerView.setAdapter(mAdapter);
        } else {
            listRecyclerView.setAdapter(mAdapter);
        }
    }

    public void removeEvent(final String eventKey){
        if (isAdded() && isValidContext()) {
            getActivity().runOnUiThread(() -> {
                Event event = new Event();
                event.eid = eventKey;

                mAdapter.removeEvent(event);
                if (mClusterManager != null) {
                    mClusterManager.removeItem(event);
                    mClusterManager.cluster();
                }
            });
        }
    }

    /******************************************************************************
     * Firebase
     *****************************************************************************/

    public void fetchUserEvents(){
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null)
                    if ((Boolean) dataSnapshot.getValue()) {
                        userEvents.add(dataSnapshot.getKey());
                        if (currentEventsIds.contains(dataSnapshot.getKey())){
                            removeEvent(dataSnapshot.getKey());
                        }
                    }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getValue() == null) return;

                final String eventKey = dataSnapshot.getKey();
                final int index = userEvents.indexOf(eventKey);
                final boolean isJoined = (Boolean) dataSnapshot.getValue();

                if (!isJoined){
                    // user left game
                    if (index > -1)
                        userEvents.remove(eventKey);
                    addEvent(eventKey);
                } else{
                    // user joined game
                    if (index == -1) {
                        userEvents.add(eventKey);
                        removeEvent(eventKey);
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };

        databaseRef.child("userEvents").child(firebaseUser.getUid()).addChildEventListener(childEventListener);
        databaseRef.child("userEvents").child(firebaseUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // will fetch events after all user events have been fired
                loadEvents();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }


    private void loadEvents(){
        if (remoteConfig == null) remoteConfig = FirebaseRemoteConfig.getInstance();
        if (!isValidContext()) return;

        remoteConfig.fetch(Constants.REMOTE_CACHE_EXPIRATION).addOnCompleteListener(getActivity(), task -> {
            if (task.isSuccessful())
                remoteConfig.activateFetched();

            if (remoteConfig.getBoolean(Constants.CONFIG_AVAILABLE_EVENTS)){
                fetchAvailableEvents();
            } else {
                checkEmptyState();

                // query all games that have not reached endTime
                final double start = new Date().getTime() / 1000.0;
                eventQuery = eventsRef.orderByChild("endTime").startAt(start);
                fetchCurrentEvents();
            }
        });
    }

    private void fetchAvailableEvents(){
        if (firebaseUser == null) return;
        if (!isValidContext()) return;

        final Observable<ResponseBody> observable = RetrofitFactory.getInstance()
                .create(EventApiService.class).getEventsAvailableToUser(firebaseUser.getUid());

        ((MainActivity)getActivity()).mCompositeDisposable.add(observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<ResponseBody>(){

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        try {
                            final String response = responseBody.string();
                            parseJsonEvent(response);
                        } catch (IOException e) {
                            showEmptyView();
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        showEmptyView();
                    }

                    @Override
                    public void onComplete() {}
                }));
    }

    private void parseJsonEvent(String response){
        Moshi moshi = new Moshi.Builder()
                .add(new EventJsonAdapter())
                .build();
        final JsonAdapter<Event> jsonAdapter = moshi.adapter(Event.class);

        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONObject resultsObj = jsonObject.getJSONObject("results");
            Iterator<String> ids = resultsObj.keys();

            // no available games
            if (!ids.hasNext()){
                showEmptyView();
                return;
            }

            final Date date = new Date();
            while (ids.hasNext()) {
                String eventId = ids.next();
                Event event;
                try {
                    event = jsonAdapter.fromJson(resultsObj.getString(eventId));

                    if (event == null) continue;
                    if (!event.active) continue;
                    if (userEvents.contains(eventId)) continue;

                    if (CommonUtils.isGameOver(CommonUtils.secondsToMillis(event.endTime),
                            date))
                        continue;

                    event.setEid(eventId);
                    updateAdapter(event);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (currentEvents.isEmpty()) showEmptyView();
            else {
                progressView.setVisibility(View.GONE);
                peakProgress.setVisibility(View.GONE);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            showEmptyView();
        }
    }

    private void checkEmptyState(){
        eventQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // initial data loaded
                if (currentEvents.size() == 0)
                    showEmptyView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void fetchCurrentEvents(){
        eventQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    final Event event = dataSnapshot.getValue(Event.class);
                    if (event == null)
                        return;

                    if (userEvents.contains(dataSnapshot.getKey()))
                        return;

                    if (dataSnapshot.hasChild("active")){
                        if (!event.active){
                            return;
                        }
                    }

                    long endTime = event.getEndTime() * 1000;
                    Date endDate = new Date(endTime);
                    if (new Date().before(endDate)) {
                        if (!userEvents.contains(dataSnapshot.getKey())) {
                            addEvent(dataSnapshot.getKey());
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    final Event event = dataSnapshot.getValue(Event.class);
                    if (event == null)
                        return;

                    // remove event from listing if no longer active
                    if (dataSnapshot.hasChild("active")){
                        if (!event.active){
                            removeEvent(dataSnapshot.getKey());
                            return;
                        }
                    }

                    long endTime = event.getEndTime() * 1000;
                    Date endDate = new Date(endTime);
                    if (new Date().before(endDate)) {
                        if (!userEvents.contains(dataSnapshot.getKey())) {
                            addEvent(dataSnapshot.getKey());
                        }
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    public void addEvent(final String eid){
        eventsRef.child(eid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final Event event = dataSnapshot.getValue(Event.class);
                final String eventId = dataSnapshot.getKey();
                if (event == null) return;

                event.eid = eventId;
                updateAdapter(event);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }


    public void updateAdapter(Event event){
        if (currentEventsIds.contains(event.eid))
            return;

        mAdapter.addEvent(event);
        if (mClusterManager != null){
            mClusterManager.addItem(event);
            mClusterManager.cluster();
        }
    }


    void showEmptyView(){
        if (isAdded() && getActivity() != null){
            getActivity().runOnUiThread(() -> {

                if (showMap) {
                    progressView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    peakProgress.setVisibility(View.GONE);

                    if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                        peakEmptyView.setVisibility(View.VISIBLE);
                    }
                } else {
                    listProgressView.setVisibility(View.GONE);
                    listEmptyView.setVisibility(View.VISIBLE);
                }
            });
        }
    }


    private void showMap(){
        if (isAdded() && getActivity() != null) {
            ((MainActivity)getActivity()).mCompositeDisposable.add(FirebaseService.Companion.getRemoteConfig()
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(remoteConfig -> {
                        // get update version if available
                        final boolean shouldShowMap = remoteConfig.getBoolean(Constants.CONFIG_MAPS);
                        if (shouldShowMap){
                            showMap = true;
                            mListView.setVisibility(View.GONE);
                            mMapView.setVisibility(View.VISIBLE);
                            mapView.getMapAsync(this);
                            setupBottomSheet();
                        }

                        setupRecycler();
                        fetchUserEvents();
                    }, error -> {
                        setupRecycler();
                        fetchUserEvents();
                    }));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (googleMap == null) return ;
        gMap = googleMap;
        gMap.setPadding(0, 50, 0, 0);
        gMap.getUiSettings().setMyLocationButtonEnabled(false);

        if (isValidContext()) {
            mClusterManager = new ClusterManager<>(getActivity(), gMap);
            final EventClusterRenderer renderer = new EventClusterRenderer(getActivity(), gMap, mClusterManager);
            mClusterManager.setRenderer(renderer);
            renderer.setMinClusterSize(1);

            mClusterManager.setOnClusterClickListener(this);
            mClusterManager.setOnClusterItemClickListener(this);

            gMap.setOnCameraIdleListener(mClusterManager);
            gMap.setOnMarkerClickListener(mClusterManager);
            gMap.setOnMapClickListener(latLng -> {
                if (mAdapter != null) {
                    mAdapter.clearClusterEvents();
                    updateBottomSheetHeight();
                }
            });
        }

        if (checkPermissions())
            getLastLocation();
        else
            requestPermissions();
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation(){
        fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                goToLocationZoom(task.getResult().getLatitude(), task.getResult().getLongitude(), DEFAULT_ZOOM_LEVEL);
            } else {
                // last location null, start update
                startLocationUpdates();
            }
        });
    }

    @SuppressLint("MissingPermission")
    protected void startLocationUpdates() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        if (isValidContext()) {
            SettingsClient settingsClient = LocationServices.getSettingsClient(getActivity());
            settingsClient.checkLocationSettings(locationSettingsRequest);
        }

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // we only care about last known location
                if (locationResult.getLocations().size() > 0){
                    goToLocationZoom(locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude(), DEFAULT_ZOOM_LEVEL);
                }
                fusedLocationClient.removeLocationUpdates(mLocationCallback);
            }
        };
        fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    protected synchronized boolean googleServicesAvailable() {
        if (!isValidContext()) return false;
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(getActivity());
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (api.isUserResolvableError(isAvailable)) {
            Dialog dialog = api.getErrorDialog(getActivity(), isAvailable, 0);
            dialog.show();
        } else
            Toast.makeText(getActivity(), "Can't connect to play services", Toast.LENGTH_LONG).show();
        return false;
    }


    private void goToLocationZoom(double lat, double lng, float zoom){
        if (gMap != null){
            final LatLng ll = new LatLng(lat, lng);
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, zoom));
        }
    }

    private boolean checkPermissions() {
        return isValidContext() && ActivityCompat.checkSelfPermission(getActivity(), ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED;
    }

    private void startLocationPermissionRequest() {
        if (isValidContext()) {
            ((AppController)getActivity().getApplication()).getDataManager().setLocationRequested(true);
            requestPermissions(arrayOf(ACCESS_COARSE_LOCATION),
                    LOCATION_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissions() {
        if (!isValidContext()) return;

        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), ACCESS_COARSE_LOCATION)) {
            startLocationPermissionRequest();
        } else {
            if (!((AppController)getActivity().getApplication()).getDataManager().getLocationRequested()){
                startLocationPermissionRequest();
            } else {
                // never show again
                DialogHelper.showDeviceSettingsDialog((MainActivity)getActivity());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) getLastLocation();
        } else {
            // location permission denied
            if (getActivity() != null)
                Toast.makeText(getActivity(), getString(R.string.permission_denied_map), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStart() {
        mapView.onStart();
        super.onStart();
        updateBottomSheetHeight();
    }

    @Override
    public void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    public boolean onClusterItemClick(Event event) {
        goToLocationZoom(event.getPosition().latitude, event.getPosition().longitude, 16);
        mAdapter.showClusterEvents(new ArrayList(Arrays.asList(event)));
        updateBottomSheetHeight();
        return false;
    }

    @Override
    public boolean onClusterClick(Cluster<Event> cluster) {
        mAdapter.showClusterEvents(new ArrayList<>(cluster.getItems()));
        updateBottomSheetHeight();
        return false;
    }

    private boolean isValidContext(){
        return isAdded() && getActivity() instanceof MainActivity && CommonUtils.isValidContext((MainActivity)getActivity());
    }
}
