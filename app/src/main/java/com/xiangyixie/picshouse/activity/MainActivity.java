package com.xiangyixie.picshouse.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.xiangyixie.picshouse.R;
import com.xiangyixie.picshouse.fragment.FragPagerAdapter;
import com.xiangyixie.picshouse.fragment.TabCameraFragment;
import com.xiangyixie.picshouse.fragment.TabDiscoverFragment;
import com.xiangyixie.picshouse.fragment.TabHouseCommentFragment;
import com.xiangyixie.picshouse.fragment.TabHouseFragment;
import com.xiangyixie.picshouse.fragment.TabNotificationFragment;
import com.xiangyixie.picshouse.fragment.TabUserEditProfileFragment;
import com.xiangyixie.picshouse.fragment.TabUserFragment;
import com.xiangyixie.picshouse.model.Post;
import com.xiangyixie.picshouse.model.User;
import com.xiangyixie.picshouse.service.NotificationRegistrationService;
import com.xiangyixie.picshouse.view.MyViewPager_notSwiping;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

//import android.app.FragmentManager;


public class MainActivity extends AppCompatActivity
implements TabHouseFragment.OnFragmentInteractionListener,
TabUserFragment.OnFragmentInteractionListener,
TabUserEditProfileFragment.OnFragmentInteractionListener {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    private static final int TAB_HOUSE = 0;
    private static final int TAB_DISCOVER = 1;
    private static final int TAB_CAMERA = 2;
    private static final int TAB_NOTIFY = 3;
    private static final int TAB_USER = 4;

    private static final int COUNT_TAB = 5;


    private static final int INTENT_CAMERA = 0;
    private static final int INTENT_FILTER = 1;

    public static final int MEDIA_TYPE_IMAGE = 0;
    public static final int MEDIA_TYPE_VIDEO = 1;

    private static final String TAG = "MainActivity";

    public static final String IMAGE_PATH = "IMAGE_PATH";

    public static MainActivity instance = null;

    private MyViewPager_notSwiping mTabPager;
    private View[] mTab;    // mTab1,mTab2,mTab3,mTab4,mTab5;
    private FragPagerAdapter mFragPagerAdapter;


    private ArrayList<Fragment> mFragmentList = null;


    private TabHouseFragment mTabHouseFrag = null;
    private TabDiscoverFragment mTabDiscoverFrag = null;
    private TabCameraFragment mTabCameraFrag = null;
    private TabNotificationFragment mTabNotificationFrag = null;
    private TabUserFragment mTabUserFrag = null;

    private Toolbar mToolbar = null;


    private int curIndex = 0;     //current page tab index for 'mTabPager': 0--4


    private LayoutInflater inflater;
    private Uri mImageUri = null;

    private BroadcastReceiver mNotifyRegBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Notification Registeration complete");
        }
    };
    private boolean mIsNotifyRegReceiverRegistered = false;


    private FragmentManager.OnBackStackChangedListener mFragBackStackChangeListener =
            new FragmentManager.OnBackStackChangedListener() {
                @Override
                public void onBackStackChanged() {

                }
            };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        instance = this;
        Fresco.initialize(this);
        setContentView(R.layout.activity_main);

        initToolbar();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mFragmentList = new ArrayList<Fragment>();
        mTabHouseFrag = new TabHouseFragment();
        mTabDiscoverFrag = new TabDiscoverFragment();
        mTabCameraFrag = new TabCameraFragment();
        mTabNotificationFrag = new TabNotificationFragment();
        mTabUserFrag = new TabUserFragment();

        mTabUserFrag.setInteractionListener(this);
        mTabHouseFrag.setInteractionListener(this);

        mFragmentList.add(mTabHouseFrag);
        mFragmentList.add(mTabDiscoverFrag);
        mFragmentList.add(mTabCameraFrag);
        mFragmentList.add(mTabNotificationFrag);
        mFragmentList.add(mTabUserFrag);


        //set viewPager view <-> fragmentPagerAdapter
        mTabPager = (MyViewPager_notSwiping) findViewById(R.id.tabpager);
        mTabPager.setPagingEnabled(false);                                   //setting not swiping!

        Log.d("DEBUG", "debug + ");

        mFragPagerAdapter = new FragPagerAdapter(getSupportFragmentManager(), mFragmentList);
        mTabPager.setAdapter(mFragPagerAdapter);
        mTabPager.setOnPageChangeListener(new MyOnPageChangeListener());

        mTab = new View[COUNT_TAB];
        mTab[TAB_HOUSE] = (View) findViewById(R.id.bottom_house);
        mTab[TAB_DISCOVER] = (View) findViewById(R.id.bottom_discover);
        mTab[TAB_CAMERA] = (View) findViewById(R.id.bottom_camera);
        mTab[TAB_NOTIFY] = (View) findViewById(R.id.bottom_notification);
        mTab[TAB_USER] = (View) findViewById(R.id.bottom_user);


        //Initial tab yellow color selected state
        mTab[TAB_HOUSE].setBackgroundColor(getResources().getColor(R.color.yellow));


        for (int i = 0; i < COUNT_TAB; ++i) {
            if (i != TAB_CAMERA) {
                mTab[i].setOnClickListener(new MyOnClickListener(i));
            }
        }
        //set TabCamera onClickListener
        mTab[TAB_CAMERA].setOnClickListener(new TabCameraClickListener());

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            public void onBackStackChanged() {
                Log.d(TAG, "back stack changed ");
            }
            });

        registerNotifyRegBroadcastReceiver();

        if (checkPlayServices()) {
            Intent intent = new Intent(this, NotificationRegistrationService.class);
            startService(intent);
        }

        Intent intent = getIntent();
        if (intent != null && intent.getStringExtra("TAB") != null &&
                intent.getStringExtra("TAB").equals("USER")) {
            mTabPager.setCurrentItem(TAB_USER);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        registerNotifyRegBroadcastReceiver();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mNotifyRegBroadcastReceiver);
        mIsNotifyRegReceiverRegistered = false;
        super.onPause();
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.main_activity_toolbar);
        setSupportActionBar(mToolbar);
    }

    // TabHouse
    @Override
    public void onComment(Post post, int comment_idx) {
        TabHouseCommentFragment comment_fragment = new TabHouseCommentFragment();
        comment_fragment.initialize(post, comment_idx);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack
        transaction.add(R.id.main_container, comment_fragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    // TabUser
    @Override
    public void onEditProfile(User user) {
        TabUserEditProfileFragment editProfileFragment = new TabUserEditProfileFragment();
        editProfileFragment.initialize(user, this);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.add(R.id.main_container, editProfileFragment);
        transaction.addToBackStack(null);

        transaction.commit();
    }

    // UserEditProfile
    @Override
    public void onProfileUpdated(User user) {
        getSupportFragmentManager().popBackStack();
    }


    //MyOnClickListener for 'mTab1' - 'mTab5'
    public class MyOnClickListener implements View.OnClickListener {

        private int index = 0;

        public MyOnClickListener(int i) {
            index = i;
        }

        @Override
        public void onClick(View v) {

            mTabPager.setCurrentItem(index, false);
            //important!Use this to control 'mTabPager' visible fragment index!

        }
    };


    //for 'mTabPager' change page
    public class MyOnPageChangeListener implements OnPageChangeListener {

        @Override
        public void onPageSelected(int arg0) {

            if (curIndex != TAB_CAMERA) {
                if (0 <= curIndex && curIndex <= 4) {
                    mTab[curIndex].setBackgroundColor(getResources().getColor(R.color.transparent));
                }
            }

            if (arg0 != TAB_CAMERA) {
                mTab[arg0].setBackgroundColor(getResources().getColor(R.color.yellow));
            }

            curIndex = arg0;
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }
    }


    private static Uri getOutputImageFileUri() {

        return Uri.fromFile(getOutputMediaFile(MEDIA_TYPE_IMAGE));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    //TabCamera onClickListener
    public class TabCameraClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            //mTabPager.setCurrentItem(TAB_CAMERA);
            /*if(checkCameraHardware(MainActivity.this)) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivityForResult(intent, INTENT_CAMERA);
            }*/
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            mImageUri = getOutputImageFileUri();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);

            MainActivity.this.startActivityForResult(intent, INTENT_CAMERA);
        }
    }

    @Override
    protected void onActivityResult(int req_code, int res_code, Intent data) {
        switch (req_code) {
            case INTENT_CAMERA: {
                //mTab[TAB_HOUSE].setBackgroundColor(getResources().getColor(R.color.blue));

                if (res_code == RESULT_OK) {
                    Intent intent = new Intent(MainActivity.this, FilterActivity.class);
                    intent.putExtra(IMAGE_PATH, mImageUri.toString());
                    mImageUri = null;
                    startActivityForResult(intent, INTENT_FILTER);
                }

                break;
            }
            case INTENT_FILTER: {
                mTab[TAB_HOUSE].setBackgroundColor(getResources().getColor(R.color.yellow));
                break;
            }
            default:
                break;
        }
    }

    // Debug menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int item_id = item.getItemId();

        if (item_id == R.id.action_notify) {
            requestPushNotification();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void requestPushNotification() {

    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    private void registerNotifyRegBroadcastReceiver() {
        if (!mIsNotifyRegReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    mNotifyRegBroadcastReceiver,
                    new IntentFilter(NotificationRegistrationService.REGISTRATION_COMPLETE));
            mIsNotifyRegReceiverRegistered = true;        }
    }

}



