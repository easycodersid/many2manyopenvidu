package in.app.chirpz.openvidu;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import in.app.chirpz.SessionActivity;
import in.app.chirpz.observers.CustomPeerConnectionObserver;
import in.app.chirpz.observers.CustomSdpObserver;
import in.app.chirpz.websocket.CustomWebSocket;

import static in.app.chirpz.SessionActivity.CALL_TYPE;
import static in.app.chirpz.constants.JsonConstants.CALL_CREATE_CLASSROOM;
import static in.app.chirpz.constants.JsonConstants.CALL_JOIN_CLASSROOM;
import static in.app.chirpz.constants.JsonConstants.CALL_NORMAL;

public class Session {

    private LocalParticipant localParticipant;
    private Map<String, RemoteParticipant> remoteParticipants = new HashMap<>();
    private String id;
    private String token;
    private LinearLayout views_container;
    private PeerConnectionFactory peerConnectionFactory;
    private CustomWebSocket websocket;
    private SessionActivity activity;

    public Session(String id, String token, LinearLayout views_container, SessionActivity activity) {
        this.id = id;
        this.token = token;
        this.views_container = views_container;
        this.activity = activity;

        PeerConnectionFactory.InitializationOptions.Builder optionsBuilder = PeerConnectionFactory.InitializationOptions.builder(activity.getApplicationContext());
        optionsBuilder.setEnableInternalTracer(true);
        PeerConnectionFactory.InitializationOptions opt = optionsBuilder.createInitializationOptions();
        PeerConnectionFactory.initialize(opt);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        final VideoEncoderFactory encoderFactory;
        final VideoDecoderFactory decoderFactory;
        encoderFactory = new SoftwareVideoEncoderFactory();
        decoderFactory = new SoftwareVideoDecoderFactory();

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .setOptions(options)
                .createPeerConnectionFactory();

    }

    public void setWebSocket(CustomWebSocket websocket) {
        this.websocket = websocket;
    }

    public PeerConnection createLocalPeerConnection() {
        final List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer();
        iceServers.add(iceServer);

        PeerConnection peerConnection = peerConnectionFactory.createPeerConnection(iceServers, new CustomPeerConnectionObserver("local") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                websocket.onIceCandidate(iceCandidate, localParticipant.getConnectionId());
            }

            @Override
            public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                activity.connectionStatus(newState);
            }
        });

        return peerConnection;
    }

    public void createRemotePeerConnection(final String connectionId) {
        final List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        PeerConnection.IceServer iceServer = PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer();
        iceServers.add(iceServer);

        PeerConnection peerConnection = peerConnectionFactory.createPeerConnection(iceServers, new CustomPeerConnectionObserver("remotePeerCreation") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                websocket.onIceCandidate(iceCandidate, connectionId);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                activity.setRemoteMediaStream(mediaStream, remoteParticipants.get(connectionId));
            }

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                if (PeerConnection.SignalingState.STABLE.equals(signalingState)) {
                    final RemoteParticipant remoteParticipant = remoteParticipants.get(connectionId);
                    Iterator<IceCandidate> it = remoteParticipant.getIceCandidateList().iterator();
                    while (it.hasNext()) {
                        IceCandidate candidate = it.next();
                        remoteParticipant.getPeerConnection().addIceCandidate(candidate);
                        it.remove();
                    }
                }
            }
        });

        MediaStream mediaStream = peerConnectionFactory.createLocalMediaStream("105");
        mediaStream.addTrack(localParticipant.getAudioTrack());
        mediaStream.addTrack(localParticipant.getVideoTrack());
        peerConnection.addStream(mediaStream);

        this.remoteParticipants.get(connectionId).setPeerConnection(peerConnection);
    }

    public void createLocalOffer(MediaConstraints constraints) {
        localParticipant.getPeerConnection().createOffer(new CustomSdpObserver("local offer sdp") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                Log.i("createOffer SUCCESS", sessionDescription.toString());
                localParticipant.getPeerConnection().setLocalDescription(new CustomSdpObserver("local set local"), sessionDescription);
                Log.d(getClass().getName(), "CALL_TYPE --> "+CALL_TYPE.equals(CALL_JOIN_CLASSROOM));
                if(CALL_TYPE.equals(CALL_JOIN_CLASSROOM)){
                    websocket.publishNoVideo(sessionDescription);
                }else {
                    websocket.publishVideo(sessionDescription);
                }
            }

            @Override
            public void onCreateFailure(String s) {
                Log.e("createOffer ERROR", s);
            }

        }, constraints);
    }

    public String getId() {
        return this.id;
    }

    public String getToken() {
        return this.token;
    }

    public LocalParticipant getLocalParticipant() {
        return this.localParticipant;
    }

    public void setLocalParticipant(LocalParticipant localParticipant) {
        this.localParticipant = localParticipant;
    }

    public RemoteParticipant getRemoteParticipant(String id) {
        return this.remoteParticipants.get(id);
    }

    public PeerConnectionFactory getPeerConnectionFactory() {
        return this.peerConnectionFactory;
    }

    public void addRemoteParticipant(RemoteParticipant remoteParticipant) {
        this.remoteParticipants.put(remoteParticipant.getConnectionId(), remoteParticipant);
    }

    public RemoteParticipant removeRemoteParticipant(String id) {
        return this.remoteParticipants.remove(id);
    }

    public void leaveSession() {
        try{
            AsyncTask.execute(() -> {
                websocket.setWebsocketCancelled(true);
                if (websocket != null) {
                    websocket.leaveRoom();
                    websocket.disconnect();
                }
                localParticipant.dispose();
            });

            activity.runOnUiThread(() -> {
                for (RemoteParticipant remoteParticipant : remoteParticipants.values()) {
                    if (remoteParticipant.getPeerConnection() != null) {
                        remoteParticipant.getPeerConnection().close();
                    }
                    views_container.removeView(remoteParticipant.getView());
                }
                activity.localVideoView.clearImage();
            });

            AsyncTask.execute(() -> {
                if (peerConnectionFactory != null) {
                    peerConnectionFactory.dispose();
                    peerConnectionFactory = null;
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void removeView(RemoteParticipant remoteParticipant) {
        activity.removeView(remoteParticipant);
        this.views_container.removeView(remoteParticipant.getView());
    }
}
