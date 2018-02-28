package jp.techacademy.yasufumi.hagiwara.taskapp;

import android.app.Application;

import io.realm.Realm;

/**
 * Created by hagiwara on 2018/02/20.
 */

public class TaskApp extends Application {
    @Override
    public void onCreate(){
        super.onCreate();
        Realm.init(this);
    }

}
