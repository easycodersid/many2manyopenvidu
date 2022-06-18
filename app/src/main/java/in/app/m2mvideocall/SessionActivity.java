package in.app.m2mvideocall;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import in.app.m2mvideocall.fragments.PermissionsDialogFragment;
import in.app.m2mvideocall.helpers.FriendsListAdapter;
import in.app.m2mvideocall.helpers.MessageListAdapter;
import in.app.m2mvideocall.models.Friend;
import in.app.m2mvideocall.models.Message;
import in.app.m2mvideocall.openvidu.LocalParticipant;
import in.app.m2mvideocall.openvidu.RemoteParticipant;
import in.app.m2mvideocall.openvidu.Session;
import in.app.m2mvideocall.utils.CustomHttpClient;
import in.app.m2mvideocall.models.Member;
import in.app.m2mvideocall.helpers.MembersAdapter;
import in.app.m2mvideocall.utils.SessionManager;
import in.app.m2mvideocall.websocket.CustomWebSocket;
import in.app.m2mvideocall.websocket.SocketSignaling;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import static in.app.m2mvideocall.constants.JsonConstants.CALL_CREATE_CLASSROOM;
import static in.app.m2mvideocall.constants.JsonConstants.CALL_JOIN_CLASSROOM;
import static in.app.m2mvideocall.constants.JsonConstants.CALL_NORMAL;
import static in.app.m2mvideocall.constants.JsonConstants.INBOUND_MSG;
import static in.app.m2mvideocall.constants.JsonConstants.MESSAGE;
import static in.app.m2mvideocall.constants.JsonConstants.MESSAGE_TYPE_DOCUMENT;
import static in.app.m2mvideocall.constants.JsonConstants.MESSAGE_TYPE_IMAGE;
import static in.app.m2mvideocall.constants.JsonConstants.MESSAGE_TYPE_PDF;
import static in.app.m2mvideocall.constants.JsonConstants.MESSAGE_TYPE_TEXT;
import static in.app.m2mvideocall.constants.JsonConstants.OUTBOUND_MSG;
import static in.app.m2mvideocall.constants.JsonConstants.SUCCESSFUL;
import static in.app.m2mvideocall.utils.GeneralUtils.getCallSession;
import static in.app.m2mvideocall.utils.GeneralUtils.getCallType;
import static in.app.m2mvideocall.utils.GeneralUtils.getUserEmail;
import static in.app.m2mvideocall.utils.GeneralUtils.getUserName;
import static in.app.m2mvideocall.utils.GeneralUtils.getUserUuid;
import static in.app.m2mvideocall.utils.GeneralUtils.shareTextToApps;

import android.database.Cursor;

public class SessionActivity extends AppCompatActivity implements SocketSignaling.SignalingInterface {

    CustomWebSocket webSocket;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 101;
    private static final int MY_PERMISSIONS_REQUEST = 102;
    private final String TAG = "SessionActivity";
    @BindView(R.id.views_container)
    LinearLayout views_container;

    @BindView(R.id.joinRoomBtn)
    AppCompatButton joinRoomBtn;

    @BindView(R.id.session_name)
    EditText session_name;

    @BindView(R.id.participant_name)
    EditText participant_name;

    public @BindView(R.id.local_gl_surface_view)
    SurfaceViewRenderer localVideoView;

    @BindView(R.id.main_participant)
    TextView main_participant;

    @BindView(R.id.peer_container)
    FrameLayout peer_container;

    @BindView(R.id.sendMessageBtn)
    ImageButton sendMessageBtn;

    @BindView(R.id.messageET)
    EditText messageET;

    @BindView(R.id.leaveRoomBtn)
    Button leaveRoomBtn;

    @BindView(R.id.messageLayout)
    ConstraintLayout messageLayout;

    @BindView(R.id.sessionDetailsLayout)
    ConstraintLayout sessionDetailsLayout;

    @BindView(R.id.participantsBottomSheet)
    ConstraintLayout participantsBottomSheet;

    @BindView(R.id.friendsBottomSheet)
    ConstraintLayout friendsBottomSheet;

    @BindView(R.id.messageListView)
    ListView messageListView;

    @BindView(R.id.volumeOnOff)
    ImageButton volumeOnOff;

    @BindView(R.id.videoOffOn)
    ImageButton videoOffOn;

    @BindView(R.id.switchCamera)
    ImageButton switchCamera;

    @BindView(R.id.shareFilesBtn)
    ImageButton shareFilesBtn;

    @BindView(R.id.inviteBtn)
    Button inviteBtn;

    @BindView(R.id.createSessionRadio)
    RadioButton createSessionRadio;

    @BindView(R.id.joinSessionRadio)
    RadioButton joinSessionRadio;

    @BindView(R.id.createClassRoomRadio)
    RadioButton createClassRoomRadio;

    @BindView(R.id.joinClassRoomRadio)
    RadioButton joinClassRoomRadio;

    @BindView(R.id.radioGrp)
    RadioGroup radioGrp;

    @BindView(R.id.sessionKeyTV)
    TextView sessionKeyTV;

    @BindView(R.id.participantsListView)
    ListView participantsListView;

    @BindView(R.id.viewParticipants)
    Button viewParticipants;

    @BindView(R.id.closeParticipantsBtn)
    Button closeParticipantsBtn;

    @BindView(R.id.openFriendsList)
    AppCompatButton openFriendsList;

    @BindView(R.id.closeFriendsBtn)
    Button closeFriendsBtn;

