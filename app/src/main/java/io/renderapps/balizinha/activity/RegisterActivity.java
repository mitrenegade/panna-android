package io.renderapps.balizinha.activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import br.com.simplepass.loading_button_lib.customViews.CircularProgressButton;
import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Player;
import io.renderapps.balizinha.util.GeneralHelpers;

/**
 * Registers a new user with email and password
 */
public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    // views
    private EditText mEmailField;
    private EditText mPasswordField;
    private EditText mConfirmPasswordField;
    private CircularProgressButton registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // views
        mEmailField = findViewById(R.id.email);
        mPasswordField = findViewById(R.id.password);
        mConfirmPasswordField = findViewById(R.id.confirm_password);
        registerButton = findViewById(R.id.sign_up_button);

        // listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        registerButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.sign_up_button:
                registerUser();
                break;
            case R.id.sign_in_button:
                onSignIn();
                break;
        }
    }

    public void onSignIn(){
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
    }

    public void registerUser(){
        enableEditing(false);

        if (!validateForm()){
            enableEditing(true);
        } else {
            // valid form
            registerButton.startAnimation();
            final String email = mEmailField.getText().toString().trim();
            final String password = mPasswordField.getText().toString().trim();
            FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                onAuthSuccess(email, task.getResult().getUser().getUid());
                            } else {
                                registerButton.revertAnimation();
                                enableEditing(true);
                                Toast.makeText(RegisterActivity.this,
                                        getResources().getString(R.string.email_in_use), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

    }

    public boolean validateForm() {
        String email = mEmailField.getText().toString().trim();
        String password = mPasswordField.getText().toString().trim();
        String confirmPassword = mConfirmPasswordField.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            mEmailField.setError("Required");
            return false;
        } else
            mEmailField.setError(null);

        if (TextUtils.isEmpty(password)) {
            mPasswordField.setError("Required");
            return false;
        } else
            mPasswordField.setError(null);

        if (TextUtils.isEmpty(confirmPassword)) {
            mConfirmPasswordField.setError("Required");
            return false;
        } else
            mConfirmPasswordField.setError(null);

        if (!GeneralHelpers.isValidEmail(email)) {
            mEmailField.setError("Invalid email");
            return false;
        } else
            mEmailField.setError(null);

        if (password.length() < 6) {
            mPasswordField.setError("Password must be at least 6 characters long");
            return false;
        } else
            mPasswordField.setError(null);

        if (!password.equals(confirmPassword)) {
            mPasswordField.setError("Passwords do not match");
            mConfirmPasswordField.setError("Passwords do not match");
            return false;
        } else {
            mPasswordField.setError(null);
            mConfirmPasswordField.setError(null);
        }

        return true;
    }

    private void onAuthSuccess(String email, String uid) {
        // Write new user
        Player player = new Player(email);
        player.setOs(getString(R.string.os_android));
        FirebaseDatabase.getInstance().getReference().child("players").child(uid).setValue(player)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        startActivity(new Intent(getBaseContext(), SetupProfileActivity.class));
                        finish();
                    }
                });
    }

    public void enableEditing(boolean isEnabled){
        mEmailField.setEnabled(isEnabled);
        mPasswordField.setEnabled(isEnabled);
        mConfirmPasswordField.setEnabled(isEnabled);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
