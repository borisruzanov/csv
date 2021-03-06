package com.boris.expert.csvmagic.view.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.boris.expert.csvmagic.R
import com.boris.expert.csvmagic.adapters.FieldListValuesAdapter
import com.boris.expert.csvmagic.model.ListItem
import com.boris.expert.csvmagic.utils.TableGenerator
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import java.util.*
import kotlin.collections.ArrayList

class FieldListValuesActivity : BaseActivity(), FieldListValuesAdapter.OnItemClickListener {

    private lateinit var context: Context
    private lateinit var toolbar: Toolbar
    private lateinit var fieldListValuesRecyclerView: RecyclerView
    private var listValues = mutableListOf<String>()
    private lateinit var tableGenerator: TableGenerator
    private lateinit var adapter: FieldListValuesAdapter
    private var listItem: ListItem? = null
    private var tableName = ""
    private var flag = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_list_values)

        initViews()
        setUpToolbar()
        getListValues()
    }

    private fun initViews() {
        context = this
        if (intent != null && intent.hasExtra("LIST_ITEM")) {
            listItem = intent.getSerializableExtra("LIST_ITEM") as ListItem
        }
        if (intent != null && intent.hasExtra("TABLE_NAME")) {
            tableName = intent.getStringExtra("TABLE_NAME")!!
        }
        if (intent != null && intent.hasExtra("FLAG")) {
            flag = intent.getStringExtra("FLAG")!!
        }
        tableGenerator = TableGenerator(context)
        toolbar = findViewById(R.id.toolbar)
        fieldListValuesRecyclerView = findViewById(R.id.field_list_values_recyclerview)
        fieldListValuesRecyclerView.layoutManager = LinearLayoutManager(context)
        fieldListValuesRecyclerView.hasFixedSize()
        adapter = FieldListValuesAdapter(context, listValues as ArrayList<String>)
        fieldListValuesRecyclerView.adapter = adapter
        adapter.setOnItemClickListener(this)
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = listItem!!.value
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar.setTitleTextColor(ContextCompat.getColor(context, R.color.black))
    }


    private fun getListValues() {
        val tempList = tableGenerator.getFieldListValues(listItem!!.id)
        if (tempList.isNotEmpty()) {
            listValues.clear()
            listValues.addAll(tempList)
            adapter.notifyDataSetChanged()
        } else {
            adapter.notifyDataSetChanged()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            onBackPressed()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onItemClick(position: Int) {

    }

    override fun onAddItemClick(position: Int) {
        addListItemDialog(listItem!!.id)
    }

    override fun onFinishItemClick() {
        if (tableName.isNotEmpty() && flag.isNotEmpty()) {
            val intent = Intent(context, CreateTableActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("TABLE_NAME",tableName)
            startActivity(intent)
            finish()
        } else {
            finish()
        }
    }

    private fun addListItemDialog(id: Int) {
        val listCreateLayout =
            LayoutInflater.from(context).inflate(R.layout.add_list_value_layout, null)
        val dialogHeading = listCreateLayout.findViewById<MaterialTextView>(R.id.dialog_heading)
        dialogHeading.text = getString(R.string.list_value_hint_text)
        val textInputBox =
            listCreateLayout.findViewById<TextInputEditText>(R.id.add_list_value_input_field)
        val listItemCreateBtn =
            listCreateLayout.findViewById<MaterialButton>(R.id.add_list_value_btn)
        val builder = MaterialAlertDialogBuilder(context)
        builder.setView(listCreateLayout)
        val alert = builder.create()
        alert.show()
        listItemCreateBtn.setOnClickListener {
            if (textInputBox.text.toString().isNotEmpty()) {

                tableGenerator.insertListValue(
                    id, textInputBox.text.toString().trim().toLowerCase(
                        Locale.ENGLISH
                    )
                )
                Toast.makeText(
                    context,
                    getString(R.string.list_item_success_text),
                    Toast.LENGTH_SHORT
                )
                    .show()
                alert.dismiss()

                getListValues()
            } else {
                showAlert(context, getString(R.string.list_name_empty_error_text))
            }
        }
    }
}