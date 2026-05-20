package com.example.utmnexevent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private TextView textViewHomeTitle, textViewNoEvents;
    private RecyclerView recyclerViewUpcomingEvents;
    private EventAdapter adapter;
    private List<Map<String, Object>> eventList;
    private Set<String> joinedEventIds = new HashSet<>();
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        textViewHomeTitle = findViewById(R.id.textViewHomeTitle);
        textViewNoEvents = findViewById(R.id.textViewNoEvents);
        recyclerViewUpcomingEvents = findViewById(R.id.recyclerViewUpcomingEvents);

        recyclerViewUpcomingEvents.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();
        adapter = new EventAdapter(eventList);
        recyclerViewUpcomingEvents.setAdapter(adapter);

        loadUserData();
        loadUpcomingEvents();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize and set click listeners for the new buttons


        findViewById(R.id.buttonViewQR).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewQRActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.buttonViewHistory).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ViewHistoryActivity.class);
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
                    Intent intent = new Intent(MainActivity.this, ParticipantProfileActivity.class);
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
                                textViewHomeTitle.setText("Welcome, " + fullName);
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

    private void loadUpcomingEvents() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        // First, load which events the user has already joined
        db.collection("event_registrations")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(regTask -> {
                    if (regTask.isSuccessful()) {
                        joinedEventIds.clear();
                        for (QueryDocumentSnapshot doc : regTask.getResult()) {
                            joinedEventIds.add(doc.getString("eventId"));
                        }

                        // Then, load the active events
                        fetchActiveEvents();
                    }
                });
    }

    private void fetchActiveEvents() {
        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        eventList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> eventData = document.getData();
                            eventData.put("id", document.getId());
                            eventList.add(eventData);
                        }

                        if (eventList.isEmpty()) {
                            textViewNoEvents.setVisibility(View.VISIBLE);
                            recyclerViewUpcomingEvents.setVisibility(View.GONE);
                        } else {
                            textViewNoEvents.setVisibility(View.GONE);
                            recyclerViewUpcomingEvents.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
        private List<Map<String, Object>> events;

        public EventAdapter(List<Map<String, Object>> events) {
            this.events = events;
        }

        @NonNull
        @Override
        public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_participant, parent, false);
            return new EventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
            Map<String, Object> event = events.get(position);
            String eventId = (String) event.get("id");

            holder.textViewName.setText(String.valueOf(event.get("name")));
            holder.textViewDate.setText(String.valueOf(event.get("date")));
            holder.textViewTime.setText(String.valueOf(event.get("time")));
            holder.textViewDescription.setText(String.valueOf(event.get("description")));

            long joined = 0;
            Object joinedObj = event.get("participantsJoined");
            if (joinedObj instanceof Long) {
                joined = (Long) joinedObj;
            } else if (joinedObj instanceof Integer) {
                joined = (Integer) joinedObj;
            }

            long limit = 0;
            Object limitObj = event.get("participantLimit");
            if (limitObj instanceof Long) {
                limit = (Long) limitObj;
            } else if (limitObj instanceof Integer) {
                limit = (Integer) limitObj;
            }

            holder.textViewParticipantInfo.setText(String.format(Locale.getDefault(), "Participants: %d / %d", joined, limit));

            // Check if already joined
            if (joinedEventIds.contains(eventId)) {
                holder.buttonJoin.setVisibility(View.GONE);
                holder.textViewJoined.setVisibility(View.VISIBLE);
            } else {
                holder.buttonJoin.setVisibility(View.VISIBLE);
                holder.textViewJoined.setVisibility(View.GONE);

                long finalJoined = joined;
                long finalLimit = limit;
                holder.buttonJoin.setOnClickListener(v -> {
                    if (finalJoined < finalLimit) {
                        joinEvent(eventId, event, position);
                    } else {
                        Toast.makeText(MainActivity.this, "Event is full!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        class EventViewHolder extends RecyclerView.ViewHolder {
            TextView textViewName, textViewDate, textViewTime, textViewParticipantInfo, textViewDescription, textViewJoined;
            Button buttonJoin;

            public EventViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewName = itemView.findViewById(R.id.textViewEventName);
                textViewDate = itemView.findViewById(R.id.textViewEventDate);
                textViewTime = itemView.findViewById(R.id.textViewEventTime);
                textViewParticipantInfo = itemView.findViewById(R.id.textViewParticipantInfo);
                textViewDescription = itemView.findViewById(R.id.textViewDescription);
                buttonJoin = itemView.findViewById(R.id.buttonJoinEvent);
                textViewJoined = itemView.findViewById(R.id.textViewAlreadyJoined);
            }
        }
    }

    private void joinEvent(String eventId, Map<String, Object> event, int position) {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> registration = new HashMap<>();
        registration.put("userId", userId);
        registration.put("eventId", eventId);
        registration.put("timestamp", FieldValue.serverTimestamp());

        db.collection("event_registrations").add(registration)
                .addOnSuccessListener(docRef -> {
                    // Update event participant count
                    db.collection("events").document(eventId)
                            .update("participantsJoined", FieldValue.increment(1))
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Joined successfully!", Toast.LENGTH_SHORT).show();
                                joinedEventIds.add(eventId);

                                // Update local data to reflect new count
                                long currentJoined = 0;
                                Object joinedObj = event.get("participantsJoined");
                                if (joinedObj instanceof Long) currentJoined = (Long) joinedObj;
                                else if (joinedObj instanceof Integer) currentJoined = (Integer) joinedObj;

                                event.put("participantsJoined", currentJoined + 1);
                                adapter.notifyItemChanged(position);
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to join: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().remove("last_role").apply();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}