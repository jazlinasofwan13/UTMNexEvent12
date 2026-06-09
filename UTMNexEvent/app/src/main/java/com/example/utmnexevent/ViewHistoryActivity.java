package com.example.utmnexevent;

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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textViewEmpty;
    private HistoryAdapter adapter;
    private List<Map<String, Object>> historyList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration historyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_history);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.recyclerViewHistory);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList);
        recyclerView.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.view_history_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        loadEventHistory();
    }

    private void loadEventHistory() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        historyListener = db.collection("event_registrations")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading history", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        historyList.clear();
                        if (value.isEmpty()) {
                            updateUI();
                        } else {
                            for (QueryDocumentSnapshot document : value) {
                                String eventId = document.getString("eventId");
                                Boolean attendedObj = document.getBoolean("attended");
                                boolean attended = attendedObj != null && attendedObj;
                                fetchEventDetails(eventId, attended);
                            }
                        }
                    }
                });
    }

    private void fetchEventDetails(String eventId, boolean attended) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("name", doc.getString("name"));
                        data.put("venue", doc.getString("venue"));
                        data.put("date", doc.getString("date"));
                        data.put("endDate", doc.getString("endDate"));
                        data.put("time", doc.getString("time"));
                        data.put("endTime", doc.getString("endTime"));
                        data.put("attended", attended);
                        historyList.add(data);
                        updateUI();
                    }
                });
    }

    private void updateUI() {
        if (historyList.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textViewEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (historyListener != null) {
            historyListener.remove();
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<Map<String, Object>> items;

        public HistoryAdapter(List<Map<String, Object>> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> item = items.get(position);
            holder.name.setText(String.valueOf(item.get("name")));
            
            String venue = (String) item.get("venue");
            holder.venue.setText(venue != null ? venue : "No venue specified");

            String date = String.valueOf(item.get("date"));
            String endDate = (String) item.get("endDate");
            if (endDate != null && !endDate.isEmpty() && !endDate.equals(date)) {
                holder.date.setText(date + " - " + endDate);
            } else {
                holder.date.setText(date);
            }

            String time = String.valueOf(item.get("time"));
            String endTime = (String) item.get("endTime");
            if (endTime != null && !endTime.isEmpty()) {
                holder.time.setText(time + " - " + endTime);
            } else {
                holder.time.setText(time);
            }

            Boolean attendedObj = (Boolean) item.get("attended");
            boolean attended = attendedObj != null && attendedObj;
            if (attended) {
                holder.status.setText("Present");
                holder.status.setTextColor(0xFF2E7D32); // Green
            } else {
                holder.status.setText("Not Present");
                holder.status.setTextColor(0xFFD32F2F); // Red
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, venue, date, time, status;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.textViewEventName);
                venue = itemView.findViewById(R.id.textViewEventVenue);
                date = itemView.findViewById(R.id.textViewEventDate);
                time = itemView.findViewById(R.id.textViewEventTime);
                status = itemView.findViewById(R.id.textViewAttendanceStatus);
            }
        }
    }
}
