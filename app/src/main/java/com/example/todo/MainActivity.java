package com.example.todo;


import android.app.DatePickerDialog;

import android.os.Bundle;


import android.os.Handler;

import android.widget.ListView;

import android.widget.Switch;

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

import java.text.SimpleDateFormat;


import java.util.ArrayList;


// Main Activity for the ToDo application, handles UI and database interactions

public class MainActivity extends AppCompatActivity {

// Database helper instance to manage SQLite operations

    private TaskDatabaseHelper dbHelper;

// UI component for entering a new task description

    private TextInputEditText taskTextInput;

// UI component to display the list of tasks

    private ListView listView;

// UI component to switch between today's tasks and future tasks

    private Switch switchTaskButton;

// Static list to hold task strings for display

    public static ArrayList<String> tasks;

// Static adapter to link the task list to the ListView

    public static ArrayAdapter<String> adapter;

// Thread dedicated to continuously updating the date and time display

    private static Thread datetimeThread;

// Stores the date selected for a new task (defaults to current date)

    private static String taskdate;

// Flag to indicate which task list is currently displayed (true for today's tasks)

    private static boolean onTodaysTasks;


// Method to continuously update the date/time on a TextView every second,

// and provide a task reminder every hour (3600 seconds).

    private void showDateTime(TextView datetimeTextView) throws InterruptedException {

// Counter to track the number of seconds elapsed since the last hour marker (or start)

        int seconds = 0;

// Infinite loop to keep the time updated

        while (true) {

// Check if 3600 seconds (1 hour) have passed AND if there is at least one task in the list

            if (seconds % 3600 == 0 && tasks.size() > 0){

// Reset the counter after the hour check

                seconds = 0;

// Runnable to safely display the Toast notification on the main (UI) thread

                runOnUiThread(() -> {

// Show a Toast reminding the user of the very first task in the currently loaded list (tasks.get(0))

                    Toast.makeText(this, "remember you task is: \""+tasks.get(0)+"\"", Toast.LENGTH_SHORT).show();

                });

            }

// Increment the seconds counter for the next check

            seconds++;


// Runnable to safely update the UI component from a background thread

            runOnUiThread(() -> datetimeTextView.setText(

// Format the current date and time

                    new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a").format(new GregorianCalendar().getTime())

            ));

// Pause the thread for 1 second (1000 milliseconds) before the next iteration

            Thread.sleep(1000);

        }

    }

// Method to add a new task (with its date) to the SQLite database

    private void addTask(TextInputEditText taskTextInput) {

// 1. Retrieve the text input and convert it to a String

        String task = taskTextInput.getText().toString();

// 2. Input Validation: Check if the task string is not empty

        if (task.length() > 0){

// Get a writable database instance

            SQLiteDatabase db = dbHelper.getWritableDatabase();

// Insert the task and its date into the 'tasks' table.

// Using '?' as placeholders prevents SQL injection.

            db.execSQL("INSERT INTO tasks(task,taskDate) VALUES(?,?)", new Object[]{task,taskdate});

// Reset the task date back to the current date for the next new task default

            taskdate = new SimpleDateFormat("dd-MM-yyyy").format(new GregorianCalendar().getTime());

            taskTextInput.setText(""); // Clear the input field after successful addition

            db.close(); // Close the database connection

// Reload the appropriate list of tasks based on the current view setting

            if (onTodaysTasks){

                loadTasks(); // Load today's tasks

            }

            else {

                loadFutureTasks(); // Load future tasks

            }

        }

// If the task input is empty, nothing happens (task is not added).

    }


// Method to remove a specific task associated with the CURRENT date from the database

    private void removeTask(String task) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();

// Get today's date for the WHERE clause

        String currentdate = new SimpleDateFormat("dd-MM-yyyy").format(new GregorianCalendar().getTime());

// Execute DELETE query based on the task name and today's date

        db.execSQL("DELETE FROM tasks WHERE task=(?) AND taskDate=(?)", new Object[]{task,currentdate});

        db.close();

    }


// Method to remove a future task from the database.

// The input string (taskAndDate) contains both the task name and its date, separated by ":\n".

    private void removeFutureTask(String taskAndDate) {

// 1. Extract the Task Name: Get the substring from the start (index 0) up to the first colon (":").

        String task = taskAndDate.substring(0,taskAndDate.indexOf(":"));

// 2. Extract the Task Date: Get the substring starting 2 characters after the colon (":")

// to account for the newline character ("\n") and include the date till the end of the string.

        String date = taskAndDate.substring(taskAndDate.indexOf(":")+2,taskAndDate.length());

// 3. Get a writable instance of the SQLite database

        SQLiteDatabase db = dbHelper.getWritableDatabase();

// 4. Execute the SQL DELETE query

// The task is deleted where both the parsed task name and the parsed date match the records.

        db.execSQL("DELETE FROM tasks WHERE task=? And taskDate=(?)", new Object[]{task,date});

// 5. Close the database connection

        db.close();

    }


