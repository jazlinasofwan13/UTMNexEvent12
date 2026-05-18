package com.example.utmnexevent;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddEventActivity extends AppCompatActivity {

    private TextInputEditText editTextEventName, editTextEventDate, editTextEventTime, editTextParticipantLimit, editTextEventDescription;
    private MaterialButton buttonSubmitEvent;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_event);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.add_event_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTextEventName = findViewById(R.id.editTextEventName);
        editTextEventDate = findViewById(R.id.editTextEventDate);
        editTextEventTime = findViewById(R.id.editTextEventTime);
        editTextParticipantLimit = findViewById(R.id.editTextParticipantLimit);
        editTextEventDescription = findViewById(R.id.editTextEventDescription);
        buttonSubmitEvent = findViewById(R.id.buttonSubmitEvent);

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());

        // Setup Pickers - aggressive listeners
        TextInputLayout dateLayout = findViewById(R.id.eventDateLayout);
        View.OnClickListener dateListener = v -> showDatePicker();
        editTextEventDate.setOnClickListener(dateListener);
        dateLayout.setStartIconOnClickListener(dateListener);

        TextInputLayout timeLayout = findViewById(R.id.eventTimeLayout);
        View.OnClickListener timeListener = v -> showTimePicker();
        editTextEventTime.setOnClickListener(timeListener);
        timeLayout.setStartIconOnClickListener(timeListener);

        buttonSubmitEvent.setOnClickListener(v -> submitEvent());
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> 
                        editTextEventDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, monthOfYear + 1, year1)),
                year, month, day);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> 
                        editTextEventTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1)),
                hour, minute, true);
        timePickerDialog.show();
    }

    private void submitEvent() {
        if (editTextEventName.getText() == null || editTextEventDate.getText() == null || 
            editTextEventTime.getText() == null || editTextParticipantLimit.getText() == null ||
            editTextEventDescription.getText() == null) return;

        String name = editTextEventName.getText().toString().trim();
        String date = editTextEventDate.getText().toString().trim();
        String time = editTextEventTime.getText().toString().trim();
        String limitStr = editTextParticipantLimit.getText().toString().trim();
        String description = editTextEventDescription.getText().toString().trim();

        if (name.isEmpty() || date.isEmpty() || time.isEmpty() || limitStr.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        int limit = Integer.parseInt(limitStr);
        String organizerId = mAuth.getCurrentUser().getUid();

        buttonSubmitEvent.setEnabled(false);

        Map<String, Object> event = new HashMap<>();
        event.put("name", name);
        event.put("date", date);
        event.put("time", time);
        event.put("participantLimit", limit);
        event.put("description", description);
        event.put("organizerId", organizerId);
        event.put("participantsJoined", 0);
        event.put("status", "active");

        db.collection("events")
                .add(event)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddEventActivity.this, "Event created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddEventActivity.this, "Error creating event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    buttonSubmitEvent.setEnabled(true);
                });
    }
}
