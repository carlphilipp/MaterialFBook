package me.zeeroooo.materialfb.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import butterknife.ButterKnife

abstract class ButterKnifeActivity(private val contentView: Int) : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!this.isFinishing) {
            setContentView(contentView)
            ButterKnife.bind(this)
            create(savedInstanceState)
        }
    }

    abstract fun create(savedInstanceState: Bundle?)
}