    @BindView(R.id.searchFriendsBtn)
    ImageButton searchFriendsBtn;

    @BindView(R.id.searchRespLayout)
    RelativeLayout searchRespLayout;

    @BindView(R.id.searchName)
    TextView searchName;

    @BindView(R.id.searchEmail)
    TextView searchEmail;

    @BindView(R.id.addFriend)
    Button addFriendBtn;

    @BindView(R.id.friendsListView)
    ListView friendsListView;

    @BindView(R.id.toggleMessageLayout)
    Button toggleMessageLayout;

    MembersAdapter membersAdapter;
    private String OPENVIDU_URL;
    private String OPENVIDU_SECRET;
    private Session session;
    private CustomHttpClient httpClient, httpClient2;
    MessageListAdapter messagesListAdapter;
    Context context = this;
    Activity activity = this;
    Random random;
    LocalParticipant localParticipant;
    boolean audioCall = true, videoCall = true;
    DisplayMetrics displayMetrics;
    SocketSignaling socketSignaling;
    private FirebaseAuth mAuth;
    Uri uri;
    private FirebaseUser currentUser;
    private AudioJackIntentReceiver myReceiver;
    AudioManager audioManager;
    ArrayList<SurfaceViewRenderer> surfaceViewRenderers = new ArrayList<>();
    ArrayList<RemoteParticipant> rendererArrayMap = new ArrayList<>();
    AlertDialog waitingRoomApprovalDiag, waitingForFriendToJoinDialog, callAnswerDialog;
    boolean waitingCancelled;
    FriendsListAdapter friendsListAdapter;
    Friend lastSearchedFriend;
    private BottomSheetBehavior bottomSheetBehavior;
    public static String CALL_TYPE = CALL_NORMAL;
    public static String CLASS_CREATOR_ID = "";

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        askForPermissions();
        ButterKnife.bind(this);
        FirebaseApp.initializeApp(context);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        myReceiver = new AudioJackIntentReceiver();
        bottomSheetBehavior = BottomSheetBehavior.from(messageLayout);

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        random = new Random();
        socketSignaling = SocketSignaling.getInstance();
        socketSignaling.init(this, context);

        participant_name.setText(getUserName(context));

        messagesListAdapter = new MessageListAdapter(context);
        messageListView.setAdapter(messagesListAdapter);
        messageListView.setDivider(null);

        friendsListAdapter = new FriendsListAdapter(this, this);
        friendsListView.setAdapter(friendsListAdapter);
        friendsListView.setDivider(null);

        membersAdapter = new MembersAdapter(context, this);
        participantsListView.setAdapter(membersAdapter);

        toggleMessageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    toggleMessageLayout.setText("OPEN");
                } else {
                    toggleMessageLayout.setText("CLOSE");
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });

        addFriendBtn.setOnClickListener(view -> {
            try {
                if (lastSearchedFriend != null) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("user_id", getUserUuid(context));

                    JSONObject friendObject = new JSONObject();
                    friendObject.put("name", lastSearchedFriend.getName());
                    friendObject.put("email", lastSearchedFriend.getEmail());
                    friendObject.put("user_id", lastSearchedFriend.getUser_id());
                    jsonObject.put("friend", friendObject);

                    socketSignaling.emitAddFriend(jsonObject);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        searchFriendsBtn.setOnClickListener(view -> openSearchDialog());

        openFriendsList.setOnClickListener(view -> {
            if (friendsBottomSheet.getVisibility() == View.GONE) {
                friendsBottomSheet.setVisibility(View.VISIBLE);
            }
            if (sessionDetailsLayout.getVisibility() == View.VISIBLE) {
                sessionDetailsLayout.setVisibility(View.GONE);
            }

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("user_id", getUserUuid(context));
                socketSignaling.emitGetFriends(jsonObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        closeFriendsBtn.setOnClickListener(view -> {
            if (friendsBottomSheet.getVisibility() == View.VISIBLE) {
                friendsBottomSheet.setVisibility(View.GONE);
            }
            if (sessionDetailsLayout.getVisibility() == View.GONE) {
                sessionDetailsLayout.setVisibility(View.VISIBLE);
            }
            friendsListAdapter = new FriendsListAdapter(context, SessionActivity.this);
            friendsListView.invalidate();

            hideSearchFriendLayout();
        });

        viewParticipants.setOnClickListener(view -> {
            if (messageLayout.getVisibility() == View.VISIBLE) {
                messageLayout.setVisibility(View.GONE);
            }
            if (participantsBottomSheet.getVisibility() == View.GONE) {
                participantsBottomSheet.setVisibility(View.VISIBLE);
            }
        });

        closeParticipantsBtn.setOnClickListener(view -> {
            if (messageLayout.getVisibility() == View.GONE) {
                messageLayout.setVisibility(View.VISIBLE);
            }
            if (participantsBottomSheet.getVisibility() == View.VISIBLE) {
                participantsBottomSheet.setVisibility(View.GONE);
            }
        });

        radioGrp.setOnCheckedChangeListener((radioGroup, i) -> {
            switch (i) {
                case R.id.createSessionRadio:
                    session_name.setVisibility(View.GONE);
                    joinRoomBtn.setText("Create");
                    CALL_TYPE = CALL_NORMAL;
                    localVideoView.setVisibility(View.VISIBLE);
                    main_participant.setVisibility(View.VISIBLE);
                    break;
                case R.id.joinSessionRadio:
                    session_name.setVisibility(View.VISIBLE);
                    joinRoomBtn.setText("Join");
                    CALL_TYPE = CALL_NORMAL;
                    localVideoView.setVisibility(View.VISIBLE);
                    main_participant.setVisibility(View.VISIBLE);
                    break;
                case R.id.createClassRoomRadio:
                    CALL_TYPE = CALL_CREATE_CLASSROOM;
                    session_name.setVisibility(View.GONE);
                    joinRoomBtn.setText("Create classroom");
                    localVideoView.setVisibility(View.VISIBLE);
                    main_participant.setVisibility(View.VISIBLE);
                    break;
                case R.id.joinClassRoomRadio:
                    CALL_TYPE = CALL_JOIN_CLASSROOM;
                    session_name.setVisibility(View.VISIBLE);
                    joinRoomBtn.setText("Join classroom");
                    localVideoView.setVisibility(View.GONE);
                    main_participant.setVisibility(View.GONE);
                    break;
            }
        });

        sendMessageBtn.setOnClickListener(view -> {
            try {
                String message = messageET.getText().toString();
                sendMessage(message, MESSAGE_TYPE_TEXT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        leaveRoomBtn.setOnClickListener(view -> leaveConfirmDialog());

        joinRoomBtn.setOnClickListener(view -> {
            String roomName = joinRoomBtn.getText().toString();
            if (roomName.equalsIgnoreCase("create") || roomName.equalsIgnoreCase("create classroom")) {
                getSessionKey(false, "");
            }else if(roomName.equalsIgnoreCase("join") || roomName.equalsIgnoreCase("join classroom")){
                checkSession();
            }
        });

        volumeOnOff.setOnClickListener(view -> {
            if (audioCall) {
                audioCall = false;
                localParticipant.getAudioTrack().setEnabled(false);
                volumeOnOff.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_volume_off));
            } else {
                audioCall = true;
                volumeOnOff.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_volume_on));
                localParticipant.getAudioTrack().setEnabled(true);
            }
        });

        switchCamera.setOnClickListener(view -> {
            localParticipant.switchCamera();
        });

        videoOffOn.setOnClickListener(view -> {
            if (videoCall) {
                videoCall = false;
                localParticipant.getVideoTrack().setEnabled(false);
                videoOffOn.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_videocam_off));
            } else {
                videoCall = true;
                localParticipant.getVideoTrack().setEnabled(true);
                videoOffOn.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_videocam_on));
            }
        });

        inviteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareTextToApps(session_name.getText().toString(), context);
            }
        });

        shareFilesBtn.setOnClickListener(view -> showFileSharingOption());
        checkForPreviousSession();

        audioManager = (AudioManager) this.activity.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_CALL);
    }

    public void connectionStatus(PeerConnection.PeerConnectionState connectionState){
        switch (connectionState){
            case NEW:
                break;
            case CONNECTING:
                break;
            case CONNECTED:
                break;
            case DISCONNECTED:
                break;
            case FAILED:
                runOnUiThread(() -> {
                    leaveSession();
                    Runnable runnable = () -> reconnectToSession();
                    new Handler().postDelayed(runnable, 500);
                });
                break;
            case CLOSED:
                break;
        }
    }

    private void openSearchDialog() {
        try {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(SessionActivity.this);
            alertDialog.setTitle("Search");
            alertDialog.setCancelable(false);
            alertDialog.setMessage("Enter email ID to search");

            final EditText input = new EditText(SessionActivity.this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            input.setLayoutParams(lp);
            alertDialog.setView(input);
            alertDialog.setIcon(R.drawable.ic_search_black);

            alertDialog.setPositiveButton("SEARCH",
                    (dialog, which) -> {
                        dialog.cancel();
                        try {
                            String email = input.getText().toString();
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("email", email);
                            socketSignaling.emitSearchFriend(jsonObject);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

            alertDialog.setNegativeButton("CANCEL",
                    (dialog, which) -> dialog.cancel());

            alertDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkSession() {
        try {
            if (session_name.getText().length() < 9) {
                Toast.makeText(context, "Please enter a valid room name", Toast.LENGTH_LONG).show();
            }else{
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("session_key", session_name.getText().toString());
                socketSignaling.checkSession(jsonObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getSessionKey(boolean boo, String friendId) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_id", SessionManager.getPreferences(context, "user_uuid"));
            socketSignaling.getSessionKey(jsonObject, boo, friendId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkForPreviousSession() {
        String prevSession = getCallSession(context);
        if (prevSession != null) {
            if (!prevSession.equalsIgnoreCase("0")) {
                rejoinConfirmDialog();
            }
        }
    }

    private void showFileSharingOption() {
        CharSequence charSequence[] = new CharSequence[]{
                "Images",
                "Pdf",
                "Docs"
        };

        final AlertDialog.Builder alert = new AlertDialog.Builder(SessionActivity.this);
        alert.setTitle("Select");
        alert.setItems(charSequence, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == 0) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent, "select image"), 20);

                } else if (i == 1) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/pdf");
                    startActivityForResult(Intent.createChooser(intent, "select document"), 30);
                } else if (i == 2) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/doc");
                    startActivityForResult(Intent.createChooser(intent, "select document"), 40);
                }

            }
        });

        alert.show();
    }

    public void askForPermissions() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST);
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }

    public void connectWithMediaServer(String callingFrom) {
        Log.d(TAG, "callingFrom --> "+callingFrom);
        if (arePermissionGranted()) {
            initViews();
            viewToConnectingState();

            OPENVIDU_URL = getResources().getString(R.string.default_openvidu_url);
            OPENVIDU_SECRET = getResources().getString(R.string.default_openvidu_secret);
            httpClient = new CustomHttpClient(OPENVIDU_URL, "Basic " + android.util.Base64.encodeToString(("OPENVIDUAPP:" + OPENVIDU_SECRET).getBytes(), android.util.Base64.DEFAULT).trim());

            String sessionId = session_name.getText().toString();
            getToken(sessionId);

            surfaceViewRenderers = new ArrayList<>();
            rendererArrayMap = new ArrayList<>();
        } else {
            DialogFragment permissionsFragment = new PermissionsDialogFragment();
            permissionsFragment.show(getSupportFragmentManager(), "Permissions Fragment");
        }
    }

    private void getToken(String sessionId) {
        try {
            // Session Request
            RequestBody sessionBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "{\"customSessionId\": \"" + sessionId + "\"}");
            this.httpClient.httpCall("/api/sessions", "POST", "application/json", sessionBody, new Callback() {

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    Log.d(TAG, "responseString: " + response.body().string());

                    // Token Request
                    RequestBody tokenBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "{\"session\": \"" + sessionId + "\"}");
                    httpClient.httpCall("/api/tokens", "POST", "application/json", tokenBody, new Callback() {

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) {
                            String responseString = null;
                            try {
                                responseString = response.body().string();
                            } catch (IOException e) {
                                Log.e(TAG, "Error getting body", e);
                            }
                            Log.d(TAG, "responseString2: " + responseString);
                            JSONObject tokenJsonObject = null;
                            String token = null;
                            try {
                                tokenJsonObject = new JSONObject(responseString);
                                token = tokenJsonObject.getString("token");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            getTokenSuccess(token, sessionId);
                        }

                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            Log.e(TAG, "Error POST /api/tokens", e);
                            connectionError();
                        }
                    });
                }

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Log.e(TAG, "Error POST /api/sessions", e);
                    connectionError();
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error getting token", e);
            e.printStackTrace();
            connectionError();
        }
    }

    private void getTokenSuccess(String token, String sessionId) {
        // Initialize our session

        session = new Session(sessionId, token, views_container, this);

        // Initialize our local participant and start local camera
        String participantName = "" + participant_name.getText().toString() + "_" + getUserUuid(context); //random.nextInt(100)
        localParticipant = new LocalParticipant(participantName, session, this.getApplicationContext(), localVideoView);
        localParticipant.startCamera();

        if(CALL_TYPE.equalsIgnoreCase(CALL_JOIN_CLASSROOM)){
            localParticipant.getAudioTrack().setEnabled(false);
            localParticipant.getVideoTrack().setEnabled(false);
        }

        Log.d(TAG, "Initialize our session");
        runOnUiThread(() -> {
            // Update local participant view
            main_participant.setText(participant_name.getText().toString());
            main_participant.setPadding(20, 3, 20, 3);
        });

        // Initialize and connect the websocket to OpenVidu Server
        startWebSocket();
    }

    private void startWebSocket() {
        webSocket = new CustomWebSocket(session, OPENVIDU_URL, this, displayMetrics);
        webSocket.execute();
        session.setWebSocket(webSocket);
    }

    private void connectionError() {
        Runnable myRunnable = () -> {
            Toast toast = Toast.makeText(this, "Error connecting to " + OPENVIDU_URL, Toast.LENGTH_LONG);
            toast.show();
            viewToDisconnectedState();
        };
        new Handler(this.getMainLooper()).post(myRunnable);
    }

    private void initViews() {

        /*localVideoView.release();
        localVideoView.clearImage();*/
        try {
            EglBase rootEglBase = EglBase.create();
            localVideoView.init(rootEglBase.getEglBaseContext(), null);
            localVideoView.setMirror(false);
            localVideoView.setEnableHardwareScaler(true);
            localVideoView.setZOrderMediaOverlay(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void viewToDisconnectedState() {
        runOnUiThread(() -> {
            localVideoView.clearImage();
            localVideoView.release();
            session_name.setEnabled(true);
            session_name.setFocusableInTouchMode(true);
            participant_name.setEnabled(true);
            participant_name.setFocusableInTouchMode(true);
            main_participant.setText(null);
            main_participant.setPadding(0, 0, 0, 0);
            sessionDetailsLayout.setVisibility(View.VISIBLE);
            messageLayout.setVisibility(View.GONE);
            messageListView.invalidate();
            videoOffOn.setVisibility(View.GONE);
            volumeOnOff.setVisibility(View.GONE);
            switchCamera.setVisibility(View.GONE);
            surfaceViewRenderers = new ArrayList<>();
            rendererArrayMap = new ArrayList<>();
            sessionKeyTV.setText("");
        });
    }

    public void viewToConnectingState() {
        runOnUiThread(() -> {
            session_name.setEnabled(false);
            session_name.setFocusable(false);
            participant_name.setEnabled(false);
            participant_name.setFocusable(false);
        });
    }

    public void viewToConnectedState() {
        runOnUiThread(() -> {
            sessionDetailsLayout.setVisibility(View.GONE);
            messageLayout.setVisibility(View.VISIBLE);

            if(!CALL_TYPE.equalsIgnoreCase(CALL_JOIN_CLASSROOM)){
                videoOffOn.setVisibility(View.VISIBLE);
                volumeOnOff.setVisibility(View.VISIBLE);
                switchCamera.setVisibility(View.VISIBLE);
            }

            SessionManager.setPreferences(context, "call-session", session_name.getText().toString());
            SessionManager.setPreferences(context, "call-type", CALL_TYPE);
            manipulateSurfaceView();
        });
    }

    private void manipulateSurfaceView() {
        if(CALL_TYPE.equals(CALL_NORMAL) || CALL_TYPE.equals(CALL_CREATE_CLASSROOM)){
            if (surfaceViewRenderers.size() <= 1) {
                int height = (int) displayMetrics.heightPixels / 2;
                localVideoView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

                if (surfaceViewRenderers.size() == 1) {
                    surfaceViewRenderers.get(0).setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
                }
            } else {
                if (surfaceViewRenderers.size() == 2) {
                    int height = (int) displayMetrics.heightPixels / 3;
                    localVideoView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
                    surfaceViewRenderers.get(0).setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
                }
            }
        }
    }

    public void removeView(RemoteParticipant remoteParticipant) {
        int index = rendererArrayMap.indexOf(remoteParticipant);
        surfaceViewRenderers.remove(index);
        rendererArrayMap.remove(remoteParticipant);

        Toast.makeText(context, remoteParticipant.getParticipantName() + " left !", Toast.LENGTH_LONG).show();
        manipulateSurfaceView();
    }

    public void createRemoteParticipantVideo(final RemoteParticipant remoteParticipant) {
        Handler mainHandler = new Handler(this.getMainLooper());
        Runnable myRunnable = () -> {

            View rowView = this.getLayoutInflater().inflate(R.layout.peer_video, null);
            LinearLayout.LayoutParams lp = null;

            String participantName, participantId;
            String[] strings = remoteParticipant.getParticipantName().split("_");
            participantName = strings[0];
            participantId = strings[1];
            boolean proceedFurther = false;
            Log.d(TAG, "createRemoteParticipantVideo --> "+participantName+" -- "+participantId);

            if(CALL_TYPE.equals(CALL_NORMAL)){
                lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                proceedFurther = true;
            }else if(CALL_TYPE.equals(CALL_JOIN_CLASSROOM) && participantId.equals(CLASS_CREATOR_ID)){
                lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                proceedFurther = true;
            }else{
                proceedFurther = false;
                lp = new LinearLayout.LayoutParams(0, 0);
            }
            lp.setMargins(0, 0, 0, 0);

            rowView.setLayoutParams(lp);
            int rowId = View.generateViewId();
            rowView.setId(rowId);
            views_container.addView(rowView);

            SurfaceViewRenderer videoView = (SurfaceViewRenderer) ((ViewGroup) rowView).getChildAt(0);

            if(!proceedFurther){
                videoView.setLayoutParams(new FrameLayout.LayoutParams(0,0));
            }

            remoteParticipant.setVideoView(videoView);

            surfaceViewRenderers.add(remoteParticipant.getVideoView());

            if(CALL_TYPE.equals(CALL_NORMAL)){
                if (surfaceViewRenderers.size() >= 2) {
                    int height = (int) displayMetrics.heightPixels / 3;
                    remoteParticipant.getVideoView().setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
                }
            }

            Log.d(TAG, "getParticipantName --> "+remoteParticipant.getParticipantName());

            videoView.setMirror(false);
            EglBase rootEglBase = EglBase.create();
            videoView.init(rootEglBase.getEglBaseContext(), null);
            videoView.setZOrderMediaOverlay(true);
            View textView = ((ViewGroup) rowView).getChildAt(1);
            remoteParticipant.setParticipantNameText((TextView) textView);
            remoteParticipant.setView(rowView);

            rendererArrayMap.add(remoteParticipant);

            remoteParticipant.getParticipantNameText().setText(remoteParticipant.getParticipantName());
            remoteParticipant.getParticipantNameText().setPadding(20, 3, 20, 3);

            manipulateSurfaceView();
        };
        mainHandler.post(myRunnable);
    }

    public void setRemoteMediaStream(MediaStream stream, final RemoteParticipant remoteParticipant) {
        final VideoTrack videoTrack = stream.videoTracks.get(0);
        videoTrack.addSink(remoteParticipant.getVideoView());
        runOnUiThread(() -> {
            remoteParticipant.getVideoView().setVisibility(View.VISIBLE);

            Toast.makeText(context, remoteParticipant.getParticipantName() + " joined !", Toast.LENGTH_LONG).show();
        });
    }

    public void leaveSession() {
        if(this.session != null){
            this.session.leaveSession();
            this.httpClient.dispose();

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("room", session_name.getText().toString());
                jsonObject.put("name", getUserName(context));
                socketSignaling.emitLeaveRoom(jsonObject.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }

            viewToDisconnectedState();
        }
    }

    private boolean arePermissionGranted() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_DENIED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_DENIED);
    }

    @Override
    public void onBackPressed() {
        if (sessionDetailsLayout.getVisibility() == View.VISIBLE) {
            super.onBackPressed();
        } else {
            leaveConfirmDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);
        super.onResume();
    }

    private class AudioJackIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        Log.d(TAG, "Headset is unplugged");
                        audioManager.setSpeakerphoneOn(true);
                        break;
                    case 1:
                        Log.d(TAG, "Headset is plugged");
                        audioManager.setSpeakerphoneOn(false);
                        break;
                    default:
                        Log.d(TAG, "I have no idea what the headset state is");
                        audioManager.setSpeakerphoneOn(true);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        leaveSession();
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void leaveConfirmDialog() {
        AlertDialog.Builder builder
                = new AlertDialog
                .Builder(this);
        builder.setMessage("Do you want to leave the room ?");
        builder.setTitle("Alert !");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", (dialog, which) -> {
            dialog.cancel();
            SessionManager.setPreferences(context, "call-session", "0");
            SessionManager.setPreferences(context, "call-type", "0");
            leaveSession();
            //finish();
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            dialog.cancel();
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void rejoinConfirmDialog() {
        runOnUiThread(() -> {
            AlertDialog.Builder builder
                    = new AlertDialog
                    .Builder(context);
            builder.setMessage("Do you want to join " + getCallSession(context) + " again ?");
            builder.setTitle("Alert !");
            builder.setCancelable(false);
            builder.setPositiveButton("Yes", (dialog, which) -> {
                dialog.cancel();
                reconnectToSession();
            });
            builder.setNegativeButton("No", (dialog, which) -> {
                dialog.cancel();
                resetSharedPreferences();
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        });
    }

    private void reconnectToSession(){
        participant_name.setText(getUserName(context));
        session_name.setText(getCallSession(context));
        sessionKeyTV.setText(getCallSession(context));

        String prevCallType = getCallType(context);
        if(!prevCallType.equalsIgnoreCase("0")){
            if(prevCallType.equals(CALL_JOIN_CLASSROOM) || prevCallType.equals(CALL_CREATE_CLASSROOM)){
                joinClassRoomRadio.setChecked(true);
                joinSessionRadio.setChecked(false);
                createSessionRadio.setChecked(false);
                createClassRoomRadio.setChecked(false);
                joinRoomBtn.setText("Join classroom");
            }else{
                joinSessionRadio.setChecked(true);
                createSessionRadio.setChecked(false);
                joinClassRoomRadio.setChecked(false);
                createClassRoomRadio.setChecked(false);
                joinRoomBtn.setText("Join");
            }
        }else{
            joinSessionRadio.setChecked(true);
            createSessionRadio.setChecked(false);
            joinClassRoomRadio.setChecked(false);
            createClassRoomRadio.setChecked(false);
            joinRoomBtn.setText("Join");
        }

        joinRoomBtn.performClick();
        resetSharedPreferences();
    }

    private void resetSharedPreferences(){
        SessionManager.setPreferences(context, "call-session", "0");
        SessionManager.setPreferences(context, "call-type", "0");
    }

    public void waitingApprovalDialog() {
        runOnUiThread(() -> {
            AlertDialog.Builder builder
                    = new AlertDialog
                    .Builder(context);
            builder.setMessage("Waiting for approval !");
            builder.setTitle("Alert !");
            builder.setCancelable(false);
            builder.setPositiveButton("", null);
            builder.setNegativeButton("Cancel", (dialog, which) -> {
                dialog.cancel();
                waitingCancelled = true;
            });
            waitingRoomApprovalDiag = builder.create();
            waitingRoomApprovalDiag.show();
        });
    }

    public void approveUser(int position) {
        runOnUiThread(() -> {
            try {

                Member member = membersAdapter.getItem(position);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("status", "approved");
                jsonObject.put("participant_id", member.getId());
                jsonObject.put("participant_name", member.getName());
                jsonObject.put("session_key", session_name.getText().toString());
                socketSignaling.emitApprovedUser(jsonObject);
                member.approved = true;
                membersAdapter.updateMember(member);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /*Socket signalling*/
    @Override
    public void onRemoteHangUp(String msg) {
        Log.d(TAG, "onRemoteHangUp --> " + msg);

    }

    @Override
    public void onLogin() {

    }

    @Override
    public void onLoginFailed() {

    }

    @Override
    public void onCreatedRoom(JSONObject data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    String roomName = data.getString("session_key");

                    Log.d(getClass().getName(), "onCreatedRoom --- > " + roomName);
                    session_name.setText(roomName);
                    sessionKeyTV.setText(roomName);
                    connectWithMediaServer("onCreatedRoom");

                    if (data.has("type")) {
                        if (data.getString("type").equalsIgnoreCase("oneonone")) {
                            showWaitingForFriendToJoinDialog();
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void makeCallToFriend(Friend friend) {
        try {

            if (friendsBottomSheet.getVisibility() == View.VISIBLE) {
                friendsBottomSheet.setVisibility(View.GONE);
            }
            if (sessionDetailsLayout.getVisibility() == View.GONE) {
                sessionDetailsLayout.setVisibility(View.VISIBLE);
            }

            getSessionKey(true, friend.getUser_id());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showWaitingForFriendToJoinDialog() {
        try {
            AlertDialog.Builder builder
                    = new AlertDialog
                    .Builder(context);
            builder.setMessage("Waiting for your buddy to join.");
            builder.setTitle("Your request has been sent!");
            builder.setCancelable(false);
            builder.setPositiveButton("CLOSE", (dialog, which) -> {
                dialog.cancel();
            });
            builder.setNegativeButton("", null);
            waitingForFriendToJoinDialog = builder.create();
            waitingForFriendToJoinDialog.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void inComingCallReq(JSONObject jsonObject) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showCallAnswerDialog(jsonObject);
            }
        });
    }

    @Override
    public void joinedClassRoom(JSONObject jsonObject) {
        runOnUiThread(() -> {
            try{
                CLASS_CREATOR_ID = jsonObject.getString("created_by");
                if(jsonObject.getString("created_by").equals(getUserUuid(context))){
                    CALL_TYPE = CALL_CREATE_CLASSROOM;
                    Log.d(TAG, "CALL_TYPE --> updated "+CALL_TYPE);
                    localVideoView.setVisibility(View.VISIBLE);
                }
                connectWithMediaServer("joinedClassRoom");
            }catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    public void showCallAnswerDialog(JSONObject jsonObject) {
        try {

            String fromName = jsonObject.getJSONObject("from").getString("name");

            JSONObject reqObj = new JSONObject();
            reqObj.put("session_key", jsonObject.getString("session_key"));
            reqObj.put("user_id", getUserUuid(context));
            reqObj.put("name", getUserName(context));
            reqObj.put("email", getUserEmail(context));

            AlertDialog.Builder builder
                    = new AlertDialog
                    .Builder(context);
            builder.setMessage(fromName + " is trying to reach you!");
            builder.setTitle("Alert!");
            builder.setCancelable(false);
            builder.setPositiveButton("ANSWER", (dialog, which) -> {
                try {
                    dialog.cancel();

                    socketSignaling.inComingCall = false;

                    reqObj.put("type", "answercall");
                    socketSignaling.emitReqOneOnOne(reqObj);

                    session_name.setText(jsonObject.getString("session_key"));
                    sessionKeyTV.setText(jsonObject.getString("session_key"));
                    connectWithMediaServer("showCallAnswerDialog");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            builder.setNegativeButton("REJECT", (dialog, which) -> {
                try {
                    dialog.cancel();

                    socketSignaling.inComingCall = false;

                    reqObj.put("type", "rejectcall");
                    socketSignaling.emitReqOneOnOne(reqObj);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            callAnswerDialog = builder.create();
            if (!callAnswerDialog.isShowing()) {
                callAnswerDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onJoinedRoom() {
        runOnUiThread(() -> {
            Toast.makeText(context, "You have joined !", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onNewPeerJoined() {

    }

    @Override
    public void onMessageReceived(JSONObject jsonObject) {
       runOnUiThread(() -> {
           try {
               Log.d(TAG, "onMessageReceived --> " + jsonObject);
               if (jsonObject.has("uuid")) {
                   if (!jsonObject.getString("uuid").equalsIgnoreCase(getUserUuid(context))) {
                       messagesListAdapter.add(new Message(jsonObject.getString("message"), INBOUND_MSG, jsonObject.getString("from"), jsonObject.getString("type")));
                       activity.runOnUiThread(() -> {
                           messagesListAdapter.notifyDataSetChanged();
                           messageListView.setSelection(messageListView.getCount() - 1);
                       });
                   }
               } else if (jsonObject.has("type")) {
                   if (waitingForFriendToJoinDialog != null) {
                       if (waitingForFriendToJoinDialog.isShowing()) {
                           waitingForFriendToJoinDialog.cancel();
                       }
                   }
                   if (jsonObject.getString("type").equalsIgnoreCase("rejectcall")) {
                       Toast.makeText(context, "Your call was rejected!", Toast.LENGTH_LONG).show();
                   }
               }
           } catch (Exception e) {
               e.printStackTrace();
           }
       });
    }

    @Override
    public void invalidSession() {
        Toast.makeText(context, "Invalid session!", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onRequestToJoinApproved(JSONObject jsonObject) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!waitingCancelled) {
                        if (waitingRoomApprovalDiag != null) {
                            if (waitingRoomApprovalDiag.isShowing()) {
                                waitingRoomApprovalDiag.cancel();
                            }
                        }
                        sessionKeyTV.setText(jsonObject.getString("session_key"));
                        connectWithMediaServer("onRequestToJoinApproved");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void addParticipant(JSONObject jsonObject) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {

                    Log.d(getClass().getName(), "reqforapproval --> addParticipant");

                    String name = jsonObject.getString("name");
                    String id = jsonObject.getString("user_id");

                    Member member = new Member(name, id, false);
                    membersAdapter.addMember(member);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void validSession() {
        try {

            waitingCancelled = false;

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("session_key", session_name.getText().toString());
            jsonObject.put("name", getUserName(context));
            jsonObject.put("user_id", getUserUuid(context));
            if(CALL_TYPE.equals(CALL_JOIN_CLASSROOM)){
                socketSignaling.emitJoinClassRoom(jsonObject);
            }else if(CALL_TYPE.equals(CALL_NORMAL)){
                socketSignaling.emitJoinRoom(jsonObject);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void waitingForApproval() {
        waitingApprovalDialog();
    }

    @Override
    public void friendSearchResponse(JSONObject jsonObject) {
        runOnUiThread(() -> {
            try {
                if (jsonObject.getString(MESSAGE).equalsIgnoreCase(SUCCESSFUL)) {
                    lastSearchedFriend = new Friend(jsonObject.getString("name"), jsonObject.getString("email"), jsonObject.getString("user_id"));
                    searchName.setText(lastSearchedFriend.getName());
                    searchEmail.setText(lastSearchedFriend.getEmail());
                    if (searchRespLayout.getVisibility() == View.GONE) {
                        searchRespLayout.setVisibility(View.VISIBLE);
                    }
                } else {
                    hideSearchFriendLayout();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void hideSearchFriendLayout() {
        if (searchRespLayout.getVisibility() == View.VISIBLE) {
            searchRespLayout.setVisibility(View.GONE);
        }
        lastSearchedFriend = null;
        searchName.setText("");
        searchEmail.setText("");
    }

    @Override
    public void addFriendResponse(JSONObject jsonObject) {
        runOnUiThread(() -> {
            try {
                if (jsonObject.getString(MESSAGE).equalsIgnoreCase(SUCCESSFUL) && jsonObject.getString("user_id").equalsIgnoreCase(getUserUuid(context))) {
                    Log.d(TAG, "friendsListAdapter -- > " + friendsListAdapter);
                    JSONObject friendData = jsonObject.getJSONObject("friendData");
                    Friend friend = new Friend(friendData.getString("name"), friendData.getString("email"), friendData.getString("user_id"));
                    friendsListAdapter.addMember(friend);
                    hideSearchFriendLayout();
                } else {
                    if (jsonObject.has("reason")) {
                        Toast.makeText(context, jsonObject.getString("reason"), Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void getFriendsResponse(JSONObject jsonObject) {
        runOnUiThread(() -> {
            try {
                if (jsonObject.getString(MESSAGE).equalsIgnoreCase(SUCCESSFUL)) {
                    JSONArray jsonArray = jsonObject.getJSONArray("connections");
                    if (jsonArray.length() > 0) {
                        for (int a = 0; a <= jsonArray.length() - 1; a++) {
                            JSONObject dataObj = jsonArray.getJSONObject(a);
                            Friend friend = new Friend(dataObj.getString("name"), dataObj.getString("email"), dataObj.getString("user_id"));
                            friendsListAdapter.addMember(friend);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendMessage(String message, String type) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("room", session_name.getText().toString());
            jsonObject.put("message", message);
            jsonObject.put("from", getUserName(context));
            jsonObject.put("uuid", getUserUuid(context));
            jsonObject.put("type", type);
            socketSignaling.emitMessage(jsonObject.toString());

            addMyMessage(jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addMyMessage(JSONObject jsonObject) {
        try {
            messagesListAdapter.add(new Message(jsonObject.getString("message"), OUTBOUND_MSG, jsonObject.getString("from"), jsonObject.getString("type")));

            activity.runOnUiThread(() -> {
                messagesListAdapter.notifyDataSetChanged();
                messageListView.setSelection(messageListView.getCount() - 1);
                messageET.setText("");
                hideKeyboard(messageET);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideKeyboard(View v) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 30 && resultCode == RESULT_OK) {
            Log.d(getClass().getName(), "document----------" + data.getData());
            uri = data.getData();
            String filename;
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);

            if (cursor == null) filename = uri.getPath();
            else {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
                filename = cursor.getString(idx);
                cursor.close();
            }

            String name = filename.substring(filename.lastIndexOf("/") + 1);
            String extension = filename.substring(filename.lastIndexOf(".") + 1);

            Log.d(getClass().getName(), "Filename--------" + name.toString() + " " + extension.toString());

            if (uri != null) {

                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("pdf files");
                final StorageReference filePath = storageReference.child(currentUser.getUid() + "/" + name);
                UploadTask uploadTask = filePath.putFile(uri);

                /*uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    }
                });*/

                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        if (taskSnapshot.getMetadata() != null) {
                            if (taskSnapshot.getMetadata().getReference() != null) {
                                Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                                result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        Log.d(getClass().getName(), "DownLoadurl" + uri);
                                        sendMessage(uri.toString(), MESSAGE_TYPE_PDF);
                                    }
                                });
                            }
                        }
                    }
                });
            }

        } else if (requestCode == 20 && resultCode == RESULT_OK) {
            Log.d(getClass().getName(), "document----------" + data);
            uri = data.getData();

            String filename;
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);

            if (cursor == null) filename = uri.getPath();
            else {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
                filename = cursor.getString(idx);
                cursor.close();
            }

            String name = filename.substring(filename.lastIndexOf("/") + 1);
            String extension = filename.substring(filename.lastIndexOf(".") + 1);

            Log.d(getClass().getName(), "Filename--------" + name.toString() + " " + extension.toString());
            if (uri != null) {
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("image files");
                final StorageReference filePath = storageReference.child(currentUser.getUid() + "/" + name);
                UploadTask uploadTask = filePath.putFile(uri);

                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        if (taskSnapshot.getMetadata() != null) {
                            if (taskSnapshot.getMetadata().getReference() != null) {
                                Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();


                                result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        Log.d(getClass().getName(), "DownLoadurl" + uri);
                                        sendMessage(uri.toString(), MESSAGE_TYPE_IMAGE);
                                    }
                                });
                            }
                        }
                    }
                });
            }
        } else if (requestCode == 40 && resultCode == RESULT_OK) {
            Log.d(getClass().getName(), "document----------" + data);
            uri = data.getData();

            String filename;
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);

            if (cursor == null) filename = uri.getPath();
            else {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
                filename = cursor.getString(idx);
                cursor.close();
            }

            // String name = filename.substring(filename.lastIndexOf("."));
            String name = filename.substring(filename.lastIndexOf("/") + 1);
            String extension = filename.substring(filename.lastIndexOf(".") + 1);

            Log.d(getClass().getName(), "Filename--------" + name.toString() + " " + extension.toString());

            if (uri != null) {

                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("document files");
                final StorageReference filePath = storageReference.child(currentUser.getUid() + "/" + name);
                UploadTask uploadTask = filePath.putFile(uri);

                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        if (taskSnapshot.getMetadata() != null) {
                            if (taskSnapshot.getMetadata().getReference() != null) {
                                Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();


                                result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        Log.d(getClass().getName(), "DownLoadurl" + uri);
                                        sendMessage(uri.toString(), MESSAGE_TYPE_DOCUMENT);
                                    }
                                });
                            }
                        }
                    }
                });
            }
        }
    }
}
