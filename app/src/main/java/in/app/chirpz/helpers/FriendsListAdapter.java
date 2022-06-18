package in.app.chirpz.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import in.app.chirpz.R;
import in.app.chirpz.SessionActivity;
import in.app.chirpz.models.Friend;

public class FriendsListAdapter extends BaseAdapter {

    Context context;
    ArrayList<Friend> arrayList;
    SessionActivity sessionActivity;

    public FriendsListAdapter(Context context, SessionActivity sessionActivity){
        this.context = context;
        this.sessionActivity = sessionActivity;
        arrayList = new ArrayList();
    }

    public void addMember(Friend friend){
        arrayList.add(friend);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public Friend getItem(int i) {
        return arrayList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        MyViewHolder myViewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.friendsrowlayout, parent, false);
            myViewHolder = new MyViewHolder(convertView);
            convertView.setTag(myViewHolder);
        } else {
            myViewHolder = (MyViewHolder) convertView.getTag();
        }

        myViewHolder.name.setText(getItem(position).getName());
        myViewHolder.email.setText(getItem(position).getEmail());
        myViewHolder.callBtn.setTag(getItem(position));

        myViewHolder.callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sessionActivity.makeCallToFriend((Friend) view.getTag());
            }
        });

        return convertView;
    }

    public class MyViewHolder{
        TextView name, email;
        Button callBtn;

        public MyViewHolder(View view){
            name = view.findViewById(R.id.name);
            email = view.findViewById(R.id.email);
            callBtn = view.findViewById(R.id.callBtn);

        }
    }
}