// Method to delete all tasks from the database whose scheduled date is in the past (before today)

    private void removePastTask() {

// 1. Get a writable instance of the SQLite database

        SQLiteDatabase db = dbHelper.getWritableDatabase();

// 2. Determine the current date in "dd-MM-yyyy" format

// This date serves as the cutoff point for defining "past" tasks.

        String currentdate = new SimpleDateFormat("dd-MM-yyyy").format(new GregorianCalendar().getTime());

// 3. Execute the SQL DELETE command

// This query deletes all rows from the 'tasks' table where the 'taskDate' is strictly less than the 'currentdate'.

        db.execSQL("DELETE FROM tasks WHERE taskDate<(?)", new Object[]{currentdate});

// 4. Close the database connection to release resources

        db.close();

    }


// Method to handle removal of a task based on the currently displayed list type

    private void removeSelectedListRow(String taskToDelete){

        if (onTodaysTasks){

// Task is from today's list

            Toast.makeText(this, "Great job, you've completd!: " + taskToDelete, Toast.LENGTH_SHORT).show();

            removeTask(taskToDelete); // Remove from DB using current date

            loadTasks(); // Reload today's tasks to update ListView

            if (tasks.size() > 0){

                Toast.makeText(this, "your next task is: " + tasks.get(0), Toast.LENGTH_SHORT).show();

            }else {

                Toast.makeText(this, "there's nothing next. ", Toast.LENGTH_SHORT).show();

            }

        }

        else {

// Task is from future list (contains task name and date)

            Toast.makeText(this, "the task has been cancelled.", Toast.LENGTH_SHORT).show();

            removeFutureTask(taskToDelete); // Remove from DB using parsed date

            loadFutureTasks(); // Reload future tasks to update ListView

        }

    }


// Method to load all tasks scheduled for the current day from the database and update the ListView

    private void loadTasks() {

        onTodaysTasks = true; // Set flag to indicate today's tasks are loaded

        tasks = new ArrayList<>(); // Clear and re-initialize the task list

        SQLiteDatabase db = dbHelper.getReadableDatabase();

// Get today's date for the query filter

        String currentdate = new SimpleDateFormat("dd-MM-yyyy").format(new GregorianCalendar().getTime());

// Query the database for tasks matching today's date

        Cursor cursor = db.rawQuery("SELECT * FROM tasks WHERE taskDate <= (?)", new String[]{currentdate});

        while (cursor.moveToNext()) {

// Get the "task" column value (index 0) and add it to the list

            tasks.add(cursor.getString(0));

// Original commented-out logic: tasks.add(cursor.getString(0)+":\n"+cursor.getString(1));

        }

        cursor.close();

        db.close();

// Set up the ArrayAdapter with the current task list and attach it to the ListView

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tasks);

        listView.setAdapter(adapter);

    }


// Method to load all tasks scheduled for a future date (after today) from the database

    private void loadFutureTasks(){

        onTodaysTasks = false; // Set flag to indicate future tasks are loaded

        tasks = new ArrayList<>(); // Clear and re-initialize the task list

        SQLiteDatabase db = dbHelper.getReadableDatabase();

// Get today's date for the query filter

        String currentdate = new SimpleDateFormat("dd-MM-yyyy").format(new GregorianCalendar().getTime());

// Query the database for tasks with a date greater than today

        Cursor cursor = db.rawQuery("SELECT * FROM tasks WHERE taskDate > (?)", new String[]{currentdate});

        while (cursor.moveToNext()) {

// Add both the task name (index 0) and the date (index 1) to the list, separated

            tasks.add(cursor.getString(0)+":\n"+cursor.getString(1));

        }

        cursor.close();

        db.close();

// Set up the ArrayAdapter with the future task list and attach it to the ListView

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tasks);

        listView.setAdapter(adapter);

    }


// Method to show a DatePickerDialog for the user to select a date for a new task

    private void openDatePicker() {

// Get the current date to initialize the DatePicker

        final Calendar caln = Calendar.getInstance();

        int year = caln.get(caln.YEAR);

        int month = caln.get(caln.MONTH);

        int day = caln.get(caln.DAY_OF_MONTH);

// Create and configure the DatePickerDialog

        DatePickerDialog datePickerDialog = new DatePickerDialog(

                this, (view, selectedYear, selectedMonth, selectedDay) -> {

// Format the selected day to be two digits

            String stringSelectedDay = (selectedDay>9)?""+selectedDay:"0"+selectedDay;

// Format the selected month (Calendar month is 0-indexed) to be two digits

            String stringSelectedMonth = ((selectedMonth+1)>9)?""+(selectedMonth+1):"0"+(selectedMonth+1);

// Construct the date string in "dd-MM-yyyy" format

            String date = stringSelectedDay + "-" + stringSelectedMonth + "-" + selectedYear;

            taskdate = date; // Update the static task date variable

        }

                , year, month, day // Initial date values

        );

        datePickerDialog.show(); // Display the dialog

    }


