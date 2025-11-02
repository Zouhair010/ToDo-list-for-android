package com.example.todo;

import android.app.DatePickerDialog;
import android.os.Bundle;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.ArrayAdapter;
import android.database.Cursor;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.Toast;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    // Database helper
    private TaskDatabaseHelper dbHelper;

    // TextView to show date and time
//    private TextView datetimeTextView;

    // Input field for new task
    private TextInputEditText taskTextInput;

    // ListView to display tasks
    private ListView listView;

    // List to store tasks and adapter for ListView
    public static ArrayList<String> tasks;
    public static ArrayAdapter<String> adapter;

    // Threads for updating time and loading tasks
    private static Thread datetimeThread;
    private static Thread loadTasksThread;
    private static String taskdate;

    // Method to continuously update the date/time every second
    private void showDateTime(TextView datetimeTextView) throws InterruptedException {
        while (true) {
            // Update UI on main thread
            runOnUiThread(() -> datetimeTextView.setText(
                    new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a").format(new GregorianCalendar().getTime())
            ));
            // Wait 1 second before next update
            Thread.sleep(1000);
        }
    }

    // Method to add a new task to the database
    private void addTask(TextInputEditText taskTextInput) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Insert the task using a placeholder to avoid SQL injection
        db.execSQL("INSERT INTO tasks(task,taskDate) VALUES(?,?)", new Object[]{taskTextInput.getText(),taskdate});
        taskdate = new SimpleDateFormat("dd-MM-yyyy").format(new GregorianCalendar().getTime());
        taskTextInput.setText(""); // Clear input after adding
        db.close();
    }

    // Method to remove a specific task from the database
    private void removeTask(String task) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM tasks WHERE task=(?)", new Object[]{task});
        db.close();
    }

    // Method to load all tasks from the database and display in ListView
    private void loadTasks() {
        tasks = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM tasks", null);
        while (cursor.moveToNext()) {
            // Get the "task" column value and add it to the list
            tasks.add(cursor.getString(cursor.getColumnIndexOrThrow("task")));
        }
        cursor.close();
        db.close();

        // Set up the ArrayAdapter and attach it to the ListView
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tasks);
        listView.setAdapter(adapter);
    }

    private void openDatePicker() {
        final Calendar caln = Calendar.getInstance();
        int year = caln.get(caln.YEAR);
        int month = caln.get(caln.MONTH);
        int day = caln.get(caln.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this, (view, selectedYear, selectedMonth, selectedDay) -> {
            String stringSelectedDay = (selectedDay>9)?""+selectedDay:"0"+selectedDay;
            String stringSelectedMonth = ((selectedMonth+1)>9)?""+(selectedMonth+1):"0"+(selectedMonth+1);
            String date = stringSelectedDay + "-" + stringSelectedMonth + "-" + selectedYear;
            taskdate = date;
        }
                , year, month, day
        );
        datePickerDialog.show();
    }

    // Helper class to manage the SQLite database
    private static class TaskDatabaseHelper extends SQLiteOpenHelper {
        public TaskDatabaseHelper(Context context) {
            super(context, "taskstodo.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // Create the tasks table if it doesn't exist
            db.execSQL("CREATE TABLE IF NOT EXISTS tasks (task TEXT NOT NULL, taskDate DATE)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // On upgrade: drop old table and recreate it
            db.execSQL("DROP TABLE IF EXISTS tasks");
            onCreate(db);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bind ListView from layout
        listView = findViewById(R.id.listView);

        // Set item click listener to remove a task when clicked
        listView.setOnItemClickListener((parent, view, position, id) -> {
            removeTask(tasks.get(position)); // Remove from DB
            Toast.makeText(this, "great you finish: " + tasks.get(position), Toast.LENGTH_SHORT).show();
            tasks.remove(position);          // Remove from list
            adapter.notifyDataSetChanged();  // Update ListView
        });

        // Initialize database helper
        dbHelper = new TaskDatabaseHelper(this);

        // Load tasks in a separate thread
        loadTasksThread = new Thread(){@Override public void run(){ loadTasks(); }};
        loadTasksThread.start();

        // Start a thread to continuously update date/time
        datetimeThread = new Thread(){@Override public void run(){ try {
            showDateTime(findViewById(R.id.datTimeTextView));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }}};
        datetimeThread.start();

        // Bind task input field and add button
        taskTextInput = findViewById(R.id.taskTextInput);

        findViewById(R.id.addBtn).setOnClickListener(v -> {
            addTask(taskTextInput); // Add task to DB
            loadTasks();            // Reload tasks to update ListView
        });
        findViewById(R.id.pickDateBtn).setOnClickListener(v -> {
            openDatePicker();
        });

    }
}