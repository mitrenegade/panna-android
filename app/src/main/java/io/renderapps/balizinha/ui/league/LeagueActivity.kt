package io.renderapps.balizinha.ui.league

import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import io.renderapps.balizinha.R
import io.renderapps.balizinha.model.League
import io.renderapps.balizinha.model.Player
import io.renderapps.balizinha.service.CloudService
import io.renderapps.balizinha.service.FireLeague
import io.renderapps.balizinha.service.PlayerService
import io.renderapps.balizinha.util.Constants.REF_LEAGUE_PLAYERS
import io.renderapps.balizinha.util.Constants.REF_PLAYERS
import io.renderapps.balizinha.util.DialogHelper
import io.renderapps.balizinha.util.PhotoHelper
import kotlinx.android.synthetic.main.dialog_add_tag.view.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class LeagueActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_LEAGUE = "league"
    }

    lateinit var league: League
    lateinit var members: ArrayList<Player>
    private lateinit var membersAdapter: MembersAdapter

    var databaseRef: DatabaseReference? = null
    var playersChildListener: ChildEventListener? = null


    var isMember: Boolean = false

    @BindView(R.id.header_img) lateinit var header: ImageView
    @BindView(R.id.title) lateinit var title: TextView
    @BindView(R.id.location_members) lateinit var locationAndMembers: TextView
    @BindView(R.id.description) lateinit var description: TextView
    @BindView(R.id.tags_recycler) lateinit var tagsRecycler: RecyclerView
    @BindView(R.id.members_recycler) lateinit var membersRecycler: RecyclerView
    @BindView(R.id.join_leave_button) lateinit var joinLeaveButton: Button
    @BindView(R.id.status_progress) lateinit var progress: ProgressBar

    @SuppressLint("RestrictedApi") // suppress the warning
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_league)
        ButterKnife.bind(this)

        if (intent.extras == null || !intent.hasExtra(EXTRA_LEAGUE)) {
            onBackPressed()
            return
        }

        databaseRef = FirebaseDatabase.getInstance().reference
        members = ArrayList()
        league = intent.getParcelableExtra(EXTRA_LEAGUE)

        // hide collapsing toolbar title
        setSupportActionBar(findViewById(R.id.league_toolbar))
        if (supportActionBar != null)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val collapsingToolbarLayout = findViewById<CollapsingToolbarLayout>(R.id.collapsing_toolbar)
        collapsingToolbarLayout.title = " "

        joinLeaveButton.setOnClickListener({ _ -> validateName()})

        layoutViews()
        loadHeader()

        fetchLeagueStatus()
        fetchLeaguePlayers()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null && item.itemId == android.R.id.home)
            onBackPressed()
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right)
    }

    override fun onStop() {
        super.onStop()
        databaseRef?.child(REF_LEAGUE_PLAYERS)?.child(league.id)?.removeEventListener(playersChildListener ?: return)
    }


    private fun layoutViews(){
        title.text = league.name
        description.text = league.info
        updateMembers()

        if (league.tags != null) {

            // remove empty tags
            league.tags.removeAll(Arrays.asList("", null))

            // add tag
            league.tags.add("Add a tag")

            val adapter = TagsAdapter(this, league.tags)
            tagsRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            tagsRecycler.adapter = adapter
            adapter.notifyDataSetChanged()
        }

        // members
        membersAdapter = MembersAdapter(this, members)
        membersRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        membersRecycler.adapter = membersAdapter
    }

    private fun loadHeader(){
        if (league.photoUrl == null || league.photoUrl.isEmpty()){
            // todo
            // no header img, add default
        } else {
            PhotoHelper.glideHeader(this, header, league.photoUrl, R.drawable.background_league_header)
        }
    }

    private fun updateMembers(){
        if (league.city == null || league.city.isEmpty())
            locationAndMembers.text = "" + members.size + " members"
        else
            locationAndMembers.text = league.city + " \u2022 " + members.size + " members"
    }

    fun updateMembersAdapter(isUpdating: Boolean, index: Int){
        if (!isDestroyed && !isFinishing){
            runOnUiThread {
                updateMembers()
                if (isUpdating)
                    membersAdapter.notifyItemInserted(index)
                else
                    membersAdapter.notifyItemRemoved(index)
            }
        }
    }

    private fun updateJoinLeaveButton(enable: Boolean){
        if (!isDestroyed && !isFinishing){
            runOnUiThread {
                joinLeaveButton.visibility =  if (enable) View.VISIBLE else View.GONE

                if (isMember){
                    joinLeaveButton.background = getDrawable(R.drawable.background_leave_button)
                    joinLeaveButton.text = getString(R.string.leave_league)
                } else {
                    joinLeaveButton.background =  if (league.isPrivate) getDrawable(R.drawable.bg_join_league_disabled) else getDrawable(R.drawable.background_join_league)
                    joinLeaveButton.text = if (league.isPrivate) getString(R.string.private_league) else getString(R.string.join_league)

                    if (league.isPrivate)
                        joinLeaveButton.isEnabled = false
                }
            }
        }
    }

    /**************************************************************************************************
     * On-Click
     *************************************************************************************************/

    private fun validateName(){
        val currUser = FirebaseAuth.getInstance().currentUser
        if (currUser == null || currUser.uid.isEmpty()){
            if (currUser == null || currUser.uid.isEmpty()){
                Toast.makeText(this, "Please log in to continue.", Toast.LENGTH_LONG).show()
                return
            }
        }

        PlayerService.getPlayer(currUser.uid, PlayerService.PlayerCallback { player ->
            if (player == null) {
                Toast.makeText(this@LeagueActivity, "Please log in to continue.", Toast.LENGTH_LONG).show()
                return@PlayerCallback
            }

            if (player.name == null || player.name.isEmpty()) {
                DialogHelper.showAddNameDialog(this@LeagueActivity)
                return@PlayerCallback
            }

            // valid name
            joinLeaveLeague(currUser.uid)
        })
    }

    private fun joinLeaveLeague(uid: String) {

        val status = if (isMember) "none" else "member"
        updateJoinLeaveButton(false)
        progress.visibility = View.VISIBLE

        CloudService(CloudService.ProgressListener {
            if (it == null || it.isEmpty()){
                Toast.makeText(this, "Unable to continue. Check your internet connection and try again.", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val jsonObject = JSONObject(it)
                    val resultsObj = jsonObject.getJSONObject("result")

                    val success = resultsObj.getString("result")
                    if (success != "success") {
                        Toast.makeText(this, "Unable to continue. Try again later.", Toast.LENGTH_SHORT).show()
                    } else {
                        // successful
                        val memberStatus = resultsObj.getString("status")
                        isMember = memberStatus != "none"
                    }

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

            progress.visibility = View.GONE
            updateJoinLeaveButton(true)

        }).changeLeaguePlayerStatus(uid, league.id, status)
    }

    @OnClick(R.id.tags_recycler)
    fun showTagDialog(){

        if (!isMember && league.isPrivate){
            Toast.makeText(this@LeagueActivity, "Only members can add tags for a private league.", Toast.LENGTH_LONG).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add a tag")

        val view = LayoutInflater.from(this@LeagueActivity).inflate(R.layout.dialog_add_tag, null)
        builder.setView(view)

        builder.setNegativeButton("Cancel"){dialog,_ ->
            dialog.cancel()
        }

        builder.setNeutralButton("Add"){dialog,_ ->
            addTag(view.edit_tag.text.toString())
            dialog.cancel()
        }

        builder.create().show()
    }

    private fun addTag(tag: String?){
        if (tag == null || tag.isEmpty()){
            Toast.makeText(this, "Tag cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        if (league.tags != null && league.tags.contains(tag.toLowerCase())){
            Toast.makeText(this, "This tag already exists.", Toast.LENGTH_SHORT).show()
            return
        }

        // add tag to current list
        league.tags.add(league.tags.size - 1, tag.toLowerCase())
        tagsRecycler.adapter.notifyItemInserted(league.tags.size - 2)

        FireLeague.addTag(league.id, tag.toLowerCase())
    }



    /**************************************************************************************************
     * Firebase
     *************************************************************************************************/

    private fun fetchLeagueStatus(){
        val uid = FirebaseAuth.getInstance().uid
        if (uid != null && !uid.isEmpty()){
            databaseRef?.child(REF_LEAGUE_PLAYERS)
                    ?.child(league.id)
                    ?.child(uid)
                    ?.addListenerForSingleValueEvent(object: ValueEventListener{

                        override fun onDataChange(ref: DataSnapshot) {
                            if (ref.exists() && ref.value != null){
                                val status = ref.getValue(String::class.java)
                                if (status != "none") {
                                    isMember = true
                                }
                            }

                            progress.visibility = View.GONE
                            updateJoinLeaveButton(true)
                        }

                        override fun onCancelled(p0: DatabaseError) { }
                    })
        }
    }

    private fun fetchLeaguePlayers(){
        val leagueId : String = league.id ?: return

        playersChildListener = databaseRef?.child(REF_LEAGUE_PLAYERS)?.child(leagueId)?.addChildEventListener(object: ChildEventListener{
            override fun onCancelled(p0: DatabaseError) {}

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                if (p0.exists() && p0.value != null){
                    val userId = p0.key
                    val status = p0.value

                    if (status == "none"){
                        // user left league
                        val player = Player("")
                        player.uid = userId

                        val index = members.indexOf(player)
                        if (index > -1){
                            members.removeAt(index)
                            updateMembersAdapter(false, index)
                        }
                        return
                    } else {
                        // user joined league
                        if (userId != null) {
                            fetchUser(userId)
                        }
                    }
                }
            }

            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                if (p0.exists() && p0.value != null){
                    val userId = p0.key
                    val status = p0.getValue(String::class.java)

                    if (status != "none" && userId != null)
                        fetchUser(userId)
                }
            }

            override fun onChildRemoved(p0: DataSnapshot) { }
        })
    }

    fun fetchUser(userId: String){

        databaseRef?.child(REF_PLAYERS)?.child(userId)?.addListenerForSingleValueEvent(object: ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.value != null){
                    val player = dataSnapshot.getValue((Player::class.java))
                    if (player != null) {
                        player.uid = userId
                        members.add(player)
                        updateMembersAdapter(true, members.size - 1)
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) { }
        })
    }
}
