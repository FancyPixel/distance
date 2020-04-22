package it.fancypixel.distance.components

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.MenuRes
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import it.fancypixel.distance.R
import kotlinx.android.synthetic.main.bottom_sheet_menu.view.*
import kotlinx.android.synthetic.main.bottom_sheet_menu_item.view.*

/**
 * [BottomSheetDialogFragment] that uses a custom
 * theme which sets a rounded background to the dialog
 * and doesn't dim the navigation bar
 */
open class BottomSheetMenu<T>(context: Context) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    private val items: ArrayList<MenuItem<T>> = ArrayList()
    private var selectedRes: T? = null
    private var callback: ((selectedValue: T) -> Unit)? = null

    fun selectResource(res: T): BottomSheetMenu<T> {
        selectedRes = res
        return this
    }

    fun addItem(title: String, value: T): BottomSheetMenu<T> {
        items.add(MenuItem(title, value))
        return this
    }

    fun addOnSelectItemListener(callback: (selectedValue: T) -> Unit): BottomSheetMenu<T> {
        this.callback = callback
        return this
    }

    override fun show() {
        val view = View.inflate(context, R.layout.bottom_sheet_menu, null)
        for (item in items) {
            val itemView = View.inflate(context, R.layout.bottom_sheet_menu_item, null)
            itemView.label.text = item.title
            itemView.isSelected = item.value == selectedRes
            itemView.setOnClickListener {
                callback?.invoke(item.value)
                this.dismiss()
            }
            view.menu.addView(itemView)
        }
        setContentView(view)
        super.show()
    }

    class MenuItem<T>(val title: String, val value: T)

}