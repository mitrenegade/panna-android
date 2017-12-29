package io.renderapps.balizinha.fragment;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.activity.MainActivity;
import io.renderapps.balizinha.adapter.AccountAdapter;


public class AccountFragment extends Fragment {
    private Context mContext;

    public AccountFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_account, container, false);
        Toolbar toolbar = root.findViewById(R.id.account_toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.title_account);

        // setup recycler
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        RecyclerView recyclerView = root.findViewById(R.id.account_recycler);
        recyclerView.hasFixedSize();
        recyclerView.setLayoutManager(layoutManager);

        // adapter
        String[] accountOptions = getResources().getStringArray(R.array.accountOptions);
        recyclerView.setAdapter(new AccountAdapter(getActivity(), accountOptions));

        return root;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
