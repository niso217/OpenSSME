package com.example.root.openssme.CitiesAutoComplete.rest.service;

import retrofit.RetrofitError;

/**
 * Created by DAVID-WORK on 22/06/2015.
 */
public interface RequestListener
{
    void onSuccess();
    void onFailed(RetrofitError error);
}
