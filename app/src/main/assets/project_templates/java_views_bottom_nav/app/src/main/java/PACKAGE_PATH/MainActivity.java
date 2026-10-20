package __PACKAGE_NAME__;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView textDashboard = findViewById(R.id.text_dashboard);
        BottomNavigationView navView = findViewById(R.id.nav_view);

        navView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                toolbar.setTitle(R.string.title_home);
                textDashboard.setText("Welcome to Home");
                return true;
            } else if (itemId == R.id.navigation_dashboard) {
                toolbar.setTitle(R.string.title_dashboard);
                textDashboard.setText("Dashboard Section");
                return true;
            } else if (itemId == R.id.navigation_notifications) {
                toolbar.setTitle(R.string.title_notifications);
                textDashboard.setText("Notifications Section");
                return true;
            }
            return false;
        });
    }
}