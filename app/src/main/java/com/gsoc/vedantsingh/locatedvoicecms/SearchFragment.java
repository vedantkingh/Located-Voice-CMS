package com.gsoc.vedantsingh.locatedvoicecms;

import android.accounts.Account;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.gsoc.vedantsingh.locatedvoicecms.beans.Category;
import com.gsoc.vedantsingh.locatedvoicecms.beans.POI;
import com.gsoc.vedantsingh.locatedvoicecms.data.POIsContract;
import com.gsoc.vedantsingh.locatedvoicecms.utils.LGUtils;
import com.gsoc.vedantsingh.locatedvoicecms.utils.PoisGridViewAdapter;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class SearchFragment extends Fragment {

    private final int REQ_CODE_SPEECH_INPUT = 100;
    private String Audio_Path = "";
    private static boolean isPlaying = false;
    private static MediaPlayer mediaPlayer = new MediaPlayer();
    View rootView;
    GridView poisGridView;
    Session session;
    private EditText editSearch;
    private FloatingActionButton buttonSearch;
    private ImageView earth, moon, mars;
    private String currentPlanet = "EARTH";
    private FloatingActionButton btnSpeak;
    private ListView categoriesListView;
    private Button nearbyplaces, sound_btn;
    public static Button listen_desc;
//    private DriveServiceHelper driveServiceHelper;
    private CategoriesAdapter adapter;
    private TextView categorySelectorTitle;
    private ImageView backIcon, backStartIcon;
    SharedPreferences sharedPreferences;
    private static ArrayList<String> backIDs = new ArrayList<>();

    private static final String TAG = "SearchFragment";

    private static final int REQUEST_CODE_SIGN_IN = 1;
    public static DriveServiceHelper mDriveServiceHelper;
    public static String recentPOI;
    public static int CategoryIdForVoice;
    String DRIVE_FOLDER_ID = "1IqFDdaIRWhqv580G2uknYaFgGQmEVQ8P";


    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.newsearch_fragment, container, false);
//        editSearch = (EditText) rootView.findViewById(R.id.search_edittext);
//        buttonSearch = (FloatingActionButton) rootView.findViewById(R.id.searchButton);
//        earth = (ImageView) rootView.findViewById(R.id.earth);
//        moon = (ImageView) rootView.findViewById(R.id.moon);
//        mars = (ImageView) rootView.findViewById(R.id.mars);

//        btnSpeak = (FloatingActionButton) rootView.findViewById(R.id.btnSpeak);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        categoriesListView = (ListView) rootView.findViewById(R.id.categories_listview);
        nearbyplaces = rootView.findViewById(R.id.nearbyplaces);
        listen_desc = rootView.findViewById(R.id.listen_desc);
        sound_btn = rootView.findViewById(R.id.sound_btn);
        backIcon = (ImageView) rootView.findViewById(R.id.back_icon);
        backStartIcon = (ImageView) rootView.findViewById(R.id.back_start_icon);//comes back to the initial category
        categorySelectorTitle = (TextView) rootView.findViewById(R.id.current_category);

//        btnSpeak.setOnClickListener(new View.OnClickListener() {

//            @Override
//            public void onClick(View v) {
//                promptSpeechInput();
//            }
//        });

//        screenSizeTreatment();
//        setSearchInLGButton();
//        setPlanetsButtonsBehaviour();

