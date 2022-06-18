package in.app.chirpz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import in.app.chirpz.openvidu.Session;
import in.app.chirpz.utils.SessionManager;
import in.app.chirpz.websocket.SocketSignaling;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.LoggingBehavior;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity implements FacebookCallback<LoginResult>, SocketSignaling.SignalingInterface {

    CallbackManager callbackManager;
    LoginManager loginManager;
    private static final String EMAIL = "email";
    GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    Context context = this;

    @BindView(R.id.button)
    Button button;

    @BindView(R.id.google_button)
    Button googleButton;

    SocketSignaling socketSignaling;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        FacebookSdk.sdkInitialize(getApplicationContext());
        FacebookSdk.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        FirebaseApp.initializeApp(context);
        mAuth = FirebaseAuth.getInstance();

        socketSignaling = SocketSignaling.getInstance();
        socketSignaling.init(this, context);

        callbackManager = CallbackManager.Factory.create();
        loginManager = com.facebook.login.LoginManager.getInstance();
        loginManager.registerCallback(callbackManager, this);

        googleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });

        button.setOnClickListener(view -> facebookLogin());

        if(!SessionManager.getPreferences(context, "user_uuid").equalsIgnoreCase("0")){
            goToDashboard();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            Log.d(getClass().getName(), "-------------" + requestCode + " -----" + resultCode + "-----" + result.isSuccess());
            if (result.isSuccess()) {
                GoogleSignInAccount acct = result.getSignInAccount();
                firebaseAuthWithGoogle(acct.getIdToken());
            }
        } else {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSuccess(LoginResult loginResult) {
        if (loginResult != null) {
            Profile profile = Profile.getCurrentProfile();
            if (profile != null) {
                String firstName = profile.getFirstName();
                String lastName = profile.getLastName();
                String social_id = profile.getId();
                String profileURL = String.valueOf(profile.getProfilePictureUri(200, 200));
                Log.e(getClass().getName(), "social Id: " + profileURL);
            }
            GraphRequest request = GraphRequest.newMeRequest(
                    loginResult.getAccessToken(),
                    new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(JSONObject object, GraphResponse response) {
                            Log.v("LoginActivity", response.toString());
                            // Application code
                            try {
                                String email = object.getString("email");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
            handleFacebookAccessToken(loginResult.getAccessToken());
            Bundle parameters = new Bundle();
            parameters.putString("fields", "id,first_name,name,last_name,email,gender,birthday");
            request.setParameters(parameters);
            request.executeAsync();
        }
    }

    @Override
    public void onCancel() {
    }

    @Override
    public void onError(FacebookException error) {
        Log.d(getClass().getName(), "FacebookException "+error.toString());
    }

    private void facebookLogin() {
        loginManager.logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
    }

    private void signInWithGoogle() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("428641028223-sov3hlk8qs3j57rkn8lt2o39n1e2b9pt.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
        final Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Log.d(getClass().getName(), "---------" + currentUser);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(getClass().getName(), "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(getClass().getName(), "user----------> " + user.getDisplayName());

                            SessionManager.setPreferences(context, "user_email", user.getEmail());
                            SessionManager.setPreferences(context, "user_name", user.getDisplayName());


                            doLogin();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(getClass().getName(), "signInWithCredential:failure", task.getException());
                            // updateUI(null);
                        }
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d(getClass().getName(), "handleFacebookAccessToken:" + token);
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(getClass().getName(), "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(getClass().getName(), " -------- "+user.getEmail());
                            doLogin();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.d(getClass().getName(), "signInWithCredential:failure", task.getException());
                        }
                    }
                });
    }

    private void doLogin(){
        try{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name",SessionManager.getPreferences(context, "user_name"));
            jsonObject.put("email",SessionManager.getPreferences(context, "user_email"));
            jsonObject.put("phonenumber","");
            socketSignaling.emitLogin(jsonObject);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void goToDashboard() {
        Intent intent = new Intent(context, SessionActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRemoteHangUp(String msg) {

    }

    @Override
    public void onLogin() {
        goToDashboard();
    }

    @Override
    public void onLoginFailed() {
        Log.d(getClass().getName(), "Login failed !");
    }

    @Override
    public void onCreatedRoom(JSONObject jsonObject) {

    }

    @Override
    public void onJoinedRoom() {

    }

    @Override
    public void onNewPeerJoined() {

    }

    @Override
    public void onMessageReceived(JSONObject jsonObject) {

    }

    @Override
    public void invalidSession() {

    }

    @Override
    public void validSession() {

    }

    @Override
    public void onRequestToJoinApproved(JSONObject jsonObject) {

    }

    @Override
    public void addParticipant(JSONObject jsonObject) {

    }

    @Override
    public void waitingForApproval() {

    }

    @Override
    public void friendSearchResponse(JSONObject jsonObject) {

    }

    @Override
    public void addFriendResponse(JSONObject jsonObject) {

    }

    @Override
    public void getFriendsResponse(JSONObject jsonObject) {

    }

    @Override
    public void inComingCallReq(JSONObject jsonObject) {

    }

    @Override
    public void joinedClassRoom(JSONObject jsonObject) {

    }
}
