package com.example.utmnexevent;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManageOrganizersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textViewEmpty;
    private OrganizerAdapter adapter;
    private List<Map<String, Object>> organizerList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_organizers);

        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerViewOrganizers);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        organizerList = new ArrayList<>();
        adapter = new OrganizerAdapter(organizerList);
        recyclerView.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.manage_organizers_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        loadOrganizers();
    }

    private void loadOrganizers() {
        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        organizerList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> data = document.getData();
                            Object roleObj = data.get("role");
                            boolean isOrganizer = false;
                            
                            if (roleObj instanceof String && "organizer".equals(roleObj)) {
                                isOrganizer = true;
                            } else if (roleObj instanceof List && ((List<?>) roleObj).contains("organizer")) {
                                isOrganizer = true;
                            }

                            if (isOrganizer) {
                                data.put("uid", document.getId());
                                organizerList.add(data);
                            }
                        }

                        if (organizerList.isEmpty()) {
                            textViewEmpty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            textViewEmpty.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(this, "Error fetching organizers", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private class OrganizerAdapter extends RecyclerView.Adapter<OrganizerAdapter.ViewHolder> {
        private List<Map<String, Object>> organizers;

        public OrganizerAdapter(List<Map<String, Object>> organizers) {
            this.organizers = organizers;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_organizer_account, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> org = organizers.get(position);
            holder.textViewName.setText(String.valueOf(org.get("fullName")));
            holder.textViewEmail.setText(String.valueOf(org.get("email")));
            
            Boolean isApproved = (Boolean) org.get("isApproved");
            if (isApproved != null && isApproved) {
                holder.textViewStatus.setText("Status: Approved");
                holder.textViewStatus.setTextColor(getResources().getColor(R.color.admin_red_primary, getTheme())); // Wait, using red for approved? maybe green. 
                // Using #2E7D32 from layout but I'll set it here to be sure.
                holder.textViewStatus.setTextColor(0xFF2E7D32);
            } else {
                holder.textViewStatus.setText("Status: Pending Approval");
                holder.textViewStatus.setTextColor(0xFFFFA000); // Orange
            }

            holder.buttonDelete.setOnClickListener(v -> showDeleteConfirmation((String) org.get("uid"), position));
        }

        @Override
        public int getItemCount() {
            return organizers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewName, textViewEmail, textViewStatus;
            ImageButton buttonDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewName = itemView.findViewById(R.id.textViewOrgName);
                textViewEmail = itemView.findViewById(R.id.textViewOrgEmail);
                textViewStatus = itemView.findViewById(R.id.textViewApprovalStatus);
                buttonDelete = itemView.findViewById(R.id.buttonDeleteOrganizer);
            }
        }
    }

    private void showDeleteConfirmation(String uid, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Organizer Account")
                .setMessage("Are you sure you want to permanently delete this organizer account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteOrganizer(uid, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteOrganizer(String uid, int position) {
        db.collection("users").document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    organizerList.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Organizer account deleted", Toast.LENGTH_SHORT).show();
                    if (organizerList.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Deletion failed", Toast.LENGTH_SHORT).show());
    }
}
