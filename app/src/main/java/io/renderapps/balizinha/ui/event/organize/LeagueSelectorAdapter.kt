package io.renderapps.balizinha.ui.event.organize

import android.content.Intent
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.renderapps.balizinha.R
import io.renderapps.balizinha.model.League
import io.renderapps.balizinha.service.StorageService
import io.renderapps.balizinha.util.PhotoHelper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_league_selector.*
import java.util.ArrayList

class LeagueSelectorAdapter(private val selectorActivity: LeagueSelectorActivity, private val leagues: ArrayList<League>) : RecyclerView.Adapter<LeagueSelectorAdapter.LeagueSelectorVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            LeagueSelectorVH(LayoutInflater.from(parent.context).inflate(R.layout.item_league_selector, parent, false))

    override fun onBindViewHolder(holder: LeagueSelectorVH, position: Int) = holder.bind(selectorActivity, leagues[position])

    override fun getItemCount(): Int = leagues.size

    class LeagueSelectorVH(override val containerView: View) : RecyclerView.ViewHolder(containerView),
            LayoutContainer {

        fun bind(selectorActivity: LeagueSelectorActivity, league: League) {
            league_title.text =  league.name
            setLogo(league.id)

            itemView.setOnClickListener { _ ->
                val intent = Intent(selectorActivity, CreateEventActivity::class.java)
                intent.putExtra(CreateEventActivity.EXTRA_LEAGUE, league)
                selectorActivity.startActivity(intent)
                selectorActivity.overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left)
            }
        }

        private fun setLogo(leagueId: String?){
            if (leagueId.isNullOrEmpty()) return

            StorageService.getLeagueHeader(leagueId, object: StorageService.StorageCallback{
                override fun onSuccess(uri: Uri?) {
                    if (uri != null) {
                        PhotoHelper.glideLeagueLogo(league_logo.context, league_logo, uri.toString(), R.drawable.ic_loading_image)
                    } else {
                        PhotoHelper.clearImage(league_logo.context, league_logo)
                        PhotoHelper.glideImageResource(league_logo.context, league_logo, R.drawable.default_league_logo)
                    }
                }
            })
        }
    }
}