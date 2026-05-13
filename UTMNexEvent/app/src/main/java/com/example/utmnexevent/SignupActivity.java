package com.example.utmnexevent;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.EdgeToEdge;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText editTextFullName;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private TextInputEditText editTextConfirmPassword;
    private RadioGroup radioGroupRole;
    private Button buttonRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.signup_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTextFullName = findViewById(R.id.editTextFullName);
        editTextEmail = findViewById(R.id.editTextEmailSignup);
        editTextPassword = findViewById(R.id.editTextPasswordSignup);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        radioGroupRole = findViewById(R.id.radioGroupRoleSignup);
        buttonRegister = findViewById(R.id.buttonRegister);
        View buttonBack = findViewById(R.id.buttonBack);

        buttonBack.setOnClickListener(v -> finish());

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void registerUser() {
        String fullName = editTextFullName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(SignupActivity.this, "Please fill in all details", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(SignupActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(SignupActivity.this, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selectedRoles = new ArrayList<>();
        int selectedId = radioGroupRole.getCheckedRadioButtonId();
        if (selectedId == R.id.radioParticipantSignup) {
            selectedRoles.add("participant");
        } else if (selectedId == R.id.radioOrganizerSignup) {
            selectedRoles.add("organizer");
        }

        if (selectedRoles.isEmpty()) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            saveUserData(fullName, email, selectedRoles);
                        } else {
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                // User already exists, try to update roles
                                updateExistingUserRoles(email, password, selectedRoles);
                            } else {
                                Toast.makeText(SignupActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                buttonRegister.setEnabled(true);
                            }
                        }
                    }
                });
    }

    private void saveUserData(String fullName, String email, List<String> roles) {
        String userId = mAuth.getCurrentUser().getUid();
        Map<String, Object> user = new HashMap<>();
        user.put("fullName", fullName);
        user.put("email", email);
        user.put("role", roles);

        db.collection("users").document(userId)
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(SignupActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(SignupActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                            buttonRegister.setEnabled(true);
                        }
                    }
                });
    }

    private void updateExistingUserRoles(String email, String password, List<String> newRoles) {
        // We need to sign in first to get the UID for the existing email
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String userId = mAuth.getCurrentUser().getUid();
                            db.collection("users").document(userId).get()
                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if (task.isSuccessful()) {
                                                DocumentSnapshot document = task.getResult();
                                                if (document != null && document.exists()) {
                                                    Object currentRoleObj = document.get("role");
                                                    List<String> combinedRoles = new ArrayList<>();

                                                    if (currentRoleObj instanceof String) {
                                                        combinedRoles.add((String) currentRoleObj);
                                                    } else if (currentRoleObj instanceof List) {
                                                        combinedRoles.addAll((List<String>) currentRoleObj);
                                                    }

                                                    for (String role : newRoles) {
                                                        if (!combinedRoles.contains(role)) {
                                                            combinedRoles.add(role);
                                                        }
                                                    }

                                                    db.collection("users").document(userId)
                                                            .update("role", combinedRoles)
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        Toast.makeText(SignupActivity.this, "Account roles updated successfully!", Toast.LENGTH_SHORT).show();
                                                                        finish();
                                                                    } else {
                                                                        Toast.makeText(SignupActivity.this, "Failed to update roles", Toast.LENGTH_SHORT).show();
                                                                        buttonRegister.setEnabled(true);
                                                                    }
                                                                }
                                                            });
                                                } else {
                                                    Toast.makeText(SignupActivity.this, "User record not found in database.", Toast.LENGTH_SHORT).show();
                                                    buttonRegister.setEnabled(true);
                                                }
                                            } else {
                                                Toast.makeText(SignupActivity.this, "Error fetching user data.", Toast.LENGTH_SHORT).show();
                                                buttonRegister.setEnabled(true);
                                            }
                                        }
                                    });
                        } else {
                            Toast.makeText(SignupActivity.this, "Incorrect password for existing account.", Toast.LENGTH_LONG).show();
                            buttonRegister.setEnabled(true);
                        }
                    }
                });
    }
}
