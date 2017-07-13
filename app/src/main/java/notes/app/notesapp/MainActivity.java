package notes.app.notesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorRes;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationRequest;

import java.util.ArrayList;

import notes.app.notesapp.checklists.ChecklistActivity;
import notes.app.notesapp.helpers.DBHelper;
import notes.app.notesapp.helpers.ItemTouchHelperClass;
import notes.app.notesapp.helpers.SimpleDividerItemDecoration;
import notes.app.notesapp.notes.NoteActivity;
import notes.app.notesapp.notes.NotesListAdapter;
import notes.app.notesapp.pojo.Note;
import notes.app.notesapp.pojo.Utils;

/*
Created by Kashif on 3/9/2017.
Application starts here, notes loaded from db and displayed here
 */

public class MainActivity extends AppCompatActivity implements Adapter2Home {

    private final String TAG = getClass().getSimpleName();
    RelativeLayout rl_rootLayout;
    RecyclerView rv_notesList;
    TextView tv_empty;
    SharedPreferences sharedPreferences;
    String themeColor;
    DBHelper dbHelper;
    ArrayList<Note> notesList = new ArrayList<>();
    Note justDeletedNote; //for undo purpose
    int justDeletedNotePosition;
    NotesListAdapter adapter;
    boolean other_fabs_visible;
    SearchView.OnQueryTextListener listener;
    LocationRequest mLocationRequest;
    Toolbar toolbar;
    int[] drawableres = {R.drawable.ic_action_cloud_up, R.drawable.ic_action_search, R.drawable.ic_action_back, R.drawable.ic_action_revert
            , R.drawable.ic_action_share, R.drawable.ic_action_delete, R.drawable.ic_action_add};

    private void drawableRes(int i, @ColorRes int color) {
        String title = getResources().getString(R.string.app_name);
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), drawableres[i], null);
        drawable = DrawableCompat.wrap(drawable);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            DrawableCompat.setTint(drawable, getColor(color));
            SpannableString s = new SpannableString(title);
            s.setSpan(new ForegroundColorSpan(getResources().getColor(color, getTheme())), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            getSupportActionBar().setTitle(s);

        }
        getSupportActionBar().setHomeAsUpIndicator(drawable);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        themeColor = sharedPreferences.getString("theme_list_check", null);
        Utils.onActivityCreateSetTheme(this);
        setContentView(R.layout.activity_main);

        for (int i = 0; i < drawableres.length; i++) {
            if (Integer.parseInt(themeColor) == 4 || Integer.parseInt(themeColor) == 5 || Integer.parseInt(themeColor) == 2 || Integer.parseInt(themeColor) == 3 || Integer.parseInt(themeColor) == 7) {
                Log.e("DRawavle value", String.valueOf(drawableres[i]));
                drawableRes(i, R.color.light_icon);

            } else {
                drawableRes(i, R.color.dark_icon);

            }
        }


        rl_rootLayout = (RelativeLayout) findViewById(R.id.activity_main);
        rv_notesList = (RecyclerView) findViewById(R.id.recyclerView);
        rv_notesList.addItemDecoration(new SimpleDividerItemDecoration(this));
        rv_notesList.setHasFixedSize(true);
        rv_notesList.setLayoutManager(new LinearLayoutManager(this));
        tv_empty = (TextView) findViewById(R.id.tvEmpty);

        //search listener from action bar
        listener = new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit query=" + query);
                searchNotes(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "onQueryTextChange query=" + newText);
                searchNotes(newText);
                return false;
            }
        };

        dbHelper = new DBHelper(this);

    }

    @Override
    public void onResume() {
        SharedPreferences themePref = PreferenceManager.getDefaultSharedPreferences(this);
        String prefList = themePref.getString("theme_list_check", null);

        if (themeColor != null && prefList != null && !themeColor.equals(prefList)) {
            Log.d("THEME Selected", prefList);
            Utils.changeTotheme(this, Integer.parseInt(prefList));
        }

        super.onResume();
        clearRecycler();
        //get notes saved in sqlite db
        notesList.addAll(dbHelper.getNotes());
        showNotes();
    }

    //clears notes list
    void clearRecycler() {

        if (notesList != null && !notesList.isEmpty() && adapter != null) {
            Log.d(TAG, "Clearing recyler");
            notesList.clear();
            adapter.notifyDataSetChanged();
        }

    }

    void showNotes() {

        if (notesList != null && !notesList.isEmpty()) {
            Log.d(TAG, "notesList!=null");
            tv_empty.setVisibility(View.GONE);

            adapter = new NotesListAdapter(this, notesList, this);
            rv_notesList.setAdapter(adapter);

            ItemTouchHelper.Callback callback = new ItemTouchHelperClass(adapter);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
            itemTouchHelper.attachToRecyclerView(rv_notesList);

        } else {
            Log.d(TAG, "empty");
            tv_empty.setVisibility(View.VISIBLE);
        }

    }

    void searchNotes(String query) {

        try {

            if (!query.equals("")) {
                clearRecycler();
                notesList = dbHelper.searchNotes(query);
                showNotes();
            } else {
                clearRecycler();
                //get notes saved in sqlite db
                notesList.addAll(dbHelper.getNotes());
                showNotes();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void addClick(View v) {
        if (!other_fabs_visible) {
            findViewById(R.id.other_fabs).setVisibility(View.VISIBLE);
            other_fabs_visible = true;
        } else {
            findViewById(R.id.other_fabs).setVisibility(View.GONE);
            other_fabs_visible = false;
        }
    }

    public void addNewNote(View v) {
        Intent intent = new Intent(this, NoteActivity.class);
        startActivity(intent);
    }

    public void addNewChecklist(View v) {
        Intent intent = new Intent(this, ChecklistActivity.class);
        startActivity(intent);
    }


    //it's called when a notes from list is clicked
    @Override
    public void adapterActionPerformed(Bundle args) {

        Log.d(TAG, "adapterActionPerformed args=" + args);

        String action = args.getString("action");
        int noteId = args.getInt("noteId");

        if (action != null && action.equals(notes.app.notesapp.RowAction.CLICK.toString())) {

            int noteType = args.getInt("noteType");
            Intent intent = new Intent(this, NoteActivity.class);

            if (noteType == 0) {
                intent.putExtra("data", true);
                intent.putExtra("noteId", noteId);
            } else {
                intent = new Intent(this, ChecklistActivity.class);
                intent.putExtra("data", true);
                intent.putExtra("noteId", noteId);
            }

            startActivity(intent);

        } else if (action.equals(notes.app.notesapp.RowAction.DELETE.toString())) {

            justDeletedNote = dbHelper.getNote(noteId);
            justDeletedNotePosition = args.getInt("position");
            dbHelper.deleteNote(noteId);

            notesList.remove(justDeletedNotePosition);
            adapter.notifyItemRemoved(justDeletedNotePosition);

            Snackbar.make(rl_rootLayout, "Note deleted", Snackbar.LENGTH_LONG).setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dbHelper.insertNote(justDeletedNote);
                    notesList.add(justDeletedNotePosition, justDeletedNote);
                    adapter.notifyItemInserted(justDeletedNotePosition);
                }
            }).show();

        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setOnQueryTextListener(listener);
        MenuItem themeItem = menu.findItem(R.id.settingsMenu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_cloud) {
            Toast.makeText(this, "Not implemented yet", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.settingsMenu) {
            Intent settingintent = new Intent(this, Settings.class);
            startActivity(settingintent);
        }

        return super.onOptionsItemSelected(item);
    }


}
