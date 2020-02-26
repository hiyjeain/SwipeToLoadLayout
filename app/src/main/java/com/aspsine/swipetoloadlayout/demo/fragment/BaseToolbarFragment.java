package com.aspsine.swipetoloadlayout.demo.fragment;


import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.aspsine.swipetoloadlayout.demo.R;

/**
 * Created by aspsine on 15/9/8.
 */
public class BaseToolbarFragment extends BaseFragment {

    public interface ToggleDrawerCallBack {
        void openDrawer();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
            final ActionBar actionbar = getSupportActionBar();
            actionbar.setHomeAsUpIndicator(R.mipmap.ic_menu);
            actionbar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                ((ToggleDrawerCallBack) getActivity()).openDrawer();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public ActionBar getSupportActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    protected void setTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }

}
