package net.teknoraver.gi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Proxy extends Activity {
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();

		startActivity(
			new Intent(this, Browser.class)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				.setAction(intent.getAction())
				.setData(intent.getData()));
	}
}