// Nested Helper class to manage the SQLite database creation and versioning

    private static class TaskDatabaseHelper extends SQLiteOpenHelper {

// Constructor for the database helper

        public TaskDatabaseHelper(Context context) {

            super(context, "taskstodo.db", null, 1); // Database name and version

        }


        @Override

// Called when the database is created for the first time

        public void onCreate(SQLiteDatabase db) {

// SQL command to create the 'tasks' table with 'task' (text) and 'taskDate' (date) columns

            db.execSQL("CREATE TABLE IF NOT EXISTS tasks (task TEXT NOT NULL, taskDate DATE)");

        }


        @Override

// Called when the database needs to be upgraded (version change)

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

// Drop the existing table and recreate it, losing old data

            db.execSQL("DROP TABLE IF EXISTS tasks");

            onCreate(db);

        }

    }


    @Override

// Called when the activity is first created

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

// Enable edge-to-edge display

        EdgeToEdge.enable(this);

// Set the activity content from the layout XML

        setContentView(R.layout.activity_main);

// Handle window insets (like system bars) for proper view padding

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);

            return insets;

        });


// Initialize taskdate to the current date in the required format

        taskdate = new SimpleDateFormat("dd-MM-yyyy").format(new GregorianCalendar().getTime());


// Bind the ListView component from the layout

        listView = findViewById(R.id.listView);

// Set an item click listener on the ListView to handle task removal

        listView.setOnItemClickListener((parent, view, position, id) -> {

// Get the task string at the clicked position and pass it for removal

            removeSelectedListRow(tasks.get(position));

        });


// Initialize the database helper

        dbHelper = new TaskDatabaseHelper(this);


// Bind the task input field

        taskTextInput = findViewById(R.id.taskTextInput);

// Set click listener for the "Add Task" button

        findViewById(R.id.addBtn).setOnClickListener(v -> {

            addTask(taskTextInput); // Call method to add the task

        });


// Set click listener for the "Pick Date" button

        findViewById(R.id.pickDateBtn).setOnClickListener(v -> {

            openDatePicker(); // Call method to open the date picker dialog

        });


// Bind the task switch button

        switchTaskButton = findViewById(R.id.switchTasksBtn);

// Set a listener for when the switch state changes

        switchTaskButton.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {

// If checked (ON state), show future tasks

                Toast.makeText(this, "future's tasks", Toast.LENGTH_SHORT).show();

                switchTaskButton.setText("Show today's tasks");

                loadFutureTasks();

            } else {

// If unchecked (OFF state), show today's tasks

                Toast.makeText(this, "today's tasks", Toast.LENGTH_SHORT).show();

                switchTaskButton.setText("Show future's tasks");

                loadTasks();

            }

        });


// Initial load of today's tasks when the activity starts (since switch is off by default)

        loadTasks();

        removePastTask();


// Start a new background thread to run the date/time updater

        datetimeThread = new Thread(){@Override public void run(){ try {

// Call the method to continuously update the time TextView

            showDateTime(findViewById(R.id.datTimeTextView));

        } catch (InterruptedException e) {

// Print the stack trace if the thread is interrupted

            e.printStackTrace();

        }}};

        datetimeThread.start(); // Start the background thread

    }


    @Override

// Called when the activity is finishing (e.g., user presses back or system destroys it).

// This method is essential for cleaning up resources, particularly background threads.

    protected void onDestroy(){

        super.onDestroy(); // Always call the superclass implementation first

// Check if the background thread (used for date/time updates) was created and is currently running

        if (datetimeThread != null && datetimeThread.isAlive()){

// 1. Request the thread to stop execution gracefully

// This sets the thread's interrupted status flag. The 'showDateTime' loop should check this flag.

            datetimeThread.interrupt();

            try {

// 2. Wait for the thread to finish executing (join) for up to 1 second (1000 ms)

// This ensures the main thread pauses briefly to let the background thread shut down cleanly.

                datetimeThread.join(1000);

            }catch (InterruptedException e){

// 3. If the main thread itself is interrupted while waiting for the background thread to join,

// re-interrupt the main thread to preserve the interrupted status.

                Thread.currentThread().interrupt();

            }

        }

// 4. Set the reference to null after shutdown or if it was never started

        datetimeThread = null;

    }

}