package fr.bbodin.courbedepoids;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowInsetsController;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;

public class SplashActivity extends Activity {
    private static final String PREFS = "settings";
    private static final String INTRO_SHOWN = "intro_shown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(INTRO_SHOWN, false)) {
            openMain(false);
            return;
        }

        configureSystemBars();
        setContentView(R.layout.activity_splash);

        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putBoolean(INTRO_SHOWN, true)
                    .apply();
            openMain();
        });
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void configureSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(20, 123, 239));
        window.setNavigationBarColor(Color.WHITE);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility()
                            | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }
    }
}
