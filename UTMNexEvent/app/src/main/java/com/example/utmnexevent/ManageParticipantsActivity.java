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

public class ManageParticipantsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textViewEmpty;
    private ParticipantAdapter adapter;
    private List<Map<String, Object>> participantList;
    private FirebaseFirestore db;
    private com.google.firebase.firestore.ListenerRegistration participantsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_participants);

        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerViewParticipants);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        participantList = new ArrayList<>();
        adapter = new ParticipantAdapter(participantList);
        recyclerView.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.manage_participants_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        loadParticipants();
    }

    private void loadParticipants() {
        participantsListener = db.collection("users")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error fetching participants", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        participantList.clear();
                        for (QueryDocumentSnapshot document : value) {
                            Map<String, Object> data = document.getData();
                            Object roleObj = data.get("role");
                            boolean isParticipant = false;

                            if (roleObj instanceof String) {
                                isParticipant = "participant".equals(roleObj);
                            } else if (roleObj instanceof List) {
                                isParticipant = ((List<?>) roleObj).contains("participant");
                            }

                            if (isParticipant) {
                                data.put("uid", document.getId());
                                participantList.add(data);
                            }
                        }

                        if (participantList.isEmpty()) {
                            textViewEmpty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            textViewEmpty.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (participantsListener != null) {
            participantsListener.remove();
        }
    }

    private class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ViewHolder> {
        private List<Map<String, Object>> participants;

        public ParticipantAdapter(List<Map<String, Object>> participants) {
            this.participants = participants;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant_account, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> part = participants.get(position);
            holder.textViewName.setText(String.valueOf(part.get("fullName")));
            holder.textViewEmail.setText(String.valueOf(part.get("email")));
            
            String phone = (String) part.get("phone");
            holder.textViewPhone.setText(phone != null ? phone : "No phone number");

            holder.buttonDelete.setOnClickListener(v -> showDeleteConfirmation((String) part.get("uid"), position));
        }

        @Override
        public int getItemCount() {
            return participants.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewName, textViewEmail, textViewPhone;
            ImageButton buttonDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewName = itemView.findViewById(R.id.textViewParticipantName);
                textViewEmail = itemView.findViewById(R.id.textViewParticipantEmail);
                textViewPhone = itemView.findViewById(R.id.textViewParticipantPhone);
                buttonDelete = itemView.findViewById(R.id.buttonDeleteParticipant);
            }
        }
    }

    private void showDeleteConfirmation(String uid, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Participant Account")
                .setMessage("Are you sure you want to permanently delete this participant account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteParticipant(uid, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteParticipant(String uid, int position) {
        db.collection("users").document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    participantList.remove(position);
                    adapter.notifyItemRemoved(position);
                    Toast.makeText(this, "Participant account deleted", Toast.LENGTH_SHORT).show();
                    if (participantList.isEmpty()) {
                        textViewEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Deletion failed", Toast.LENGTH_SHORT).show());
    }
}
