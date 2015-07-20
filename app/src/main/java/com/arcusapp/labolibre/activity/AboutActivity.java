package com.arcusapp.labolibre.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.webkit.WebView;
import android.widget.Toast;

import com.arcusapp.labolibre.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AboutActivity extends PreferenceActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat_Light_DarkActionBar);
        super.onCreate(savedInstanceState);

        // Add the preferences
        addPreferencesFromResource(R.xml.about);

        showOpenSourceLicenses();
        seeOnGitHub();

        try{
            final PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            findPreference("version").setSummary(packageInfo.versionName);
        } catch (Exception ex) {
            Toast.makeText(this, getString(R.string.error_version), Toast.LENGTH_LONG).show();
            findPreference("version").setSummary("?");
        }
    }

    private void showOpenSourceLicenses() {
        final Preference mOpenSourceLicenses = findPreference("open_source");
        mOpenSourceLicenses.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final WebView webView = new WebView(AboutActivity.this);
                String html = readRawTextFile(R.raw.license);
                webView.loadData(html, "text/html; charset=UTF-8", null);
                AlertDialog licenseDialog = new AlertDialog.Builder(AboutActivity.this)
                        .setTitle(R.string.license)
                        .setView(webView)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                licenseDialog.show();
                return true;
            }
        });
    }

    private void seeOnGitHub() {
        final Preference mGitHub = findPreference("github");
        mGitHub.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                Uri uri = Uri.parse("http://github.com/FlyingPumba/LaboLibre");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            }
        });
    }

    private String readRawTextFile(int resId) {
        InputStream inputStream = getResources().openRawResource(resId);

        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;
        StringBuilder text = new StringBuilder();

        try {
            while (( line = buffreader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }
}