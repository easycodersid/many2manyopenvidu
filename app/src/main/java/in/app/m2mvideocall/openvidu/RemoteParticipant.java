package in.app.m2mvideocall.openvidu;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import org.webrtc.SurfaceViewRenderer;

public class RemoteParticipant extends Participant {

    private View view;
    private SurfaceViewRenderer videoView;
    private TextView participantNameText;
    private ImageButton videoOnOff;

    public RemoteParticipant(String connectionId, String participantName, Session session) {
        super(connectionId, participantName, session);
        this.session.addRemoteParticipant(this);
    }

    public View getView() {
        return this.view;
    }

    public void setView(View view) {
        this.view = view;
    }

    public SurfaceViewRenderer getVideoView() {
        return this.videoView;
    }

    public void setVideoView(SurfaceViewRenderer videoView) {
        this.videoView = videoView;
    }

    public TextView getParticipantNameText() {
        return this.participantNameText;
    }

    public void setParticipantNameText(TextView participantNameText) {
        this.participantNameText = participantNameText;
    }

    public ImageButton getVideoOnOff() {
        return videoOnOff;
    }

    public void setVideoOnOff(ImageButton videoOnOff) {
        this.videoOnOff = videoOnOff;
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
