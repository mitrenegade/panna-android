package io.renderapps.balizinha.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.activity.CreateEventActivity;
import io.renderapps.balizinha.adapter.MapAdapter;
import io.renderapps.balizinha.model.Event;

public class MapFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback, LocationListener {

    // properties
    private Context mContext;
    private Date todaysDate;
    private MapAdapter mAdapter;
    private List<Event> currentEvents;
    private List<String> currentEventsIds;
    private List<String> userEvents;
    private List<String> processingPayments;

    // firebase
    private  FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser firebaseUser;
    private DatabaseReference databaseRef;
    private DatabaseReference eventsRef;
    private ChildEventListener childEventListener;
    private ValueEventListener valueEventListener;
    private Query eventQuery;

    // map
    private MapView mMapView;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private Location mLocation;

    private static final int LOCATION_REQUEST_CODE = 101;
    private long UPDATE_INTERVAL = 1000;  /* 60 secs */

    // views
    private FrameLayout progressView;
    private LinearLayout emptyView;
    private RecyclerView recyclerView;

    public MapFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (mGoogleApiClient == null) {
//            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
//                    .addConnectionCallbacks(this)
//                    .addOnConnectionFailedListener(this)
//                    .addApi(LocationServices.API)
//                    .build();
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.fragment_map, container, false);
        setHasOptionsMenu(true);

        // toolbar
        Toolbar toolbar = root.findViewById(R.id.map_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.toolbar_title_map);

        // properties
        todaysDate = new Date();
        currentEventsIds = new ArrayList<>();
        currentEvents = new ArrayList<>();
        userEvents = new ArrayList<>();
        processingPayments = new ArrayList<>();

        // firebase
        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference();
        eventsRef = databaseRef.child("events");
        firebaseUser = auth.getCurrentUser();
        firebaseUser = auth.getCurrentUser();
        databaseRef.child("userEvents").child(firebaseUser.getUid()).keepSynced(true);
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                firebaseUser = firebaseAuth.getCurrentUser();
            }
        };

        // query all games that have not reached endTime
        final double start = todaysDate.getTime() / 1000.0;
        eventQuery = eventsRef.orderByChild("endTime").startAt(start);

        // views
        recyclerView = root.findViewById(R.id.event_recycler);
        emptyView = root.findViewById(R.id.empty_view);
        progressView = root.findViewById(R.id.progress_view);
        ((ProgressBar)progressView.findViewById(R.id.progressbar)).getIndeterminateDrawable()
                .setColorFilter(ContextCompat.getColor(mContext, R.color.colorPrimary),
                PorterDuff.Mode.SRC_IN);
        setupRecycler();

        // map