//        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE));
//        credential.setSelectedAccount(new Account(userAccount, "com.google"));
//        Drive driveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();


        poisGridView = (GridView) rootView.findViewById(R.id.POISgridview);

        if (getArguments() != null) {
            String currentplanet = getArguments().getString("currentplanet");
            if(Objects.equals(currentplanet, "EARTH")){Earth();}
            else if(Objects.equals(currentplanet, "MOON")){Moon();}
            else if(Objects.equals(currentplanet, "MARS")){Mars();};

        }

        backStartIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backIDs.clear();
                Category category = getCategoryByName(currentPlanet);
                backIDs.add(String.valueOf(category.getId()));
                if(isPlaying){
                    mediaPlayer.stop();
                    sound_btn.setText("Play Sound  ");
                    sound_btn.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_volume_up_24, 0);
                }
                Audio_Path = "";
                showPoisByCategory();
            }
        });

        backIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (backIDs.size() > 1) {
                    backIDs.remove(0);
                    if(isPlaying){
                        mediaPlayer.stop();
                        sound_btn.setText("Play Sound  ");
                        sound_btn.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_volume_up_24, 0);
                    }
                    Audio_Path = "";
                }
                showPoisByCategory();
            }
        });

        nearbyplaces.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String machinesString = sharedPreferences.getString("Machines", "3");
                int machines = Integer.parseInt(machinesString);
                int slave_num = Math.floorDiv(machines, 2) + 1;
                String slave_name = "slave_" + slave_num;
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                SearchFragment.NearbyPlacesTask nearbyPlacesTask = new SearchFragment.NearbyPlacesTask(slave_name, session, getContext());
                Future<Void> future = executorService.submit(nearbyPlacesTask);
                try {
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                executorService.shutdown();
            }
        });

        if (!isAudioSaved(sharedPreferences)) {
            POIsContract.CategoryEntry.saveAudioToDevice(getContext());

            // Store the updated value of audioSaved in shared preferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("audioSaved", true);
            editor.apply();
        }

        sound_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Audio_Path.isEmpty()) {
                    if (isPlaying) {
                        // If audio is already playing, stop it
                        mediaPlayer.stop();
                        mediaPlayer.reset();
                        sound_btn.setText("Play Sound  ");
                        sound_btn.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_volume_up_24, 0);
                        isPlaying = false;
                    } else {
                        // Start playing the audio from the beginning
                        try {
                            mediaPlayer.setDataSource(Audio_Path);
                            mediaPlayer.prepare();
                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    sound_btn.setText("Play Sound  ");
                                    sound_btn.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_volume_up_24, 0);
                                }
                            });
                            mediaPlayer.start();
                            sound_btn.setText("Stop Sound  ");
                            sound_btn.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_stop_24, 0);
                            isPlaying = true;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    Toast.makeText(getContext(), "Audio not set", Toast.LENGTH_SHORT).show();
                }
            }
        });

//        driveServiceHelper = new DriveServiceHelper();

        listen_desc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                try {
//                    DriveServiceHelper.testMethod(getContext());
//                } catch (IOException e) {
//                    Log.d("Here is the problem",e.getMessage());
//                    throw new RuntimeException(e);
//                } catch (GeneralSecurityException e) {
//                    throw new RuntimeException(e);
//                }
                if(listen_desc.getText().toString().equals("Listen Description  ")){
                    requestSignIn();
                } else {
                    mDriveServiceHelper.stopVoicePlayer();
                    listenDescButtonResetState();
                }
            }
        });

        return rootView;
    }

    public static void listenDescButtonResetState(){
        listen_desc.setText("Listen Description  ");
        listen_desc.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_library_books_24, 0);
    }

    public static void listenDescButtonPlayState(){
        listen_desc.setText("Stop Listening to Description  ");
        listen_desc.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_stop_24, 0);
    }

    private void requestSignIn() {
        Log.d(TAG, "Requesting sign-in");

        GoogleSignInAccount lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(getContext());
        if (lastSignedInAccount != null) {
            // User is already signed in, handle the signed-in account
            handleSignInResult(lastSignedInAccount);
        } else {
            // User is not signed in, initiate the sign-in process
            GoogleSignInOptions signInOptions =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(new Scope(DriveScopes.DRIVE_READONLY))
                            .build();
            GoogleSignInClient client = GoogleSignIn.getClient(getContext(), signInOptions);

            // The result of the sign-in Intent is handled in onActivityResult.
            startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
        }
    }

    private void handleSignInResult(GoogleSignInAccount googleAccount) {
        if(googleAccount != null){
            Log.d(TAG, "Signed in as " + googleAccount.getEmail());

            // Use the authenticated account to sign in to the Drive service.
            GoogleAccountCredential credential =
                    GoogleAccountCredential.usingOAuth2(
                            getContext(), Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(googleAccount.getAccount());
            Drive googleDriveService =
                    new Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            new GsonFactory(),
                            credential)
                            .setApplicationName("Located Voice CMS")
                            .build();

            // The DriveServiceHelper encapsulates all REST API and SAF functionality.
            // Its instantiation is required before handling any onClick actions.
            mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
            if(recentPOI != null){
                mDriveServiceHelper.playAudioFileInFolder(DRIVE_FOLDER_ID, POIsContract.CategoryEntry.getNameById(getContext(), CategoryIdForVoice), recentPOI +".mp3");
            }else{
                Toast.makeText(getContext(), "Please select a POI to listen to its description", Toast.LENGTH_SHORT).show();
            }
            Log.d("Sign In", "Completed");
        } else {
            Log.e(TAG, "Unable to sign in.");
        }
    }

    public static void setCategoryForVoice(){
        CategoryIdForVoice = Integer.parseInt(backIDs.get(0));
    };


