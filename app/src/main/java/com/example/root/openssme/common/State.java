package com.example.root.openssme.common;

/**
 * Created by nir on 22/04/2016.
 */
public enum State {

    OPENED {
        @Override
        void disconnect(GoogleConnection googleConnection) {
            googleConnection.onSignOut();
        }

    },
    CLOSED {
        @Override
        void connect(GoogleConnection googleConnection) {
            googleConnection.onSignIn();
        }
    };

    void connect(GoogleConnection googleConnection) {
    }

    void disconnect(GoogleConnection googleConnection) {
    }

}
