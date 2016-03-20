package org.remote.smartmirror.smartmirror_remote;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class ContextFragment extends Fragment {

    private static final String MENU_NAME = "menu name";
    public static final String ARTICLE_CONTROLS ="article controls";
    public static final String CAMERA_CONTROLS = "camera controls";
    public static final String DEFAULT_CONTROLS = "default controls";
    public static final String GMAIL_CONTROLS = "gmail_controls";
    public static final String NEWS_CONTROLS = "news controls";
    public static final String SETTINGS_CONTROLS = "settings controls";

    private String menuName;

    public ContextFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a ContextFragment that will display a group of buttons based
     * on the menuName supplied during creation.
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
            case ARTICLE_CONTROLS:
                layoutId = R.layout.article_controls;
                break;
            case CAMERA_CONTROLS:
                layoutId = R.layout.camera_controls;
                break;
            case DEFAULT_CONTROLS:
                layoutId = R.layout.default_controls;
                break;
            case GMAIL_CONTROLS:
                layoutId = R.layout.gmail_controls;
                break;
            case NEWS_CONTROLS:
                layoutId = R.layout.news_controls;
                break;
            case SETTINGS_CONTROLS:
                layoutId = R.layout.settings_controls;
                break;
            default:
                layoutId = R.layout.default_controls;
                break;
        }

        return inflater.inflate(layoutId, container, false);
    }
}
