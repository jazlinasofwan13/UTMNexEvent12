package com.example.utmnexevent;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminManageEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textViewEmpty;
    private EventAdapter adapter;
    private List<Map<String, Object>> eventList;
    private FirebaseFirestore db;
    private ListenerRegistration eventsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_manage_events);

        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerViewAllEvents);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();
        adapter = new EventAdapter(eventList);
        recyclerView.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.admin_manage_events_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        loadAllEvents();
    }

    private void loadAllEvents() {
        eventsListener = db.collection("events")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error fetching events", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        eventList.clear();
                        for (QueryDocumentSnapshot document : value) {
                            Map<String, Object> data = document.getData();
                            data.put("id", document.getId());
                            eventList.add(data);
                        }

                        if (eventList.isEmpty()) {
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
        if (eventsListener != null) {
            eventsListener.remove();
        }
    }

    private class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
        private List<Map<String, Object>> events;

        public EventAdapter(List<Map<String, Object>> events) {
            this.events = events;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_admin, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> event = events.get(position);
            String eventId = (String) event.get("id");
            
            holder.textViewName.setText(String.valueOf(event.get("name")));
            holder.textViewDate.setText(String.valueOf(event.get("date")));
            holder.textViewTime.setText(String.valueOf(event.get("time")));
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

            holder.buttonViewParticipants.setOnClickListener(v -> {
                Intent intent = new Intent(AdminManageEventsActivity.this, AdminEventParticipantsActivity.class);
                intent.putExtra("eventId", eventId);
                intent.putExtra("eventName", String.valueOf(event.get("name")));
                startActivity(intent);
            });

            holder.buttonEdit.setOnClickListener(v -> showEditDialog(event, position));
            holder.buttonCancel.setOnClickListener(v -> showCancelConfirmation(eventId, position));
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewName, textViewDate, textViewTime, textViewParticipantInfo, textViewDescription;
            View buttonEdit, buttonCancel, buttonViewParticipants;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewName = itemView.findViewById(R.id.textViewEventName);
                textViewDate = itemView.findViewById(R.id.textViewEventDate);
                textViewTime = itemView.findViewById(R.id.textViewEventTime);
                textViewParticipantInfo = itemView.findViewById(R.id.textViewParticipantInfo);
                textViewDescription = itemView.findViewById(R.id.textViewDescription);
                buttonEdit = itemView.findViewById(R.id.buttonEditEvent);
                buttonCancel = itemView.findViewById(R.id.buttonCancelEvent);
                buttonViewParticipants = itemView.findViewById(R.id.buttonViewParticipants);
            }
        }
    }

    private void showCancelConfirmation(String eventId, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("As Admin, you are about to permanently delete this event. Continue?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("events").document(eventId).delete()
                            .addOnSuccessListener(aVoid -> {
                                eventList.remove(position);
                                adapter.notifyItemRemoved(position);
                                Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog(Map<String, Object> event, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Event (Admin)");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_event, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.editEventName);
        EditText editDate = dialogView.findViewById(R.id.editEventDate);
        EditText editTime = dialogView.findViewById(R.id.editEventTime);
        EditText editDescription = dialogView.findViewById(R.id.editEventDescription);
        EditText editLimit = dialogView.findViewById(R.id.editParticipantLimit);

        editName.setText(String.valueOf(event.get("name")));
        editDate.setText(String.valueOf(event.get("date")));
        editTime.setText(String.valueOf(event.get("time")));
        editDescription.setText(String.valueOf(event.get("description")));
        editLimit.setText(String.valueOf(event.get("participantLimit")));

        editDate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, y, m, d) -> 
                editDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", d, m + 1, y)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        editTime.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, h, min) -> 
                editTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, min)),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });

        builder.setPositiveButton("Save Changes", (dialog, which) -> {
            String newName = editName.getText().toString().trim();
            String newDate = editDate.getText().toString().trim();
            String newTime = editTime.getText().toString().trim();
            String newDesc = editDescription.getText().toString().trim();
            String limitStr = editLimit.getText().toString().trim();

            if (newName.isEmpty() || newDate.isEmpty() || newTime.isEmpty() || newDesc.isEmpty() || limitStr.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            int newLimit = Integer.parseInt(limitStr);
            String eventId = (String) event.get("id");

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("date", newDate);
            updates.put("time", newTime);
            updates.put("description", newDesc);
            updates.put("participantLimit", newLimit);

            db.collection("events").document(eventId).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        event.putAll(updates);
                        adapter.notifyItemChanged(position);
                        Toast.makeText(this, "Event updated by Admin!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
