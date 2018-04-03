package io.renderapps.balizinha.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.util.GeneralHelpers;

/**
 * Authenticates a user using email/password or through Facebook
 */

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    final private String TAG = LoginActivity.class.getSimpleName();

    // views
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private EditText mEmailField;
    private EditText mPasswordField;
    private CircularProgressButton loginButton;

    // firebase
    private FirebaseAuth auth;
    private DatabaseReference databaseRef;
    private Handler mHandler;

    // facebook
    private LoginButton facebookButton;
    /* The callback manager for Facebook */
    private CallbackManager mFacebookCallbackManager;
    private LoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // views
        emailLayout = findViewById(R.id.email_layout);
        mEmailField = findViewById(R.id.email_field);
        passwordLayout = findViewById(R.id.password_layout);
        mPasswordField = findViewById(R.id.password_field);
        loginButton = findViewById(R.id.loginButton);
        facebookButton = findViewById(R.id.facebook_button);

        // listeners
        findViewById(R.id.signUpButton).setOnClickListener(this);
        findViewById(R.id.facebook_login_button).setOnClickListener(this);
        loginButton.setOnClickListener(this);

        // init
        mHandler = new Handler();
        databaseRef = FirebaseDatabase.getInstance().getReference().child("players");
        auth = FirebaseAuth.getInstance();
        setupFacebookLogin();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // user is already logged in
        if (auth.getCurrentUser() != null)
            launchMainActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // avoid memory leak
        loginButton.dispose();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.signUpButton:
                onSignUp();
                break;
            case R.id.loginButton:
                onSignIn();
                break;
            case R.id.facebook_login_button:
                facebookButton.performClick();
                break;
        }
    }

    /******************************************************************************
     * Private methods handling authentication
     *****************************************************************************/

    private void onSignUp(){
        startActivity(new Intent(this, RegisterActivity.class));
    }

    private void launchMainActivity(){
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    // check for valid email and password entry
    public boolean validateForm(){
        boolean isValid = true;

        if (TextUtils.isEmpty(mEmailField.getText().toString())) {
            emailLayout.setError("Please enter your email");
            emailLayout.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_error));
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        if (TextUtils.isEmpty(mPasswordField.getText().toString())) {
            passwordLayout.setError("Please enter your password");
            passwordLayout.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake_error));
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        return isValid;
    }

    private void onSignIn(){
        if (!validateForm()) {
            return;
        }

        loginButton.startAnimation();
        mEmailField.setEnabled(false);
        mPasswordField.setEnabled(false);
        String email = mEmailField.getText().toString();
        String password = mPasswordField.getText().toString();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Bitmap check = GeneralHelpers.getBitmapFromVectorDrawable(getBaseContext(),
                                    R.drawable.ic_check);
                            loginButton.doneLoadingAnimation(android.R.color.white, check);
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    launchMainActivity();
                                }
                            }, 1000);
                        } else {
                            handleException(task.getException());
                            mEmailField.setEnabled(true);
                            mPasswordField.setEnabled(true);
                            loginButton.revertAnimation();
                        }
                    }
                });
    }

    void handleException(Exception exception){
        try {
            throw exception;
        } catch(FirebaseNetworkException e) {
            showToast("Could not connect to network.");
        } catch(FirebaseAuthInvalidCredentialsException e) {
            showToast("Invalid email address or password.");
        } catch (FirebaseAuthInvalidUserException e){
            showToast("There is no account associated with this email address.");
        } catch(Exception e) {
            showToast("Looks like something went wrong, try again.");
        }
    }

    void showToast(String message){
        Toast.makeText(LoginActivity.this, message,
                Toast.LENGTH_SHORT).show();
    }

    /* *************************************
     *              FACEBOOK               *
     ***************************************/
    public void setupFacebookLogin(){
        /* Load the Facebook login button and set up the tracker to monitor access token changes */
        // [START initialize_fblogin]
        loginManager = LoginManager.getInstance();
        mFacebookCallbackManager = CallbackManager.Factory.create();
        facebookButton.setReadPermissions("email", "public_profile");
        facebookButton.registerCallback(mFacebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {}

            @Override
            public void onError(FacebookException error) {}
        });

        loginManager.registerCallback(mFacebookCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        // App code
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        // App code
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // App code
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            onFacebookAuth(auth.getCurrentUser());
                        } else {
                            try {
                                throw task.getException();
                                // If sign in fails, display a message to the user.
                            } catch(FirebaseAuthUserCollisionException e) {
                                Toast.makeText(LoginActivity.this, "There is " +
                                                "already an account with the email associated with" +
                                                " your Facebook account. Please log in using the email option.",
                                        Toast.LENGTH_LONG).show();
                            } catch(Exception e) {
                                Toast.makeText(LoginActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        loginManager.logOut();
                    }
                });
    }

    /*
     * Check if facebook user is associated with player object before signing in
     * if not, then create one
     */
    public void onFacebookAuth(final FirebaseUser user){
        databaseRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null)
                    launchMainActivity();
                else {
                    // associate fb user with player object
                    Player player = new Player(user.getEmail());
                    if (user.getDisplayName() != null)
                        player.setName(user.getDisplayName());
                    if (user.getPhotoUrl() != null)
                        player.setPhotoUrl(user.getPhotoUrl().toString());

                    // set player & sign in
                    dataSnapshot.getRef().setValue(player);
                    launchMainActivity();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mFacebookCallbackManager
                .onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}


