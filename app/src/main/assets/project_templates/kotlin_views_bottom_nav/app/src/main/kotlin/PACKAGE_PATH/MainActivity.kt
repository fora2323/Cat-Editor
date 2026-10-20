package __PACKAGE_NAME__

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val textDashboard = findViewById<TextView>(R.id.text_dashboard)
        val navView = findViewById<BottomNavigationView>(R.id.nav_view)

        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    toolbar.title = getString(R.string.title_home)
                    textDashboard.text = "Welcome to Home"
                    true
                }
                R.id.navigation_dashboard -> {
                    toolbar.title = getString(R.string.title_dashboard)
                    textDashboard.text = "Dashboard Section"
                    true
                }
                R.id.navigation_notifications -> {
                    toolbar.title = getString(R.string.title_notifications)
                    textDashboard.text = "Notifications Section"
                    true
                }
                else -> false
            }
        }
    }
}