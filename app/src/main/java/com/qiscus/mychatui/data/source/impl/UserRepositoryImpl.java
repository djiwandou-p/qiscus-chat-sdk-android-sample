package com.qiscus.mychatui.data.source.impl;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.data.model.User;
import com.qiscus.mychatui.data.source.UserRepository;
import com.qiscus.mychatui.util.Action;
import com.qiscus.mychatui.util.AvatarUtil;
import com.qiscus.mychatui.util.Const;
import com.qiscus.sdk.chat.core.data.model.QAccount;
import com.qiscus.sdk.chat.core.data.model.QUser;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import rx.Emitter;


/**
 * Created on : January 30, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class UserRepositoryImpl implements UserRepository {

    private Context context;
    private SharedPreferences sharedPreferences;
    private Gson gson;

    public UserRepositoryImpl(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("user", Context.MODE_PRIVATE);
        gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    @Override
    public void login(String email, String password, String name, Action<User> onSuccess, Action<Throwable> onError) {
        Const.qiscusCore().setUser(email, password)
                .withUsername(name)
                .withAvatarUrl(AvatarUtil.generateAvatar(name))
                .save()
                .map(this::mapFromQiscusAccount)
                .doOnNext(this::setCurrentUser)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onSuccess::call, onError::call);
    }

    @Override
    public void getCurrentUser(Action<User> onSuccess, Action<Throwable> onError) {
        getCurrentUserObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onSuccess::call, onError::call);
    }

    @Override
    public void getUsers(long page, int limit, String searchUsername, Action<List<User>> onSuccess, Action<Throwable> onError) {
        Const.qiscusCore().getApi().getUsers(searchUsername, page, limit)
                .flatMap(Observable::fromIterable)
                .filter(user -> !user.equals(getCurrentUser()))
                .filter(user -> !user.getName().equals(""))
                .map(this::mapFromQiscusUser)
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onSuccess::call, onError::call);

    }

    @Override
    public void updateProfile(String name, Action<User> onSuccess, Action<Throwable> onError) {
        Const.qiscusCore().updateUserAsObservable(name, getCurrentUser().getAvatarUrl())
                .map(this::mapFromQiscusAccount)
                .doOnNext(this::setCurrentUser)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(onSuccess::call, onError::call);
    }

    @Override
    public void logout() {
        Const.qiscusCore1().removeDeviceToken(getCurrentDeviceToken());
        Const.qiscusCore2().removeDeviceToken(getCurrentDeviceToken());
        Const.qiscusCore1().clearUser();
        Const.qiscusCore2().clearUser();
        sharedPreferences.edit().clear().apply();
    }

    @Override
    public void setDeviceToken(String token) {
        setCurrentDeviceToken(token);
    }

    private Observable<User> getCurrentUserObservable() {
        return Observable.create(subscriber -> {
            try {
                subscriber.onNext(getCurrentUser());
            } catch (Exception e) {
                subscriber.onError(e);
            } finally {
                subscriber.onComplete();
            }
        });
    }

    private User getCurrentUser() {
        return gson.fromJson(sharedPreferences.getString("current_user", ""), User.class);
    }

    private void setCurrentUser(User user) {
        sharedPreferences.edit()
                .putString("current_user", gson.toJson(user))
                .apply();
    }

    private String getCurrentDeviceToken() {
        return sharedPreferences.getString("current_device_token", "");
    }

    private void setCurrentDeviceToken(String token) {
        sharedPreferences.edit()
                .putString("current_device_token", token)
                .apply();
    }

    private String getUsersData() throws IOException, JSONException {
        Resources resources = context.getResources();
        InputStream inputStream = resources.openRawResource(R.raw.users);

        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes);
        return new String(bytes);
    }

    private User mapFromQiscusAccount(QAccount qiscusAccount) {
        User user = new User();
        user.setId(qiscusAccount.getId());
        user.setName(qiscusAccount.getName());
        user.setAvatarUrl(qiscusAccount.getAvatarUrl());
        return user;
    }

    private User mapFromQiscusUser(QUser qUser) {
        User user = new User();
        user.setId(qUser.getId());
        user.setName(qUser.getName());
        user.setAvatarUrl(qUser.getAvatarUrl());
        return user;
    }
}
