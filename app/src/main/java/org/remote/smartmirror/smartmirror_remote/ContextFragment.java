package org.remote.smartmirror.smartmirror_remote;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class ContextFragment extends Fragment {

    private static final String MENU_NAME = "menu name";
    public static final String MAIN_MENU = "main menu";
    private String menuName;

    public ContextFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a ContextFragment that will display a layout based on the menuName passed in.
     *
     * @param menuName the name of the menu to display
     * @return A new instance of fragment ContextFragment.
     */

    public static ContextFragment newInstance(String menuName) {
        ContextFragment fragment = new ContextFragment();
        Bundle args = new Bundle();
        args.putString(MENU_NAME, menuName);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            menuName = getArguments().getString(MENU_NAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Use MENU_NAME passed with on the bundle to decide which layout to show
        int layoutId = 0;
        switch (menuName) {
            case MAIN_MENU:
                layoutId = R.layout.menu_controls;
                break;
        }

        return inflater.inflate(layoutId, container, false);
    }
}