//    private void navigateFoldersAndFindAudioFile(String folderName) {
//        try {
//            String folderId = driveServiceHelper.getFolderIdByName(folderName);
//            if (folderId != null) {
//                List<File> files = driveServiceHelper.listFilesInFolder(folderId);
//                for (File file : files) {
//                    if (file.getMimeType().startsWith("audio/") && file.getName().equals("desired_audio_file.mp3")) {
//                        playAudioFile(file);
//                        return;
//                    }
//                }
//            } else {
//                // Handle case when the specified folder name does not exist
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            // Handle error during navigation or file retrieval
//        }
//    }
//
//    private void playAudioFile(File audioFile) {
//        // Use MediaPlayer or your chosen audio playback library to play the file
//        String audioFileUrl = audioFile.getWebContentLink();
//        // Configure MediaPlayer or library to play the audioFileUrl
//        // ...
//    }

//    public static Context getSearchFragmentContext() {
//        return this.getContext();
//    }

    public static boolean isAudioSaved(SharedPreferences sharedPrefs) {
        return sharedPrefs.getBoolean("audioSaved", false);
    }

    public static void audioPlayerStop(){
        if(isPlaying){
            mediaPlayer.stop();
            mediaPlayer.reset();
            isPlaying = false;
        }
    }

    private void setAudioFile(){
        String audioFilePath = POIsContract.CategoryEntry.getAudioPathByID(getActivity(), Integer.parseInt(backIDs.get(0)));
        if(audioFilePath != null){
            Audio_Path = audioFilePath;
        }
    }

    private void showPoisByCategory() {

        Cursor queryCursor = getCategoriesCursor();
        showCategoriesOnScreen(queryCursor);

        String currentCategoryName = POIsContract.CategoryEntry.getNameById(getActivity(), Integer.parseInt(backIDs.get(0)));

        categorySelectorTitle.setText(currentCategoryName);

        final List<POI> poisList = getPoisList(Integer.parseInt(backIDs.get(0)));
        if (poisList != null) {
            poisGridView.setAdapter(new PoisGridViewAdapter(poisList, getActivity(), getActivity()));
        }
    }

    private Cursor getCategoriesCursor() {
        //we get only the categories that the admin user wants to be shown on the app screen and have father category ID the once of the parameters.
        return POIsContract.CategoryEntry.getNotHiddenCategoriesByFatherID(getActivity(), backIDs.get(0));
    }

    private void showCategoriesOnScreen(Cursor queryCursor) {
        adapter = new CategoriesAdapter(getActivity(), queryCursor, 0);

        if (queryCursor.getCount() > 0) {
            categoriesListView.setAdapter(adapter);

            categoriesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    Cursor cursor = (Cursor) parent.getItemAtPosition(position);//gets the category selected
                    if (cursor != null) {
                        int itemSelectedID = cursor.getInt(0);
                        backIDs.add(0, String.valueOf(itemSelectedID));
                        //this method is call to see AGAIN the categories list. However, the view will
                        //correspond to the categories inside the current category just clicked.
                        showPoisByCategory();
                        setAudioFile();
                    }
                }
            });
        } else {
            categoriesListView.setAdapter(null);
        }
    }

