package net.teknoraver.gi;

import java.io.File;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

public final class Settings extends PreferenceActivity implements OnPreferenceClickListener {
	public static final String IMG = "img";
	public static final String FLASH = "flashod";
	public static final String CACHEON = "cache";
	public static final String CACHEPATH = "cachepath";
	private ListPreference lt;
	private Preference p;
	private File cachedir;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		lt = (ListPreference)findPreference("cachepath");

		if(lt.getValue().equals("0"))
			cachedir = new File(getCacheDir(), "webviewCache");
		else
			cachedir = new File(Environment.getExternalStorageDirectory(), "Android/data/" + getPackageName());

		p = findPreference("info");
		p.setSummary(getString(R.string.size) + " " + size(cachedir) / 10486 / 100f + " MB");
		p.setEnabled(true);
		p.setOnPreferenceClickListener(this);

		if(!new File(Environment.getExternalStorageDirectory(), "Android").exists()) {
			lt.setEnabled(false);
			lt.setValueIndex(0);
		}

		if(Integer.parseInt(Build.VERSION.SDK) >= 8) {
			CheckBoxPreference fod = (CheckBoxPreference)findPreference("flashod");
			fod.setEnabled(true);
		}
	}

	private static int size(final File path) {
		int size = 0;
		if(path.exists())
			for(final File file : path.listFiles())
				if(file.isDirectory())
					size += size(file);
				else
					size += file.length();
		return size;
	}

	@Override
	public boolean onPreferenceClick(final Preference preference) {
		p.setEnabled(false);
		Browser.emptyCache(this);
		p.setSummary(getString(R.string.size) + " 0 MB");
		return true;
	}
}
