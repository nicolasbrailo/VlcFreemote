package com.nicolasbrailo.vlcfreemote;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.nicolasbrailo.vlcfreemote.vlc_connector.RemoteVlc;

public abstract class VlcFragment extends Fragment {
    private RemoteVlc.ConnectionProvider vlcProvider;

    @Override
    public void onAttach(@NonNull Context activity) {
        super.onAttach(activity);

        try {
            vlcProvider = (RemoteVlc.ConnectionProvider) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement RemoteVlc.ConnectionProvider");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        vlcProvider = null;
    }

    RemoteVlc getVlc() {
        return vlcProvider.getActiveVlcConnection();
    }
}
