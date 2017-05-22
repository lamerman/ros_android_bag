package org.lamerman.rosandroidbag;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((RadioGroup)findViewById(R.id.radioGroupTopics)).setOnCheckedChangeListener(this);

        SharedPreferences preferences =
                getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        ((EditText)findViewById(R.id.editTextArguments))
                .setText(preferences.getString(Common.KEY_ARGUMENTS, ""));

        ((EditText)findViewById(R.id.editTextEnvVariables))
                .setText(preferences.getString(Common.KEY_ENVIRONMENT_VARIABLES, ""));

        ((EditText)findViewById(R.id.editTextMasterUrl))
                .setText(preferences.getString(Common.KEY_MASTER_URL, "http://localhost:11311"));

        ((EditText)findViewById(R.id.editTextTopics))
                .setText(preferences.getString(Common.KEY_TOPICS_TO_RECORD, ""));

        boolean recordAllTopics = preferences.getBoolean(Common.KEY_RECORD_ALL_TOPICS, true);

        if (recordAllTopics) {
            ((RadioButton) findViewById(R.id.radioButtonAllTopics)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.radioButtonSelectedTopics)).setChecked(true);
        }

        if (Common.isServiceRunning(RecordService.class, this)) {
            bindRecordService();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_logs, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_help:
                Intent intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void onStartClicked(View view) {
        final String userArguments = ((EditText)findViewById(R.id.editTextArguments)).getText().toString();
        final String environmentVariables = ((EditText)findViewById(R.id.editTextEnvVariables)).getText().toString();
        final String masterUrl = ((EditText)findViewById(R.id.editTextMasterUrl)).getText().toString();

        boolean recordAllTopics = ((RadioButton) findViewById(R.id.radioButtonAllTopics)).isChecked();

        Intent intent = new Intent(getBaseContext(), RecordService.class);

        SharedPreferences.Editor preferences =
                getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE).edit();

        String allArguments;
        if (recordAllTopics) {
            allArguments = "--all " + userArguments;
        } else {
            String commaSeparatedTopics = ((EditText)findViewById(R.id.editTextTopics)).getText().toString();

            String topics = commaSeparatedTopics.replace(',', ' ');

            allArguments = topics + " " + userArguments;

            preferences.putString(Common.KEY_TOPICS_TO_RECORD, commaSeparatedTopics);
        }

        intent
            .putExtra(Common.KEY_ARGUMENTS, allArguments)
            .putExtra(Common.KEY_ENVIRONMENT_VARIABLES, environmentVariables)
            .putExtra(Common.KEY_MASTER_URL, masterUrl);

        preferences
            .putString(Common.KEY_ARGUMENTS, userArguments)
            .putString(Common.KEY_ENVIRONMENT_VARIABLES, environmentVariables)
            .putString(Common.KEY_MASTER_URL, masterUrl)
            .putBoolean(Common.KEY_RECORD_ALL_TOPICS, recordAllTopics)
            .commit();

        startService(intent);
        bindRecordService();
    }

    public void onStopClicked(View view) {
        stopService(new Intent(getBaseContext(), RecordService.class));
    }

    public void onShowOutputClicked(View view) {
        Intent intent = new Intent(this, LogsActivity.class);
        startActivity(intent);
    }

    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        switch (checkedId) {
            case R.id.radioButtonAllTopics:
                findViewById(R.id.editTextTopics).setEnabled(false);
                break;
            case R.id.radioButtonSelectedTopics:
                findViewById(R.id.editTextTopics).setEnabled(true);
                break;
        }
    }

    private void updateButtonsVisibility() {
        Button buttonStart = (Button)findViewById(R.id.buttonStart);
        Button buttonStop = (Button)findViewById(R.id.buttonStop);

        boolean serviceRunning = Common.isServiceRunning(RecordService.class, this);
        if (serviceRunning) {
            buttonStart.setVisibility(View.GONE);
            buttonStop.setVisibility(View.VISIBLE);
        } else {
            buttonStart.setVisibility(View.VISIBLE);
            buttonStop.setVisibility(View.GONE);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            updateButtonsVisibility();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            updateButtonsVisibility();
        }
    };

    private void bindRecordService() {
        Intent intent = new Intent(getBaseContext(), RecordService.class);
        bindService(intent, mConnection, 0);
    }

}
