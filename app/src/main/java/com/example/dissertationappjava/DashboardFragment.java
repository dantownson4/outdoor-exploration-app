package com.example.dissertationappjava;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.dissertationappjava.databinding.FragmentFirstBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DashboardFragment extends Fragment {

    private MainActivity activity;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        activity = (MainActivity) getActivity();

        return inflater.inflate(R.layout.fragment_dashboard, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        DatabaseReference userDatabaseRef = FirebaseDatabase.getInstance("https://dissertation-androidstudio-default-rtdb.europe-west1.firebasedatabase.app/").getReference().child("users");

        final TextView textViewToChangeID = view.findViewById(R.id.userIDText);
        final TextView textViewToChangeScore = view.findViewById(R.id.userScoreText);
        final TextView textViewToChangeVisits = view.findViewById(R.id.userVisitedText);

        textViewToChangeID.setText(activity.userID);

        //Adds constant listener to score value in database to update UI when changed
        userDatabaseRef.child(activity.userID).child("score").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                textViewToChangeScore.setText(snapshot.getValue().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //Adds constant listener to score value in database to update UI when changed
        userDatabaseRef.child(activity.userID).child("visitedCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                textViewToChangeVisits.setText(snapshot.getValue().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

}