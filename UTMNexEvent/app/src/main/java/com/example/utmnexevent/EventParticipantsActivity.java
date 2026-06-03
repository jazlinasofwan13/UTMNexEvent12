package com.example.utmnexevent;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventParticipantsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textViewEmpty, textViewPageTitle;
    private ParticipantAdapter adapter;
    private List<Map<String, Object>> participantList;
    private FirebaseFirestore db;
    private String eventId, eventName;
    private com.google.firebase.firestore.ListenerRegistration registrationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_participants);

        db = FirebaseFirestore.getInstance();
        eventId = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");

        recyclerView = findViewById(R.id.recyclerViewParticipants);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        textViewPageTitle = findViewById(R.id.textViewPageTitle);

        if (eventName != null) {
            textViewPageTitle.setText(eventName + " - Participants");
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        participantList = new ArrayList<>();
        adapter = new ParticipantAdapter(participantList);
        recyclerView.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.event_participants_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        loadParticipants();
    }

    private void loadParticipants() {
        if (eventId == null) return;

        registrationListener = db.collection("event_registrations")
                .whereEqualTo("eventId", eventId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error syncing participants", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (value != null) {
                        participantList.clear();
                        if (value.isEmpty()) {
                            updateUI();
                        } else {
                            for (QueryDocumentSnapshot document : value) {
                                String userId = document.getString("userId");
                                String registrationId = document.getId();
                                Boolean attendedObj = document.getBoolean("attended");
                                boolean attended = attendedObj != null && attendedObj;
                                fetchUserDetails(userId, registrationId, attended);
                            }
                        }
                    }
                });
    }

    private void fetchUserDetails(String userId, String registrationId, boolean attended) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        Map<String, Object> participant = new HashMap<>();
                        participant.put("name", userDoc.getString("fullName"));
                        participant.put("email", userDoc.getString("email"));
                        participant.put("registrationId", registrationId);
                        participant.put("userId", userId);
                        participant.put("attended", attended);
                        participantList.add(participant);
                        updateUI();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registrationListener != null) {
            registrationListener.remove();
        }
    }

    private void updateUI() {
        if (participantList.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textViewEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> participant = participants.get(position);
            holder.name.setText(String.valueOf(participant.get("name")));
            holder.email.setText(String.valueOf(participant.get("email")));

            boolean attended = participant.get("attended") != null && (boolean) participant.get("attended");
            if (attended) {
                holder.textAttended.setVisibility(View.VISIBLE);
                holder.imageAttended.setVisibility(View.VISIBLE);
            } else {
                holder.textAttended.setVisibility(View.GONE);
                holder.imageAttended.setVisibility(View.GONE);
            }

            holder.btnRemove.setOnClickListener(v -> showRemoveConfirmation(participant, position));
        }

        @Override
        public int getItemCount() {
            return participants.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, email, textAttended;
            ImageView imageAttended;
            ImageButton btnRemove;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.textViewParticipantName);
                email = itemView.findViewById(R.id.textViewParticipantEmail);
                textAttended = itemView.findViewById(R.id.textViewAttended);
                imageAttended = itemView.findViewById(R.id.imageViewAttended);
                btnRemove = itemView.findViewById(R.id.buttonRemoveParticipant);
            }
        }
    }

    private void showRemoveConfirmation(Map<String, Object> participant, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Participant")
                .setMessage("Are you sure you want to remove " + participant.get("name") + " from this event?")
                .setPositiveButton("Remove", (dialog, which) -> removeParticipant(participant, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeParticipant(Map<String, Object> participant, int position) {
        String regId = (String) participant.get("registrationId");
        
        db.collection("event_registrations").document(regId).delete()
                .addOnSuccessListener(aVoid -> {
                    // Update count in events collection
                    db.collection("events").document(eventId)
                            .update("participantsJoined", FieldValue.increment(-1))
                            .addOnSuccessListener(v -> {
                                participantList.remove(position);
                                adapter.notifyItemRemoved(position);
                                Toast.makeText(this, "Participant removed", Toast.LENGTH_SHORT).show();
                                updateUI();
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to remove participant", Toast.LENGTH_SHORT).show());
    }
}
