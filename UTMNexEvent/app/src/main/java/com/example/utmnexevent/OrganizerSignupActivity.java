package com.example.utmnexevent;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class OrganizerSignupActivity extends AppCompatActivity {

    private TextInputEditText editTextFullName;
    private TextInputEditText androidEditTextEmail;
    private TextInputEditText editTextPassword;
    private TextInputEditText editTextConfirmPassword;
    private Button buttonRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.organizer_signup_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTextFullName = findViewById(R.id.editTextOrgFullName);
        androidEditTextEmail = findViewById(R.id.editTextOrgEmailSignup);
        editTextPassword = findViewById(R.id.editTextOrgPasswordSignup);
        editTextConfirmPassword = findViewById(R.id.editTextOrgConfirmPassword);
        buttonRegister = findViewById(R.id.buttonOrgRegister);

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerOrganizer();
            }
        });
    }

    private void registerOrganizer() {
        String fullName = editTextFullName.getText().toString().trim();
        String email = androidEditTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all details", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonRegister.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Save organizer info to Firestore
                            String userId = mAuth.getCurrentUser().getUid();
                            Map<String, Object> organizer = new HashMap<>();
                            organizer.put("fullName", fullName);
                            organizer.put("email", email);
                            organizer.put("role", "organizer");

                            db.collection("users").document(userId)
                                    .set(organizer)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(OrganizerSignupActivity.this, "Organizer Registration successful!", Toast.LENGTH_SHORT).show();
                                                finish();
                                            } else {
                                                Toast.makeText(OrganizerSignupActivity.this, "Failed to save organizer data", Toast.LENGTH_SHORT).show();
                                                buttonRegister.setEnabled(true);
                                            }
                                        }
                                    });
                        } else {
                            Toast.makeText(OrganizerSignupActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            buttonRegister.setEnabled(true);
                        }
                    }
                });
    }
}
