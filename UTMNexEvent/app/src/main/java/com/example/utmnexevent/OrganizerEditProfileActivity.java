package com.example.utmnexevent;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class OrganizerEditProfileActivity extends AppCompatActivity {

    private TextInputEditText editTextFullName, editTextPhone;
    private Button buttonSave;
    private ImageButton buttonBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.edit_profile_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTextFullName = findViewById(R.id.editTextEditFullName);
        editTextPhone = findViewById(R.id.editTextEditPhone);
        buttonSave = findViewById(R.id.buttonSaveInfo);
        buttonBack = findViewById(R.id.buttonEditBack);

        loadCurrentData();

        buttonBack.setOnClickListener(v -> finish());

        buttonSave.setOnClickListener(v -> {
            String newName = editTextFullName.getText().toString().trim();
            String newPhone = editTextPhone.getText().toString().trim();
            
            if (newName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            } else if (newPhone.length() < 10 || newPhone.length() > 11) {
                Toast.makeText(this, "Phone number must be between 10 and 11 digits", Toast.LENGTH_SHORT).show();
            } else {
                updateProfile(newName, newPhone);
            }
        });
    }

    private void loadCurrentData() {
        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            editTextFullName.setText(documentSnapshot.getString("fullName"));
                            editTextPhone.setText(documentSnapshot.getString("phone"));
                        }
                    });
        }
    }

    private void updateProfile(String newName, String newPhone) {
        if (mAuth.getCurrentUser() != null) {
            buttonSave.setEnabled(false);
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("fullName", newName);
            updates.put("phone", newPhone);

            db.collection("users").document(mAuth.getCurrentUser().getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Organizer Info updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        buttonSave.setEnabled(true);
                    });
        }
    }
}
