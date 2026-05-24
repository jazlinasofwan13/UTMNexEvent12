package com.example.utmnexevent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class AdminHomeActivity extends AppCompatActivity {

    private TextView textViewAdminHomeTitle;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_home);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        textViewAdminHomeTitle = findViewById(R.id.textViewAdminHomeTitle);
        
        loadUserData();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admin_home_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.buttonApproveOrganizers).setOnClickListener(v -> {
            Intent intent = new Intent(AdminHomeActivity.this, ApproveOrganizersActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.buttonManageAllEvents).setOnClickListener(v -> {
            Intent intent = new Intent(AdminHomeActivity.this, AdminManageEventsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.buttonManageOrganizers).setOnClickListener(v -> {
            Intent intent = new Intent(AdminHomeActivity.this, ManageOrganizersActivity.class);
            startActivity(intent);
        });

        ImageButton buttonMenu = findViewById(R.id.buttonMenu);
        buttonMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu(v);
            }
        });
    }

    private void showMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("My Profile");
        popup.getMenu().add("Log Out");

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getTitle().equals("My Profile")) {
                    Intent intent = new Intent(AdminHomeActivity.this, AdminProfileActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item.getTitle().equals("Log Out")) {
                    logout();
                    return true;
                }
                return false;
            }
        });
        popup.show();
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            userListener = db.collection("users").document(userId)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null) return;
                        if (snapshot != null && snapshot.exists()) {
                            String fullName = snapshot.getString("fullName");
                            if (fullName != null && !fullName.isEmpty()) {
                                textViewAdminHomeTitle.setText("Welcome, " + fullName);
                            }
                        }
                    });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) {
            userListener.remove();
        }
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().remove("last_role").apply();

        Intent intent = new Intent(AdminHomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
