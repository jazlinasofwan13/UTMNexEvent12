package com.example.utmnexevent;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class ApproveOrganizersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textViewEmpty;
    private OrganizerAdapter adapter;
    private List<Map<String, Object>> pendingList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_approve_organizers);

        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerViewPendingOrganizers);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        pendingList = new ArrayList<>();
        adapter = new OrganizerAdapter(pendingList);
        recyclerView.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.approve_organizers_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        loadPendingOrganizers();
    }

    private void loadPendingOrganizers() {
        db.collection("users")
                .whereArrayContains("role", "organizer")
                .whereEqualTo("isApproved", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        pendingList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> data = document.getData();
                            data.put("uid", document.getId());
                            pendingList.add(data);
                        }

                        if (pendingList.isEmpty()) {
                            textViewEmpty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            textViewEmpty.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(this, "Error fetching pending organizers", Toast.LENGTH_SHORT).show();
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_organizer_approval, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> org = organizers.get(position);
            String uid = (String) org.get("uid");
            holder.textViewName.setText(String.valueOf(org.get("fullName")));
            holder.textViewEmail.setText(String.valueOf(org.get("email")));

            holder.buttonApprove.setOnClickListener(v -> approveOrganizer(uid, position));
            holder.buttonReject.setOnClickListener(v -> showRejectConfirmation(uid, position));
        }

        @Override
        public int getItemCount() {
            return organizers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewName, textViewEmail;
            View buttonApprove, buttonReject;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewName = itemView.findViewById(R.id.textViewOrgName);
                textViewEmail = itemView.findViewById(R.id.textViewOrgEmail);
                buttonApprove = itemView.findViewById(R.id.buttonApprove);
                buttonReject = itemView.findViewById(R.id.buttonReject);
            }
        }
    }

    private void showRejectConfirmation(String uid, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Reject Organizer")
                .setMessage("Are you sure you want to reject this organizer? This will delete their registration request.")
                .setPositiveButton("Yes, Reject", (dialog, which) -> rejectOrganizer(uid, position))
                .setNegativeButton("No", null)
                .show();
    }

    private void rejectOrganizer(String uid, int position) {
        db.collection("users").document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    pendingList.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Organizer rejected and request removed.", Toast.LENGTH_SHORT).show();
                    if (pendingList.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Rejection failed", Toast.LENGTH_SHORT).show());
    }

    private void approveOrganizer(String uid, int position) {
        db.collection("users").document(uid)
                .update("isApproved", true)
                .addOnSuccessListener(aVoid -> {
                    pendingList.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Organizer approved!", Toast.LENGTH_SHORT).show();
                    if (pendingList.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Approval failed", Toast.LENGTH_SHORT).show());
    }
}
