package io.renderapps.balizinha.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.stripe.android.CustomerSession;
import com.stripe.android.model.Customer;
import com.stripe.android.model.CustomerSource;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.adapter.AccountAdapter;
import io.renderapps.balizinha.service.CloudService;
import io.renderapps.balizinha.service.EphemeralKeyGenerator;
import io.renderapps.balizinha.util.Constants;


public class AccountFragment extends Fragment {

    private boolean paymentRequired;
    private int cacheExpiration;
    private String customerId = "";
    private boolean didSetAdapter = false;
    private boolean initiatedCustomerSession = false;

    @BindView(R.id.account_progressbar) FrameLayout progressView;
    @BindView(R.id.account_recycler) RecyclerView recyclerView;

    // firebase
    private DatabaseReference databaseRef;
    private FirebaseRemoteConfig remoteConfig;
    private FirebaseUser firebaseUser;

    public AccountFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        remoteConfig = FirebaseRemoteConfig.getInstance();
        cacheExpiration = Constants.CACHE_EXPIRATION;
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_account, container, false);
        ButterKnife.bind(this, root);

        // toolbar
        Toolbar toolbar = root.findViewById(R.id.account_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.title_account);

        setupRecycler();
        fetchPaymentRequired();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (customerId.isEmpty()) {
            fetchCustomerId();
        } else {
            createCustomerSessions(customerId);
        }
    }

    public void setupRecycler(){
        if (getActivity() == null || getActivity().isDestroyed() || getActivity().isFinishing()) return;

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.hasFixedSize();
        recyclerView.setLayoutManager(layoutManager);
    }

    public void setAdapter(final int optionsId){
        // always update adapter on main thread
        if (isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String[] accountOptions = getResources().getStringArray(optionsId);
                    recyclerView.setAdapter(new AccountAdapter(getActivity(), accountOptions, paymentRequired));
                    didSetAdapter = true;
                    if (initiatedCustomerSession)
                        progressView.setVisibility(View.GONE);
                }
            });
        }
    }

    /**************************************************************************************************
     * Stripe - create customer session / verify payment methods
     *************************************************************************************************/

    public void fetchCustomerId(){
        databaseRef.child("stripe_customers")
                .child(firebaseUser.getUid())
                .child("customer_id")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() && dataSnapshot.getValue() != null){
                            customerId = dataSnapshot.getValue(String.class);
                            createCustomerSessions(customerId);
                        } else {
                            new CloudService(new CloudService.ProgressListener() {
                                @Override
                                public void onStringResponse(String string) {
                                    try {
                                        JSONObject jsonObject = new JSONObject(string);
                                        final String id = jsonObject.getString("customer_id");
                                        if (id != null && !id.isEmpty())
                                            customerId = id;
                                        createCustomerSessions(customerId);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).validateStripeCustomer(firebaseUser.getUid(), firebaseUser.getEmail());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
    }

    public void createCustomerSessions(String customerId){
        CustomerSession.initCustomerSession(new EphemeralKeyGenerator(
                new EphemeralKeyGenerator.ProgressListener() {
                    @Override
                    public void onStringResponse(String string) {
                        initiatedCustomerSession = true;
                        if (didSetAdapter)
                            progressView.setVisibility(View.GONE);
                    }
                }, customerId));


        CustomerSession.getInstance().retrieveCurrentCustomer(
                new CustomerSession.CustomerRetrievalListener() {
                    @Override
                    public void onCustomerRetrieved(@NonNull Customer customer) {
                        if (customer.getDefaultSource() != null) {
                            CustomerSource customerSource = customer.getSourceById(customer.getDefaultSource());
                            if (customerSource == null) return;

                            Source source = Source.fromString(customerSource.toString());

                            // Note: it isn't possible for a null or non-card source to be returned.
                            if (source != null && Source.CARD.equals(source.getType())) {
                                SourceCardData cardData = (SourceCardData) source.getSourceTypeModel();
                                updateDb(source.getId(), cardData);
                            }
                        }
                    }

                    @Override
                    public void onError(int errorCode, @Nullable String errorMessage) {}
                });
    }

    public void fetchPaymentRequired(){
        remoteConfig.fetch(cacheExpiration).addOnCompleteListener(getActivity(),
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful())
                            remoteConfig.activateFetched();
                        paymentRequired = remoteConfig.getBoolean(Constants.PAYMENT_CONFIG_KEY);

                        // check if user has added a payment method
                        if (paymentRequired) {
                            verifyPaymentMethod();
                         } else {
                            setAdapter(R.array.accountOptionsWithoutPayment);
                        }
                    }
                });
    }

    public void verifyPaymentMethod(){
        databaseRef.child("stripe_customers")
                .child(firebaseUser.getUid())
                .child("source")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        final int selectedOption;
                        if (dataSnapshot.exists() && dataSnapshot.getValue() != null)
                            selectedOption = R.array.accountOptionsWithPayment;
                        else
                            selectedOption = R.array.accountOptions;
                        // set adapter
                        setAdapter(selectedOption);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
    }

    public void updateDb(final String sourceId, final SourceCardData cardData){
        if (isAdded()) {
            setAdapter(R.array.accountOptionsWithPayment);
            databaseRef.child("stripe_customers")
                    .child(firebaseUser.getUid())
                    .child("source")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (!dataSnapshot.exists() || dataSnapshot.getValue() == null){
                                final String last4 = cardData.getLast4();
                                if (last4 == null) return;

                                Map<String, Object> childUpdates = new HashMap<>();
                                childUpdates.put("source", sourceId);
                                childUpdates.put("last4", last4);
                                childUpdates.put("label", cardData.getBrand().concat(" ")
                                        .concat(last4));

                                databaseRef.child("stripe_customers")
                                        .child(firebaseUser.getUid()).updateChildren(childUpdates);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {}
                    });
        }
    }
}
