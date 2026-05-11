package com.example.utmnexevent;

import android.content.Intent;
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

public class OrganizerProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ImageView imageViewProfilePic;
    private TextView textViewDisplayName, textViewDisplayEmail, textViewDisplayPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.organizer_profile_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageViewProfilePic = findViewById(R.id.imageViewOrgProfilePic);
        textViewDisplayName = findViewById(R.id.textViewOrgDisplayName);
        textViewDisplayEmail = findViewById(R.id.textViewOrgDisplayEmail);
        textViewDisplayPhone = findViewById(R.id.textViewOrgDisplayPhone);
        Button buttonChangePassword = findViewById(R.id.buttonOrgChangePassword);
        Button buttonEditInfo = findViewById(R.id.buttonEditInfo);
        View buttonBack = findViewById(R.id.buttonOrgProfileBack);

        loadUserData();

        imageViewProfilePic.setOnClickListener(v -> showAvatarSelectionDialog());

        buttonEditInfo.setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        buttonChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        buttonBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData(); // Refresh data
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
                            String phone = document.getString("phone");
                            String avatarId = document.getString("avatarId");

                            textViewDisplayName.setText(fullName);
                            textViewDisplayEmail.setText(email);
                            textViewDisplayPhone.setText(phone != null ? phone : "No phone number");
                            
                            imageViewProfilePic.setImageResource(AvatarHelper.getAvatarResource(avatarId));
                        }
                    });
        }
    }

    private void showAvatarSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_avatar_selection, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.avatar1).setOnClickListener(v -> updateAvatar(AvatarHelper.AVATAR_1, dialog));
        dialogView.findViewById(R.id.avatar2).setOnClickListener(v -> updateAvatar(AvatarHelper.AVATAR_2, dialog));
        dialogView.findViewById(R.id.avatar3).setOnClickListener(v -> updateAvatar(AvatarHelper.AVATAR_3, dialog));
        dialogView.findViewById(R.id.avatar4).setOnClickListener(v -> updateAvatar(AvatarHelper.AVATAR_4, dialog));

        dialog.show();
    }

    private void updateAvatar(String avatarId, AlertDialog dialog) {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).update("avatarId", avatarId)
                .addOnSuccessListener(aVoid -> {
                    imageViewProfilePic.setImageResource(AvatarHelper.getAvatarResource(avatarId));
                    dialog.dismiss();
                    Toast.makeText(this, "Avatar updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update avatar", Toast.LENGTH_SHORT).show());
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
                    Toast.makeText(OrganizerProfileActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!newPassword.equals(confirmNewPassword)) {
                    Toast.makeText(OrganizerProfileActivity.this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (newPassword.length() < 6) {
                    Toast.makeText(OrganizerProfileActivity.this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(OrganizerProfileActivity.this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(OrganizerProfileActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(OrganizerProfileActivity.this, "Authentication failed. Check current password.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
