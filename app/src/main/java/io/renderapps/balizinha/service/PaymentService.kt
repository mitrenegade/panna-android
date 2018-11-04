package io.renderapps.balizinha.service

import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import io.renderapps.balizinha.R
import io.renderapps.balizinha.model.Event
import io.renderapps.balizinha.module.RetrofitFactory
import io.renderapps.balizinha.service.stripe.StripeService
import io.renderapps.balizinha.ui.event.EventDetailsActivity
import io.renderapps.balizinha.ui.main.MainActivity
import io.renderapps.balizinha.util.CommonUtils.isValidContext
import io.renderapps.balizinha.util.Constants
import io.renderapps.balizinha.util.Constants.*
import io.renderapps.balizinha.util.DialogHelper
import io.renderapps.balizinha.util.DialogHelper.createJoinDialog
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class PaymentService {

    companion object {
        private var joinDialog: AlertDialog? = null
        private var paymentDialog: AlertDialog? = null
        private val paymentRef = FirebaseDatabase.getInstance().reference
                .child(REF_CHARGES).child(REF_EVENTS)


        fun hasUserAlreadyPaid(activity: AppCompatActivity, event: Event, uid: String) {
            paymentRef.child(event.getEid()).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                        for (child in dataSnapshot.children) {

                            val pid = child.child("player_id").getValue(String::class.java)
                            val status = child.child("status").getValue(String::class.java)

                            if (pid != null && status != null && pid == uid && status == "succeeded") {
                                // user has already paid
                                joinDialog?.dismiss()
                                EventService.joinEvent(activity, event.eid)
                                return
                            }
                        }
                    }

                    // user has not paid
                    joinDialog = createJoinDialog(activity)
                    showJoinDialog(activity)

                    isPaymentConfigEnabled(activity, event)
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }


        private fun isPaymentConfigEnabled(activity: AppCompatActivity, event: Event) {
            val mRemoteConfig = FirebaseRemoteConfig.getInstance()
            val cacheExpiration = Constants.REMOTE_CACHE_EXPIRATION
            FirebaseRemoteConfig.getInstance().fetch(cacheExpiration.toLong()).addOnCompleteListener { task ->
                if (task.isSuccessful)
                    mRemoteConfig.activateFetched()
                val paymentRequired = mRemoteConfig.getBoolean(Constants.CONFIG_PAYMENT_KEY)

                // check if user has added a payment method
                if (paymentRequired)
                    checkUserPaymentMethod(activity, event)
                else {
                    showDefaultPaymentDialog(activity, event)
                }
            }
        }

        private fun checkUserPaymentMethod(activity: AppCompatActivity, event: Event) {
            val uid = FirebaseAuth.getInstance().uid
            if (uid.isNullOrEmpty()) {
                hideJoinDialog()
                return
            }

            FirebaseDatabase.getInstance().reference.child("stripe_customers")
                    .child(uid!!).child("source")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.value != null)
                                confirmPaymentDialog(activity, event)
                            else {
                                hideJoinDialog()
                                DialogHelper.showPaymentMethodRequiredDialog(activity)
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Toast.makeText(activity.applicationContext, "Unable to make payment.", Toast.LENGTH_LONG).show()
                            hideJoinDialog()
                        }
                    })
        }


        private fun onAddCharge(activity: AppCompatActivity, event: Event?) {
            val uid = FirebaseAuth.getInstance().uid
            if (uid.isNullOrEmpty() || event == null) {
                return
            }

            if (event.getEid().isNullOrEmpty()) {
                Toast.makeText(activity.applicationContext, "Unable to make payment.", Toast.LENGTH_LONG).show()
                return
            }

            if (!isValidContext(activity)) return

            showProcessingPayment(activity)
            var mCompositeDisposable: CompositeDisposable? = null
            if (activity is MainActivity) mCompositeDisposable = activity.mCompositeDisposable
            if (activity is EventDetailsActivity) mCompositeDisposable = activity.mCompositeDisposable

            val observable = RetrofitFactory.getInstance().create(StripeService::class.java)
                    .holdPaymentForEvent(uid!!, event.eid!!)

            mCompositeDisposable?.add(observable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableObserver<ResponseBody>() {

                        override fun onNext(resposeBody: ResponseBody) {
                            val response = resposeBody.string()

                            if (response == null || response.isEmpty()) {
                                showFailedPayment(activity)
                            }

                            try {
                                val jsonObject = JSONObject(response)
                                if (jsonObject.has("error")) {
                                    showFailedPayment(activity)
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                                showFailedPayment(activity)
                            }

                            // successful
                            hideProcessingPayment()
                            EventService.joinEvent(activity, event.eid)
                        }

                        override fun onError(e: Throwable) {
                            showFailedPayment(activity)
                        }

                        override fun onComplete() {}
                    }))
        }


        private fun confirmPaymentDialog(activity: AppCompatActivity, event: Event) {
            if (!isValidContext(activity))
                return

            val builder = AlertDialog.Builder(activity)
            builder.setTitle(activity.getString(R.string.confirm_payment))
            builder.setCancelable(false)

            // Get the layout inflater
            val inflater = LayoutInflater.from(activity)
            val view = inflater.inflate(R.layout.dialog_layout_payment, null)
            (view.findViewById<View>(R.id.payment_details) as TextView).text = "Press Ok to pay $" + String.format(Locale.getDefault(), "%.2f", event.getAmount()) + " for this game."
            builder.setView(view)
                    // Add action buttons
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        hideJoinDialog()
                        onAddCharge(activity, event)
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        hideJoinDialog()
                        dialog.cancel()
                    }

            builder.create().show()
        }


        private fun showDefaultPaymentDialog(activity: AppCompatActivity, event: Event) {
            if (isValidContext(activity)) {

                val builder = AlertDialog.Builder(activity)
                builder.setTitle(activity.getString(R.string.payment_required_title))
                builder.setCancelable(false)

                val inflater = LayoutInflater.from(activity)
                builder.setView(inflater.inflate(R.layout.dialog_layout_payment, null))
                        .setPositiveButton(R.string.continue_button) { _, _ ->
                            hideProcessingPayment()
                            EventService.joinEvent(activity, event.eid)
                        }
                        .setNegativeButton(R.string.cancel) { dialog, _ ->
                            hideJoinDialog()
                            dialog.cancel()
                        }

                builder.create().show()
            }
        }

        private fun createProcessingDialog(activity: AppCompatActivity): AlertDialog {
            val builder = AlertDialog.Builder(activity)
            builder.setCancelable(false)

            val inflater = LayoutInflater.from(activity)
            val v = inflater.inflate(R.layout.dialog_progress, null)
            (v.findViewById(R.id.progress_text) as TextView).text = "Processing payment..."

            builder.setView(v)

            return builder.create()
        }

        private fun showJoinDialog(activity: AppCompatActivity){
            if (isValidContext(activity)){
                joinDialog = createJoinDialog(activity)
                joinDialog?.show()
            }
        }

        private fun hideJoinDialog() {
            if (joinDialog != null) joinDialog?.dismiss()
        }

        private fun showProcessingPayment(activity: AppCompatActivity){
            if (isValidContext(activity)) {
                paymentDialog = createProcessingDialog(activity)
                paymentDialog?.show()
            }
        }

        private fun hideProcessingPayment() {
            if (paymentDialog != null) paymentDialog?.dismiss()
        }


        private fun showFailedPayment(activity: AppCompatActivity){
            Toast.makeText(activity.applicationContext, activity.getString(R.string.payment_failed), Toast.LENGTH_LONG).show()
            hideProcessingPayment()
        }
    }
}