package org.sil.storyproducer.controller.keyterm

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.model.Phase
import org.sil.storyproducer.model.PhaseType
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.Workspace.termToKeyterm

class KeyTermListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_term_list)

        val keytermList = termToKeyterm.keys.toTypedArray()
        keytermList.sortWith(String.CASE_INSENSITIVE_ORDER)

        viewManager = LinearLayoutManager(this)
        viewAdapter = MyAdapter(keytermList, this)

        recyclerView = findViewById<RecyclerView>(R.id.keyterm_list).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        setSupportActionBar(findViewById(R.id.toolbar3))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Keyterm List"
    }

    override fun onPause() {
        super.onPause()
        if(intent.hasExtra("phase")) {
            Workspace.activePhase = Phase(intent.getSerializableExtra("phase") as PhaseType)
        }
    }
}

class MyAdapter(private val myDataset: Array<String>, private val context: Context) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    class MyViewHolder(val item: View) : RecyclerView.ViewHolder(item)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter.MyViewHolder {

        val rootView = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)

        return MyViewHolder(rootView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        holder.item.findViewById<TextView>(android.R.id.text1).text = myDataset[position]
        holder.item.setOnClickListener {
            Workspace.activeKeyterm = Workspace.termToKeyterm[myDataset[position]]!!
            val intent = Intent(context , KeyTermActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = myDataset.size
}


