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
import in.app.chirpz.models.Member;

public class MembersAdapter extends BaseAdapter {

    Context context;
    ArrayList<Member> arrayList = new ArrayList();
    SessionActivity sessionActivity;

    public MembersAdapter(Context context, SessionActivity sessionActivity){
        this.context = context;
        this.sessionActivity = sessionActivity;
    }

    public void addMember(Member member){
        arrayList.add(member);
        notifyDataSetChanged();
    }

    public void updateMember(Member member){
        for(int a=0; a<=arrayList.size()-1; a++){
            if(arrayList.get(a).getId().equalsIgnoreCase(member.getId())){
                arrayList.get(a).approved = member.approved;
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public  Member getItem(int i) {
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
                    inflate(R.layout.participantsrow, parent, false);
            myViewHolder = new MyViewHolder(convertView);
            convertView.setTag(myViewHolder);
        } else {
            myViewHolder = (MyViewHolder) convertView.getTag();
        }

        myViewHolder.name.setText(getItem(position).name);
        myViewHolder.id.setText(getItem(position).id);

        if(getItem(position).approved){
            myViewHolder.approvalBtn.setEnabled(false);
            myViewHolder.approvalBtn.setText("Approved");
        }else{
            myViewHolder.approvalBtn.setEnabled(true);
            myViewHolder.approvalBtn.setText("Approve");
        }

        myViewHolder.approvalBtn.setTag(position);

        myViewHolder.approvalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sessionActivity.approveUser((Integer) view.getTag());
            }
        });

        return convertView;
    }

    public class MyViewHolder{
        TextView name, id;
        Button approvalBtn;

        public MyViewHolder(View view){
            name = view.findViewById(R.id.name);
            id = view.findViewById(R.id.id);
            approvalBtn = view.findViewById(R.id.approveBtn);
        }
    }

}
