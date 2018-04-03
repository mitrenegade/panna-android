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
import com.stripe.android.CustomerSession;
import com.stripe.android.view.PaymentMethodsActivity;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.activity.MainActivity;
import io.renderapps.balizinha.activity.SetupProfileActivity;
import io.renderapps.balizinha.util.Constants;

import static io.renderapps.balizinha.activity.MainActivity.REQUEST_CODE_SELECT_SOURCE;

/**
 * Account adapter to handle profile, payment and settings changes
 */

public class AccountAdapter  extends RecyclerView.Adapter<AccountAdapter.ViewHolder> {

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
    private boolean paymentRequired;
    public AccountAdapter(Context context, String[] options, boolean paymentRequired){
        this.mContext = context;
        this.options = options;
        this.paymentRequired = paymentRequired;
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

        if (option.startsWith("Version")) {
            holder.rightArrow.setVisibility(View.GONE);
            holder.option.setText(Constants.APP_VERSION);
            if (Constants.IN_DEV_MODE)
                holder.option.setText(holder.option.getText().toString().concat("t"));
        }

        if (paymentRequired) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (position) {
                        case 0:
                            Intent updateIntent = new Intent(mContext, SetupProfileActivity.class);
                            updateIntent.putExtra(SetupProfileActivity.EXTRA_PROFILE_UPDATE, true);
                            mContext.startActivity(updateIntent);
                            ((MainActivity) mContext).overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                            break;
                        case 1:
                            Intent payIntent = PaymentMethodsActivity.newIntent(mContext);
                            ((MainActivity) mContext).startActivityForResult(payIntent, REQUEST_CODE_SELECT_SOURCE);
                            ((MainActivity) mContext).overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                            break;
                        case 2:
                            break;
                        case 3:
                            // logout
                            FirebaseAuth auth = FirebaseAuth.getInstance();
                            for (UserInfo info : auth.getCurrentUser().getProviderData())
                                if (info.getProviderId().equals("facebook.com"))
                                    LoginManager.getInstance().logOut();
                            CustomerSession.endCustomerSession();
                            auth.signOut();
                            break;
                    }
                }
            });
        } else {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (position) {
                        case 0:
                            Intent updateIntent = new Intent(mContext, SetupProfileActivity.class);
                            updateIntent.putExtra(SetupProfileActivity.EXTRA_PROFILE_UPDATE, true);
                            mContext.startActivity(updateIntent);
                            ((MainActivity) mContext).overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);
                            break;
                        case 1:
                            break;
                        case 2:
                            // logout
                            FirebaseAuth auth = FirebaseAuth.getInstance();
                            for (UserInfo info : auth.getCurrentUser().getProviderData())
                                if (info.getProviderId().equals("facebook.com"))
                                    LoginManager.getInstance().logOut();
                            CustomerSession.endCustomerSession();
                            auth.signOut();
                            break;
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return options.length;
    }

}
