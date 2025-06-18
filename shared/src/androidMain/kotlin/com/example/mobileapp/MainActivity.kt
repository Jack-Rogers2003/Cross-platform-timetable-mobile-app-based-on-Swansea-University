package com.example.mobileapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.core.content.edit
import java.io.File

class MainActivity : AppCompatActivity() {
    private var db = UserInformation.getDatabase().getReference(UserInformation.getCurrentUserID().toString())
    private var itemList  = db.child("modules").toString().split(",").toMutableList()
    private lateinit var adapter: ModulePopUpAdapter
    private lateinit var societyAdapter: SocietyPopUp
    private val map: MutableMap<String, String> = mutableMapOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("modules", Context.MODE_PRIVATE)


        setContentView(R.layout.main_screen)
        val toolbar : Toolbar = findViewById(R.id.main_toolbar)
        setSupportActionBar(toolbar)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val addModulesFab = findViewById<FloatingActionButton>(R.id.addModulesfab)
        addModulesFab.setOnClickListener {
            showCenteredPopup()
        }

        val refreshFab = findViewById<FloatingActionButton>(R.id.refreshfab)
        refreshFab.setOnClickListener { refreshTimetable() }

        itemList = sharedPreferences.getString("modules", "")?.split(",")!!.toMutableList()

        adapter = ModulePopUpAdapter(this, itemList) { position ->  // 'this' refers to the Activity context
            itemList.removeAt(position)
            adapter.notifyDataSetChanged()
        }


        val sections: MutableList<Pair<String, MutableList<List<String>>>> = mutableListOf(
            "Monday" to mutableListOf(),
            "Tuesday" to mutableListOf(),
            "Wednesday" to mutableListOf(),
            "Thursday" to mutableListOf(),
            "Friday" to mutableListOf(),
        )

        recyclerView.adapter = RecyclerviewAdapter(sections, this)

