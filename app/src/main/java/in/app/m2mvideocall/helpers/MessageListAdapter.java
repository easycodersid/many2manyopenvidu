package in.app.m2mvideocall.helpers;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.widget.AppCompatImageView;
import in.app.m2mvideocall.R;
import in.app.m2mvideocall.models.Message;

import static in.app.m2mvideocall.constants.JsonConstants.INBOUND_MSG;
import static in.app.m2mvideocall.constants.JsonConstants.MESSAGE_TYPE_IMAGE;
import static in.app.m2mvideocall.constants.JsonConstants.MESSAGE_TYPE_TEXT;
import static in.app.m2mvideocall.constants.JsonConstants.OUTBOUND_MSG;
import static in.app.m2mvideocall.utils.GeneralUtils.loadUrl;

public class MessageListAdapter extends BaseAdapter {

    List<Message> messages = new ArrayList<Message>();
    Context context;

    public MessageListAdapter(Context context){
        this.context = context;
    }

    public void add(Message message) {
        this.messages.add(message);
        //this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int i) {
        return messages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {

        MessageViewHolder holder = new MessageViewHolder();
        LayoutInflater messageInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        Message message = messages.get(i);

        if(message.getTypeOfMessage().equalsIgnoreCase(INBOUND_MSG)){
            if(message.getMessageType().equalsIgnoreCase(MESSAGE_TYPE_TEXT)){
                convertView = messageInflater.inflate(R.layout.incoming_message, null);

                TextView userName = convertView.findViewById(R.id.userName);
                TextView userMsg = convertView.findViewById(R.id.userMsg);
                userName.setText(message.getFrom());
                userMsg.setText(message.getMessage());

                convertView.setTag(holder);
            }else if(message.getMessageType().equalsIgnoreCase(MESSAGE_TYPE_IMAGE)){
                convertView = messageInflater.inflate(R.layout.incoming_image_message, null);

                TextView userName = convertView.findViewById(R.id.userName);
                userName.setText(message.getFrom());

                AppCompatImageView appCompatImageView = convertView.findViewById(R.id.imageMessage);
                loadUrl(message.getMessage(), context, appCompatImageView);
                convertView.setTag(holder);
            }
        }else if(message.getTypeOfMessage().equalsIgnoreCase(OUTBOUND_MSG)){
            if(message.getMessageType().equalsIgnoreCase(MESSAGE_TYPE_TEXT)){
                convertView = messageInflater.inflate(R.layout.my_message, null);
                TextView message_body = convertView.findViewById(R.id.message_body);
                message_body.setText(message.getMessage());

                convertView.setTag(holder);
            }else if(message.getMessageType().equalsIgnoreCase(MESSAGE_TYPE_IMAGE)){
                convertView = messageInflater.inflate(R.layout.my_image_message, null);

                AppCompatImageView appCompatImageView = convertView.findViewById(R.id.imageMessage);
                loadUrl(message.getMessage(), context, appCompatImageView);
                convertView.setTag(holder);
            }
        }


        return convertView;
    }
}
