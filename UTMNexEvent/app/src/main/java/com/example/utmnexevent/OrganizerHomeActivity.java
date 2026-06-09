package com.example.utmnexevent;

import android.app.AlertDialog;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrganizerHomeActivity extends AppCompatActivity {

    private TextView textViewOrgHomeTitle, textViewNoEvents;
    private RecyclerView recyclerViewUpcomingEvents;
    private EventAdapter adapter;
    private List<Map<String, Object>> eventList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration userListener, eventsListener;

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() == null) {
                    Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT).show();
                } else {
                    processAttendance(result.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_organizer_home);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        textViewOrgHomeTitle = findViewById(R.id.textViewOrgHomeTitle);
        textViewNoEvents = findViewById(R.id.textViewNoEvents);
        recyclerViewUpcomingEvents = findViewById(R.id.recyclerViewUpcomingEvents);

        recyclerViewUpcomingEvents.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();
        adapter = new EventAdapter(eventList);
        recyclerViewUpcomingEvents.setAdapter(adapter);
        
        loadUserData();
        loadUpcomingEvents();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.organizer_home_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.buttonManageEvents).setOnClickListener(v -> {
            Intent intent = new Intent(OrganizerHomeActivity.this, ManageEventsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.buttonScanAttendance).setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan Participant QR Ticket");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            barcodeLauncher.launch(options);
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
                    Intent intent = new Intent(OrganizerHomeActivity.this, OrganizerProfileActivity.class);
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
                                textViewOrgHomeTitle.setText("Welcome, " + fullName);
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
        if (eventsListener != null) {
            eventsListener.remove();
        }
    }

    private void loadUpcomingEvents() {
        eventsListener = db.collection("events")
                .whereEqualTo("status", "active")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        eventList.clear();
                        for (QueryDocumentSnapshot document : value) {
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_simple, parent, false);
            return new EventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
            Map<String, Object> event = events.get(position);
            
            holder.textViewName.setText(String.valueOf(event.get("name")));
            
            String venue = (String) event.get("venue");
            holder.textViewVenue.setText(venue != null ? venue : "No venue specified");

            String date = String.valueOf(event.get("date"));
            String endDate = (String) event.get("endDate");
            if (endDate != null && !endDate.isEmpty() && !endDate.equals(date)) {
                holder.textViewDate.setText(date + " - " + endDate);
            } else {
                holder.textViewDate.setText(date);
            }

            String time = String.valueOf(event.get("time"));
            String endTime = (String) event.get("endTime");
            if (endTime != null && !endTime.isEmpty()) {
                holder.textViewTime.setText(time + " - " + endTime);
            } else {
                holder.textViewTime.setText(time);
            }

            holder.textViewDescription.setText(String.valueOf(event.get("description")));
            
            long joined = 0;
            Object joinedObj = event.get("participantsJoined");
            if (joinedObj instanceof Long) joined = (Long) joinedObj;
            else if (joinedObj instanceof Integer) joined = (Integer) joinedObj;
            
            long limit = 0;
            Object limitObj = event.get("participantLimit");
            if (limitObj instanceof Long) limit = (Long) limitObj;
            else if (limitObj instanceof Integer) limit = (Integer) limitObj;
            
            holder.textViewParticipantInfo.setText(String.format(Locale.getDefault(), "Participants: %d / %d", joined, limit));
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        class EventViewHolder extends RecyclerView.ViewHolder {
            TextView textViewName, textViewVenue, textViewDate, textViewTime, textViewParticipantInfo, textViewDescription;

            public EventViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewName = itemView.findViewById(R.id.textViewEventName);
                textViewVenue = itemView.findViewById(R.id.textViewEventVenue);
                textViewDate = itemView.findViewById(R.id.textViewEventDate);
                textViewTime = itemView.findViewById(R.id.textViewEventTime);
                textViewParticipantInfo = itemView.findViewById(R.id.textViewParticipantInfo);
                textViewDescription = itemView.findViewById(R.id.textViewDescription);
            }
        }
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().remove("last_role").apply();

        Intent intent = new Intent(OrganizerHomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void processAttendance(String registrationId) {
        db.collection("event_registrations").document(registrationId).get()
                .addOnSuccessListener(regDoc -> {
                    if (regDoc.exists()) {
                        String eventId = regDoc.getString("eventId");
                        String userId = regDoc.getString("userId");
                        Boolean alreadyAttended = regDoc.getBoolean("attended");

                        if (alreadyAttended != null && alreadyAttended) {
                            db.collection("events").document(eventId != null ? eventId : "").get().addOnSuccessListener(eventDoc -> {
                                String eventName = eventDoc.getString("name");
                                db.collection("users").document(userId != null ? userId : "").get().addOnSuccessListener(userDoc -> {
                                    String userName = userDoc.getString("fullName");
                                    new AlertDialog.Builder(this)
                                            .setTitle("Already Checked In")
                                            .setMessage("User: " + userName + "\nhas already been marked as present for\nEvent: " + eventName)
                                            .setPositiveButton("OK", null)
                                            .show();
                                });
                            });
                            return;
                        }
                        
                        // Fetch event and user name for a better success message
                        db.collection("events").document(eventId != null ? eventId : "").get().addOnSuccessListener(eventDoc -> {
                            String eventName = eventDoc.getString("name");
                            db.collection("users").document(userId != null ? userId : "").get().addOnSuccessListener(userDoc -> {
                                String userName = userDoc.getString("fullName");
                                
                                // Mark as attended
                                db.collection("event_registrations").document(registrationId)
                                        .update("attended", true)
                                        .addOnSuccessListener(aVoid -> {
                                            new AlertDialog.Builder(this)
                                                    .setTitle("Attendance Marked!")
                                                    .setMessage("User: " + userName + "\nEvent: " + eventName)
                                                    .setPositiveButton("OK", null)
                                                    .show();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show());
                            });
                        });
                    } else {
                        Toast.makeText(this, "Invalid QR Code: Ticket not found", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
