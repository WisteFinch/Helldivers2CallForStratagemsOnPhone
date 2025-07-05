package indie.wistefinch.callforstratagems

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import indie.wistefinch.callforstratagems.utils.Utils

class MainActivity : AppCompatActivity() {

    /**
     * Main navigation controller.
     */
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Setup immersive mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

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
            editor.putString("sid", Utils.getRandomString(16))
            editor.apply()
        }
    }
}