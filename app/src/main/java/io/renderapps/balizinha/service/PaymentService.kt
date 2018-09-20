package io.renderapps.balizinha.service

import android.content.Context
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.renderapps.balizinha.R
import io.renderapps.balizinha.model.Event
import io.renderapps.balizinha.ui.event.EventDetailsActivity
import io.renderapps.balizinha.util.CommonUtils.isValidContext
import io.renderapps.balizinha.util.Constants
import io.renderapps.balizinha.util.Constants.*
import io.renderapps.balizinha.util.DialogHelper
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class PaymentService {

    companion object {
        private var joinDialog: AlertDialog? = null
        private var paymentDialog: AlertDialog? = null

        private val mDatabase = FirebaseDatabase.getInstance().reference
        private val paymentRef = FirebaseDatabase.getInstance().reference
                .child(REF_CHARGES).child(REF_EVENTS)


        fun hasUserAlreadyPaid(context: Context, event: Event, uid: String) {
            if (context !is AppCompatActivity) return

            joinDialog = createJoinDialog(context)
            showJoinDialog(context)

            paymentRef.child(event.getEid()).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                        for (child in dataSnapshot.children) {

                            val pid = child.child("player_id").getValue(String::class.java)
                            val status = child.child("status").getValue(String::class.java)

                            if (pid != null && status != null && pid == uid && status == "succeeded") {
                                // user has already paid
                                onUserJoin(context, event)
                                return
                            }
                        }
                    }

                    // user has not paid
                    isPaymentConfigEnabled(context, event)
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }


        private fun isPaymentConfigEnabled(context: Context, event: Event) {
            val mRemoteConfig = FirebaseRemoteConfig.getInstance()
            val cacheExpiration = Constants.REMOTE_CACHE_EXPIRATION
            FirebaseRemoteConfig.getInstance().fetch(cacheExpiration.toLong()).addOnCompleteListener { task ->
                if (task.isSuccessful)
                    mRemoteConfig.activateFetched()
                val paymentRequired = mRemoteConfig.getBoolean(Constants.CONFIG_PAYMENT_KEY)

                // check if user has added a payment method
                if (paymentRequired)
                    checkUserPaymentMethod(context, event)
                else {
                    showDefaultPaymentDialog(context, event)
                }
            }
        }

        private fun checkUserPaymentMethod(context: Context, event: Event) {
            val uid = FirebaseAuth.getInstance().uid
            if (uid.isNullOrEmpty()) {
                hideJoinDialog(context)
                return
            }

            FirebaseDatabase.getInstance().reference.child("stripe_customers")
                    .child(uid!!).child("source")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.value != null)
                                confirmPaymentDialog(context, event)
                            else {
                                hideJoinDialog(context)
                                DialogHelper.showPaymentMethodRequiredDialog(context as AppCompatActivity)
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Toast.makeText(context.applicationContext, "Unable to make payment.", Toast.LENGTH_LONG).show()
                            hideJoinDialog(context)
                        }
                    })
        }


        private fun onAddCharge(context: Context, event: Event?) {
            val uid = FirebaseAuth.getInstance().uid
            if (uid.isNullOrEmpty()) {
                return
            }

            if (event?.getEid() == null || event.getEid().isEmpty()) {
                Toast.makeText(context.applicationContext, "Unable to make payment.", Toast.LENGTH_LONG).show()
                return
            }

            showProcessingPayment(context)
            CloudService(CloudService.ProgressListener { response ->
                if (response == null || response.isEmpty()) {
                    showFailedPayment(context)
                    return@ProgressListener
                }

                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.has("error")) {
                        showFailedPayment(context)
                        return@ProgressListener
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    showFailedPayment(context)
                    return@ProgressListener
                }

                // successful
                onUserJoin(context, event)
            }).holdPaymentForEvent(uid!!, event.getEid())
        }

        fun onUserJoin(context: Context, event: Event){
            hideProcessingPayment(context)

            val uid = FirebaseAuth.getInstance().uid
            if (uid.isNullOrEmpty()) {
                return
            }

            if (joinDialog == null) {
                joinDialog = createJoinDialog(context)
                joinDialog!!.show()
            }

            mDatabase.child(REF_EVENT_USERS).child(event.getEid()).runTransaction(object : Transaction.Handler {
                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                    var numOfPlayers = 0
                    for (child in mutableData.children) {
                        if (child != null && child.value != null) {
                            if ((child.value as Boolean?)!!) {
                                numOfPlayers++
                            }
                        }
                    }

                    return if (numOfPlayers < event.getMaxPlayers()) {
                        mDatabase.child(REF_USER_EVENTS).child(uid!!).child(event.getEid()).setValue(true)
                        mDatabase.child(REF_EVENT_USERS).child(event.getEid()).child(uid).setValue(true)
                        Transaction.success(mutableData)
                    } else {
                        Transaction.abort()
                    }
                }

                override fun onComplete(databaseError: DatabaseError?, successful: Boolean, dataSnapshot: DataSnapshot?) {
                    hideJoinDialog(context)
                    if (databaseError != null || !successful) {
                        Toast.makeText(context.applicationContext, "Unable to join game. Max number of players reached. ", Toast.LENGTH_LONG).show()
                        return
                    }

                    showSuccessfulPayment(context)
                }
            })
        }

        private fun confirmPaymentDialog(context: Context, event: Event) {
            if (!isValidContext(context as  AppCompatActivity))
                return

            val builder = AlertDialog.Builder(context)
            builder.setTitle(context.getString(R.string.confirm_payment))
            builder.setCancelable(false)

            // Get the layout inflater
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.dialog_layout_payment, null)
            (view.findViewById<View>(R.id.payment_details) as TextView).text = "Press Ok to pay $" + String.format(Locale.getDefault(), "%.2f", event.getAmount()) + " for this game."
            builder.setView(view)
                    // Add action buttons
                    .setPositiveButton(android.R.string.ok) { dialog, id ->
                        hideJoinDialog(context)
                        onAddCharge(context, event)
                    }
                    .setNegativeButton(R.string.cancel) { dialog, id ->
                        hideJoinDialog(context)
                        dialog.cancel()
                    }

            builder.create().show()
        }


        private fun showDefaultPaymentDialog(context: Context, event: Event) {
            if (isValidContext(context as AppCompatActivity)) {

                val builder = AlertDialog.Builder(context)
                builder.setTitle(context.getString(R.string.payment_required_title))
                builder.setCancelable(false)

                val inflater = LayoutInflater.from(context)
                builder.setView(inflater.inflate(R.layout.dialog_layout_payment, null))
                        .setPositiveButton(R.string.continue_button) { dialog, id -> onUserJoin(context, event) }
                        .setNegativeButton(R.string.cancel) { dialog, id ->
                            hideJoinDialog(context)
                            dialog.cancel()
                        }

                builder.create().show()
            }
        }

        private fun createJoinDialog(context: Context): AlertDialog {
            val builder = AlertDialog.Builder(context)
            builder.setCancelable(false)

            val inflater = LayoutInflater.from(context)
            val v = inflater.inflate(R.layout.dialog_progress, null)
            (v.findViewById(R.id.progress_text) as TextView).text = "Joining game..."

            builder.setView(v)

            return builder.create()
        }

        private fun createProcessingDialog(context: Context): AlertDialog {


            val builder = AlertDialog.Builder(context)
            builder.setCancelable(false)

            val inflater = LayoutInflater.from(context)
            val v = inflater.inflate(R.layout.dialog_progress, null)
            (v.findViewById(R.id.progress_text) as TextView).text = "Processing payment..."

            builder.setView(v)

            return builder.create()
        }

        private fun showJoinDialog(context: Context){
            if (isValidContext(context as AppCompatActivity)){
                joinDialog = createJoinDialog(context)
                joinDialog?.show()
            }
        }

        private fun hideJoinDialog(context: Context){
            if (joinDialog != null) joinDialog?.dismiss()
        }

        private fun showProcessingPayment(context: Context){
            if (isValidContext(context as AppCompatActivity)) {
                paymentDialog = createProcessingDialog(context)
                paymentDialog?.show()
            }
        }

        private fun hideProcessingPayment(context: Context){
            if (paymentDialog != null) paymentDialog?.dismiss()
        }


        private fun showFailedPayment(context: Context){
            Toast.makeText(context.applicationContext, context.getString(R.string.payment_failed), Toast.LENGTH_LONG).show()
            hideProcessingPayment(context)
        }

        private fun showSuccessfulPayment(context: Context){
            if (isValidContext(context as AppCompatActivity)) {
                DialogHelper.showSuccessfulJoin(context)

                if (context is EventDetailsActivity){
                    context.showSuccessfulJoin()
                }
            }
        }
    }
}