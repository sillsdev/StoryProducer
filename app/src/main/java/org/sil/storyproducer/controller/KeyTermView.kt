package org.sil.storyproducer.controller

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.sil.storyproducer.R
import org.sil.storyproducer.model.KeyTerm
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.tools.file.stringToSpannableString


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [KeyTermView.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [KeyTermView.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class KeyTermView : Fragment() {

    private var term: String = ""
    private var termForms: MutableList<String> = ArrayList()
    private var alternateRenderings: String = ""
    private var explanation: String = ""
    private var relatedTerms: MutableList<String> = ArrayList()
    //private var listener: OnFragmentInteractionListener? = null

    companion object {

        fun newInstance(): KeyTermView {
            return KeyTermView()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val item = this.arguments!!.getParcelable<KeyTerm>("KeyTerm")
        term = item.term
        termForms = item.termForms
        alternateRenderings = item.alternateRenderings
        explanation = item.explanation
        relatedTerms = item.relatedTerms
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_key_term_view, container, false)
        view.findViewById<TextView>(R.id.term_text).text = term
        view.findViewById<TextView>(R.id.termForms_text).text = termForms.toString()
        view.findViewById<TextView>(R.id.alternateRenderings_text).text = alternateRenderings
        view.findViewById<TextView>(R.id.explanation_text).text = explanation

        val relatedTermsView = view.findViewById<TextView>(R.id.relatedTerms_text)
        relatedTermsView.text = stringToSpannableString(relatedTerms)
        relatedTermsView.movementMethod = LinkMovementMethod.getInstance()

        return view
    }

    // TODO: Rename method, update argument and hook method into UI event
//    fun onButtonPressed(uri: Uri) {
//        listener?.onFragmentInteraction(uri)
//    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        if (context is OnFragmentInteractionListener) {
//            listener = context
//        } else {
//            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
//        }
    }

    override fun onDetach() {
        super.onDetach()
        //listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
//    interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
//        fun onFragmentInteraction(uri: Uri)
//    }
}
