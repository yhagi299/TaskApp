package jp.techacademy.yasufumi.hagiwara.taskapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity {
    public final static String EXTRA_TASK = "jp.techacademy.yasufumi.hagiwara.taskapp.TASK";

    private Realm mRealm;
    private RealmChangeListener mRealmListener = new RealmChangeListener() {
        @Override
        public void onChange(Object element) {
            reloadListView();
        }
    };

    private ListView mListView;
    private TaskAdapter mTaskAdapter;

    private Spinner mSpinner;
    private List<String> mSpinnerList = Arrays.asList("NN");
    private String spinnerItems[] = {"test", "aaa"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                startActivity(intent);
            }
        });

        //Realm設定
        mRealm = Realm.getDefaultInstance();
        mRealm.addChangeListener(mRealmListener);

        //ListView設定
        mTaskAdapter = new TaskAdapter(MainActivity.this);
        mListView = (ListView) findViewById(R.id.listView1);

        //spinner
        mSpinner = findViewById(R.id.find_spinner);


        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Spinner spinner1 = (Spinner)parent;
                String item = (String)mSpinner.getSelectedItem();
                Log.d("Spinner", item);

                if (item != "すべて"){
                    RealmResults<Task> find_list = mRealm.where(Task.class).equalTo("category",item).findAll();
                    mTaskAdapter.setTaskList(mRealm.copyFromRealm(find_list));

                    mListView.setAdapter(mTaskAdapter);
                } else {
                    RealmResults<Task> all_list = mRealm.where(Task.class).findAllSorted("date", Sort.DESCENDING);
                    mTaskAdapter.setTaskList(mRealm.copyFromRealm(all_list));
                    mListView.setAdapter(mTaskAdapter);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        //ListViewをタップした時の処理
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Task task = (Task) parent.getAdapter().getItem(position);

                Intent intent = new Intent(MainActivity.this, InputActivity.class);
                intent.putExtra(EXTRA_TASK, task.getId());

                startActivity(intent);

            }
        });

        //ListViewを長押しした時の処理
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                final Task task = (Task) parent.getAdapter().getItem(position);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setTitle("削除");
                builder.setMessage(task.getTitle() + "を削除しますか");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RealmResults<Task> results = mRealm.where(Task.class).equalTo("id" ,task.getId()).findAll();

                        mRealm.beginTransaction();
                        results.deleteAllFromRealm();
                        mRealm.commitTransaction();

                        Intent resultIntent = new Intent(getApplicationContext(), TaskAlarmReceiver.class);
                        PendingIntent resultPendinngIntent = PendingIntent.getBroadcast(
                                MainActivity.this,
                                task.getId(),
                                resultIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );

                        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                        alarmManager.cancel(resultPendinngIntent);

                        reloadListView();
                    }
                });
                builder.setNegativeButton("CANCEL", null);

                AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            }
        });

        reloadListView();
    }


    private void reloadListView() {
        //Spinner
        mSpinnerList = new ArrayList<String>();

        RealmResults<Task> allCategory = mRealm.where(Task.class).distinct("category");
        List<Task>  test_list = mRealm.copyFromRealm(allCategory);
        int num = test_list.size();
        String category;

        for (int i =0; i < num+1; i++){
            if(i == num){
                category = "すべて";
            } else {
                category = test_list.get(i).getCategory();
            }
            mSpinnerList.add(category);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mSpinnerList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);


        //listView
        RealmResults<Task> taskRealmResults = mRealm.where(Task.class).findAllSorted("date", Sort.DESCENDING);

        mTaskAdapter.setTaskList(mRealm.copyFromRealm(taskRealmResults));

        mListView.setAdapter(mTaskAdapter);
        mTaskAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mRealm.close();
    }
}
