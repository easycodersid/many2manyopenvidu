package in.app.chirpz.websocket;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import in.app.chirpz.utils.SessionManager;
import io.socket.client.IO;
import io.socket.client.Socket;
import kotlin.Deprecated;

import static in.app.chirpz.SessionActivity.CALL_TYPE;
import static in.app.chirpz.SessionActivity.CLASS_CREATOR_ID;
import static in.app.chirpz.constants.JsonConstants.CALL_CREATE_CLASSROOM;
import static in.app.chirpz.constants.JsonConstants.MESSAGE;
import static in.app.chirpz.constants.JsonConstants.SUCCESSFUL;
import static in.app.chirpz.utils.GeneralUtils.getCallSession;
import static in.app.chirpz.utils.GeneralUtils.getUserName;
import static in.app.chirpz.utils.GeneralUtils.getUserUuid;

public class SocketSignaling {

    private static SocketSignaling instance;
    public String roomName = null;
    private Socket socket;
    boolean isChannelReady = false;
    boolean isInitiator = false;
    boolean isStarted = false;
    private SignalingInterface callback;
    Context context;
    boolean makingOneOnOne = false;
    String friendId = "";
    public boolean inComingCall = false;

    @SuppressLint("TrustAllX509TrustManager")
    private final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }

        public void checkClientTrusted(X509Certificate[] chain,
                                       String authType) {
        }

        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) {
        }
    }};

    public static SocketSignaling getInstance() {
        if (instance == null) {
            instance = new SocketSignaling();
        }
        return instance;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomName() {
        return this.roomName;
    }

    public void init(SignalingInterface signalingInterface, Context context) {
        Log.d("SignallingClient", "init()");
        this.context = context;
        this.callback = signalingInterface;
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, trustAllCerts, null);
            IO.setDefaultHostnameVerifier((hostname, session) -> true);
            IO.setDefaultSSLContext(sslcontext);

            //set the socket.io url here
            socket = IO.socket("http://139.59.66.228:3000"); //192.168.1.12:3000
            socket.connect();
            Log.d("SignallingClient", "init() called");

            /*if (!roomName.isEmpty()) {
                emitInitStatement(roomName);
            }*/

            Log.d(getClass().getName(), "socket_connected --> "+socket.connected());

            socket.on("getfriends", args -> {
                try{
                    Log.d(getClass().getName(), "getfriends --> "+args[0]);
                    JSONObject jsonObject = new JSONObject(""+args[0]);
                    callback.getFriendsResponse(jsonObject);
                }catch (Exception e){
                    e.printStackTrace();
                }
            });

            socket.on("searchfriend", args -> {
                try{
                    Log.d(getClass().getName(), "searchfriend --> "+args[0]);
                    callback.friendSearchResponse(new JSONObject(""+args[0]));
                }catch (Exception e){
                    e.printStackTrace();
                }
            });

            socket.on("addfriend", args -> {
                try{
                    Log.d(getClass().getName(), "addfriend --> "+args[0]);
                    callback.addFriendResponse(new JSONObject(""+args[0]));
                }catch (Exception e){
                    e.printStackTrace();
                }
            });

            socket.on("login", args -> {
                try {
                    Log.d(getClass().getName(), "Login_info --> " + args[0]);
                    JSONObject data = new JSONObject("" + args[0]);
                    if (data.getString("message").equalsIgnoreCase(SUCCESSFUL)) {
                        SessionManager.setPreferences(context, "user_uuid", data.getString("user_id"));
                        callback.onLogin();
                    } else {
                        callback.onLoginFailed();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            /*Get a session key*/
            socket.on("getsession", args -> {
                try {
                    Log.d(getClass().getName(), "getsession --> " + args[0]);
                    JSONObject data = new JSONObject("" + args[0]);
                    if (data.getString(MESSAGE).equalsIgnoreCase(SUCCESSFUL) && data.getString("user_id").equalsIgnoreCase(getUserUuid(context))) {
                        this.roomName = data.getString("session_key");
                        if(CALL_TYPE.equals(CALL_CREATE_CLASSROOM)){
                            createClassRoom(data.getString("session_key"));
                        }else{
                            createRoom(data.getString("session_key"));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            socket.on("createroom", args -> {
                try {
                    Log.d(getClass().getName(), "createroom --> " + args[0]);
                    JSONObject data = new JSONObject("" + args[0]);
                    String status = data.getString("status");
                    if (status.equalsIgnoreCase("created") && data.getString("created_by").equalsIgnoreCase(getUserUuid(context))) {
                        Log.d(getClass().getName(), "Creation status --> " + status);
                        callback.onCreatedRoom(data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            socket.on("sessioncheck", args -> {
                try {
                    Log.d(getClass().getName(), "sessioncheck --> " + args[0]);
                    JSONObject data = new JSONObject("" + args[0]);
                    if (data.getString(MESSAGE).equalsIgnoreCase(SUCCESSFUL)) {
                        //write code to join room
                        callback.validSession();
                    } else {
                        callback.invalidSession();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            socket.on("joinroom", args -> {
                try{
                    Log.d(getClass().getName(), "joinroom --> " + args[0]);
                    JSONObject jsonObject = new JSONObject(""+args[0]);
                    if(jsonObject.getString(MESSAGE).equalsIgnoreCase(SUCCESSFUL)){
                        callback.waitingForApproval();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            });

            socket.on("reqforapproval", args -> {
                try {
                    Log.d(getClass().getName(), "reqforapproval --> " + args[0]);
                    JSONObject data = new JSONObject("" + args[0]);
                    if (data.getString("created_by").equalsIgnoreCase(getUserUuid(context)) && data.getString("session_key").equalsIgnoreCase(getCallSession(context))) {
                        Log.d(getClass().getName(), "reqforapproval --> entered condition");
                        callback.addParticipant(data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            //room created event.
            socket.on("created", args -> {
                Log.d("SignallingClient", "Room created");
                Log.d("SignallingClient", "created call() called with: args = [" + Arrays.toString(args) + "]");
                isInitiator = true;
                //callback.onCreatedRoom(roomName);
            });

            //room is full event
            socket.on("full", args -> Log.d("SignallingClient", "full call() called with: args = [" + Arrays.toString(args) + "]"));

            //peer joined event
            socket.on("join", args -> {
                Log.d("SignallingClient", "join call() called with: args = [" + Arrays.toString(args) + "]");
                isChannelReady = true;
                callback.onNewPeerJoined();
            });

            //when you joined a chat room successfully
            socket.on("joined", args -> {
                Log.d("SignallingClient", "joined call() called with: args = [" + Arrays.toString(args) + "]");
                isChannelReady = true;
                callback.onJoinedRoom();
            });

            socket.on("reqoneonone", args -> {
                try{
                    Log.d(getClass().getName(), "reqoneonone --> " + args[0]);
                    JSONObject jsonObject = new JSONObject("" + args[0]);

                    String toUserId = jsonObject.getString("to_id");
                    Log.d(getClass().getName(), "reqoneonone --> "+toUserId.equalsIgnoreCase(getUserUuid(context)));
                    Log.d(getClass().getName(), "reqoneonone --> my_id :: "+getUserUuid(context));
                    if(toUserId.equalsIgnoreCase(getUserUuid(context))){
                        if(!inComingCall){
                            inComingCall = true;
                            callback.inComingCallReq(jsonObject);
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            });

            socket.on("joinapproved", args -> {
                try {

                    Log.d(getClass().getName(), "joinapproved --> " + args[0]);
                    JSONObject jsonObject = new JSONObject("" + args[0]);
                    if(jsonObject.getString("participant_id").equalsIgnoreCase(getUserUuid(context)) && isChannelReady == false){
                        isChannelReady = true;
                        callback.onRequestToJoinApproved(jsonObject);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            socket.on("joinclassroom", args -> {
                try{
                    Log.d(getClass().getName(), "joinapproved --> " + args[0]);
                    JSONObject jsonObject = new JSONObject("" + args[0]);
                    if(jsonObject.getString(MESSAGE).equalsIgnoreCase(SUCCESSFUL)){
                        callback.joinedClassRoom(jsonObject);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            });

            socket.on("log", args -> Log.d("SignallingClient", "log call() called with: args = [" + Arrays.toString(args) + "]"));

            //bye event
            //socket.on("bye", args -> callback.onRemoteHangUp((String) args[0]));

            socket.on("leaveroom", args -> callback.onRemoteHangUp((String) args[0]));

            //messages - SDP and ICE candidates are transferred through this
            socket.on("message", args -> {
                Log.d("SignallingClient", "Message received --> " + args[0]);
                try {
                    JSONObject data = new JSONObject("" + args[0]);
                    callback.onMessageReceived(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            socket.on("callstatus", args -> {
                Log.d(getClass().getName(), "callstatus --> " + args[0]);
                try {
                    JSONObject data = new JSONObject("" + args[0]);
                    callback.onMessageReceived(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void emitReqOneOnOne(JSONObject jsonObject){
        socket.emit("reqoneonone", jsonObject);
    }

    public void emitGetFriends(JSONObject jsonObject){
        socket.emit("getfriends", jsonObject);
    }

    public void emitSearchFriend(JSONObject jsonObject){
        socket.emit("searchfriend", jsonObject);
    }

    public void emitAddFriend(JSONObject jsonObject){
        socket.emit("addfriend", jsonObject);
    }

    public void checkSession(JSONObject jsonObject) {
        socket.emit("sessioncheck", jsonObject);
    }

    private void createRoom(String session_key) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("session_key", session_key);
            jsonObject.put("name", getUserName(context));
            jsonObject.put("user_id", getUserUuid(context));
            if(makingOneOnOne){
                jsonObject.put("friend_id", friendId);
                socket.emit("createoneonone", jsonObject);
            }else{
                socket.emit("createroom", jsonObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createClassRoom(String session_key) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("session_key", session_key);
            jsonObject.put("name", getUserName(context));
            jsonObject.put("user_id", getUserUuid(context));
            socket.emit("createclassroom", jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getSessionKey(JSONObject jsonObject, boolean makingOneOnOne, String friendId) {
        this.friendId = friendId;
        this.makingOneOnOne = makingOneOnOne;
        socket.emit("getsession", jsonObject);
    }

    /*Login function*/
    public void emitLogin(JSONObject jsonObject) {
        socket.emit("login", jsonObject);
    }

    /*Send only email here*/
    public void emitLogout(JSONObject jsonObject) {
        socket.emit("logout", jsonObject);
    }

    public void emitApprovedUser(JSONObject jsonObject) {
        socket.emit("joinapproved", jsonObject);
    }

    public void emitJoinRoom(JSONObject jsonObject){
        socket.emit("joinroom", jsonObject);
    }

    public void emitJoinClassRoom(JSONObject jsonObject){
        socket.emit("joinclassroom", jsonObject);
    }

    private void emitInitStatement(String message) {
        Log.d("SignallingClient", "emitInitStatement() called with: event = [" + "create or join" + "], message = [" + message + "]");
        socket.emit("create or join", message);
    }

    public void emitLeaveRoom(String message) {
        Log.d("SignallingClient", "Leave Room");
        socket.emit("leaveroom", message);
    }

    public void emitMessage(String message) {
        Log.d("SignallingClient", "emitMessage() called with: message = [" + message + "]");
        socket.emit("message", message);
    }

    public void close() {
        socket.emit("bye", roomName);
        socket.disconnect();
        socket.close();
    }

    public interface SignalingInterface {
        void onRemoteHangUp(String msg);

        void onLogin();

        void onLoginFailed();

        void onCreatedRoom(JSONObject data);

        void onJoinedRoom();

        void onNewPeerJoined();

        void onMessageReceived(JSONObject jsonObject);

        void invalidSession();

        void validSession();

        void onRequestToJoinApproved(JSONObject jsonObject);

        void addParticipant(JSONObject jsonObject);

        void waitingForApproval();

        void friendSearchResponse(JSONObject jsonObject);

        void addFriendResponse(JSONObject jsonObject);

        void getFriendsResponse(JSONObject jsonObject);

        void inComingCallReq(JSONObject jsonObject);

        void joinedClassRoom(JSONObject jsonObject);
    }
}
