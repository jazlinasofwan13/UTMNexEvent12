package com.example.utmnexevent;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private Button buttonLogin;
    private RadioGroup radioGroupRole;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        TextView textViewSignup = findViewById(R.id.textViewSignup);
        TextView textViewForgotPassword = findViewById(R.id.textViewForgotPassword);

        textViewForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editTextEmail.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter your email address first", Toast.LENGTH_SHORT).show();
                } else {
                    mAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(LoginActivity.this, "Password reset email sent to " + email, Toast.LENGTH_LONG).show();
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            }
        });

        textViewSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // For simplicity, default signup goes to participant. 
                // You might want to ask the user which one they want.
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = radioGroupRole.getCheckedRadioButtonId();
        final String selectedRole;
        if (selectedId == R.id.radioParticipant) {
            selectedRole = "participant";
        } else if (selectedId == R.id.radioOrganizer) {
            selectedRole = "organizer";
        } else if (selectedId == R.id.radioAdmin) {
            selectedRole = "admin";
        } else {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            checkUserRole(selectedRole);
                        } else {
                            Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            buttonLogin.setEnabled(true);
                        }
                    }
                });
    }

    private void checkUserRole(String selectedRole) {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            DocumentSnapshot document = task.getResult();
                            Object rolesObj = document.get("role"); // Could be String or List<String>
                            boolean hasAccess = false;

                            if (rolesObj instanceof String) {
                                if (selectedRole.equals(rolesObj)) {
                                    hasAccess = true;
                                }
                            } else if (rolesObj instanceof List) {
                                List<String> rolesList = (List<String>) rolesObj;
                                if (rolesList.contains(selectedRole)) {
                                    hasAccess = true;
                                }
                            }

                            if (hasAccess) {
                                navigateToHome(selectedRole);
                            } else {
                                mAuth.signOut();
                                Toast.makeText(LoginActivity.this, "Access denied for " + selectedRole + " role.", Toast.LENGTH_SHORT).show();
                                buttonLogin.setEnabled(true);
                            }
                        } else {
                            mAuth.signOut();
                            Toast.makeText(LoginActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                            buttonLogin.setEnabled(true);
                        }
                    }
                });
    }

    private void navigateToHome(String role) {
        Intent intent;
        if ("participant".equals(role)) {
            intent = new Intent(LoginActivity.this, MainActivity.class);
        } else if ("organizer".equals(role)) {
            intent = new Intent(LoginActivity.this, OrganizerHomeActivity.class);
        } else if ("admin".equals(role)) {
            intent = new Intent(LoginActivity.this, AdminHomeActivity.class);
        } else {
            return;
        }
        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
        startActivity(intent);
        finish();
    }
}