//    private void promptSpeechInput() {
//
//        Locale spanish = new Locale("es", "ES");
//        Locale catalan = new Locale("ca", "ES");
//
//        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
//        intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, catalan);
//        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, catalan);
//        intent.putExtra(RecognizerIntent.EXTRA_RESULTS, catalan);
//        intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, spanish);
//        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, spanish);
//        intent.putExtra(RecognizerIntent.EXTRA_RESULTS, spanish);
//        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));
//        try {
//            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
//        } catch (ActivityNotFoundException a) {
//            Toast.makeText(getActivity().getApplicationContext(), getString(R.string.speech_not_supported),
//                    Toast.LENGTH_SHORT).show();
//        }
//    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == Activity.RESULT_OK && null != data) {

                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    String placeToSearch = result.get(0);
                    if (placeToSearch != null && !placeToSearch.equals("")) {
                        editSearch.setText(placeToSearch);
                        String command = buildSearchCommand(placeToSearch);
                        SearchTask searchTask = new SearchTask(command, false);
                        searchTask.execute();

                    } else {
                        Toast.makeText(getActivity(), getResources().getString(R.string.please_enter_search), Toast.LENGTH_LONG).show();
                    }
                }
                break;
            }
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    GoogleSignIn.getSignedInAccountFromIntent(data)
                            .addOnSuccessListener(googleSignInAccount -> {
                                handleSignInResult(googleSignInAccount);
                            });
                }
                break;
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            backIDs = savedInstanceState.getStringArrayList("backIds");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("backIds", backIDs);
    }

    @Override
    public void onResume() {
        super.onResume();
        showPoisByCategory();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (currentPlanet.equals("EARTH")) {
            Category category = getCategoryByName(currentPlanet);

            final List<POI> poisList = getPoisList(category.getId());
            if (poisList != null) {
                poisGridView.setAdapter(new PoisGridViewAdapter(poisList, getActivity(), getActivity()));
            }
        }

        Category category = getCategoryByName(currentPlanet);
        categorySelectorTitle.setText(category.getName());

        backIDs.add(String.valueOf(category.getId()));
        Cursor queryCursor = POIsContract.CategoryEntry.getNotHiddenCategoriesByFatherID(getActivity(), String.valueOf(category.getId()));
        showCategoriesOnScreen(queryCursor);

        GetSessionTask getSessionTask = new GetSessionTask();
        getSessionTask.execute();
    }

    private void setPlanetsButtonsBehaviour() {
        Earth();
        Moon();
        Mars();
    }

    private List<POI> getPoisList(int categoryId) {

        List<POI> lPois = new ArrayList<>();

        try (Cursor allPoisByCategoryCursor = POIsContract.POIEntry.getPOIsByCategory(getActivity(), String.valueOf(categoryId))) {

            while (allPoisByCategoryCursor.moveToNext()) {

                int poiId = allPoisByCategoryCursor.getInt(0);

                POI poiEntry = getPoiData(poiId);
                lPois.add(poiEntry);
            }
        }
        return lPois;
    }

    private POI getPoiData(int poiId) {
        POI poiEntry = new POI();
        Cursor poiCursor = POIsContract.POIEntry.getPoiByID(poiId);

        if (poiCursor.moveToNext()) {

            poiEntry.setId(poiCursor.getLong(poiCursor.getColumnIndex(POIsContract.POIEntry.COLUMN_ID)));
            poiEntry.setName(poiCursor.getString(poiCursor.getColumnIndex(POIsContract.POIEntry.COLUMN_COMPLETE_NAME)));
            poiEntry.setAltitude(poiCursor.getDouble(poiCursor.getColumnIndex(POIsContract.POIEntry.COLUMN_ALTITUDE)));
            poiEntry.setAltitudeMode(poiCursor.getString(poiCursor.getColumnIndex(POIsContract.POIEntry.COLUMN_ALTITUDE_MODE)));
            poiEntry.setCategoryId(poiCursor.getInt(poiCursor.getColumnIndex(POIsContract.POIEntry.COLUMN_CATEGORY_ID)));
            poiEntry.setHeading(poiCursor.getDouble(poiCursor.getColumnIndex(POIsContract.POIEntry.COLUMN_HEADING)));
            poiEntry.setLatitude(poiCursor.getDouble(poiCursor.getColumnIndex(POIsContract.POIEntry.COLUMN_LATITUDE)));
            poiEntry.setLongitude(poiCursor.getDouble(poiCursor.getColumnIndex(POIsContract.POIEntry.COLUMN_LONGITUDE)));
            poiEntry.setHidden(poiCursor.getInt(poiCursor.getColumnIndex(POIsContract.POIEntry.COLUMN_HIDE)) == 1);
            poiEntry.setRange(poiCursor.getDouble(poiCursor.getColumnIndex(POIsContract.POIEntry.COLUMN_RANGE)));
            poiEntry.setTilt(poiCursor.getDouble(poiCursor.getColumnIndex(POIsContract.POIEntry.COLUMN_TILT)));
            poiEntry.setVisited_place(poiCursor.getString(poiCursor.getColumnIndex(POIsContract.POIEntry.COLUMN_VISITED_PLACE_NAME)));
        }
        poiCursor.close();
        return poiEntry;
    }

    private Category getCategoryByName(String categoryName) {
        Category category = new Category();
        try (Cursor categoryCursor = POIsContract.CategoryEntry.getCategoriesByName(getActivity(), categoryName)) {

            if (categoryCursor.moveToNext()) {
                category.setId(categoryCursor.getInt(categoryCursor.getColumnIndex(POIsContract.CategoryEntry.COLUMN_ID)));
                category.setFatherID(categoryCursor.getInt(categoryCursor.getColumnIndex(POIsContract.CategoryEntry.COLUMN_FATHER_ID)));
                category.setName(categoryCursor.getString(categoryCursor.getColumnIndex(POIsContract.CategoryEntry.COLUMN_NAME)));
                category.setShownName(categoryCursor.getString(categoryCursor.getColumnIndex(POIsContract.CategoryEntry.COLUMN_SHOWN_NAME)));
            }
        }
        return category;
    }

    private void Earth() {
        String command = "echo 'planet=earth' > /tmp/query.txt";

//        if (!Objects.equals(getArguments().getString("currentplanet"), "EARTH")) {
            SearchTask searchTask = new SearchTask(command, true);
            searchTask.execute();
            currentPlanet = "EARTH";
//        }

        Category category = getCategoryByName(currentPlanet);
        categorySelectorTitle.setText(category.getName());

        backIDs = new ArrayList<>();
        backIDs.add(String.valueOf(category.getId()));

        Cursor queryCursor = POIsContract.CategoryEntry.getNotHiddenCategoriesByFatherID(getActivity(), String.valueOf(category.getId()));
        showCategoriesOnScreen(queryCursor);

        final List<POI> poisList = getPoisList(category.getId());
        if (poisList != null) {
            poisGridView.setAdapter(new PoisGridViewAdapter(poisList, getActivity(), getActivity()));
        }
    }

    void Moon() {

        String command = "echo 'planet=moon' > /tmp/query.txt";
        if (!currentPlanet.equals("MOON")) {
            //setConnectionWithLiquidGalaxy(command);
            SearchTask searchTask = new SearchTask(command, true);
            searchTask.execute();
            currentPlanet = "MOON";
            Category category = getCategoryByName(currentPlanet);
            categorySelectorTitle.setText(category.getName());

            backIDs = new ArrayList<>();
            backIDs.add(String.valueOf(category.getId()));

            Cursor queryCursor = POIsContract.CategoryEntry.getNotHiddenCategoriesByFatherID(getActivity(), String.valueOf(category.getId()));
            showCategoriesOnScreen(queryCursor);

            final List<POI> poisList = getPoisList(category.getId());
            poisGridView.setAdapter(new PoisGridViewAdapter(poisList, getActivity(), getActivity()));
        }
    }

    private void Mars() {

        String command = "echo 'planet=mars' > /tmp/query.txt";
        if (!currentPlanet.equals("MARS")) {
            SearchTask searchTask = new SearchTask(command, true);
            searchTask.execute();
            currentPlanet = "MARS";
            Category category = getCategoryByName(currentPlanet);
            categorySelectorTitle.setText(category.getName());

            backIDs = new ArrayList<>();
            backIDs.add(String.valueOf(category.getId()));

            Cursor queryCursor = POIsContract.CategoryEntry.getNotHiddenCategoriesByFatherID(getActivity(), String.valueOf(category.getId()));
            showCategoriesOnScreen(queryCursor);

            final List<POI> poisList = getPoisList(category.getId());
            poisGridView.setAdapter(new PoisGridViewAdapter(poisList, getActivity(), getActivity()));
        }
    }

    private void screenSizeTreatment() {
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        float scaleFactor = metrics.density;


        //The size of the diagonal in inches is equal to the square root of the height in inches squared plus the width in inches squared.
        float widthDp = widthPixels / scaleFactor;
        float heightDp = heightPixels / scaleFactor;

        float smallestWidth = Math.min(widthDp, heightDp);


        if (smallestWidth == 800) {
            //Samsung Tab E => smallestWidth:800

            editSearch.setTextSize(30);
            earth.getLayoutParams().height = 160;
            moon.getLayoutParams().height = 160;
            mars.getLayoutParams().height = 160;
            earth.getLayoutParams().width = 160;
            moon.getLayoutParams().width = 160;
            mars.getLayoutParams().width = 160;
            earth.requestLayout();
            moon.requestLayout();
            mars.requestLayout();
            categoriesListView.getLayoutParams().width = 350;
            if (rootView.findViewById(R.id.layoutPlanets) != null) {
                LinearLayout layoutPlanets = (LinearLayout) rootView.findViewById(R.id.layoutPlanets);
                LinearLayout.LayoutParams actualParams = (LinearLayout.LayoutParams) layoutPlanets.getLayoutParams();
                actualParams.setMarginStart(0);
                layoutPlanets.setLayoutParams(actualParams);
            }

        } else if (smallestWidth == 1032) {
            //Tablet All In One Big => smallesWidth:1032
            editSearch.setTextSize(50);
            earth.getLayoutParams().height = 160;
            moon.getLayoutParams().height = 160;
            mars.getLayoutParams().height = 160;
            earth.getLayoutParams().width = 160;
            moon.getLayoutParams().width = 160;
            mars.getLayoutParams().width = 160;
            earth.requestLayout();
            moon.requestLayout();
            mars.requestLayout();
            categoriesListView.getLayoutParams().width = 350;
        } else if (smallestWidth > 720) {
            editSearch.setTextSize(50);
            earth.getLayoutParams().height = 160;
            moon.getLayoutParams().height = 160;
            mars.getLayoutParams().height = 160;
            earth.getLayoutParams().width = 160;
            moon.getLayoutParams().width = 160;
            mars.getLayoutParams().width = 160;
            earth.requestLayout();
            moon.requestLayout();
            mars.requestLayout();
        } else if (smallestWidth <= 720 && smallestWidth >= 600) {
            editSearch.setTextSize(20);
            earth.getLayoutParams().height = 320;
            moon.getLayoutParams().height = 320;
            mars.getLayoutParams().height = 320;
            earth.getLayoutParams().width = 320;
            moon.getLayoutParams().width = 320;
            mars.getLayoutParams().width = 320;
            earth.requestLayout();
            moon.requestLayout();
            mars.requestLayout();
            categoriesListView.getLayoutParams().width = 450;
            if (rootView.findViewById(R.id.layoutPlanets) != null) {
                LinearLayout layoutPlanets = (LinearLayout) rootView.findViewById(R.id.layoutPlanets);
                LinearLayout.LayoutParams actualParams = (LinearLayout.LayoutParams) layoutPlanets.getLayoutParams();
                actualParams.setMarginStart(0);
                layoutPlanets.setLayoutParams(actualParams);
            }
            if (rootView.findViewById(R.id.searchLayout) != null) {
                LinearLayout searchLayout = (LinearLayout) rootView.findViewById(R.id.searchLayout);
                LinearLayout.LayoutParams actualParams = (LinearLayout.LayoutParams) searchLayout.getLayoutParams();
                actualParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
                searchLayout.setLayoutParams(actualParams);
            }

        } else {
            editSearch.setTextSize(15);
            earth.getLayoutParams().height = 75;
            moon.getLayoutParams().height = 75;
            mars.getLayoutParams().height = 75;
            earth.getLayoutParams().width = 75;
            moon.getLayoutParams().width = 75;
            mars.getLayoutParams().width = 75;
            earth.requestLayout();
            moon.requestLayout();
            mars.requestLayout();
        }
    }

    public class NearbyPlacesTask implements Callable<Void> {
        private String slaveName;
        private Session session;
        private Context context;

        public NearbyPlacesTask(String slaveName, Session session, Context context) {
            this.slaveName = slaveName;
            this.session = session;
            this.context = context;
        }

        @Override
        public Void call() throws Exception {
            try {
                String sentence = "chmod 777 /var/www/html/kml/" + slaveName + ".kml; echo '" +
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\" xmlns:kml=\"http://www.opengis.net/kml/2.2\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n" +
                        "  <Document>\n" +
                        "    <name>historic.kml</name>\n" +
                        "    <ScreenOverlay>\n" +
                        "      <name><![CDATA[<div style=\"text-align: center; font-size: 20px; font-weight: bold; vertical-align: middle;\">Nearby Places</div>]]></name>\n" +
                        "      <description><![CDATA[\n" +
                        "        <html>\n" +
                        "          <body>\n" +
                        "            <table width=\"400\" border=\"0\" cellspacing=\"0\" cellpadding=\"5\" style=\"font-size: 14px;\" border=1 frame=void rules=rows>\n" +
                        "              <tr>\n" +
                        "                <td colspan=\"2\" align=\"center\">\n" +
                        "                  <p><b>Place 1:</b> Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</p>\n" +
                        "                </td>\n" +
                        "              </tr>\n" +
                        "              <tr>\n" +
                        "                <td colspan=\"2\" align=\"center\">\n" +
                        "                  <p><b>Place 2:</b> Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</p>\n" +
                        "                </td>\n" +
                        "              </tr>\n" +
                        "              <tr>\n" +
                        "                <td colspan=\"2\" align=\"center\">\n" +
                        "                  <p><b>Place 3:</b> Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</p>\n" +
                        "                </td>\n" +
                        "              </tr>\n" +
                        "              <tr>\n" +
                        "                <td colspan=\"2\" align=\"center\">\n" +
                        "                  <p><b>Place 4:</b> Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</p>\n" +
                        "                </td>\n" +
                        "              </tr>\n" +
                        "              <tr>\n" +
                        "                <td colspan=\"2\" align=\"center\">\n" +
                        "                  <p><b>Place 5:</b> Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</p>\n" +
                        "                </td>\n" +
                        "              </tr>\n" +
                        "            </table>\n" +
                        "          </body>\n" +
                        "        </html>\n" +
                        "      ]]></description>\n" +
                        "      <overlayXY x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
                        "      <screenXY x=\"1\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
                        "      <rotationXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
                        "      <size x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
                        "    </ScreenOverlay>\n" +
                        "  </Document>\n" +
                        "</kml>\n' > /var/www/html/kml/" + slaveName + ".kml";



                LGUtils.setConnectionWithLiquidGalaxy(session, sentence, context);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void setSearchInLGButton() {

        buttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                GetSessionTask getSessionTask = new GetSessionTask();
                getSessionTask.execute();

                String placeToSearch = editSearch.getText().toString();
                if (!placeToSearch.equals("") && placeToSearch != null) {

                    String command = "echo 'search=" + placeToSearch + "' > /tmp/query.txt";
                    SearchTask searchTask = new SearchTask(command, false);
                    searchTask.execute();

                } else {
                    Toast.makeText(getActivity(), getResources().getString(R.string.please_enter_search), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private String buildSearchCommand(String search) {
        return "echo 'search=" + search + "' > /tmp/query.txt";
    }

    private class GetSessionTask extends AsyncTask<Void, Void, Void> {

        public GetSessionTask() {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (getActivity() != null) {
                session = LGUtils.getSession(getActivity());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void success) {
            super.onPostExecute(success);
        }
    }

    private class SearchTask extends AsyncTask<Void, Void, String> {

        String command;
        boolean isChangingPlanet;
        private ProgressDialog dialog;
        private Handler handler;
        Context taskContext=getContext();


        public SearchTask(String command, boolean isChangingPlanet) {
            this.command = command;
            this.isChangingPlanet = isChangingPlanet;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (dialog == null) {
                dialog = new ProgressDialog(getActivity(), R.style.CustomProgressDialog);
                if (isChangingPlanet) {
                    dialog.setMessage(getResources().getString(R.string.changingPlanet));
                } else {
                    dialog.setMessage(getResources().getString(R.string.searching));
                }
                dialog.setIndeterminate(false);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancel(true);
                    }
                });
                dialog.show();

                handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (dialog != null && dialog.isShowing()) {
                            dialog.dismiss();
                            Toast.makeText(taskContext, taskContext.getResources().getString(R.string.connection_failure), Toast.LENGTH_LONG).show();
                        }
                    }
                }, 10000); // 10 seconds (10000 milliseconds)
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                return LGUtils.setConnectionWithLiquidGalaxy(session, command, getActivity());
            } catch (JSchException e) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String success) {
            super.onPostExecute(success);
            if (success != null) {
                if (dialog != null) {
                    dialog.dismiss();
                    handler.removeCallbacksAndMessages(null);
                }
            } else {
                Toast.makeText(taskContext, taskContext.getResources().getString(R.string.connection_failure), Toast.LENGTH_LONG).show();
            }
        }
    }

}