        val spinner = findViewById<Spinner>(R.id.selectWeeks)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, getWeeklyDates())

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                refreshCheck()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }

    private fun refreshCheck() {
        val file = File(filesDir, "downloadedTimetable.txt")
        if (file.exists()) {
            loadFile()
        } else {
            refreshTimetable()
        }
    }

    private fun loadFile() {
        val listOfEvents = ArrayList<String>()
        val spinner = findViewById<Spinner>(R.id.selectWeeks)
        val selected = spinner.selectedItem.toString()
        val dateStr = selected.removePrefix("Week of ").trim()


        val file = File(filesDir, "downloadedTimetable.txt")
        var flag = true
        file.bufferedReader().use { reader  ->
            while (flag) {
                var line = reader.readLine()
                if (line == dateStr) {
                    while (line.isNotEmpty()) {
                        line = reader.readLine()
                        listOfEvents.add(line)
                    }
                    flag = false
                }
            }
        }

        listOfEvents.removeAt(listOfEvents.size -1)

        updateList(listOfEvents)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        db.child("userType").addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value != "society") {
                    menu!!.findItem(R.id.formIcon).isVisible = false
                    menu.findItem(R.id.checkBookings).isVisible = false
                } else {
                    createBookingsPopUp()
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        return true
    }

    private fun createBookingsPopUp() {
        UserInformation.getCurrentUserID()?.let { userId ->
            UserInformation.getDatabase().reference.child(userId).child("Username")
                .get().addOnSuccessListener { usernameSnapshot ->
                    val username = usernameSnapshot.getValue(String::class.java)
                    if (username != null) {
                        UserInformation.getDatabase().reference.child("Bookings").child(username)
                            .get().addOnSuccessListener { bookingsSnapshot ->
                                bookingsSnapshot.children.forEach { booking ->
                                    booking.key?.let { map[it] = booking.value.toString() }
                                    societyAdapter = SocietyPopUp(map) { position ->
                                        itemList.removeAt(position)
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                            }
                    }
                }
        }

    }

    private fun getWeeklyDates(): List<String> {
        val format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        val ranges = listOf(
            "30-09-2024" to "08-12-2024",
            "27-01-2025" to "07-04-2025"
        )

        val weeks = mutableListOf<String>()

        for ((startStr, endStr) in ranges) {
            val start = format.parse(startStr)!!
            val end = format.parse(endStr)!!
            val calendar = Calendar.getInstance()
            calendar.time = start

            while (calendar.time <= end) {
                weeks.add("Week of ${format.format(calendar.time)}")
                calendar.add(Calendar.DATE, 7)
            }
        }

        return weeks
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.logOutIcon -> {
                UserInformation.logOut()
                val file = File(this.filesDir, "downloadedTimetable.txt")
                if (file.exists()) {
                    file.delete()
                }
                getSharedPreferences("modulecolours", Context.MODE_PRIVATE).edit { clear() }
                getSharedPreferences("modules", Context.MODE_PRIVATE).edit { clear() }
                startActivity(Intent(this, LoginOrSignUpActivity::class.java))
                true
            }
            R.id.additionalSettings -> {
                startActivity(Intent(this, Settings::class.java))
                true
            }
            R.id.formIcon -> {
                startActivity(Intent(this, SocietyBookingApplicationActivity::class.java))
                true
            }
            R.id.checkBookings -> {
                showSocietyPopUp()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSocietyPopUp() {
        val popupView = layoutInflater.inflate(R.menu.popup_menu, null)
        val recyclerView: RecyclerView = popupView.findViewById(R.id.recycler_view)
        popupView.findViewById<EditText>(R.id.edit_text).visibility = View.GONE
        popupView.findViewById<Button>(R.id.enter_button).visibility = View.GONE

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = societyAdapter

        val dialog = AlertDialog.Builder(this)
            .setView(popupView)
            .setCancelable(false)
            .create()

        val backButton: Button = popupView.findViewById(R.id.back_button)

        backButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun showCenteredPopup() {
        val popupView = layoutInflater.inflate(R.menu.popup_menu, null)

        val editText: EditText = popupView.findViewById(R.id.edit_text)
        val enterButton: Button = popupView.findViewById(R.id.enter_button)
        val backButton: Button = popupView.findViewById(R.id.back_button)
        val recyclerView: RecyclerView = popupView.findViewById(R.id.recycler_view)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setView(popupView)
            .setCancelable(false)
            .create()

        enterButton.setOnClickListener {
            enterButton.isEnabled = false
            if (editText.text.isEmpty()) {
                Toast.makeText(this@MainActivity, "No module entered", Toast.LENGTH_SHORT).show()
                enterButton.isEnabled = true
            } else if(adapter.itemCount >= 8) {
                Toast.makeText(this@MainActivity, "8 module max", Toast.LENGTH_SHORT).show()
                enterButton.isEnabled = true
            } else {
                popUpEnter(editText, popupView)
            }

            editText.text.clear()
        }

        backButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun popUpEnter(module: EditText, popupView: View) {
        val moduleToAdd = module.text.toString()
        CoroutineScope(Dispatchers.IO).launch {
            withTimeout(60000) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "adding...", Toast.LENGTH_LONG).show()
                }
                val client = OkHttpClient.Builder()
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()

                //val url = "http://10.0.2.2:8080/checkModuleExists/${moduleToAdd}"

                val url = "http://ec2-13-49-49-156.eu-north-1.compute.amazonaws.com.com:8080/checkModuleExists/${moduleToAdd}"


                // Create a GET request
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                // Execute the request and handle the response
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Server Error", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val responseBody = response.body?.string().toBoolean()

                        if (responseBody) {
                            val sharedPreferences = getSharedPreferences("modules", Context.MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            val savedModules = sharedPreferences.getString("modules", "")
                            if (savedModules != null && moduleToAdd in savedModules.split(",")) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@MainActivity, "Module already Added", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val toAdd: String = if (savedModules != "") {
                                    "$savedModules,$moduleToAdd"
                                } else {
                                    "" + moduleToAdd
                                }
                                editor.putString("modules", toAdd).apply()
                                db.child("modules").setValue(toAdd)
                                itemList.add(moduleToAdd.trim())
                                withContext(Dispatchers.Main) {
                                    adapter.notifyDataSetChanged()
                                }

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@MainActivity, "Module Added!", Toast.LENGTH_SHORT).show()
                                }

                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Module not found, is the ID right?", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    popupView.findViewById<Button>(R.id.enter_button).isEnabled = true
                }

            }
        }
    }

    private fun refreshTimetable() {
        Toast.makeText(this, "Refreshing...", Toast.LENGTH_LONG).show()

        val modules = getSharedPreferences("modules", Context.MODE_PRIVATE).getString("modules", null)
        if (modules != null) {
            getModules(modules)
        }
    }

    private fun getModules(modulesToGet: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val spinner = findViewById<Spinner>(R.id.selectWeeks)
                val selected = spinner.selectedItem.toString() // e.g. "Week of 29-09-2025"
                val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                val dateStr = selected.removePrefix("Week of ").trim()
                val date = inputFormat.parse(dateStr)
                val formattedDate = outputFormat.format(date!!)

                withTimeout(60000) {
                    val client = OkHttpClient.Builder()
                        .readTimeout(60, TimeUnit.SECONDS)
                        .build()

                    //val url = "http://10.0.2.2:8080/getModules/$modulesToGet,$formattedDate"




                    val url = "http://ec2-13-49-49-156.eu-north-1.compute.amazonaws.com:8080/getModules/$modulesToGet,$formattedDate"


                    // Create a GET request
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .build()

                    // Execute the request and handle the response
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Server Error Occurred", Toast.LENGTH_SHORT).show()
                            }
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
                                updateList(events)
                            }
                        }
                    }
                }
            }  catch (e: IOException) {
                // Handle network or I/O issues specifically
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Network Error", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Catch other unexpected exceptions
                withContext(Dispatchers.Main) {
                    println("Unexpected error: ${e.message}")
                }
            }
        }
    }

    private fun updateList(events: ArrayList<String>) {
        val daysOfRecyclerView: MutableList<Pair<String, MutableList<List<String>>>> = mutableListOf(
            "Monday" to mutableListOf(),
            "Tuesday" to mutableListOf(),
            "Wednesday" to mutableListOf(),
            "Thursday" to mutableListOf(),
            "Friday" to mutableListOf(),
        )


        for (entry in events) {
            println(entry)
            val (module, eventType, timeRange, day, lecturer,room) = entry.toTuple6("$$$")

            // Format the event as a List<String>
            val toAdd = "$timeRange\n" +
                    "Module: $module\n" +
                    "Type: $eventType\n" +
                    "Given By: $lecturer\n" +
                    "Room: $room"


            // Find the correct day and add the event to the day's list
            val dayPair = daysOfRecyclerView.find { it.first == day }
            dayPair?.second?.add(listOf(toAdd))
        }


        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = RecyclerviewAdapter(daysOfRecyclerView, this)

    }

    private fun String.toTuple6(delimiter: String): Tuple6<String, String, String, String, String, String> {
        val splitList = this.split(delimiter)
        if (splitList.size != 6) throw IllegalArgumentException("String must have exactly 6 elements after splitting.")
        return Tuple6(splitList[0], splitList[1], splitList[2], splitList[3], splitList[4], splitList[5])
    }

    data class Tuple6<A, B, C, D, E, F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)

}

