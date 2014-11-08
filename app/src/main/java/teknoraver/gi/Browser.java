package net.teknoraver.gi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings.RenderPriority;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

public final class Browser extends Activity implements OnClickListener, AdapterView.OnItemSelectedListener {
	private Spinner selector;
	private WebView content;
	private WebSettings settings;
	private LinearLayout toolbar;
	private ImageButton refresh;
	private SharedPreferences sp;
	private String sites[];
	private String oldcachepath;

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.main);

		toolbar = (LinearLayout)findViewById(R.id.toolbar);
		selector = (Spinner)findViewById(R.id.selector);
		content = (WebView)findViewById(R.id.content);
		refresh = (ImageButton)findViewById(R.id.refresh);
		sites = getResources().getStringArray(R.array.urls);
		sites[0] = "file://" + getFilesDir() + "/welcome.html";

		settings = content.getSettings();
		settings.setUserAgentString("Mozilla/5.0 (X11; U; Linux; it-IT) AppleWebKit/528.10 (KHTML, like Gecko) Chrome/7.0 Safari/528.10");
		settings.setBuiltInZoomControls(true);
		settings.setJavaScriptEnabled(true);

		sp = PreferenceManager.getDefaultSharedPreferences(this);

		findViewById(R.id.refresh).setOnClickListener(this);
		findViewById(R.id.share).setOnClickListener(this);
		findViewById(R.id.settings).setOnClickListener(this);

		content.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(final WebView view, final int progress) {
				setProgress(progress * 100);
			}
		});
		content.setWebViewClient(new WebViewClient() {
			@Override
			public void onReceivedError(final WebView view, final int errorCode, final String description, final String failingUrl) {
				Toast.makeText(Browser.this, description, Toast.LENGTH_SHORT).show();
			}
			@Override
			public void onPageFinished(final WebView view, final String url) {
				if(!url.startsWith("file://"))
					toolbar.setVisibility(View.GONE);
				refresh.setImageResource(R.drawable.refresh);
			}
			@Override
			public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
				refresh.setImageResource(R.drawable.stop);
			}
		});

		getFile("welcome.html");
		getFile("gi.png");

		selector.setOnItemSelectedListener(this);

		final Intent intent = getIntent();
		final String action = intent.getAction();

		if (Intent.ACTION_VIEW.equals(action)) {
			System.out.println("intent.getAction() = " + intent.getAction());
			System.out.println("intent.getData() = " + intent.getData());
			content.loadUrl(intent.getData().toString());
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		settings.setRenderPriority(RenderPriority.HIGH);

		settings.setBlockNetworkImage(!sp.getBoolean(Settings.IMG, true));

		if(Integer.parseInt(Build.VERSION.SDK) >= 8)
			wrapper.setPluginState(settings, sp.getBoolean(Settings.FLASH, true));

		if(!new File(Environment.getExternalStorageDirectory(), "Android").exists()) {
			final SharedPreferences.Editor editor = sp.edit();
			editor.putString(Settings.CACHEPATH, "0");
			editor.commit();
		}
		if(oldcachepath != null && !sp.getString(Settings.CACHEPATH, "0").equals(oldcachepath))
			emptyCache(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		if(!sp.getBoolean(Settings.CACHEON, true)) {
			content.stopLoading();
			emptyCache(this);
		}
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if(toolbar.getVisibility() == View.VISIBLE &&
				(keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_BACK)) {
			toolbar.setVisibility(View.GONE);
			return false;
		} else if(keyCode == KeyEvent.KEYCODE_BACK && content.canGoBack()) {
			content.goBack();
			return true;
		} else if(keyCode == KeyEvent.KEYCODE_MENU)
			if(toolbar.getVisibility() == View.GONE)
				toolbar.setVisibility(View.VISIBLE);
			else
				toolbar.setVisibility(View.GONE);
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
		/* when the program starts we always get an onItemSelected() event
		   so let's ignore the first event if we are called from some Intent
		 */
		if(getIntent().getData() != null) {
			getIntent().setData(null);
			return;
		}

		// always show the bar when displaying the intro
		if(position == 0)
			toolbar.setVisibility(View.VISIBLE);
		else
			toolbar.setVisibility(View.GONE);
		content.loadUrl(sites[position]);
	}

	@Override
	public void onClick(final View v) {
		switch(v.getId()) {
		case R.id.refresh:
			if(content.getProgress() < 100) {
				content.stopLoading();
				refresh.setImageResource(R.drawable.refresh);
			} else
				content.reload();
			break;
		case R.id.share:
			startActivity(Intent.createChooser(
					new Intent(Intent.ACTION_SEND)
					.setType("text/plain")
					.putExtra(Intent.EXTRA_SUBJECT, content.getTitle())
					.putExtra(Intent.EXTRA_TEXT, content.getUrl())
				, "Condividi"));
			break;
		case R.id.settings:
			oldcachepath = sp.getString(Settings.CACHEPATH, "0");
			startActivity(new Intent(this, Settings.class));
		}
	}

	@Override
	public void onNothingSelected(final AdapterView<?> parent) {
		content.loadUrl(sites[0]);
	}

	static void emptyCache(final Context c) {
		final boolean internal = PreferenceManager.getDefaultSharedPreferences(c).getString(Settings.CACHEPATH, "0").equals("0");
		final File in = new File(c.getCacheDir(), "webviewCache");
		final File ext = new File(Environment.getExternalStorageDirectory(), "Android/data/" + c.getPackageName());
		System.out.println("[GIORNALI_ITALIANI]: rm -rf " + in);
		System.out.println("[GIORNALI_ITALIANI]: rm -rf " + ext);
		deleteDirectory(in);
		deleteDirectory(ext);
		if(internal)
			in.mkdirs();
		else
			ext.mkdirs();
		if(!internal)
			try {
				System.out.println("[GIORNALI_ITALIANI]: ln -s " + ext.getAbsolutePath() + " " + c.getCacheDir() + "/webviewCache");
				Runtime.getRuntime().exec(new String[]{"ln", "-s", ext.getAbsolutePath(), c.getCacheDir() + "/webviewCache"});
			} catch (final IOException e) {
				Toast.makeText(c, R.string.cacheerr, Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
	}

	private static void deleteDirectory(final File path) {
		if(path.exists())
			for(final File file : path.listFiles())
				if(file.isDirectory())
					deleteDirectory(file);
				else
					file.delete();
		path.delete();
	}

	private void getFile(final String name) {
		final File file = new File(getFilesDir(), name);
		if (!file.exists())
			try {
				getFilesDir().mkdirs();
				final InputStream in = getAssets().open(name);
				final OutputStream out = new FileOutputStream(file);
				final byte[] buf = new byte[65536];
				int len;
				while ((len = in.read(buf)) > 0)
					out.write(buf, 0, len);
				in.close();
				out.close();
			} catch (final IOException ex) {
				ex.printStackTrace();
				finish();
				return;
			}
	}
}
