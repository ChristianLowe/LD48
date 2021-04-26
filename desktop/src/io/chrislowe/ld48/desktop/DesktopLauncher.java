package io.chrislowe.ld48.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import io.chrislowe.ld48.Ld48;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "LD48";
		config.width = 1024;
		config.height = 768;
		new LwjglApplication(new Ld48(), config);
	}
}
