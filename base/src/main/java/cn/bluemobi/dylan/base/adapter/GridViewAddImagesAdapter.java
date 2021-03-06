package cn.bluemobi.dylan.base.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;

import java.io.File;
import java.util.List;

import cn.bluemobi.dylan.base.R;


/**
 * com.bm.falvzixun.adapter.GridViewAddImgAdpter
 *
 * @author yuandl on 2015/12/24.
 *         添加上传图片适配器
 */
public class GridViewAddImagesAdapter extends BaseAdapter {
    private List<String> paths;
    private Context context;
    private LayoutInflater inflater;
    private int itemWidth;

    private int addImageResourceId= R.drawable.image_add;
    /**
     * 可以动态设置最多上传几张，之后就不显示+号了，用户也无法上传了
     * 默认9张
     */
    private int maxImages = 9;

    public GridViewAddImagesAdapter(List<String> paths, Context context, int numColumns, int spacing) {
        this.paths = paths;
        this.context = context;
        int displayWidth = context.getResources().getDisplayMetrics().widthPixels;
         itemWidth = (displayWidth - spacing * (numColumns + 1)) / numColumns;
        inflater = LayoutInflater.from(context);
    }

    /**
     * 设置默认的加号图片
     * @param addImageResourceId
     * @return
     */
    public GridViewAddImagesAdapter setAddImageResourceId(int addImageResourceId) {
        this.addImageResourceId = addImageResourceId;
        return this;
    }
    /**
     * 获取最大上传张数
     *
     * @return
     */
    public int getMaxImages() {
        return maxImages;
    }

    /**
     * 设置最大上传张数
     *
     * @param maxImages
     */
    public GridViewAddImagesAdapter setMaxImages(int maxImages) {
        this.maxImages = maxImages;
        return this;
    }

    /**
     * 让GridView中的数据数目加1最后一个显示+号
     * 当到达最大张数时不再显示+号
     *
     * @return 返回GridView中的数量
     */
    @Override
    public int getCount() {
        int count = paths == null ? 1 : paths.size() + 1;
        if (count > maxImages) {
            return paths.size();
        } else {
            return count;
        }
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public void notifyDataSetChanged(List<String> paths) {
        this.paths = paths;
        this.notifyDataSetChanged();

    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {


        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.pub_item_grid_add_image, parent, false);
            convertView.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.MATCH_PARENT, itemWidth));
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        /**代表+号之前的需要正常显示图片**/
        if (paths != null && position < paths.size()) {

            final File file = new File(paths.get(position));
            Glide.with(context)
                    .load(paths.get(position).startsWith("http")?paths.get(position):file)
                    .priority(Priority.HIGH)
                    .into(viewHolder.ivimage);
            viewHolder.btdel.setVisibility(View.VISIBLE);
            viewHolder.btdel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (file.exists()) {
                        file.delete();
                    }
                    paths.remove(position);
                    notifyDataSetChanged();
                }
            });
        } else {
            /**代表+号的需要+号图片显示图片**/
            Glide.with(context)
                    .load(addImageResourceId)
                    .priority(Priority.HIGH)
                    .into(viewHolder.ivimage);
            viewHolder.ivimage.setScaleType(ImageView.ScaleType.FIT_XY);
            viewHolder.btdel.setVisibility(View.GONE);
        }

        return convertView;

    }

    public class ViewHolder {
        public final ImageView ivimage;
        public final Button btdel;
        public final View root;

        public ViewHolder(View root) {
            ivimage = (ImageView) root.findViewById(R.id.iv_image);
            btdel = (Button) root.findViewById(R.id.bt_del);
            this.root = root;
        }
    }
}
