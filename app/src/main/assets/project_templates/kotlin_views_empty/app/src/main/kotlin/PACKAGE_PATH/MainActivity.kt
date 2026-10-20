package __PACKAGE_NAME__

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val button = findViewById<MaterialButton>(R.id.button_first)
        button.setOnClickListener {
            Toast.makeText(this, "Hello from __PROJECT_NAME__!", Toast.LENGTH_SHORT).show()
        }
    }
}