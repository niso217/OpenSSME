package com.example.root.openssme;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import com.example.root.openssme.SocialNetwork.User;
import com.example.root.openssme.Utils.Constants;

/**
 * Created by nir on 18/03/2016.
 */
public class ProfileFragmentTest extends Fragment implements
        View.OnClickListener {

    private static final String TAG = "ProfileFragmentTest";
    private static final int REQ_SELECT_PHOTO = 1;
    private EditText subject,body,url;

/*
    ****example of using social network functions*****
 */

    public ProfileFragmentTest(){

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==REQ_SELECT_PHOTO){

            if (User.getInstance().source.equals(Constants.GPLUS)) {
                MyApplication.getSocialNetworkHelper().GooglePostPhoto(getActivity(), data);
            }
            if (User.getInstance().source.equals(Constants.FACEBOOK)) {
                MyApplication.getSocialNetworkHelper().FacebookPostPhoto(getActivity(), data);

            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View rootFragment = inflater.inflate(R.layout.fragment_profile_test, null);

        Picasso.with(getActivity())
                .load(User.getInstance().image)
                .resize(400, 400)
                .centerCrop()
                .into((ImageView) rootFragment.findViewById(R.id.imageView));
        TextView id = (TextView)rootFragment.findViewById(R.id.textViewId);
        TextView name = (TextView)rootFragment.findViewById(R.id.textViewName);
        TextView token = (TextView)rootFragment.findViewById(R.id.textViewToken);
        TextView email = (TextView)rootFragment.findViewById(R.id.textViewEmail);
        TextView source = (TextView)rootFragment.findViewById(R.id.textViewSource);
        rootFragment.findViewById(R.id.buttonLogOut).setOnClickListener(this);
        rootFragment.findViewById(R.id.buttonSharePhoto).setOnClickListener(this);
        rootFragment.findViewById(R.id.buttonSharePost).setOnClickListener(this);

         subject = (EditText)rootFragment.findViewById(R.id.editTextSubject);
         body = (EditText)rootFragment.findViewById(R.id.editTextBody);
        url = (EditText)rootFragment.findViewById(R.id.editTextURL);





        id.setText(id.getText() + User.getInstance().id);
        name.setText(name.getText() + User.getInstance().name);
        token.setText(token.getText() + User.getInstance().accesstoken);
        email.setText(email.getText() + User.getInstance().email);
        source.setText(source.getText() + User.getInstance().source);

        return rootFragment;
    }

    private void postStatusUpdate() {
        if (User.getInstance().source.equals(Constants.GPLUS)) {
            MyApplication.getSocialNetworkHelper().PostOnGoogle(getActivity(),subject.getText().toString(),body.getText().toString(),url.getText().toString());
        }
        if (User.getInstance().source.equals(Constants.FACEBOOK)) {
            MyApplication.getSocialNetworkHelper().PostOnFacebook(getActivity(),subject.getText().toString(),body.getText().toString(),url.getText().toString());
        }
    }



    private void SharePhoto() {
        Intent photoPicker = new Intent(Intent.ACTION_PICK);
        photoPicker.setType("video/*, image/*");
        startActivityForResult(photoPicker, REQ_SELECT_PHOTO);
    }

            @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.buttonLogOut:
                    //signOut();
                break;

            case R.id.buttonSharePost:
                postStatusUpdate();
                break;

            case R.id.buttonSharePhoto:
                SharePhoto();
                break;

        }
    }


}
