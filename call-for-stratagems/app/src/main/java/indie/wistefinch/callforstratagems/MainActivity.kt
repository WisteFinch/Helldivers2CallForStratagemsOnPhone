package indie.wistefinch.callforstratagems

import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {

    /**
     * Main navigation controller.
     */
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set window attributes
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window?.attributes?.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        setContentView(R.layout.activity_main)

        // Setup nav
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup action bar
        setSupportActionBar(findViewById(R.id.toolbar))
        val barConfig = AppBarConfiguration(navController.graph)

        findViewById<Toolbar>(R.id.toolbar).setupWithNavController(navController, barConfig)

        // Setup sid
        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val sid = preferences.getString("sid", "0")!!
        val editor = preferences.edit()
        if (sid == "0") {
            editor.putString("sid", Util.getRandomString(16))
            editor.apply()
        }
    }
}