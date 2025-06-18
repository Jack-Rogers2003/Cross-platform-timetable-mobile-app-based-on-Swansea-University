package com.example.mobileapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SocietyBookingApplicationActivity: AppCompatActivity() {

    var startTime = 0
    var eventLength = 0
    private var selectedSpecificDate = ""
    var selectedDay = ""
    var campus = ""
    private var recurring = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.society_booking)

        val reoccurringButton = findViewById<Button>(R.id.isEventReoccurring)
        reoccurringButton.setOnClickListener{eventRecurring()}

        val selectDateButton = findViewById<Button>(R.id.selectSpecificDate)
        selectDateButton.setOnClickListener{selectFromCalender()}

        val submitButton = findViewById<Button>(R.id.submiRequest)
        submitButton.setOnClickListener{submitButtonPressed()}

        val exitButton = findViewById<Button>(R.id.exitButtonSociety)
        exitButton.setOnClickListener{exitButtonClicked()}


        val daySpinner = findViewById<Spinner>(R.id.selectDay)
        val startTimeSpinner = findViewById<Spinner>(R.id.startTime)
        val eventLengthSpinner = findViewById<Spinner>(R.id.eventLength)
        val campusSpinner = findViewById<Spinner>(R.id.selectCampus)


        val dayAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.daysOfWeek,
            android.R.layout.simple_spinner_item
        )
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        daySpinner.adapter = dayAdapter

        daySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Get the selected item (it's a String at this point)
                val selectedItem = parent.getItemAtPosition(position) as String

                // Convert the selected string to an integer and update the variable
                selectedDay = selectedItem

                val startTimeAdapter = ArrayAdapter(
                    this@SocietyBookingApplicationActivity,
                    android.R.layout.simple_spinner_item,
                    getStartTimes()
                )
                startTimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                startTimeSpinner.adapter = startTimeAdapter
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Optional: handle when nothing is selected (e.g., when the spinner is empty)
            }
        }

        startTimeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Get the selected item (it's a String at this point)
                startTime = (parent.getItemAtPosition(position) as String).split(":")[0].toInt()
                // Convert the selected string to an integer and update the variable
                val eventLengthAdapter = ArrayAdapter(
                    this@SocietyBookingApplicationActivity,
                    android.R.layout.simple_spinner_item,
                    allowedEventTimes()
                )
                eventLengthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                eventLengthSpinner.adapter = eventLengthAdapter
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Optional: handle when nothing is selected (e.g., when the spinner is empty)
            }
        }

        eventLengthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {

                eventLength = (parent.getItemAtPosition(position) as Int)

            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Optional: handle when nothing is selected (e.g., when the spinner is empty)
            }
        }

        val campusAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            arrayOf("Singleton Campus", "Bay Campus")
            )
        campusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        campusSpinner.adapter = campusAdapter

        campusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {

                campus = parent.getItemAtPosition(position) as String

            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Optional: handle when nothing is selected (e.g., when the spinner is empty)
            }
        }

        findViewById<Button>(R.id.selectSpecificDate).visibility = View.VISIBLE
        findViewById<Spinner>(R.id.selectDay).visibility = View.GONE
        findViewById<TextView>(R.id.selectDayHeaderText).visibility = View.GONE

    }

    private fun getStartTimes(): Array<String> {
        return when (selectedDay) {
            "Wednesday" -> {
                arrayOf("13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00")
            }
            "Saturday", "Sunday" -> {
                arrayOf(
                    "09:00",
                    "10:00",
                    "11:00",
                    "12:00",
                    "13:00",
                    "14:00",
                    "15:00",
                    "16:00",
                    "17:00",
                    "18:00",
                    "19:00",
                    "20:00"
                )
            }
            else -> arrayOf("18:00", "19:00", "20:00")
        }
    }

    private fun allowedEventTimes(): Array<Int> {
        return (1..(21 - startTime)).take(3).toTypedArray()
    }

    private fun submitButtonPressed() {
        val db = UserInformation.getDatabase()
        val size = findViewById<EditText>(R.id.sizeOfEvent).text.toString().toIntOrNull() ?: 0
        val hash =
            (UserInformation.getCurrentUserID().toString() + System.currentTimeMillis()).hashCode()
                .toString()
        var username: String
        GlobalScope.launch(Dispatchers.Main) {
            username = UserInformation.getUsername().toString()
            if (recurring) {
                if (campus.isNotEmpty() && selectedDay.isNotEmpty() && startTime != 0 && eventLength != 0 && size > 0) {
                    val data = mapOf(
                        "recurring" to "1",
                        "name" to username,
                        "campus" to campus,
                        "selectedDay" to selectedDay,
                        "startTime" to startTime,
                        "eventLength" to eventLength,
                        "size" to size
                    )
                    db.reference.child("Bookings")
                        .child(username).child(hash)
                        .setValue(data)
                    buildAlertBox("Submitted!")

                } else {
                    buildAlertBox("Missing Information")
                }
            } else {
                if (campus.isNotEmpty() && selectedSpecificDate.isNotEmpty() && startTime != 0 && eventLength != 0 && size > 0) {

                    val dateParts = selectedSpecificDate.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val day = String.format("%02d", dateParts.get(0).toInt())
                    val month = String.format("%02d", dateParts.get(1).toInt())
                    val year: String = dateParts[2]


                    val formattedDate = "$day/$month/$year"
                    val data = mapOf(
                        "recurring" to "0",
                        "name" to username,
                        "campus" to campus,
                        "selectedSpecificDate" to formattedDate,
                        "startTime" to startTime,
                        "eventLength" to eventLength,
                        "size" to size
                    )
                    db.reference.child("Bookings")
                        .child(username).child(hash)
                        .setValue(data)
                    buildAlertBox("Submitted!")

                } else {
                    buildAlertBox("Missing Information")
                }
            }
        }
    }

    private fun exitButtonClicked() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun buildAlertBox(text: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Info")
        builder.setMessage(text)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun selectFromCalender() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                // Handle the selected date
                selectedSpecificDate = "$dayOfMonth/${month + 1}/$year"
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                selectedDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(dateFormat.parse(selectedSpecificDate))

                val startTimeAdapter = ArrayAdapter(
                    this@SocietyBookingApplicationActivity,
                    android.R.layout.simple_spinner_item,
                    getStartTimes()
                )
                val startTimeSpinner = findViewById<Spinner>(R.id.startTime)
                startTimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                startTimeSpinner.adapter = startTimeAdapter
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set the date range
        val minDate = Calendar.getInstance().apply {
            set(2025, Calendar.SEPTEMBER, 29)  // Start Date: 01/01/23
        }
        val maxDate = Calendar.getInstance().apply {
            set(2025, Calendar.DECEMBER, 12)  // End Date: 31/12/25
        }

        datePickerDialog.datePicker.minDate = minDate.timeInMillis
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

        datePickerDialog.show()
    }

    private fun eventRecurring() {
        recurring = !recurring

        if (recurring) {
            findViewById<Button>(R.id.selectSpecificDate).visibility = View.GONE
            findViewById<Spinner>(R.id.selectDay).visibility = View.VISIBLE
            findViewById<TextView>(R.id.selectDayHeaderText).visibility = View.VISIBLE
        } else {
            findViewById<Button>(R.id.selectSpecificDate).visibility = View.VISIBLE
            findViewById<Spinner>(R.id.selectDay).visibility = View.GONE
            findViewById<TextView>(R.id.selectDayHeaderText).visibility = View.GONE
        }
    }

}