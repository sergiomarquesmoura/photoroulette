package com.photoroulette;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.facebook.widget.LoginButton.UserInfoChangedCallback;
import com.facebook.widget.ProfilePictureView;
import com.photoroulette.network.DiscoverServices;
import com.photoroulette.network.RegisterService;

public class MainActivity extends FragmentActivity {

    private LoginButton loginBtn;
    private Button postImageBtn;
    private Button updateStatusBtn;
    private String selectedImagePath;
    protected int counter = 0;
    private Bitmap myBitmap = null;
    private Button registerService;
    private Button discoverServices;

    private TextView userName;
    private ProfilePictureView profile_pic;

    private UiLifecycleHelper uiHelper;

    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    private static String message = "Sample status posted from android app";
    private String selectedImage = null;
    private boolean postSuccess = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uiHelper = new UiLifecycleHelper(this, statusCallback);
        uiHelper.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        userName = (TextView) findViewById(R.id.user_name);
        profile_pic = (ProfilePictureView) findViewById(R.id.profilePicture);
        loginBtn = (LoginButton) findViewById(R.id.authButton);
        loginBtn.setUserInfoChangedCallback(new UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                if (user != null) {
                    userName.setText("Hello, " + user.getName());
                    profile_pic.setProfileId(user.getId());
                } else {
                    userName.setText("You are not logged");
                    profile_pic.setProfileId(null);
                }
            }
        });

        registerService = (Button)findViewById(R.id.registerService);
        registerService.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, RegisterService.class);
                startActivity(intent);
            }
        });

        discoverServices = (Button)findViewById(R.id.discoverServices);
        discoverServices.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DiscoverServices.class);
                startActivity(intent);
            }
        });


        updateStatusBtn = (Button) findViewById(R.id.update_status);
        updateStatusBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                buttonsEnabled(false);
                updateStatusBtn.setText("Please Wait");
                Log.d("buttonclicked", "before randomimagepath");
                selectedImage = getRandomImagePath();
                Log.d("buttonclicked", "after randomimagepath");

                Log.d("buttonclicked", "before get file");

                File imgFile = new  File(selectedImage);
                Log.d("buttonclicked", "after get file");

                if(imgFile.exists()){
//                    Log.d("buttonclicked", "before convertBitmap");
//
//                    myBitmap = convertBitmap(imgFile.getAbsolutePath());
//                    Log.d("buttonclicked", "after convertBitmap");
//
//
//                    //String _orientation;
//                    try { //photo details like orientation, are stored in the exif file from the respective photo
//                        ExifInterface exif = new ExifInterface(imgFile.getPath());
//                        int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
//                        int photoRotationInDegrees = exifToDegrees(rotation);
//                        Log.d("Orientation:", ""+photoRotationInDegrees+" Degree");
//                    }
//                    catch (IOException e){
//                        e.printStackTrace();
//                    }

//                    if(myBitmap != null) {
//                        Log.d("buttonclicked", "before postimage");
//                        postImage(myBitmap);
//                        Log.d("buttonclicked", "after postimage");
//                    }
                    PhotoUploadTask _task = new PhotoUploadTask();
                    _task.execute(imgFile.getAbsolutePath());
                }


            }
        });

        buttonsEnabled(false);
    }

    private Session.StatusCallback statusCallback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state,
                         Exception exception) {
            if (state.isOpened()) {
                buttonsEnabled(true);
                Log.d("FacebookSampleActivity", "Facebook session opened");
            } else if (state.isClosed()) {
                buttonsEnabled(false);
                Log.d("FacebookSampleActivity", "Facebook session closed");
            }
        }
    };

    public void buttonsEnabled(boolean isEnabled) {
        updateStatusBtn.setEnabled(isEnabled);
    }

    public boolean postImage(Bitmap bitmap) {


        if (checkPermissions()) {
            Bitmap img = bitmap;
            Request uploadRequest = Request.newUploadPhotoRequest(
                    Session.getActiveSession(), img, new Request.Callback() {
                        @Override
                        public void onCompleted(Response response) {
                            Log.d("buttonclicked", "oncompleted");
                            postSuccess = true;

                        }
                    });
            Bundle params = uploadRequest.getParameters();
            params.putString("message", "Uploaded via PhotoRoulette. Is this embarrassing?");

            uploadRequest.executeAndWait();
        } else {
            requestPermissions();
        }
        return postSuccess;
    }

    public void postStatusMessage() {
        if (checkPermissions()) {
            Request request = Request.newStatusUpdateRequest(
                    Session.getActiveSession(), message,
                    new Request.Callback() {
                        @Override
                        public void onCompleted(Response response) {
                            if (response.getError() == null)
                                Toast.makeText(MainActivity.this,
                                        "Status updated successfully",
                                        Toast.LENGTH_LONG).show();
                        }
                    });
            request.executeAsync();
        } else {
            requestPermissions();
        }
    }

    public boolean checkPermissions() {
        Session s = Session.getActiveSession();
        if (s != null) {
            return s.getPermissions().contains("publish_actions");
        } else
            return false;
    }

    public void requestPermissions() {
        Session s = Session.getActiveSession();
        if (s != null)
            s.requestNewPublishPermissions(new Session.NewPermissionsRequest(
                    this, PERMISSIONS));
    }

    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
        buttonsEnabled(Session.getActiveSession().isOpened());
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle savedState) {
        super.onSaveInstanceState(savedState);
        uiHelper.onSaveInstanceState(savedState);
    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }

    public static Bitmap convertBitmap(String path)   {

        Bitmap bitmap=null;
        BitmapFactory.Options bfOptions=new BitmapFactory.Options();
        bfOptions.inDither=false;                     //Disable Dithering mode
        bfOptions.inPurgeable=true;                   //Tell to gc that whether it needs free memory, the Bitmap can be cleared
        bfOptions.inInputShareable=true;              //Which kind of reference will be used to recover the Bitmap data after being clear, when it will be used in the future
        bfOptions.inTempStorage=new byte[32 * 1024];


        File file=new File(path);
        FileInputStream fs=null;
        try {
            fs = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            if(fs!=null)
            {
                bitmap=BitmapFactory.decodeFileDescriptor(fs.getFD(), null, bfOptions);
            }
        } catch (IOException e) {

            e.printStackTrace();
        } finally{
            if(fs!=null) {
                try {
                    fs.close();
                } catch (IOException e) {

                    e.printStackTrace();
                }
            }
        }

        return bitmap;
    }

    public String getRandomImagePath(){

        String[] projection = new String[]{
                MediaStore.Images.Media.DATA,
        };

        Uri images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cur = this.getContentResolver().query(images,
                projection,
                "",
                null,
                ""
        );

        final ArrayList<String> imagesPath = new ArrayList<String>();
        if (cur.moveToFirst()) {

            int dataColumn = cur.getColumnIndex(
                    MediaStore.Images.Media.DATA);
            do {
                imagesPath.add(cur.getString(dataColumn));
            } while (cur.moveToNext());
        }
        cur.close();
        final Random random = new Random();
        final int count = imagesPath.size();

        int randomInt = random.nextInt(count-1);
        String randomImage = imagesPath.get(randomInt);
        Log.d("ESCOLHIDOOOO",imagesPath.get(randomInt));

        return randomImage;

    }

    private class PhotoUploadTask extends AsyncTask<String, Request, Boolean>{

        @Override
        protected void onProgressUpdate(Request... values) {
            super.onProgressUpdate(values);

        }

        @Override
        protected Boolean doInBackground(String... strings) {
            myBitmap = convertBitmap(strings[0]);
            //String _orientation;
//        try { //photo details like orientation, are stored in the exif file from the respective photo
//            ExifInterface exif = new ExifInterface(imgFile.getPath());
//            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
//            int photoRotationInDegrees = exifToDegrees(rotation);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
            if (myBitmap != null) {
                postSuccess = postImage(myBitmap);

            }
            return postSuccess;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            Log.d("onpostexecute", Boolean.toString(postSuccess));

            if(result) {
                buttonsEnabled(true);
                updateStatusBtn.setText("Play the Roulette");
                Toast.makeText(MainActivity.this,
                        "Photo uploaded successfully",
                        Toast.LENGTH_LONG).show();
            }
            else
                Toast.makeText(MainActivity.this,
                        "Photo upload failed",
                        Toast.LENGTH_LONG).show();
        }

    }
}