package com.qiscus.mychatui.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.qiscus.mychatui.ui.ChatRoomActivity;
import com.qiscus.mychatui.ui.GroupChatRoomActivity;
import com.qiscus.mychatui.util.Const;
import com.qiscus.sdk.chat.core.data.model.QChatRoom;
import com.qiscus.sdk.chat.core.data.model.QMessage;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


/**
 * @author Yuana andhikayuana@gmail.com
 * @since Aug, Tue 14 2018 12.40
 **/
public class NotificationClickReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        QMessage qiscusComment = intent.getParcelableExtra("data");

        if (qiscusComment.getAppId().equals(Const.qiscusCore1().getAppId())) {
            Const.setQiscusCore(Const.qiscusCore1());
        } else {
            Const.setQiscusCore(Const.qiscusCore2());
        }
        Const.qiscusCore().getApi()
                .getChatRoomInfo(qiscusComment.getChatRoomId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(qiscusChatRoom -> Const.qiscusCore().getDataStore().addOrUpdate(qiscusChatRoom))
                .map(qiscusChatRoom -> getChatRoomActivity(context, qiscusChatRoom))
                .subscribe(newIntent -> start(context, newIntent), throwable ->
                        Toast.makeText(context, throwable.getLocalizedMessage(), Toast.LENGTH_SHORT).show());
    }

    private Intent getChatRoomActivity(Context context, QChatRoom qiscusChatRoom) {
        return qiscusChatRoom.getType().equals("group") ? GroupChatRoomActivity.generateIntent(context, qiscusChatRoom) :
                ChatRoomActivity.generateIntent(context, qiscusChatRoom);
    }

    private void start(Context context, Intent newIntent) {
        context.startActivity(newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
}
