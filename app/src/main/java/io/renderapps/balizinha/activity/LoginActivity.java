package io.renderapps.balizinha.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;;
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
import com.google.firebase.auth.FirebaseAuthEmailException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.service.FirebaseService;

/**
 * Authenticates a user using email/password or through Facebook
 */

public class LoginActivity extends AppCompatActivity {
    final private String TAG = LoginActivity.class.getSimpleName();

    // views
    @BindView(R.id.email_layout) TextInputLayout emailLayout;
    @BindView(R.id.password_layout) TextInputLayout passwordLayout;

    @BindView(R.id.email_field) EditText mEmailField;
    @BindView(R.id.password_field) EditText mPasswordField;

    @BindView(R.id.loginButton) CircularProgressButton loginButton;
    @BindView(R.id.signUpButton) Button registerButton;

    @BindView(R.id.facebook_login_button) CircularProgressButton fbLoginButton;
    @BindView(R.id.facebook_button) LoginButton facebookButton;

    @OnClick(R.id.signUpButton) void onSignUp(){
        startActivity(new Intent(this, RegisterActivity.class));
    }

    // on-click
    @OnClick(R.id.facebook_login_button) void onFacebookClick(){
        enableViews(false);
        fbLoginButton.startAnimation();
        facebookButton.performClick();
    }

    @OnClick(R.id.loginButton) void onSignIn(){
        if (!validateForm()) {
            return;
        }

        loginButton.startAnimation();
        enableViews(false);
        String email = mEmailField.getText().toString();
        String password = mPasswordField.getText().toString();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_check);
                            loginButton.doneLoadingAnimation(android.R.color.white, bm);
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    launchMainActivity();
                                }
                            }, 1000);
                        } else {
                            handleException(task.getException());
                            enableViews(true);
                            loginButton.revertAnimation();
                        }
                    }
                });
    }



    // firebase
    private FirebaseAuth auth;
    private DatabaseReference databaseRef;
    private Handler mHandler;

    // facebook
    private CallbackManager mFacebookCallbackManager;
    private LoginManager loginManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        mHandler = new Handler();
        databaseRef = FirebaseDatabase.getInstance().getReference().child("players");
        auth = FirebaseAuth.getInstance();
        setupFacebookLogin();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // user is already logged in
        if (auth.getCurrentUser() != null) {
            checkFacebookPhoto(this, auth.getCurrentUser());
            launchMainActivity();
            return;
        }

        // user not authenticated - sign out if token != null
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null && !accessToken.isExpired()) {
            loginManager.logOut();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // avoid memory leak
        loginButton.dispose();
        fbLoginButton.dispose();
    }

    /******************************************************************************
     * Private methods handling authentication
     *****************************************************************************/

    private void launchMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    // check for valid email and password entry
    public boolean validateForm() {
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

    void handleException(Exception exception) {
        try {
            throw exception;
        } catch (FirebaseNetworkException e) {
            showToast("Could not connect to network.");
        } catch (FirebaseAuthInvalidCredentialsException e) {
            showToast("Invalid email address or password.");
        } catch (FirebaseAuthInvalidUserException e) {
            showToast("There is no account associated with this email address.");
        } catch(FirebaseAuthEmailException e){
            showToast("Invalid email address.");
        } catch (Exception e) {
            showToast("Looks like something went wrong, try again.");
        }
    }

    void showToast(String message) {
        Toast.makeText(LoginActivity.this, message,
                Toast.LENGTH_SHORT).show();
    }

    void enableViews(boolean isEnabled) {
        emailLayout.setEnabled(isEnabled);
        passwordLayout.setEnabled(isEnabled);
        registerButton.setEnabled(isEnabled);
        fbLoginButton.setEnabled(isEnabled);
        loginButton.setEnabled(isEnabled);
    }

    /* *************************************
     *              FACEBOOK               *
     ***************************************/
    public void setupFacebookLogin() {
        /* Load the Facebook login button and set up the tracker to monitor access token changes */
        // [START initialize_fblogin]
        loginManager = LoginManager.getInstance();
        mFacebookCallbackManager = CallbackManager.Factory.create();
        facebookButton.setReadPermissions("email", "public_profile");
        facebookButton.registerCallback(mFacebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                fbLoginButton.revertAnimation();
                enableViews(true);
            }

            @Override
            public void onError(FacebookException error) {
                // App code
                fbLoginButton.revertAnimation();
                enableViews(true);
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
                            } catch (FirebaseAuthUserCollisionException e) {
                                Toast.makeText(LoginActivity.this, "There is " +
                                                "already an account with the email associated with" +
                                                " your Facebook account. Please log in using the email option.",
                                        Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(LoginActivity.this,
                                        "Failed to authenticate using Facebook.",
                                        Toast.LENGTH_SHORT).show();
                            }

                            loginManager.logOut();
                            fbLoginButton.revertAnimation();
                            enableViews(true);
                        }
                    }
                });
    }

    /*
     * Check if facebook user is associated with player object before signing in
     * if not, then create one
     */
    public void onFacebookAuth(final FirebaseUser user) {
        final Context mContext = this;
        checkFacebookPhoto(mContext, user);

        databaseRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                    launchMainActivity();
                } else {
                    // associate fb user with player object
                    Player player = new Player(user.getEmail());
                    String name = user.getDisplayName();
                    Uri photoUrl = user.getPhotoUrl();
                    for (UserInfo userInfo : user.getProviderData()) {
                        if (userInfo.getDisplayName() != null && !userInfo.getDisplayName().isEmpty()) {
                            name = userInfo.getDisplayName();
                        }

                        if (userInfo.getPhotoUrl() != null && !userInfo.getPhotoUrl().toString().isEmpty()) {
                            photoUrl = userInfo.getPhotoUrl();
                        }
                    }

                    // set player properties
                    if (name != null)
                        player.setName(name);
                    if (photoUrl != null) {
                        player.setPhotoUrl(photoUrl.toString());
                        FirebaseService.startActionUploadFacebookPhoto(mContext, user.getUid());
                    }

                    // set player & sign in
                    databaseRef.child(user.getUid()).setValue(player)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            launchMainActivity();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
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

    void checkFacebookPhoto(Context mContext, FirebaseUser firebaseUser) {
        final Uri photoUrl = firebaseUser.getPhotoUrl();
        if (photoUrl != null && !photoUrl.toString().isEmpty()) {
            if (!photoUrl.toString().startsWith("https://firebasestorage")) {
                FirebaseService.startActionUploadFacebookPhoto(mContext,
                        firebaseUser.getUid());
            }
        }
    }
}


