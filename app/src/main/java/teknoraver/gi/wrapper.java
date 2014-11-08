package net.teknoraver.gi;

import android.annotation.SuppressLint;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;

@SuppressLint("NewApi")
final class wrapper {
	static void setPluginState(final WebSettings settings, final boolean ondemand) {
		settings.setPluginState(ondemand ? PluginState.ON_DEMAND : PluginState.ON);
	}
}
