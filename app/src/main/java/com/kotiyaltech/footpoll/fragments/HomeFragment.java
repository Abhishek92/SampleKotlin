package com.kotiyaltech.footpoll.fragments;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareButton;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.kotiyaltech.footpoll.R;
import com.kotiyaltech.footpoll.activity.PollResultsActivity;
import com.kotiyaltech.footpoll.config.FirebaseConfig;
import com.kotiyaltech.footpoll.database.Poll;
import com.kotiyaltech.footpoll.database.Polls;
import com.kotiyaltech.footpoll.database.Response;
import com.kotiyaltech.footpoll.database.VotedUser;
import com.kotiyaltech.footpoll.dialog.VoteDialog;
import com.kotiyaltech.footpoll.util.Util;
import com.kotiyaltech.footpoll.viewmodel.PollsViewModel;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {

    public static final String TAG = HomeFragment.class.getSimpleName();

    private Polls mIplPolls;
    private PollsViewModel pollsViewModel;
    private LinearLayout mPollContainer;
    private FirebaseUser mFirebaseUser;
    private VoteDialog voteDialog;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment getInstance(){
        return new HomeFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPollContainer = view.findViewById(R.id.pollsContainer);
        final ProgressBar progressBar = view.findViewById(R.id.progressBar);
        pollsViewModel = ViewModelProviders.of(this).get(PollsViewModel.class);
        pollsViewModel.getPolls().observe(this, new Observer<Polls>() {
            @Override
            public void onChanged(@Nullable Polls iplPolls) {
                progressBar.setVisibility(View.GONE);
                if(null != iplPolls){
                    loadPolls(iplPolls);
                }
            }
        });
    }

    private void loadPolls(Polls iplPolls) {
        mIplPolls = iplPolls;
        List<Poll> pollList = iplPolls.getPolls();
        mPollContainer.removeAllViews();
        if (Util.listNotNull(pollList)) {
            for (int i = 0; i < pollList.size(); i++) {
                final Poll poll = pollList.get(i);
                CardView pollsLayout = (CardView) LayoutInflater.from(getContext()).inflate(R.layout.polls_layout, null);
                TextView mPollQuestion = pollsLayout.findViewById(R.id.pollsQuestion);
                ImageView teamAFlagImg = pollsLayout.findViewById(R.id.teamOneImg);
                ImageView teamBFlagImg = pollsLayout.findViewById(R.id.teamTwoImg);
                TextView alreadyVotedText = pollsLayout.findViewById(R.id.alreadyVotedTxt);
                TextView teamAName = pollsLayout.findViewById(R.id.teamOneName);
                TextView teamBName = pollsLayout.findViewById(R.id.teamTwoName);
                Button mVoteBtn = pollsLayout.findViewById(R.id.voteBtn);
                RadioGroup pollRadioGroup = pollsLayout.findViewById(R.id.pollRg);
                RadioButton mTeamARadioBtn = pollsLayout.findViewById(R.id.teamARb);
                RadioButton mTeamBRadioBtn = pollsLayout.findViewById(R.id.teamBRb);
                TextView pollResults = pollsLayout.findViewById(R.id.pollResults);
                ShareButton deviceShareButton = pollsLayout.findViewById(R.id.fbShare);

                Glide.with(this).load(poll.getTeamAFlagUrl()).into(teamAFlagImg);
                Glide.with(this).load(poll.getTeamBFlagUrl()).into(teamBFlagImg);

                pollResults.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PollResultsActivity.startActivity(getContext(), poll.getId());
                    }
                });
                mVoteBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        CardView linearLayout = (CardView) view.getParent().getParent().getParent().getParent();
                        setVotingData(linearLayout);
                    }
                });


                mTeamARadioBtn.setTag(poll.getTeamA());
                mTeamBRadioBtn.setTag(poll.getTeamB());
                boolean isAlreadyVoted = checkIfUserAlreadyVoted(poll.getVotedUsers());
                deviceShareButton.setVisibility(!isAlreadyVoted ? View.GONE : View.VISIBLE);
                enableDisableRadioButton(isAlreadyVoted, pollRadioGroup);
                mVoteBtn.setVisibility(!isAlreadyVoted ? View.VISIBLE : View.GONE);
                deviceShareButton.setVisibility(!isAlreadyVoted ? View.GONE : View.VISIBLE);
                enableDisableRadioButton(isAlreadyVoted, pollRadioGroup);
                mVoteBtn.setVisibility(!isAlreadyVoted ? View.VISIBLE : View.GONE);
                VotedUser votedUser = getCurrentVotedUser(poll.getVotedUsers());

                if (votedUser != null) {
                    alreadyVotedText.setText(String.format("You've voted for %s", votedUser.getTeamVoted()));
                    createShareContent(deviceShareButton, votedUser.getTeamVoted());
                }
                alreadyVotedText.setVisibility(isAlreadyVoted ? View.VISIBLE : View.GONE);

                mPollQuestion.setText(poll.getQuestion());
                teamAName.setText(poll.getTeamA());
                teamBName.setText(poll.getTeamB());


                pollsLayout.setTag(i);
                mPollContainer.addView(pollsLayout);
                if (i == pollList.size() - 1)
                    loadAd(mPollContainer);

            }
        }
    }

    private void setVotingData(CardView cardView) {
        int position = (int) cardView.getTag();
        RadioGroup mPollRadioGroup = cardView.findViewById(R.id.pollRg);
        int selectedRadioButtonId = mPollRadioGroup.getCheckedRadioButtonId();
        if(selectedRadioButtonId == -1){
            Toast.makeText(getActivity(), "Choose your favourite team first", Toast.LENGTH_SHORT).show();
            return;
        }
        RadioButton selectedRadioButton = mPollRadioGroup.findViewById(selectedRadioButtonId);
        Poll poll = mIplPolls.getPolls().get(position);
        VotedUser votedUser = new VotedUser();
        votedUser.setTeamVoted(selectedRadioButton.getTag().toString());
        votedUser.setEmail(mFirebaseUser.getEmail());
        votedUser.setName(mFirebaseUser.getDisplayName());
        votedUser.setImageUrl(getProfilePicUrl());
        votedUser.setUId(mFirebaseUser.getUid());
        votedUser.setFacebookId(Util.getFacebookProfileId(mFirebaseUser));
        poll.addVotedUsers(votedUser);
        switch (selectedRadioButtonId){
            case R.id.teamARb:
                poll.voteForTeamA();
                break;
            case R.id.teamBRb:
                poll.voteForTeamB();
                break;
            default:
                break;
        }
        voteDialog = new VoteDialog(getContext(), votedUser.getTeamVoted(), poll.getId());
        saveIplPollsData();
    }

    private void saveIplPollsData() {
        pollsViewModel.savePollsData(mIplPolls).observe(this, new Observer<Response>() {
            @Override
            public void onChanged(@Nullable Response response) {
                if(null != response){
                    if (response.isSuccess()) {
                        voteDialog.show();
                        Toast.makeText(getContext(), "Vote submitted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Vote not submitted", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void enableDisableRadioButton(boolean isAlreadyVoted, RadioGroup radioGroup){
        int childCount = radioGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            radioGroup.getChildAt(i).setEnabled(!isAlreadyVoted);
        }
    }

    private void createShareContent(ShareButton deviceShareButton, String teamName){
        ShareLinkContent content = new ShareLinkContent.Builder()
                .setContentUrl(Uri.parse(FirebaseConfig.getInstance().getConfig().getString(FirebaseConfig.KEY.KEY_SHARE_LINK)))
                .setQuote("I've voted for my favourite team "+teamName)
                .build();
        deviceShareButton.setShareContent(content);

    }

    private boolean checkIfUserAlreadyVoted(List<VotedUser> votedUserList){
        VotedUser votedUser = new VotedUser();
        votedUser.setUId(mFirebaseUser.getUid());
        return votedUserList.contains(votedUser);
    }

    private VotedUser getCurrentVotedUser(List<VotedUser> votedUserList){
        VotedUser votedUser = new VotedUser();
        votedUser.setEmail(mFirebaseUser.getEmail());
        votedUser.setUId(mFirebaseUser.getUid());
        int index = votedUserList.indexOf(votedUser);
        if(index != -1)
            return votedUserList.get(index);
        else return null;
    }

    private String getProfilePicUrl(){
        // find the Facebook profile and get the user's id
        for(UserInfo profile : mFirebaseUser.getProviderData()) {
            // check if the provider id matches "facebook.com"
            if(FacebookAuthProvider.PROVIDER_ID.equals(profile.getProviderId())) {
                Uri photoUri = profile.getPhotoUrl();
                if(photoUri != null)
                    return photoUri.toString();
            }
        }
        return "";
    }

    private void loadAd(ViewGroup viewGroup) {
        AdView adView = new AdView(getActivity());
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(getActivity().getString(R.string.ad_unit_id));
        viewGroup.addView(adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }


}
