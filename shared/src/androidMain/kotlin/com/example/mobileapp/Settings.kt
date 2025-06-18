package com.example.mobileapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mrudultora.colorpicker.ColorPickerPopUp
import com.mrudultora.colorpicker.ColorPickerPopUp.OnPickColorListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import android.app.AlertDialog
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit


class Settings: AppCompatActivity() {

    private lateinit var downloadDialog: AlertDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_screen)
        val btnBack = findViewById<Button>(R.id.settingsBackButton)
        btnBack.setOnClickListener{backButtonClicked()}

        val downloadBtn = findViewById<Button>(R.id.settingsDownloadButton)
        downloadBtn.setOnClickListener{getModules()}

        val modules = getSharedPreferences("modules", Context.MODE_PRIVATE).getString("modules", "")?.split(",")
        val buttonContainer: LinearLayout = findViewById(R.id.buttonContainer)

        if (modules != null) {
            for (module in modules) {
                // Create a new button
                val button = Button(this)
                button.text = module  // Set the text of the button as the list item

                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, // Width: Full width of the container
                    LinearLayout.LayoutParams.WRAP_CONTENT  // Height: Based on content
                )

                layoutParams.setMargins(0, 10, 0, 10)  // Adjust top and bottom margins
                button.layoutParams = layoutParams

                // Set button gravity to center
                button.gravity = Gravity.CENTER

                // Optionally, set additional properties for the button
                button.setBackgroundColor(Color.BLACK)  // Set background color
                button.setTextColor(Color.WHITE)
                button.setOnClickListener {setColour(module)}
                button.scaleY = -1f

                buttonContainer.addView(button, 0)
            }
        }

    }


    private fun getModules() {
        val dialogView = layoutInflater.inflate(R.menu.download_dialogue, null)
        downloadDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        downloadDialog.show()


        CoroutineScope(Dispatchers.IO).launch {
            try {

                val modulesToGet = getSharedPreferences("modules", Context.MODE_PRIVATE).getString("modules", "")

                withTimeout(60000) {
                    val client = OkHttpClient.Builder()
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build()


                    //val url = "http://10.0.2.2:8080/getModulesForLocalSave/$modulesToGet"



                    val url = "http://ec2-13-49-49-156.eu-north-1.compute.amazonaws.com:8080/getModulesForLocalSave/$modulesToGet"

                    // Create a GET request
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .build()

                    // Execute the request and handle the response
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            println("Unexpected response: ${response.code}")
                        } else {
                            val responseBody = response.body?.string()

                            println(responseBody)

                            val events = ArrayList<String>()
                            val jsonArray = JSONArray(responseBody)

                            for (i in 0 until jsonArray.length()) {
                                events.add(jsonArray.getString(i))
                            }
                            withContext(Dispatchers.Main) {
                                // Show error message on UI if needed
                                writeToFile(events)
                            }
                        }
                    }
                }
            }  catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Settings, "Network Error", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    println("Unexpected error: ${e.message}")
                }
            }
        }
    }

    private fun writeToFile(events: ArrayList<String>) {
        val ranges = listOf(
            "30-09-2024" to "08-12-2024",
            "27-01-2025" to "07-04-2025"
        )

        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        val combinedList = ranges.flatMap { (start, end) ->
            val startDate = LocalDate.parse(start, formatter)
            val endDate = LocalDate.parse(end, formatter)
            generateSequence(startDate) { it.plusDays(1) }
                .takeWhile { !it.isAfter(endDate) }
                .toList()
        }

        val weekStarts = combinedList
            .filter { it.dayOfWeek == DayOfWeek.MONDAY }
            .map { it.format(formatter) }

        println(weekStarts)

        val weekArray: ArrayList<Pair<String, ArrayList<String>>> = ArrayList()


        val file = File(this.filesDir, "downloadedTimetable.txt")

        for (date: String in weekStarts) {
            weekArray.add(Pair(date, ArrayList()))
        }

        for (entry in events) {
            val (module, eventType, timeRange, day, lecturer, room, date) = entry.toTuple7("$$$")
            for (week in weekArray) {
                if (isDateInRange(week.first, date)) {
                    week.second.add("$module$$$$eventType$$$$timeRange$$$$day$$$$lecturer$$$$room")
                }
            }
        }

        file.writeText("")
        for (week in weekArray) {
            file.appendText(week.first + "\n")
            for (event in week.second) {
                file.appendText(event + "\n")
            }
            file.appendText("\n")
        }

        val loadingText = downloadDialog.findViewById<TextView>(R.id.loadingText)
        val progressBar = downloadDialog.findViewById<ProgressBar>(R.id.progressBar)
        val okButton = downloadDialog.findViewById<Button>(R.id.okButton)

        progressBar.visibility = View.GONE
        loadingText.text = "Downloaded"
        okButton.visibility = View.VISIBLE

        okButton.setOnClickListener {
            downloadDialog.dismiss()
        }
    }

    private fun isDateInRange(startOfWeek: String, dayToCheck: String): Boolean {
        val formatter = DateTimeFormatter.ofPattern("d-M-yyyy")

        val startDate = LocalDate.parse(startOfWeek, formatter)
        val endDate = startDate.plusDays(6)
        val checkDate = LocalDate.parse(dayToCheck, formatter)

        return !checkDate.isBefore(startDate) && !checkDate.isAfter(endDate)
    }

    private fun String.toTuple7(delimiter: String): Tuple7<String, String, String, String, String, String, String> {
        val splitList = this.split(delimiter)
        if (splitList.size != 7) throw IllegalArgumentException("String must have exactly 7 elements after splitting.")
        return Tuple7(splitList[0], splitList[1], splitList[2], splitList[3], splitList[4], splitList[5], splitList[6])
    }

    data class Tuple7<A, B, C, D, E, F, G>(
        val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G
    )

    private fun backButtonClicked() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun setColour(module: String) {
        println(module)
        val colorPickerPopUp = ColorPickerPopUp(this) // Pass the context.
        colorPickerPopUp.setShowAlpha(true) // By default show alpha is true.
            .setDefaultColor(Color.WHITE) // By default red color is set.
            .setDialogTitle("Pick a Color")
            .setOnPickColorListener(object : OnPickColorListener {
                override fun onColorPicked(color: Int) {

                    getSharedPreferences("modulecolours", Context.MODE_PRIVATE).edit().putInt(module, color).apply()
                }

                override fun onCancel() {
                    colorPickerPopUp.dismissDialog() // Dismiss the dialog.
                }
            })
            .show()
    }
}