package io.renderapps.balizinha.service

import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import io.renderapps.balizinha.R
import io.renderapps.balizinha.module.RetrofitFactory
import io.renderapps.balizinha.ui.event.EventDetailsActivity
import io.renderapps.balizinha.ui.main.MainActivity
import io.renderapps.balizinha.util.CommonUtils.isValidContext
import io.renderapps.balizinha.util.DialogHelper
import io.renderapps.balizinha.util.DialogHelper.createJoinDialog
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class EventService {

    companion object {

        var joinDialog: AlertDialog? = null

        fun isEventOver(endTimeSec: Long): Boolean {
            return Date().time > endTimeSec * 1000
        }

        fun showLeaveDialog(activity: AppCompatActivity, eventId: String, paymentRequired: Boolean){
            if (!isValidContext(activity)) return

            val builder = AlertDialog.Builder(activity)
            builder.setCancelable(false)

            if (!paymentRequired) {
                builder.setMessage(activity.getString(R.string.leave_event))
            } else {
                builder.setTitle(activity.getString(R.string.leave_event_title))
                builder.setMessage(activity.getString(R.string.leave_paid_event))
            }

            builder.setPositiveButton(
                    "Yes, I'm sure"
            ) { dialog, _ ->
                dialog.dismiss()
                leaveEvent(eventId, activity)
            }

            builder.setNegativeButton(
                    "Cancel"
            ) { dialog, _ -> dialog.cancel() }

            builder.create().show()
        }



        fun joinEvent(activity: AppCompatActivity, eventId: String){
            if (!isValidContext(activity)) return

            val uid = FirebaseAuth.getInstance().uid
            if (uid.isNullOrEmpty() || eventId.isNullOrEmpty()) {
                return
            }

            var mCompositeDisposable: CompositeDisposable? = null

            if (activity is MainActivity) mCompositeDisposable = activity.mCompositeDisposable
            if (activity is EventDetailsActivity) mCompositeDisposable = activity.mCompositeDisposable


            val params = HashMap<String, Any>()
            params["userId"] = uid!!
            params["eventId"] = eventId
            params["join"] = true

            val observable = RetrofitFactory.getInstance().create(EventApiService::class.java)
                    .joinOrLeaveEvent(params)

            joinDialog = createJoinDialog(activity)
            if (joinDialog != null) joinDialog!!.show()

            mCompositeDisposable?.add(observable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableObserver<ResponseBody>() {
                        override fun onComplete() {}

                        override fun onNext(responseBody: ResponseBody) {
                            val response = responseBody.string()

                            if (joinDialog != null)
                                joinDialog!!.dismiss()

                            if (response.isNullOrEmpty()){
                                Toast.makeText(activity.applicationContext, "Unable to join event.", Toast.LENGTH_LONG).show()
                            }

                            val json = JSONObject(response)
                            if (json.has("eventId")){
                                showSuccessfulPayment(activity)
                            } else {
                                Toast.makeText(activity.applicationContext, "Unable to join event.", Toast.LENGTH_LONG).show()
                            }

                        }

                        override fun onError(e: Throwable) {
                            if (joinDialog != null)
                                joinDialog!!.dismiss()
                            Toast.makeText(activity.applicationContext, "Unable to join event.", Toast.LENGTH_LONG).show()
                        }
                    }))
        }

        private fun leaveEvent(eventId: String, activity: AppCompatActivity){
            val uid = FirebaseAuth.getInstance().uid
            if (uid.isNullOrEmpty()) return
            if (!isValidContext(activity)) return

            var mCompositeDisposable: CompositeDisposable? = null
            if (activity is MainActivity) mCompositeDisposable = activity.mCompositeDisposable
            if (activity is EventDetailsActivity) mCompositeDisposable = activity.mCompositeDisposable

            val params = HashMap<String, Any>()
            params["userId"] = uid!!
            params["eventId"] = eventId
            params["join"] = false

            val observable = RetrofitFactory.getInstance().create(EventApiService::class.java).joinOrLeaveEvent(params)

            mCompositeDisposable?.add(observable
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableObserver<ResponseBody>() {
                        override fun onComplete() {}

                        override fun onNext(responseBody: ResponseBody) {
                            val result = responseBody.string()
                            if (result.isNullOrEmpty()) {
                                Toast.makeText(activity.applicationContext, "Oops, looks like something went wrong.", Toast.LENGTH_LONG).show()
                                return
                            }

                            try {
                                val resultJson = JSONObject(result)
                                if (resultJson.has("eventId")) {
                                    if (isValidContext(activity)) {
                                        (activity as? EventDetailsActivity)?.onUserLeave()
                                    }
                                } else {
                                    Toast.makeText(activity.applicationContext, "Oops, looks like something went wrong.", Toast.LENGTH_LONG).show()
                                }
                            } catch (iox: JSONException) {
                                Toast.makeText(activity.applicationContext, "Oops, looks like something went wrong.", Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun onError(e: Throwable) {
                            Toast.makeText(activity.applicationContext, "Oops, looks like something went wrong.", Toast.LENGTH_LONG).show()
                        }
                    }))
        }

        private fun showSuccessfulPayment(activity: AppCompatActivity){
            if (isValidContext(activity)) {
                DialogHelper.showSuccessfulJoin(activity)

                if (activity is EventDetailsActivity){
                    activity.showSuccessfulJoin()
                }
            }
        }
    }
}
