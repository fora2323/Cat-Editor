package __PACKAGE_NAME__;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        MaterialButton button = findViewById(R.id.button_first);
        button.setOnClickListener(v -> {
            Toast.makeText(this, "Hello from __PROJECT_NAME__!", Toast.LENGTH_SHORT).show();
        });
    }
}