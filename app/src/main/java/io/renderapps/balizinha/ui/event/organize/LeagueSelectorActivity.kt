package io.renderapps.balizinha.ui.event.organize

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import io.renderapps.balizinha.R
import io.renderapps.balizinha.model.League
import io.renderapps.balizinha.module.RetrofitFactory
import io.renderapps.balizinha.service.LeagueService
import io.renderapps.balizinha.util.Constants.REF_LEAGUES
import kotlinx.android.synthetic.main.activity_select_league.*
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject

class LeagueSelectorActivity : AppCompatActivity() {

    lateinit var leagues: ArrayList<League>
    lateinit var mCompositeDisposable: CompositeDisposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_league)

        supportActionBar?.title = "Select League"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)

        val uid = FirebaseAuth.getInstance().uid
        if (uid.isNullOrEmpty()){
            onBackPressed()
            return
        }

        mCompositeDisposable = CompositeDisposable()
        setupRecycler()
        fetchUserLeagues(uid!!)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home)
            finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mCompositeDisposable != null) mCompositeDisposable.dispose()
    }

    private fun setupRecycler(){
        leagues = ArrayList()
        league_recycler.layoutManager = GridLayoutManager(this@LeagueSelectorActivity, 2)
        val adapter = LeagueSelectorAdapter(this@LeagueSelectorActivity, leagues)

        league_recycler.adapter = adapter
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (progressbar.visibility == View.VISIBLE)
                    progressbar.visibility = View.GONE
            }
        })
    }

    private fun fetchUserLeagues(uid: String){

        val observable = RetrofitFactory.getInstance()
                .create(LeagueService::class.java)
                .getLeaguesForPlayer(uid)

        mCompositeDisposable.add(observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object: DisposableObserver<ResponseBody>(){
                    override fun onComplete() {}

                    override fun onNext(t: ResponseBody) {
                        val it = t.string()
                        parseLeaguesJson(it)
                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(this@LeagueSelectorActivity, "Unable to connect to network.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                })
        )
    }

    private fun parseLeaguesJson(it: String){
        if (it.isNullOrEmpty()){
            showOrganizerDialog()
            return
        }

        try {
            val jsonObj = JSONObject(it)
            if (jsonObj.isNull("result")){
                showOrganizerDialog()
                return
            }

            val resultObj = jsonObj.getJSONObject("result")
            val keysIterator = resultObj.keys()

            var isOrganizer = false
            while (keysIterator.hasNext()) {
                val leagueId = keysIterator.next() as String
                val status = resultObj.getString(leagueId)

                if (status == "organizer"){
                    isOrganizer = true
                    addLeague(leagueId)
                }
            }

            if (!isOrganizer){
                showOrganizerDialog()
            }

        } catch (e: JSONException) {
            Toast.makeText(this@LeagueSelectorActivity, "Unable to connect to network.", Toast.LENGTH_LONG).show()
            finish()
            e.printStackTrace()
        }
    }

    private fun addLeague(leagueId: String){
        FirebaseDatabase.getInstance().reference
                .child(REF_LEAGUES)
                .child(leagueId)
                .addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {}

                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists() && snapshot.value != null){
                            val key = snapshot.key
                            val league = snapshot.getValue(League::class.java)
                            if (key != null && league != null) {
                                league.id = key
                                leagues.add(league)
                                runOnUiThread {
                                    league_recycler.adapter?.notifyItemInserted(leagues.size - 1)
                                }
                            }
                        }
                    }
                })
    }

    private fun showOrganizerDialog(){
        progressbar.visibility = View.GONE
        no_league_text.visibility= View.VISIBLE

        AlertDialog.Builder(this@LeagueSelectorActivity)
                .setTitle("You're not an organizer")
                .setMessage("You currently can't organize games for any leagues. Please contact the league owners to become an organizer.")
                .setPositiveButton("Close") { p0, _ ->
                    finish()
                    p0.dismiss()
                }
                .create()
                .show()
    }
}


