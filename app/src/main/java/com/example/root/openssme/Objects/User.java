package com.example.root.openssme.Objects;

/**
 * Created by nir on 16/03/2016.
 */
public class User {


    public String source;
    public String name;
    public String email;
    public String id;
    public String gender;
    public String accesstoken;
    public String image;
    public Gate gate;

    private static User ourInstance = new User();

    public static User getInstance() {
        return ourInstance;
    }

    public User(){

    }


    public void copy(User user){
        this.image = user.image;
        this.source = user.source;
        this.name = user.name;
        this.email = user.email;
        this.id = user.id;
        this.gender = user.gender;
        this.accesstoken = user.accesstoken;

    }

    public void clear(){
        ourInstance = new User();
    }








}
