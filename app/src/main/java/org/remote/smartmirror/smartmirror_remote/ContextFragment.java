package org.remote.smartmirror.smartmirror_remote;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class ContextFragment extends Fragment {

    private static final String MENU_ID = "menu name";

    private int menuId;

    public ContextFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a ContextFragment that will display a group of buttons based
     * on the menuName supplied during creation.
     *
     * @param layoutId layout resource to show
     * @return A new instance of fragment ContextFragment.
     */

    public static ContextFragment NewInstance(int layoutId) {
        ContextFragment fragment = new ContextFragment();
        Bundle args = new Bundle();
        args.putInt(MENU_ID, layoutId);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        menuId = getArguments().getInt(MENU_ID);

        return inflater.inflate(menuId, container, false);
    }
}
