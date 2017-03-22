package com.eltonviana.vehicle_data_collector.activities;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import com.eltonviana.vehicle_data_collector.exceptions.BluetoothNotAvailableException;
import com.eltonviana.vehicle_data_collector.exceptions.NoBondedDevicesException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    // Data Sync
    public static final String DATA_SYNC_SWITCH = "data_sync_switch";
    public static final String DATA_SYNC_POST_URL = "post_url";
    public static final String DATA_SYNC_VEHICLE_ID = "vehicle_id";
    public static final String DATA_SYNC_FREQUENCY = "sync_frequency";
    // Bluetooth
    public static final String BLUETOOTH_SWITCH = "bluetooth_switch";
    public static final String BLUETOOTH_DEVICES = "bluetooth_devices";
    // GPS
    public static final String GPS_SWITCH = "gps_switch";
    // OBD
    public static final String OBD_PROTOCOL = "obd_protocol";
    public static final String ENGINE_DISPLACEMENT = "engine_displacement";

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || BluetoothPreferenceFragment.class.getName().equals(fragmentName)
                || GPSPreferenceFragment.class.getName().equals(fragmentName)
                || OBDPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(DATA_SYNC_POST_URL));
            bindPreferenceSummaryToValue(findPreference(DATA_SYNC_VEHICLE_ID));
            bindPreferenceSummaryToValue(findPreference(DATA_SYNC_FREQUENCY));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows bluetooth preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class BluetoothPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_bluetooth);
            setHasOptionsMenu(true);

            SwitchPreference btSwitch = (SwitchPreference) findPreference(BLUETOOTH_SWITCH);

            btSwitch.setChecked(false);

            try {
                fillBluetoothDevicesList((ListPreference) findPreference(BLUETOOTH_DEVICES));
            } catch (BluetoothNotAvailableException e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "This device does not support Bluetooth.",
                        Toast.LENGTH_LONG).show();
            } catch (NoBondedDevicesException e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "This device doesn't have any bonded device.",
                        Toast.LENGTH_LONG).show();
            }

            bindPreferenceSummaryToValue(findPreference(BLUETOOTH_DEVICES));
        }

        private void fillBluetoothDevicesList(ListPreference bluetoothDevicesList) throws BluetoothNotAvailableException, NoBondedDevicesException {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBluetoothAdapter == null) {
                throw new BluetoothNotAvailableException();
            }

            if (mBluetoothAdapter.getBondedDevices().isEmpty()) {
                throw new NoBondedDevicesException();
            }

            ArrayList<CharSequence> pairedDevicesStrings = new ArrayList<>();
            ArrayList<CharSequence> pairedDevicesAddress = new ArrayList<>();
            for (BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
                pairedDevicesStrings.add(device.getName() + "\n" + device.getAddress());
                pairedDevicesAddress.add(device.getAddress());
            }

            bluetoothDevicesList.setEntries(pairedDevicesStrings.toArray(new CharSequence[0]));
            bluetoothDevicesList.setEntryValues(pairedDevicesAddress.toArray(new CharSequence[0]));
        }

        private void turnOnBluetooth() throws BluetoothNotAvailableException {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBluetoothAdapter == null) {
                throw new BluetoothNotAvailableException();
            }

            mBluetoothAdapter.enable();
        }

        private void turnOffBluetooth() throws BluetoothNotAvailableException {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBluetoothAdapter == null) {
                throw new BluetoothNotAvailableException();
            }

            mBluetoothAdapter.disable();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows GPS preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GPSPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_gps);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows OBD preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class OBDPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_obd);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference(OBD_PROTOCOL));
            bindPreferenceSummaryToValue(findPreference(ENGINE_DISPLACEMENT));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}