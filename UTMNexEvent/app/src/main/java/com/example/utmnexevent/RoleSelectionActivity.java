package com.example.utmnexevent;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RoleSelectionActivity extends AppCompatActivity {

    private Button buttonParticipantLogin;
    private Button buttonOrganizerLogin;
    private Button buttonAdminLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_role_selection);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.role_selection_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        buttonParticipantLogin = findViewById(R.id.buttonParticipantLogin);
        buttonOrganizerLogin = findViewById(R.id.buttonOrganizerLogin);
        buttonAdminLogin = findViewById(R.id.buttonAdminLogin);

        buttonParticipantLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // Organizer button now links to OrganizerLoginActivity
        buttonOrganizerLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoleSelectionActivity.this, OrganizerLoginActivity.class);
                startActivity(intent);
            }
        });

        buttonAdminLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RoleSelectionActivity.this, AdminLoginActivity.class);
                startActivity(intent);
            }
        });
    }
}
