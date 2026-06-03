package com.example.utmnexevent;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ViewQRActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textViewEmpty;
    private QRAdapter adapter;
    private List<Map<String, Object>> registrationList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration registrationsListener;
    private Map<String, ListenerRegistration> eventListeners = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_qr);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.recyclerViewQrTickets);
        textViewEmpty = findViewById(R.id.textViewEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        registrationList = new ArrayList<>();
        adapter = new QRAdapter(registrationList);
        recyclerView.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.view_qr_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        loadMyRegistrations();
    }

    private void loadMyRegistrations() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        registrationsListener = db.collection("event_registrations")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error fetching tickets", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        // Clear existing event listeners to avoid duplicates
                        for (ListenerRegistration lr : eventListeners.values()) {
                            lr.remove();
                        }
                        eventListeners.clear();
                        registrationList.clear();
                        
                        if (value.isEmpty()) {
                            updateUI();
                        } else {
                            for (QueryDocumentSnapshot document : value) {
                                String eventId = document.getString("eventId");
                                String registrationId = document.getId();
                                fetchEventDetails(eventId, registrationId);
                            }
                        }
                    }
                });
    }

    private void fetchEventDetails(String eventId, String registrationId) {
        if (eventId == null) return;
        
        ListenerRegistration lr = db.collection("events").document(eventId)
                .addSnapshotListener((doc, error) -> {
                    if (doc != null && doc.exists()) {
                        String status = doc.getString("status");
                        
                        // Find if this registration is already in the list
                        int index = -1;
                        for (int i = 0; i < registrationList.size(); i++) {
                            if (registrationId.equals(registrationList.get(i).get("registrationId"))) {
                                index = i;
                                break;
                            }
                        }

                        if ("active".equals(status)) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("eventName", doc.getString("name"));
                            data.put("eventDate", doc.getString("date"));
                            data.put("eventTime", doc.getString("time"));
                            data.put("registrationId", registrationId);
                            
                            if (index == -1) {
                                registrationList.add(data);
                            } else {
                                registrationList.set(index, data);
                            }
                        } else {
                            // If status changed to something else (like completed), remove it
                            if (index != -1) {
                                registrationList.remove(index);
                            }
                        }
                        updateUI();
                    }
                });
        
        eventListeners.put(registrationId, lr);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registrationsListener != null) {
            registrationsListener.remove();
        }
        for (ListenerRegistration lr : eventListeners.values()) {
            lr.remove();
        }
    }

    private void updateUI() {
        if (registrationList.isEmpty()) {
            textViewEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textViewEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private class QRAdapter extends RecyclerView.Adapter<QRAdapter.ViewHolder> {
        private List<Map<String, Object>> tickets;

        public QRAdapter(List<Map<String, Object>> tickets) {
            this.tickets = tickets;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_qr_ticket, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> ticket = tickets.get(position);
            holder.textViewName.setText(String.valueOf(ticket.get("eventName")));
            holder.textViewDetails.setText(String.format(Locale.getDefault(), "%s | %s", ticket.get("eventDate"), ticket.get("eventTime")));

            String registrationId = (String) ticket.get("registrationId");
            generateQRCode(registrationId, holder.imageViewQr);
        }

        private void generateQRCode(String text, ImageView imageView) {
            MultiFormatWriter writer = new MultiFormatWriter();
            try {
                BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512);
                BarcodeEncoder encoder = new BarcodeEncoder();
                Bitmap bitmap = encoder.createBitmap(matrix);
                imageView.setImageBitmap(bitmap);
            } catch (WriterException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return tickets.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textViewName, textViewDetails;
            ImageView imageViewQr;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textViewName = itemView.findViewById(R.id.textViewQrEventName);
                textViewDetails = itemView.findViewById(R.id.textViewQrEventDetails);
                imageViewQr = itemView.findViewById(R.id.imageViewQrCode);
            }
        }
    }
}
