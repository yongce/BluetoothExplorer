package me.ycdev.android.bluetooth.explorer

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import me.ycdev.android.bluetooth.ble.R
import me.ycdev.android.bluetooth.ble.R.id
import me.ycdev.android.bluetooth.ble.R.layout
import me.ycdev.android.bluetooth.ble.R.string

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_main)
        val toolbar = findViewById<Toolbar>(id.toolbar)
        setSupportActionBar(toolbar)

        val fab = findViewById<FloatingActionButton>(id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        val drawer = findViewById<DrawerLayout>(id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar,
            string.navigation_drawer_open,
            string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        findViewById<View>(id.ble_advertiser).setOnClickListener {
            val intent = Intent(this, AdvertiserActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(id.ble_scanner).setOnClickListener {
            val intent = Intent(this, ScannerActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(id.paired_devices).setOnClickListener {
            val intent = Intent(this, PairedDevicesActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_settings) {
            Toast.makeText(
                this, application.getString(string.action_settings),
                Toast.LENGTH_SHORT
            ).show()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            id.nav_camera -> {
                // Handle the camera action
            }
            id.nav_gallery -> {
            }
            id.nav_slideshow -> {
            }
            id.nav_manage -> {
            }
            id.nav_share -> {
            }
            id.nav_send -> {
            }
        }

        val drawer = findViewById<DrawerLayout>(id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }
}
