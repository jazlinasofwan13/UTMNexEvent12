package com.example.utmnexevent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ParticipantProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    private ImageView imageViewProfilePic;
    private TextView textViewDisplayName, textViewDisplayEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_participant_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.participant_profile_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageViewProfilePic = findViewById(R.id.imageViewProfilePic);
        textViewDisplayName = findViewById(R.id.textViewDisplayName);
        textViewDisplayEmail = findViewById(R.id.textViewDisplayEmail);
        Button buttonChangePassword = findViewById(R.id.buttonChangePassword);
        Button buttonBack = findViewById(R.id.buttonBack);

        // Set default profile picture
        imageViewProfilePic.setImageResource(android.R.drawable.ic_menu_gallery);

        loadUserData();

        buttonChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        buttonBack.setOnClickListener(v -> finish());
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            db.collection("users").document(userId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            DocumentSnapshot document = task.getResult();
                            String fullName = document.getString("fullName");
                            String email = document.getString("email");

                            textViewDisplayName.setText(fullName);
                            textViewDisplayEmail.setText(email);
                        }
                    });
        }
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        TextInputEditText editTextCurrentPassword = dialogView.findViewById(R.id.editTextCurrentPassword);
        TextInputEditText editTextNewPassword = dialogView.findViewById(R.id.editTextNewPassword);
        TextInputEditText editTextConfirmNewPassword = dialogView.findViewById(R.id.editTextConfirmNewPassword);

        builder.setPositiveButton("Update", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String currentPassword = editTextCurrentPassword.getText().toString().trim();
                String newPassword = editTextNewPassword.getText().toString().trim();
                String confirmNewPassword = editTextConfirmNewPassword.getText().toString().trim();

                if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                    Toast.makeText(ParticipantProfileActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!newPassword.equals(confirmNewPassword)) {
                    Toast.makeText(ParticipantProfileActivity.this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (newPassword.length() < 6) {
                    Toast.makeText(ParticipantProfileActivity.this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                updatePassword(currentPassword, newPassword, dialog);
            }
        });
    }

    private void updatePassword(String currentPassword, String newPassword, AlertDialog dialog) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

            user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        user.updatePassword(newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(ParticipantProfileActivity.this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(ParticipantProfileActivity.this, "Failed to update password: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(ParticipantProfileActivity.this, "Authentication failed. Check your current password.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
