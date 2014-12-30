package pct.droid.adapters;

import android.app.Activity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import pct.droid.R;
import pct.droid.base.providers.media.types.Media;
import pct.droid.base.utils.PixelUtils;


public class OverviewGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private int mItemWidth, mItemHeight, mMargin, mColumns;
    private ArrayList<Media> mItems;
    private OverviewGridAdapter.OnItemClickListener mItemClickListener;
    final int NORMAL = 0, LOADING = 1;

    public OverviewGridAdapter(Activity activity, ArrayList<Media> items, Integer columns) {
        mColumns = columns;

        int screenWidth = PixelUtils.getScreenWidth(activity);
        mItemWidth = (screenWidth / columns);
        mItemHeight = (int) (1.5 * (double) mItemWidth);
        mMargin = PixelUtils.getPixelsFromDp(activity, 2);

        setItems(items);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        switch (viewType) {
            case LOADING:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_overview_griditem_loading, parent, false);
                return new OverviewGridAdapter.LoadingHolder(v);
            case NORMAL:
            default:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_overview_griditem, parent, false);
                return new OverviewGridAdapter.ViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        int double_margin = mMargin * 2;
        int top_margin = (position < mColumns) ? mMargin * 2 : mMargin;

        GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams) viewHolder.itemView.getLayoutParams();
        if (position % mColumns == 0) {
            layoutParams.setMargins(double_margin, top_margin, mMargin, mMargin);
        } else if (position % mColumns == mColumns - 1) {
            layoutParams.setMargins(mMargin, top_margin, double_margin, mMargin);
        } else {
            layoutParams.setMargins(mMargin, top_margin, mMargin, mMargin);
        }
        viewHolder.itemView.setLayoutParams(layoutParams);

        if (getItemViewType(position) == NORMAL) {
            ViewHolder videoViewHolder = (ViewHolder) viewHolder;
            Media item = getItem(position);
            videoViewHolder.coverImage.setImageResource(android.R.color.transparent);
            if (item.image != null && !item.image.equals("")) {
                Picasso.with(videoViewHolder.coverImage.getContext()).load(item.image)
                        .resize(mItemWidth, mItemHeight)
                        .into(videoViewHolder.coverImage);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (getItem(position).type.equals("loading")) {
            return LOADING;
        }
        return NORMAL;
    }

    public Media getItem(int position) {
        return mItems.get(position);
    }

    public void setOnItemClickListener(OverviewGridAdapter.OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    public void removeLoading() {
        if (getItemCount() <= 0) return;
        Media item = mItems.get(getItemCount() - 1);
        if (item.type.equals("loading")) {
            mItems.remove(getItemCount() - 1);
            notifyDataSetChanged();
        }
    }

    public void addLoading() {
        Media item = null;
        if (getItemCount() != 0) {
            item = mItems.get(getItemCount() - 1);
        }

        if (getItemCount() == 0 || (item != null && !item.type.equals("loading"))) {
            Media loadingItem = new Media();
            loadingItem.type = "loading";
            mItems.add(loadingItem);
            notifyDataSetChanged();
        }
    }

    public ArrayList<Media> getItems() {
        ArrayList<Media> returnData = (ArrayList<Media>) mItems.clone();
        Media item = returnData.get(getItemCount() - 1);
        if (item.type.equals("loading")) {
            returnData.remove(getItemCount() - 1);
        }
        return returnData;
    }

    public void setItems(ArrayList<Media> items) {
        mItems = items;
        notifyDataSetChanged();
    }

    public void clearItems() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        public void onItemClick(View v, Media item, int position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        View itemView;
        @InjectView(R.id.coverImage)
        ImageView coverImage;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            this.itemView = itemView;
            itemView.setOnClickListener(this);
            coverImage.setMinimumHeight(mItemHeight);
        }

        @Override
        public void onClick(View view) {
            if (mItemClickListener != null) {
                int position = getPosition();
                Media item = getItem(position);
                mItemClickListener.onItemClick(view, item, position);
            }
        }

    }

    public class LoadingHolder extends RecyclerView.ViewHolder {

        View itemView;

        public LoadingHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            itemView.setMinimumHeight(mItemHeight);
        }

    }

}
