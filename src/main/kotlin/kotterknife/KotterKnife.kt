package kotterknife

import android.app.Activity
import android.app.Dialog
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// Minimal in-repo replacement for the unmaintained kotterknife library (was only available
// via jitpack). Views are looked up on every access instead of being cached, so no
// reset/unbind on destroy is needed and KotterKnife.reset is a no-op.

private class BindViewDelegate<T : View>(
    private val id: Int,
    private val root: (Any) -> View?,
) : ReadWriteProperty<Any, T> {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(
        thisRef: Any,
        property: KProperty<*>,
    ): T {
        val view = root(thisRef)?.findViewById<T>(id)

        return view ?: error("No view with id $id found in ${thisRef::class.simpleName} for '${property.name}'")
    }

    override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: T,
    ) = Unit
}

fun <T : View> Activity.bindView(id: Int): ReadWriteProperty<Any, T> =
    BindViewDelegate(id) { (it as Activity).findViewById(id) }

fun <T : View> Fragment.bindView(id: Int): ReadWriteProperty<Any, T> = BindViewDelegate(id) { (it as Fragment).view }

fun <T : View> Dialog.bindView(id: Int): ReadWriteProperty<Any, T> =
    BindViewDelegate(id) { (it as Dialog).window?.decorView }

fun <T : View> View.bindView(id: Int): ReadWriteProperty<Any, T> = BindViewDelegate(id) { it as View }

fun <T : View> RecyclerView.ViewHolder.bindView(id: Int): ReadWriteProperty<Any, T> =
    BindViewDelegate(id) { (it as RecyclerView.ViewHolder).itemView }

object KotterKnife {
    fun reset(target: Any) = Unit
}
