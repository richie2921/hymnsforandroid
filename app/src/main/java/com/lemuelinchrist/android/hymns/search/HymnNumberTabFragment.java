package com.lemuelinchrist.android.hymns.search;

import android.text.InputType;
import android.util.Log;
import com.lemuelinchrist.android.hymns.R;
import com.lemuelinchrist.android.hymns.dao.HymnsDao;
import com.lemuelinchrist.android.hymns.search.searchadapters.FirstLineChorusAdapter;
import com.lemuelinchrist.android.hymns.search.searchadapters.HymnNumberAdapter;


public class HymnNumberTabFragment extends TabFragment {
    // TODO: Rename parameter arguments, choose names that match

    private HymnsDao dao;


    @Override
    public void setRecyclerViewAdapter() {

        dao = new HymnsDao(container.getContext());
        dao.open();

        Log.d(this.getClass().getName(), "selectedHymnGroup=" + selectedHymnGroup);
        if (selectedHymnGroup != null) {
            mRecyclerView.setAdapter(new HymnNumberAdapter(container.getContext(),
                    dao.getIndexListOrderBy(selectedHymnGroup,null,HymnsDao.ORDER_BY_HYMN_NUMBER), R.layout.recyclerview_hymn_list));
        }

    }

    public void setSearchFilter(String filter) {

        mRecyclerView.setAdapter(new HymnNumberAdapter(container.getContext(),
                dao.getFilteredHymns(selectedHymnGroup, filter)
                , R.layout.recyclerview_hymn_list));


    }

    public void cleanUp() {
        dao.close();
    }

    @Override
    public boolean canBeSearched() {
        return true;
    }

    @Override
    public int getSearchTabIndex() {
        return 0;
    }

    @Override
    public String getTabName() {
        return "Hymn Number";
    }

    @Override
    public int getInputType() {
        return InputType.TYPE_CLASS_PHONE;
    }
}