class ModulePopUpAdapter(
    private val context: Context,
    private val items: MutableList<String>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<ModulePopUpAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.item_text)
        val deleteButton: ImageButton = view.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.menu.modules_popup_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.textView.text = items[position]
        if (holder.textView.text != "") {
            holder.deleteButton.setOnClickListener {
                deleteButtonClicked(position)
            }
        } else {
            holder.deleteButton.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size


    private fun deleteButtonClicked(position: Int) {
        onDeleteClick(position)

        val sharedPreferences = context.getSharedPreferences("modules", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val savedModules = sharedPreferences.getString("modules", "")
        val list = savedModules?.split(",")
        editor.remove("modules")
        val listAsString = convertToString(createNewList(list, position))
        editor.putString("modules", listAsString.dropLast(1))
        editor.apply()
        UserInformation.getDatabase().getReference(UserInformation.getCurrentUserID().toString()).child("modules").setValue(listAsString.dropLast(1))
    }

    private fun convertToString(list: List<String>?): String {
        var modulesAsList = ""
        if (list != null) {
            for(module in list) {
                modulesAsList = "$modulesAsList$module,"
            }
        }
        return modulesAsList
    }

    private fun createNewList(list: List<String>?, position: Int) : List<String> {
        val listToReturn = mutableListOf<String>()
        if (list != null) {
            for(i in list.indices) {
                if (i != position) {
                    listToReturn.add(list[i])
                }
            }
        }
        return listToReturn
    }
}

class SocietyPopUp(
    private val items: MutableMap<String, String>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<SocietyPopUp.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.item_text)
        val deleteButton: ImageButton = view.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.menu.modules_popup_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val key = items.keys.toList()[position]
        println(items[key])
        holder.textView.text = items[key]?.let { convertFormat(it) }
        if (holder.textView.text != "") {
            holder.deleteButton.setOnClickListener {
                deleteButtonClicked(position)
            }
        } else {
            holder.deleteButton.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    private fun convertFormat(toDisplay: String): String {
        val regex = "\\{(.*?)\\}".toRegex()
        val match = regex.find(toDisplay)

        val eventDetails = match?.groups?.get(1)?.value?.split(", ")?.map { it.split("=") }?.associate { it[0] to it[1] }

        val size = eventDetails?.get("size") ?: "N/A"
        val day = eventDetails?.get("selectedDay") ?:  eventDetails?.get("selectedSpecificDate")
        val campus = eventDetails?.get("campus") ?: "N/A"
        val recurring = if (eventDetails?.get("recurring") == "1") "Yes" else "No"
        val startTime = eventDetails?.get("startTime") ?: "N/A"
        val eventLength = eventDetails?.get("eventLength") ?: "N/A"

        return "Day: $day\nSize: $size\nCampus: $campus\nRecurring: $recurring\nStart Time: $startTime:00\nLength: $eventLength hours"
    }


    private fun deleteButtonClicked(position: Int) {
        onDeleteClick(position)
        val key = items.keys.toList()[position]
        items.remove(key)

        UserInformation.getCurrentUserID()?.let { it ->
            UserInformation.getDatabase().reference.child(it).child("Username")
                .get().addOnSuccessListener { usernameSnapshot ->
                    val username = usernameSnapshot.getValue(String::class.java)
                    if (username != null) {
                        val ref = UserInformation.getDatabase().reference
                            .child("Bookings")
                            .child(username)
                            .child(key)

                        ref.removeValue().addOnSuccessListener {
                            println("Deleted successfully")
                        }.addOnFailureListener {
                            println("Delete failed: ${it.message}")
                        }
                    }
                }
        }
        this.notifyItemRemoved(position)
    }
}