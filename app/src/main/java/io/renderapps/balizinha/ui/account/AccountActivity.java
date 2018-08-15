package io.renderapps.balizinha.ui.account;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
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
import com.stripe.android.view.PaymentMethodsActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.service.CloudService;
import io.renderapps.balizinha.service.EphemeralKeyGenerator;
import io.renderapps.balizinha.service.FirebaseService;
import io.renderapps.balizinha.util.Constants;

public class AccountActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_SELECT_SOURCE = 55;
    private boolean paymentRequired;
    private int cacheExpiration;
    private String customerId = "";
    private boolean didSetAdapter = false;
    private boolean initiatedCustomerSession = false;

    // firebase
    private DatabaseReference databaseRef;
    private FirebaseRemoteConfig remoteConfig;
    private FirebaseUser firebaseUser;

    @BindView(R.id.account_recycler) RecyclerView mRecycler;
    @BindView(R.id.account_progressbar) FrameLayout mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        ButterKnife.bind(this);

        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(R.string.title_account);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);

        remoteConfig = FirebaseRemoteConfig.getInstance();
        cacheExpiration = Constants.CACHE_EXPIRATION;
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        setupRecycler();
        fetchPaymentRequired();
    }

    public void setupRecycler(){
        mRecycler.hasFixedSize();
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    public void setAdapter(final int optionsId){
        final AccountActivity accountActivity = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String[] accountOptions = getResources().getStringArray(optionsId);
                mRecycler.setAdapter(new AccountAdapter(accountActivity, accountOptions, paymentRequired));
                didSetAdapter = true;
                if (initiatedCustomerSession)
                    mProgress.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) finish();
        return super.onOptionsItemSelected(item);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_SOURCE && resultCode == RESULT_OK) {
            String selectedSource = data.getStringExtra(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT);

            Source source = Source.fromString(selectedSource);
            // Note: it isn't possible for a null or non-card source to be returned.
            if (source != null && Source.CARD.equals(source.getType())) {
                SourceCardData cardData = (SourceCardData) source.getSourceTypeModel();
                FirebaseService.startActionSavePayment(this, firebaseUser.getUid(), source.getId(),
                        cardData.getBrand(), cardData.getLast4());

                // update adapter
                setAdapter(R.array.accountOptionsWithPayment);
            }
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
                            mProgress.setVisibility(View.GONE);
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
        remoteConfig.fetch(cacheExpiration).addOnCompleteListener(this, new OnCompleteListener<Void>() {
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
        if (!isDestroyed() && !isFinishing())
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

                            if (cardData.getBrand() != null)
                                childUpdates.put("label", cardData.getBrand().concat(" ").concat(last4));

                            databaseRef.child("stripe_customers")
                                    .child(firebaseUser.getUid()).updateChildren(childUpdates);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
