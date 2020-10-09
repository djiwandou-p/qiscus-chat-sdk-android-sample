package com.qiscus.mychatui.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.RequestOptions;
import com.qiscus.mychatui.R;
import com.qiscus.mychatui.ui.view.QiscusProgressView;
import com.qiscus.mychatui.util.Const;
import com.qiscus.mychatui.util.DateUtil;
import com.qiscus.mychatui.util.QiscusImageUtil;
import com.qiscus.nirmana.Nirmana;
import com.qiscus.sdk.chat.core.data.model.QChatRoom;
import com.qiscus.sdk.chat.core.data.model.QMessage;
import com.qiscus.sdk.chat.core.util.QiscusDateUtil;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


/**
 * Created on : January 31, 2018
 * Author     : zetbaitsu
 * Name       : Zetra
 * GitHub     : https://github.com/zetbaitsu
 */
public class CommentsAdapter extends SortedRecyclerViewAdapter<QMessage, CommentsAdapter.VH> {

    private static final int TYPE_MY_TEXT = 1;
    private static final int TYPE_OPPONENT_TEXT = 2;
    private static final int TYPE_MY_IMAGE = 3;
    private static final int TYPE_OPPONENT_IMAGE = 4;
    private static final int TYPE_MY_FILE = 5;
    private static final int TYPE_OPPONENT_FILE = 6;


    private Context context;
    private long lastDeliveredCommentId;
    private long lastReadCommentId;
    private String myEmail;

    public CommentsAdapter(Context context) {
        this.context = context;
        this.myEmail = Const.qiscusCore().getQiscusAccount().getId();
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    @Override
    protected Class<QMessage> getItemClass() {
        return QMessage.class;
    }

    @Override
    protected int compare(QMessage item1, QMessage item2) {
        if (item2.equals(item1)) { //Same comments
            return 0;
        } else if (item2.getId() == -1 && item1.getId() == -1) { //Not completed comments
            return item2.getTimestamp().compareTo(item1.getTimestamp());
        } else if (item2.getId() != -1 && item1.getId() != -1) { //Completed comments
            return Const.qiscusCore().getAndroidUtil().compare(item2.getId(), item1.getId());
        } else if (item2.getId() == -1) {
            return 1;
        } else if (item1.getId() == -1) {
            return -1;
        }
        return item2.getTimestamp().compareTo(item1.getTimestamp());
    }

    @Override
    public int getItemViewType(int position) {
        QMessage comment = getData().get(position);
        comment.getType().toString();
        switch (comment.getType()) {
            case IMAGE:
                return comment.isMyComment(myEmail) ? TYPE_MY_IMAGE : TYPE_OPPONENT_IMAGE;
            case FILE:
                return comment.isMyComment(myEmail) ? TYPE_MY_FILE : TYPE_OPPONENT_FILE;
            default:
                return comment.isMyComment(myEmail) ? TYPE_MY_TEXT : TYPE_OPPONENT_TEXT;
        }
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_MY_TEXT:
            case TYPE_OPPONENT_TEXT:
                return new TextVH(getView(parent, viewType));
            case TYPE_MY_IMAGE:
            case TYPE_OPPONENT_IMAGE:
                return new ImageVH(getView(parent, viewType));
            case TYPE_MY_FILE:
            case TYPE_OPPONENT_FILE:
                return new FileVH(getView(parent, viewType));
            default:
                return new TextVH(getView(parent, viewType));
        }
    }

    private View getView(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_MY_TEXT:
                return LayoutInflater.from(context).inflate(R.layout.item_my_text_comment, parent, false);
            case TYPE_OPPONENT_TEXT:
                return LayoutInflater.from(context).inflate(R.layout.item_opponent_text_comment, parent, false);
            case TYPE_MY_IMAGE:
                return LayoutInflater.from(context).inflate(R.layout.item_my_image_comment, parent, false);
            case TYPE_OPPONENT_IMAGE:
                return LayoutInflater.from(context).inflate(R.layout.item_opponent_image_comment, parent, false);
            case TYPE_MY_FILE:
                return LayoutInflater.from(context).inflate(R.layout.item_my_file_comment, parent, false);
            case TYPE_OPPONENT_FILE:
                return LayoutInflater.from(context).inflate(R.layout.item_opponent_file_comment, parent, false);
            default:
                return LayoutInflater.from(context).inflate(R.layout.item_opponent_text_comment, parent, false);
        }
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        holder.bind(getData().get(position));
        holder.position = position;