//        if (googleServicesAvailable()) {
//            mMapView = root.findViewById(R.id.mapView);
//            mMapView.onCreate(savedInstanceState);
//            initMap();
//        } else
//            Toast.makeText(getActivity(), "Google Maps is unavailable", Toast.LENGTH_LONG).show();
//
        fetchUserEvents();
        checkEmptyState();
        fetchCurrentEvents();
        return root;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void onStart() {
//        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        auth.addAuthStateListener(authStateListener);
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }

    public void onStop() {
//        mGoogleApiClient.disconnect();
        super.onStop();
        if(authStateListener != null && auth != null){
            auth.removeAuthStateListener(authStateListener);
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
//        inflater.inflate(R.menu.menu_map, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.create_event)
            startActivity(new Intent(mContext, CreateEventActivity.class));
        return super.onOptionsItemSelected(item);
    }

//    public void startTimer(){
//        new CountDownTimer(15000, 1000) {
//
//            @Override
//            public void onTick(long millisUntilFinished) {}
//
//            @Override
//            public void onFinish() {
//                if (progressView.isShown()){
//                    // if we are still loading after 15 sec, stop and show empty view
//                    progressView.setVisibility(View.GONE);
//                    emptyView.setVisibility(View.VISIBLE);
//                }
//            }
//        }.start();
//    }

    public void setupRecycler(){
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
//        linearLayoutManager.setReverseLayout(true);
//        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.hasFixedSize();
        mAdapter = new MapAdapter(getActivity(), this, currentEvents);
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                // hide empty and progress view when first item is loaded into adapter
                emptyView.setVisibility(View.GONE);
                progressView.setVisibility(View.GONE);
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                if (mAdapter.getItemCount() == 0)
                    emptyView.setVisibility(View.VISIBLE);
            }
        });
        recyclerView.setAdapter(mAdapter);
    }


    public void removeEvent(String eventKey){
        final int index = currentEventsIds.indexOf(eventKey);
        if (index > -1){
            if (isAdded()) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentEventsIds.remove(index);
                        currentEvents.remove(index);
                        mAdapter.notifyItemRemoved(index);
                    }
                });
            }
        }
    }

    public void addProcessingPayment(String eventId){
        processingPayments.add(eventId);
    }

    public void removeProcessingPayment(String eventId){
        final int index = processingPayments.indexOf(eventId);
        if (index > -1)
            processingPayments.remove(index);
    }

    public boolean isPaymentInProcess(String eventId){
        return processingPayments.contains(eventId);
    }

    /******************************************************************************
     * Firebase
     *****************************************************************************/

    public void fetchUserEvents(){
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null)
                    if (dataSnapshot.getValue(Boolean.class)) {
                        userEvents.add(dataSnapshot.getKey());
                        if (currentEventsIds.contains(dataSnapshot.getKey())){
                            removeEvent(dataSnapshot.getKey());
                        }
                    }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                final String eventKey = dataSnapshot.getKey();
                final int index = userEvents.indexOf(eventKey);
                final boolean isJoined = dataSnapshot.getValue(Boolean.class);

                if (!isJoined){
                    // user left game
                    if (index > -1) {
                        userEvents.remove(eventKey);
                        addEvent(eventKey);
                    }
                } else{
                    // user joined game
                    if (index == -1) {
                        // add to list
                        userEvents.add(eventKey);
                        removeEvent(eventKey);
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };

        databaseRef.child("userEvents").child(firebaseUser.getUid()).addChildEventListener(childEventListener);
    }

    private void checkEmptyState(){
        eventQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean isEmpty = true;
                if (!dataSnapshot.exists() || !dataSnapshot.hasChildren()) {
                    // no events for today
                    progressView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    for (DataSnapshot child: dataSnapshot.getChildren()){
                        if (!userEvents.contains(child.getKey())) {
                            isEmpty = false;
                            break;
                        }
                    }
                    if (isEmpty) {
                        progressView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void fetchCurrentEvents(){
        eventQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    // check if event is today / upcoming
//                    double time = dataSnapshot.child("startTime").getValue(Double.class) * 1000;
                    long endTime = dataSnapshot.child("endTime").getValue(Long.class) * 1000;
//                    Date eDate = new Date((long)time);
                    Date endDate = new Date(endTime);
                    if (todaysDate.before(endDate)) {
                        if (!userEvents.contains(dataSnapshot.getKey())) {
                            // game is upcoming, add listener to it
                            addEvent(dataSnapshot.getKey());
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    // check if event is today / upcoming
//                    Date eDate = new Date(dataSnapshot.child("startTime").getValue(Long.class) * 1000);
                    long endTime = dataSnapshot.child("endTime").getValue(Long.class) * 1000;
                    Date endDate = new Date(endTime);
                    if (todaysDate.before(endDate)) {
                        if (!userEvents.contains(dataSnapshot.getKey())) {
                            // game is upcoming, add listener to it
                            addEvent(dataSnapshot.getKey());
                        }
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    public void addEvent(String eid){
        eventsRef.child(eid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final Event event = dataSnapshot.getValue(Event.class);
                event.setEid(dataSnapshot.getKey());
                // updating or appending to list
                final int index = currentEventsIds.indexOf(dataSnapshot.getKey());
                if (index > -1){
                    // update
                    currentEvents.set(index, event);
                    updateAdapter(index, false);
                } else {
                    // appending
                    currentEventsIds.add(dataSnapshot.getKey());
                    currentEvents.add(event);
                    updateAdapter(currentEvents.size() - 1, true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }


    public void updateAdapter(final int index, final boolean isItemInserted){
        if (isAdded()){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isItemInserted)
                        mAdapter.notifyItemInserted(index);
                    else
                        mAdapter.notifyItemChanged(index);
                }
            });
        }
    }

    /******************************************************************************
     * Initialize map
     *****************************************************************************/

    private void initMap() {
        mMapView.onResume();// needed to get the map to display immediately
        mMapView.getMapAsync(this);
    }


    // check for google services availability
    public boolean googleServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(mContext);
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (api.isUserResolvableError(isAvailable)) {
            Dialog dialog = api.getErrorDialog(getActivity(), isAvailable, 0);
            dialog.show();
        } else {
            Toast.makeText(getActivity(), "Can't connect to play services", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && android.support.v4.app.ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);
    }

    private void goToLocationZoom(double lat, double lng, float zoom) {
        LatLng ll = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, zoom);
        mMap.moveCamera(update);
    }

    /******************************************************************************
     * Location handlers
     *****************************************************************************/
    protected void startLocationUpdates() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        //mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        if (android.support.v4.app.ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && android.support.v4.app.ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Display the connection status
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && android.support.v4.app.ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_REQUEST_CODE);
            return;
        }

        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLocation != null) {
            goToLocationZoom(mLocation.getLatitude(), mLocation.getLongitude(), 15);
        } else {
            Toast.makeText(mContext, "Unable to determine location, check your network connection",
                    Toast.LENGTH_SHORT).show();
        }
        startLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
//        if (location == null) {
//            Toast.makeText(getActivity(), "Can't get current location", Toast.LENGTH_LONG).show();
//        } else {
//            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
//            goToLocationZoom(ll.latitude, ll.longitude, 15); // no animation
//        }
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    /******************************************************************************
     * Permission handler
     *****************************************************************************/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mGoogleApiClient.isConnected())
                    startLocationUpdates();
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // user also CHECKED "never ask again"
                Dialog dialog = showPermissionsMessage();
                dialog.show();
            } else {
                showMessageOKCancel("You need to allow location access to create a game!",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(getActivity(),
                                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                        LOCATION_REQUEST_CODE);
                            }
                        });
            }
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private Dialog showPermissionsMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("You need to enable location on your device in order to use this " +
                "feature to organize games.")
                .setPositiveButton("Turn on", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (getActivity() == null) {
                            return;
                        }

                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
                        intent.setData(uri);
                        MapFragment.this.startActivity(intent);

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        getActivity().getFragmentManager().popBackStack();
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
