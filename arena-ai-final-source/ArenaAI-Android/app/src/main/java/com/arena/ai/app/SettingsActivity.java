package com.arena.ai.app;

import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_title);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            SwitchPreferenceCompat darkMode = findPreference("pref_dark_mode");
            if (darkMode != null) {
                darkMode.setOnPreferenceChangeListener((preference, newValue) -> {
                    requireActivity().recreate();
                    return true;
                });
            }

            Preference clearCache = findPreference("pref_clear_cache");
            if (clearCache != null) {
                clearCache.setOnPreferenceClickListener(preference -> {
                    new Thread(() -> {
                        CookieManager.getInstance().removeAllCookies(null);
                        WebStorage.getInstance().deleteAllData();
                    }).start();
                    return true;
                });
            }

            Preference aboutPref = findPreference("pref_about");
            if (aboutPref != null) {
                aboutPref.setSummary("Arena Android App v1.0.0");
            }
        }
    }
}