        if (position == getData().size() - 1) {
            holder.setNeedToShowDate(true);
        } else {
            holder.setNeedToShowDate(!QiscusDateUtil.isDateEqualIgnoreTime(getData().get(position).getTimestamp(),
                    getData().get(position + 1).getTimestamp()));
        }


        setOnClickListener(holder.itemView, position);

    }

    public void addOrUpdate(List<QMessage> comments) {
        for (QMessage comment : comments) {
            int index = findPosition(comment);
            if (index == -1) {
                getData().add(comment);
            } else {
                getData().updateItemAt(index, comment);
            }
        }
        notifyDataSetChanged();
    }

    public void addOrUpdate(QMessage comment) {
        int index = findPosition(comment);
        if (index == -1) {
            getData().add(comment);
        } else {
            getData().updateItemAt(index, comment);
        }
        notifyDataSetChanged();
    }

    public void remove(QMessage comment) {
        getData().remove(comment);
        notifyDataSetChanged();
    }

    public QMessage getLatestSentComment() {
        int size = getData().size();
        for (int i = 0; i < size; i++) {
            QMessage comment = getData().get(i);
            if (comment.getStatus() >= QMessage.STATE_SENT) {
                return comment;
            }
        }
        return null;
    }

    public void updateLastDeliveredComment(long lastDeliveredCommentId) {
        this.lastDeliveredCommentId = lastDeliveredCommentId;
        updateCommentState();
        notifyDataSetChanged();
    }

    public void updateLastReadComment(long lastReadCommentId) {
        this.lastReadCommentId = lastReadCommentId;
        this.lastDeliveredCommentId = lastReadCommentId;
        updateCommentState();
        notifyDataSetChanged();
    }

    private void updateCommentState() {
        int size = getData().size();
        for (int i = 0; i < size; i++) {
            if (getData().get(i).getStatus() > QMessage.STATE_SENDING) {
                if (getData().get(i).getId() <= lastReadCommentId) {
                    if (getData().get(i).getStatus() == QMessage.STATE_READ) {
                        break;
                    }
                    getData().get(i).setStatus(QMessage.STATE_READ);
                } else if (getData().get(i).getId() <= lastDeliveredCommentId) {
                    if (getData().get(i).getStatus() == QMessage.STATE_DELIVERED) {
                        break;
                    }
                    getData().get(i).setStatus(QMessage.STATE_DELIVERED);
                }
            }
        }
    }

    public interface RecyclerViewItemClickListener {
        void onItemClick(View view, int position);

        void onItemLongClick(View view, int position);
    }

    static class VH extends RecyclerView.ViewHolder {
        public int position = 0;
        private ImageView avatar;
        private TextView sender;
        private TextView date;
        private TextView dateOfMessage;
        @Nullable
        private ImageView state;
        private int pendingStateColor;
        private int readStateColor;
        private int failedStateColor;

        VH(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            sender = itemView.findViewById(R.id.sender);
            date = itemView.findViewById(R.id.date);
            dateOfMessage = itemView.findViewById(R.id.dateOfMessage);
            state = itemView.findViewById(R.id.state);

            pendingStateColor = ContextCompat.getColor(itemView.getContext(), R.color.pending_message);
            readStateColor = ContextCompat.getColor(itemView.getContext(), R.color.read_message);
            failedStateColor = ContextCompat.getColor(itemView.getContext(), R.color.qiscus_red);

        }

        void bind(QMessage comment) {
            Nirmana.getInstance().get()
                    .setDefaultRequestOptions(new RequestOptions()
                            .placeholder(R.drawable.ic_qiscus_avatar)
                            .error(R.drawable.ic_qiscus_avatar)
                            .dontAnimate())
                    .load(comment.getSender().getAvatarUrl())
                    .into(avatar);
            if (sender != null) {
                sender.setText(comment.getSender().getName());
            }
            date.setText(DateUtil.getTimeStringFromDate(comment.getTimestamp()));
            if (dateOfMessage != null) {
                dateOfMessage.setText(DateUtil.toFullDate(comment.getTimestamp()));
            }

            renderState(comment);

        }

        void setNeedToShowDate(Boolean showDate) {
            if (showDate == true) {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.VISIBLE);
                }
            } else {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.GONE);
                }
            }
        }

        private void renderState(QMessage comment) {
            if (state != null) {
                switch (comment.getStatus()) {
                    case QMessage.STATE_PENDING:
                    case QMessage.STATE_SENDING:
                        state.setColorFilter(pendingStateColor);
                        state.setImageResource(R.drawable.ic_qiscus_info_time);
                        break;
                    case QMessage.STATE_SENT:
                        state.setColorFilter(pendingStateColor);
                        state.setImageResource(R.drawable.ic_qiscus_sending);
                        break;
                    case QMessage.STATE_DELIVERED:
                        state.setColorFilter(pendingStateColor);
                        state.setImageResource(R.drawable.ic_qiscus_read);
                        break;
                    case QMessage.STATE_READ:
                        state.setColorFilter(readStateColor);
                        state.setImageResource(R.drawable.ic_qiscus_read);
                        break;
                    case QMessage.STATE_FAILED:
                        state.setColorFilter(failedStateColor);
                        state.setImageResource(R.drawable.ic_qiscus_sending_failed);
                        break;
                }
            }
        }
    }

    static class TextVH extends VH {
        private TextView message;
        private TextView sender;
        private TextView dateOfMessage;

        TextVH(View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.message);
            sender = itemView.findViewById(R.id.sender);
            dateOfMessage = itemView.findViewById(R.id.dateOfMessage);
        }

        @Override
        void bind(QMessage comment) {
            super.bind(comment);
            message.setText(comment.getText());
            QChatRoom chatRoom = Const.qiscusCore().getDataStore().getChatRoom(comment.getChatRoomId());

            if (sender != null && chatRoom != null) {
                if (!chatRoom.getType().equals("group")) {
                    sender.setVisibility(View.GONE);
                } else {
                    sender.setVisibility(View.VISIBLE);
                }
            }

            if (dateOfMessage != null) {
                dateOfMessage.setText(DateUtil.toFullDate(comment.getTimestamp()));
            }
        }

        @Override
        void setNeedToShowDate(Boolean showDate) {
            if (showDate == true) {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.VISIBLE);
                }
            } else {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.GONE);
                }
            }
        }
    }

    static class ImageVH extends VH {
        private ImageView thumbnail;
        private TextView messageCaption;
        private TextView sender;
        private TextView dateOfMessage;

        ImageVH(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            messageCaption = itemView.findViewById(R.id.messageCaption);
            sender = itemView.findViewById(R.id.sender);
            dateOfMessage = itemView.findViewById(R.id.dateOfMessage);
        }

        @Override
        void bind(QMessage comment) {
            super.bind(comment);

            try {
                JSONObject obj = new JSONObject(comment.getPayload());
                String url = obj.getString("url");
                String caption = obj.getString("caption");
                String filename = obj.getString("file_name");

                if (url.startsWith("http")) { //We have sent it
                    showSentImage(comment, url);
                } else { //Still uploading the image
                    showSendingImage(url);
                }

                if (caption.isEmpty()) {
                    messageCaption.setVisibility(View.GONE);
                } else {
                    messageCaption.setVisibility(View.VISIBLE);
                    messageCaption.setText(caption);
                }

                QChatRoom chatRoom = Const.qiscusCore().getDataStore().getChatRoom(comment.getChatRoomId());

                if (sender != null) {
                    if (!chatRoom.getType().equals("group")) {
                        sender.setVisibility(View.GONE);
                    } else {
                        sender.setVisibility(View.VISIBLE);
                    }
                }

                if (dateOfMessage != null) {
                    dateOfMessage.setText(DateUtil.toFullDate(comment.getTimestamp()));
                }

                thumbnail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        File localPath = Const.qiscusCore().getDataStore().getLocalPath(comment.getId());
                        if (localPath != null) {
                            Toast.makeText(itemView.getContext(), "Image already in the gallery", Toast.LENGTH_SHORT).show();
                        } else {
                            downloadFile(comment, filename, url);
                        }
                    }
                });

            } catch (Throwable t) {
                Log.e("SampleCore", "Could not parse malformed JSON: \"" + comment.getPayload() + "\"");
            }

        }

        public void downloadFile(QMessage qiscusComment, String fileName, String URLImage) {
            Const.qiscusCore().getApi()
                    .downloadFile(URLImage, fileName, total -> {
                        // here you can get the progress total downloaded
                    })
                    .doOnNext(file -> {
                        // here we update the local path of file
                        Const.qiscusCore().getDataStore()
                                .addOrUpdateLocalPath(qiscusComment.getChatRoomId(), qiscusComment.getId(), file.getAbsolutePath());

                        QiscusImageUtil.addImageToGallery(file);
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(file -> {
                        //on success
                        Toast.makeText(itemView.getContext(), "success save image to gallery", Toast.LENGTH_SHORT).show();
                    }, throwable -> {
                        //on error
                    });
        }

        @Override
        void setNeedToShowDate(Boolean showDate) {
            if (showDate == true) {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.VISIBLE);
                }
            } else {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.GONE);
                }
            }
        }

        private void showSendingImage(String url) {
            File localPath = new File(url);
            showLocalImage(localPath);
        }

        private void showSentImage(QMessage comment, String url) {
            File localPath = Const.qiscusCore().getDataStore().getLocalPath(comment.getId());
            if (localPath != null) {
                showLocalImage(localPath);
            } else {
                Nirmana.getInstance().get()
                        .setDefaultRequestOptions(new RequestOptions()
                                .placeholder(R.drawable.ic_qiscus_add_image)
                                .error(R.drawable.ic_qiscus_add_image)
                                .dontAnimate())
                        .load(url)
                        .into(thumbnail);
            }
        }

        private void showLocalImage(File localPath) {
            Nirmana.getInstance().get()
                    .setDefaultRequestOptions(new RequestOptions()
                            .placeholder(R.drawable.ic_qiscus_add_image)
                            .error(R.drawable.ic_qiscus_add_image)
                            .dontAnimate())
                    .load(localPath)
                    .into(thumbnail);
        }
    }

    static class FileVH extends VH implements QMessage.ProgressListener, QMessage.DownloadingListener {
        private TextView fileName;
        private TextView sender;
        private TextView dateOfMessage;
        private QiscusProgressView progress;
        private ImageView icFile;

        FileVH(View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.file_name);
            sender = itemView.findViewById(R.id.sender);
            dateOfMessage = itemView.findViewById(R.id.dateOfMessage);
            progress = itemView.findViewById(R.id.progress);
            icFile = itemView.findViewById(R.id.ic_file);
        }

        @Override
        void bind(QMessage comment) {
            super.bind(comment);
            QChatRoom chatRoom = Const.qiscusCore().getDataStore().getChatRoom(comment.getChatRoomId());

            if (sender != null) {
                if (!chatRoom.getType().equals("group")) {
                    sender.setVisibility(View.GONE);
                } else {
                    sender.setVisibility(View.VISIBLE);
                }
            }

            try {
                JSONObject obj =  new JSONObject (comment.getPayload());
                String url = obj.getString("url");
                String filename = obj.getString("file_name");
                fileName.setText(filename);

                if (dateOfMessage != null) {
                    dateOfMessage.setText(DateUtil.toFullDate(comment.getTimestamp()));
                }

                fileName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        File localPath = Const.qiscusCore().getDataStore().getLocalPath(comment.getId());
                        if (localPath != null) {
                            QiscusImageUtil.addImageToGallery(localPath);
                            Toast.makeText(itemView.getContext(), "File already save", Toast.LENGTH_SHORT).show();
                        } else {
                            downloadFile(comment, filename, url);
                        }
                    }
                });

            } catch (Throwable t) {
                Log.e("SampleCore", "Could not parse malformed JSON: \"" + comment.getPayload() + "\"");
            }

        }

        @Override
        void setNeedToShowDate(Boolean showDate) {
            if (showDate == true) {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.VISIBLE);
                }
            } else {
                if (dateOfMessage != null) {
                    dateOfMessage.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void onProgress(QMessage qiscusComment, int percentage) {
            progress.setProgress(percentage);
            icFile.setVisibility(View.GONE);
            if (percentage == 100) {
                progress.setVisibility(View.GONE);
                icFile.setVisibility(View.VISIBLE);
            } else {
                progress.setVisibility(View.VISIBLE);
                icFile.setVisibility(View.GONE);
            }
        }

        @Override
        public void onDownloading(QMessage qiscusComment, boolean downloading) {
            progress.setVisibility(downloading ? View.VISIBLE : View.GONE);
        }

        public void downloadFile(QMessage qiscusComment, String fileName, String URLFile) {
            Const.qiscusCore().getApi()
                    .downloadFile(URLFile, fileName, total -> {
                        // here you can get the progress total downloaded
                    })
                    .doOnNext(file -> {
                        // here we update the local path of file
                        Const.qiscusCore().getDataStore()
                                .addOrUpdateLocalPath(qiscusComment.getChatRoomId(), qiscusComment.getId(), file.getAbsolutePath());

                        QiscusImageUtil.addImageToGallery(file);
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(file -> {
                        //on success
                        Toast.makeText(itemView.getContext(), "Success save file", Toast.LENGTH_SHORT).show();
                    }, throwable -> {
                        //on error
                    });
        }
    }
}
