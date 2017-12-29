package io.renderapps.balizinha.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.activity.MainActivity;
import io.renderapps.balizinha.activity.SetupProfileActivity;

/**
 * Created by Joel Goncalves
 * 12/10/17
 */

public class AccountAdapter  extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {

    // stores and recycles views as they are scrolled off screen
    class ViewHolder extends RecyclerView.ViewHolder {
        TextView option;
        ImageView rightArrow;
        Switch notificationSwitch;

        ViewHolder(View itemView) {
            super(itemView);
            option = itemView.findViewById(R.id.option);
            rightArrow = itemView.findViewById(R.id.option_icon);
            notificationSwitch = itemView.findViewById(R.id.notification_switch);
        }
    }

    // properties
    private String[] options;
    private Context mContext;
    public AccountAdapter(Context context, String[] options){
        this.mContext = context;
        this.options = options;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final String option = options[position];
        holder.option.setText(option);
//        if (position == 1) {
//            // show notification switch
//            holder.notificationSwitch.setVisibility(View.VISIBLE);
//            holder.rightArrow.setVisibility(View.GONE);
//        } else {
//            // hide notification switch
//            holder.notificationSwitch.setVisibility(View.GONE);
//            holder.rightArrow.setVisibility(View.VISIBLE);
//        }
        // hide arrow for version option
        if (position == 1)
            holder.rightArrow.setVisibility(View.GONE);

        // listener
//        holder.itemView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                switch (position) {
//                    case 0:
//                        Intent updateIntent = new Intent(mContext, SetupProfileActivity.class);
//                        updateIntent.putExtra(SetupProfileActivity.EXTRA_PROFILE_UPDATE, true);
//                        mContext.startActivity(updateIntent);
//                        break;
//                    case 1:
//                        holder.notificationSwitch.setChecked(!holder.notificationSwitch.isChecked());
//                        break;
//                    case 2:
//                        Toast.makeText(mContext, "Coming soon", Toast.LENGTH_SHORT).show();
//                        break;
//                    case 3:
//                        break;
//                    case 4:
//                        // logout
//                        FirebaseAuth auth = FirebaseAuth.getInstance();
//                        for (UserInfo info : auth.getCurrentUser().getProviderData())
//                            if (info.getProviderId().equals("facebook.com"))
//                                LoginManager.getInstance().logOut();
//                        auth.signOut();
//                        break;
//                }
//            }
//        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (position) {
                    case 0:
                        Intent updateIntent = new Intent(mContext, SetupProfileActivity.class);
                        updateIntent.putExtra(SetupProfileActivity.EXTRA_PROFILE_UPDATE, true);
                        mContext.startActivity(updateIntent);
                        ((MainActivity)mContext).overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                        break;
                    case 1:
                        break;
                    case 2:
                        // logout
                        FirebaseAuth auth = FirebaseAuth.getInstance();
                        for (UserInfo info : auth.getCurrentUser().getProviderData())
                            if (info.getProviderId().equals("facebook.com"))
                                LoginManager.getInstance().logOut();
                        auth.signOut();
                        break;
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return options.length;
    }
}
