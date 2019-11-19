package ua.makovskyi.overlapanimationlayout

import android.os.Bundle
import android.view.View
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.l1).setOnClickListener {
            toast = Toast.makeText(this, "Layout 1 clicked", Toast.LENGTH_SHORT).apply {
                show()
            }
        }
        findViewById<View>(R.id.l2).setOnClickListener {
            toast = Toast.makeText(this, "Layout 2 clicked", Toast.LENGTH_SHORT).apply {
                show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        toast?.cancel()
    }
}
