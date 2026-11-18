package kotterknife

import android.app.Activity
import android.app.Dialog
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// Minimal in-repo replacement for the unmaintained kotterknife library (was only available
// via jitpack). Views are looked up on every access instead of being cached, so no
// reset/unbind on destroy is needed and KotterKnife.reset is a no-op.

private class BindViewDelegate<T : View>(
    private val finder: (Any) -> View?,
) : ReadWriteProperty<Any, T> {
    @Suppress("UNCHECKED_CAST")
    override fun getValue(
        thisRef: Any,
        property: KProperty<*>,
    ): T =
        (finder(thisRef) ?: error("No view with id found in ${thisRef::class.simpleName} for '${property.name}'")) as T

    override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: T,
    ) = Unit
}

fun <T : View> Activity.bindView(id: Int): ReadWriteProperty<Any, T> =
    BindViewDelegate { (it as Activity).findViewById(id) }

fun <T : View> Fragment.bindView(id: Int): ReadWriteProperty<Any, T> =
    BindViewDelegate { (it as Fragment).view?.findViewById(id) }

// DialogFragment needs its own overload (most specific receiver wins): the dialog's
// decor view is used before the fragment view, matching upstream kotterknife.
fun <T : View> DialogFragment.bindView(id: Int): ReadWriteProperty<Any, T> =
    BindViewDelegate {
        (it as DialogFragment).dialog?.findViewById(id) ?: it.view?.findViewById(id)
    }

fun <T : View> Dialog.bindView(id: Int): ReadWriteProperty<Any, T> =
    BindViewDelegate { (it as Dialog).findViewById(id) }

fun <T : View> View.bindView(id: Int): ReadWriteProperty<Any, T> = BindViewDelegate { (it as View).findViewById(id) }

fun <T : View> RecyclerView.ViewHolder.bindView(id: Int): ReadWriteProperty<Any, T> =
    BindViewDelegate { (it as RecyclerView.ViewHolder).itemView.findViewById(id) }

object KotterKnife {
    fun reset(target: Any) = Unit
}
