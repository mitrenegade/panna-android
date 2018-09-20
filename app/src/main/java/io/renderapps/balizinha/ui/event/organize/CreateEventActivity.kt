package io.renderapps.balizinha.ui.event.organize

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.design.widget.Snackbar.LENGTH_INDEFINITE
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View

import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import io.renderapps.balizinha.AppController
import io.renderapps.balizinha.BuildConfig.APPLICATION_ID
import io.renderapps.balizinha.R
import io.renderapps.balizinha.model.Event
import io.renderapps.balizinha.model.League
import io.renderapps.balizinha.service.CloudService
import io.renderapps.balizinha.service.StorageService
import io.renderapps.balizinha.ui.main.MainActivity
import io.renderapps.balizinha.util.CommonUtils.showSnackbar
import io.renderapps.balizinha.util.Constants.*
import io.renderapps.balizinha.util.CustomTimePickerDialog
import io.renderapps.balizinha.util.DialogHelper
import io.renderapps.balizinha.util.PhotoHelper
import io.renderapps.balizinha.util.PhotoHelper.getImageAsBytes
import kotlinx.android.synthetic.main.activity_create_event.*
import kotlinx.android.synthetic.main.dialog_max_players.view.*
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class CreateEventActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LEAGUE: String = "extra_league"
        const val EXTRA_EVENT: String = "extra_event"
    }

    private val PLACE_PICKER_REQUEST = 1
    private val LOCATION_REQUEST_CODE = 34

    private val pattern = "^([-+]?)([\\d]{1,2})(\\°)[\\d]*(\\')[\\d]*(\\.)[\\d]*(\\\")(N|S){1}(\\s)([-+]?)([\\d]{1,3})(\\°)[\\d]*(\\')[\\d]*(\\.)[\\d]*(\\\")(E|W){1}"
    private val timeFormatter = SimpleDateFormat("hh:mm aa", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())

    lateinit var latLng: LatLng
    lateinit var dialog: Dialog

    private var photoHelper: PhotoHelper? = null
    var league: League? = null
    var event: Event? = null
    var isEditing = false

    // properties
    private var mBytes: ByteArray? = null
    private var leagueId: String? = null

    private var day: Date? = null
    private var startTime: Date? = null
    private var endTime: Date? = null

    private var state: String? = null
    private var city: String? = null
    private var venue: String? = null
    private var street: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        supportActionBar?.title = "Create Event"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (intent.hasExtra(EXTRA_LEAGUE)) {
            league = intent.getParcelableExtra(EXTRA_LEAGUE)
            leagueId = league?.id ?: ""
            loadHeader()
        }

        if (intent.hasExtra(EXTRA_EVENT)) {
            event = intent.getParcelableExtra(EXTRA_EVENT)
            if (event == null || event?.eid.isNullOrEmpty()) {
                onBackPressed()
                return
            }
            isEditing = true
            supportActionBar?.title = "Edit Event"
            loadEvent()
        }

        // listeners
        ll_add_photo.setOnClickListener { _ ->
            if (photoHelper == null)
                photoHelper = PhotoHelper(this@CreateEventActivity)
            else
                photoHelper!!.showCaptureOptions()
        }

        ll_event_type.setOnClickListener { _ ->
            showEventTypeDialog()
        }

        ll_max_players.setOnClickListener { _ ->
            showMaxPlayersPicker()
        }

        rl_venue_layout.setOnClickListener { _ ->
            if (checkPermissions())
                launchMap()
            else
                requestPermissions()
        }

        rl_day_layout.setOnClickListener { _ ->
            showDatePicker()
        }

        rl_start_time.setOnClickListener { _ ->
            showTimePickerDialog(true)
        }

        rl_end_time.setOnClickListener { _ ->
            showTimePickerDialog(false)
        }

        payment_required_switch.setOnCheckedChangeListener { _, isChecked ->
            val isVisible = if (isChecked) View.VISIBLE else View.GONE
            ll_payment_amount.visibility = isVisible
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home)
            onBackPressed()
        if (item?.itemId == R.id.action_create)
            validateForm()

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_create_event, menu)
        if (isEditing) {
            val item = menu?.findItem(R.id.action_create)
            item?.title = "Update"
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right)
    }

    private fun finishUp() {
        if (isEditing) {
            onBackPressed()
        } else {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun launchMap() {
        val builder = PlacePicker.IntentBuilder()
        startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST)
    }

    private fun disableViews(isEnabled: Boolean) {
        ll_add_photo.isEnabled = isEnabled
        name.isEnabled = isEnabled
        ll_event_type.isEnabled = isEnabled
        ll_max_players.isEnabled = isEnabled
        rl_venue_layout.isEnabled = isEnabled
        event_venue.isEnabled = isEnabled
        rl_day_layout.isEnabled = isEnabled
        rl_start_time.isEnabled = isEnabled
        rl_end_time.isEnabled = isEnabled
        payment_required_switch.isEnabled = isEnabled
        payment_amount.isEnabled = isEnabled
        event_description.isEnabled = isEnabled
    }

    private fun loadEvent() {
        latLng = LatLng(event?.lat!!, event?.lon!!)
        state = event?.state ?: ""
        city = event?.city ?: ""
        venue = event?.place ?: ""

        name.setText(event?.name ?: "")
        type.text = event?.type ?: ""
        type_arrow.visibility = View.GONE

        max_players.text = event?.maxPlayers?.toString() ?: ""
        max_players_arrow.visibility = View.GONE

        venue_hint.visibility= View.GONE
        event_venue.setText(event?.place ?: "")

        payment_required_switch.isChecked = event?.paymentRequired!!

        if (event?.paymentRequired!!)
            payment_amount.setText(event?.amount.toString())
        else
            ll_payment_amount.visibility = View.GONE

        // time
        val startMillis = (event?.getStartTime() ?: 1) * 1000
        val endMillis = (event?.getEndTime() ?: 1) * 1000
        val startDate = Date(startMillis)
        val endDate = Date(endMillis)

        day = startDate
        startTime = startDate
        endTime = endDate

        event_day.text = dateFormatter.format(startDate)
        day_arrow.visibility = View.GONE

        start_time.text = timeFormatter.format(startDate)
        start_time_arrow.visibility = View.GONE

        end_time.text = timeFormatter.format(endDate)
        end_time_arrow.visibility = View.GONE

        name.setSelection(name.text.length)

        // load header
        loadHeader()
    }

    private fun getTimeCalendar(time: Date): Calendar {
        val dayCal = Calendar.getInstance()
        dayCal.time = day

        val cal = Calendar.getInstance()
        cal.time = time
        cal.set(Calendar.DAY_OF_MONTH, dayCal.get(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.MONTH, dayCal.get(Calendar.MONTH))
        cal.set(Calendar.YEAR, dayCal.get(Calendar.YEAR))

        return cal
    }

    private fun saveEvent() {
        disableViews(false)

        val uid = FirebaseAuth.getInstance().uid
        if (uid.isNullOrEmpty()) finish()

        dialog = DialogHelper.showProgressDialog(this@CreateEventActivity, "Creating event...")
        dialog.show()

        val params = mapEvent()
        params["userId"] = uid!!

        // format amount
        payment_amount.setText(params["amount"].toString())

        CloudService(CloudService.ProgressListener {
            try {
                if (!it.isNullOrEmpty()) {
                    val jsonObject = JSONObject(it)

                    if (jsonObject.has("error")) {
                        showError(jsonObject.getString("error"))
                        return@ProgressListener
                    }

                    // successful
                    if (!jsonObject.isNull("result")) {
                        val eventId = jsonObject.getString("eventId")
                        if (!eventId.isNullOrEmpty())
                            uploadImage(eventId)
                        return@ProgressListener
                    }
                }

                // error
                showError("")

            } catch (e: JSONException) {
                e.printStackTrace()
                showError("Unable to create game, try again.")
                return@ProgressListener
            }
        }).createEvent(params)
    }

    private fun mapEvent(): HashMap<String, Any> {
        val startTimeSec = getTimeCalendar(startTime!!).timeInMillis / 1000
        val endTimeSec = getTimeCalendar(endTime!!).timeInMillis / 1000

        val title = name.text.toString().trim()
        val type = this.type.text.toString().trim()

        val place = if (venue.isNullOrEmpty()) street ?: "" else venue
        val amount = String.format("%.2f", payment_amount.text.toString().trim().toDouble()).toDouble()
        val max = max_players.text.toString().toInt()
        val info = event_description.text.toString().trim()

        val updates = HashMap<String, Any>()
        updates["city"] = city ?: ""
        updates["place"] = place ?: ""
        updates["startTime"] = startTimeSec
        updates["endTime"] = endTimeSec

        updates["maxPlayers"] = max
        updates["paymentRequired"] = payment_required_switch.isChecked
        updates["amount"] = amount
        updates["lat"] = latLng.latitude
        updates["lon"] = latLng.longitude

        // optionals
        updates["league"] = leagueId ?: ""
        updates["state"] = state ?: ""
        updates["name"] = title
        updates["type"] = type
        updates["info"] = info

        return updates
    }

    private fun updateEvent() {
        disableViews(true)
        dialog = DialogHelper.showProgressDialog(this@CreateEventActivity, "Updating...")
        dialog.show()

        val updates = mapEvent()
        // format amount
        payment_amount.setText(updates["amount"].toString())

        FirebaseDatabase.getInstance().reference.child(REF_EVENTS).child(event!!.eid)
                .updateChildren(updates)
                .addOnCompleteListener { taskResult ->
                    if (taskResult.isSuccessful) {
                        uploadImage(event!!.eid)
                    } else {
                        showError("Failed to update event.")
                    }
                }
    }

    private fun uploadImage(eventId: String) {
        val bytes = mBytes
        if (bytes == null || bytes.isEmpty()) {
            dialog.dismiss()
            finishUp()
        } else {
            FirebaseStorage.getInstance().reference.child(REF_STORAGE_IMAGES)
                    .child(REF_STORAGE_EVENT).child(eventId).putBytes(bytes)
                    .addOnCompleteListener {
                        dialog.dismiss()
                        finishUp()
                    }
        }
    }

    private fun showError(error: String) {
        showSnackbar(scrollView, error)
        disableViews(true)
        dialog.dismiss()
    }


    /**************************************************************************************************
     * Validate Form
     *************************************************************************************************/

    private fun validateForm() {

        if (name.text.isNullOrEmpty()){
            showSnackbar(scrollView, "Add a name for the event.")
            return
        }

        if (max_players.text.isNullOrEmpty()) {
            showSnackbar(scrollView, "Add max number of players for event.")
            return
        }

        if (event_venue.text.isNullOrEmpty()) {
            showSnackbar(scrollView, "Add a venue for the event.")
            return
        }

        if (event_day.text.isNullOrEmpty()) {
            showSnackbar(scrollView, "Add the day of event.")
            return
        }

        if (start_time.text.isNullOrEmpty()) {
            showSnackbar(scrollView, "Add the start time of event.")
            return
        }

        if (end_time.text.isNullOrEmpty()) {
            showSnackbar(scrollView, "Add the end time of event.")
            return
        }

        if (!validateTime())
            return

        if (!validatePayment())
            return

        if (isEditing) updateEvent() else saveEvent()
    }

    private fun validateTime(): Boolean {
        // time
        val dayCal = Calendar.getInstance()
        val currCal = Calendar.getInstance()
        val startCal = Calendar.getInstance()

        dayCal.time = day
        val start = timeFormatter.parse(start_time.text.toString())
        val end = timeFormatter.parse(end_time.text.toString())

        dayCal.time = day
        startCal.time = start

        startCal.set(Calendar.YEAR, dayCal.get(Calendar.YEAR))
        startCal.set(Calendar.MONTH, dayCal.get(Calendar.MONTH))
        startCal.set(Calendar.DAY_OF_MONTH, dayCal.get(Calendar.DAY_OF_MONTH))

        if (startCal.before(currCal)) {
            showSnackbar(scrollView, "Starting time cannot be before the current time.")
            return false
        }

        if (end <= start) {
            showSnackbar(scrollView, "End time cannot be the same or before the starting time.")
            return false
        }

        return true
    }

    private fun validatePayment(): Boolean {

        // no payment
        if (!payment_required_switch.isChecked) return true

        if (payment_required_switch.isChecked && payment_amount.text.isNullOrEmpty()) {
            showSnackbar(scrollView, "Add the payment amount for event.")
            return false
        }

        val amountString = payment_amount.text.toString().trim()
        val amount = amountString.toDouble()

        if (amount <= 1.00) {
            showSnackbar(scrollView, "Payment amount must be greater than $1.00.")
            return false
        }

        return true
    }


    /**************************************************************************************************
     * Dialogs
     *************************************************************************************************/

    private fun showEventTypeDialog() {
        val builder = AlertDialog.Builder(this@CreateEventActivity)
        val types = Event.Type.names()

        builder.setTitle("Select Event Type")
        builder.setItems(types, { _, which ->
            // Get the joinDialog selected item
            val selected = types[which]

            type_arrow.visibility = View.GONE
            type.text = selected
        })

        builder.create().show()
    }

    private fun showMaxPlayersPicker() {
        val builder = AlertDialog.Builder(this@CreateEventActivity)
        val view = layoutInflater.inflate(R.layout.dialog_max_players, null)
        builder.setTitle("Select Max Players")
        builder.setView(view)

        view.number_picker.minValue = 2
        view.number_picker.maxValue = 64

        builder.setPositiveButton("Set", { dialog, _ ->
            max_players_arrow.visibility = View.GONE
            max_players.text = view.number_picker.value.toString()
            dialog.dismiss()
        })

        builder.setNegativeButton("Cancel", { dialog, _ ->
            dialog.dismiss()
        })

        builder.create().show()
    }


    private fun showDatePicker() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(this@CreateEventActivity, R.style.DatePickerDialogTheme, DatePickerDialog.OnDateSetListener { _, setYear, monthOfYear, dayOfMonth ->

            c.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            c.set(Calendar.MONTH, monthOfYear)
            c.set(Calendar.YEAR, setYear)

            day_arrow.visibility = View.GONE
            event_day.text = dateFormatter.format(c.time)
            this.day = c.time

        }, year, month, day)

        c.add(Calendar.DATE, 30)
        dpd.datePicker.minDate = Date().time
        dpd.datePicker.maxDate = c.timeInMillis

        dpd.show()
    }

    private fun showTimePickerDialog(isStartTime: Boolean) {
        val cal = Calendar.getInstance()

        if (!isStartTime && endTime != null) {
            cal.time = endTime
        } else if (isStartTime && startTime != null) {
            cal.time = startTime
        } else {
            // set time to next quarter interval
            val offset: Long = 1000 * 60 * 15
            val date = Date()
            val rounded = Date((Math.ceil(date.time / offset.toDouble()) * offset).toLong())
            cal.time = rounded
        }

        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val format = SimpleDateFormat("hh:mm aa", Locale.getDefault())

        val timePicker = CustomTimePickerDialog(this@CreateEventActivity, TimePickerDialog.OnTimeSetListener { _, setHour, setMinute ->

            cal.set(Calendar.HOUR_OF_DAY, setHour)
            cal.set(Calendar.MINUTE, setMinute)

            if (isStartTime) {
                start_time_arrow.visibility = View.GONE
                start_time.text = format.format(cal.time)
                startTime = cal.time

                // set end time 1 hr ahead
                end_time_arrow.visibility = View.GONE
                cal.add(Calendar.HOUR_OF_DAY, 1)
                end_time.text = format.format(cal.time)
                endTime = cal.time

            } else {
                end_time_arrow.visibility = View.GONE
                end_time.text = format.format(cal.time)
                endTime = cal.time
            }

        }, hour, minute, false)

        val title = if (isStartTime) "Start Time" else "End Time"
        timePicker.setTitle(title)
        timePicker.show()
    }


    /**************************************************************************************************
     * Header / Image
     *************************************************************************************************/

    private fun loadHeader() {
        if (!isEditing){
            loadLeagueHeader()
        } else {
            StorageService.getEventImage(event!!.eid, object: StorageService.StorageCallback{
                override fun onSuccess(uri: Uri?) {
                    if (uri != null){
                        PhotoHelper.glideHeader(this@CreateEventActivity, event_header, uri.toString(), R.drawable.background_league_header)
                    } else {
                        loadLeagueHeader()
                    }
                }
            })
        }
    }

    private fun loadLeagueHeader(){
        StorageService.getLeagueHeader(leagueId, object: StorageService.StorageCallback{
            override fun onSuccess(uri: Uri?) {
                if (uri != null){
                    PhotoHelper.glideHeader(this@CreateEventActivity, event_header, uri.toString(), R.drawable.background_league_header)
                } else {
                    event_header.setImageResource(R.drawable.default_league_header)
                }
            }
        })
    }

    fun onAddPhoto(bytes: ByteArray) {
        val myOptions = RequestOptions()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .override(500, 500)

        if (!isDestroyed && !isFinishing) {
            Glide.with(this)
                    .asBitmap()
                    .apply(myOptions)
                    .load(bytes)
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            event_header.setImageBitmap(resource)

                            mBytes = getImageAsBytes(resource)
                        }
                    })
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                PERMISSION_CAMERA -> photoHelper?.cameraIntent()
                PERMISSION_GALLERY -> photoHelper?.galleryIntent()
                LOCATION_REQUEST_CODE -> {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) launchMap()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE -> photoHelper?.onSelectFromGalleryResult(data)
                REQUEST_CAMERA -> photoHelper?.onCaptureImageResult(data)
                PLACE_PICKER_REQUEST -> {

                    val place = PlacePicker.getPlace(this, data) ?: return
                    val address = place.address.toString().split(",")

                    when (address.size) {
                        5 -> {
                            city = address[2].trim()
                            state = "\\d".toRegex().replace(address[3], "").trim()
                        }

                        4 -> {
                            city = address[1].trim()
                            state = "\\d".toRegex().replace(address[2], "").trim()
                        }

                        3 -> {
                            city = address[0].trim()
                            state = "\\d".toRegex().replace(address[1], "").trim()
                        }
                    }

                    venue_hint.visibility= View.GONE
                    if (place.name.toString().matches(pattern.toRegex())) {
                        // venue has no name, parse address
                        if (address.isNotEmpty()) {
                            event_venue.text = address[0]
                            street = address[0]
                        }
                    } else {
                        event_venue.text = place.name.toString()
                        venue = place.name.toString()
                    }

                    latLng = place.latLng
                }
            }
        }
    }

    private fun checkPermissions() =
            ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED

    private fun startLocationPermissionRequest() {
        (applicationContext as AppController).getDataManager().locationRequested = true
        ActivityCompat.requestPermissions(this, arrayOf(ACCESS_COARSE_LOCATION),
                LOCATION_REQUEST_CODE)
    }

    private fun requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, ACCESS_COARSE_LOCATION)) {
            showSnackbar(R.string.permission_rationale, android.R.string.ok, View.OnClickListener {
                // Request permission
                startLocationPermissionRequest()
            })

        } else {
            if ((applicationContext as AppController).getDataManager().locationRequested){
                // user checked "never ask again"
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        View.OnClickListener {
                            // Build intent that displays the App settings screen.
                            val intent = Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", APPLICATION_ID, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(intent)
                        })
                return
            }

            // first time asking permission
            startLocationPermissionRequest()
        }
    }

    private fun showSnackbar(
            snackStrId: Int,
            actionStrId: Int = 0,
            listener: View.OnClickListener? = null
    ) {
        val snackbar = Snackbar.make(scrollView, getString(snackStrId), LENGTH_INDEFINITE)
        if (actionStrId != 0 && listener != null) {
            snackbar.setActionTextColor(ContextCompat.getColor(this@CreateEventActivity, R.color.colorPrimary))
            snackbar.setAction(getString(actionStrId), listener)
        }
        snackbar.show()
    }
}
