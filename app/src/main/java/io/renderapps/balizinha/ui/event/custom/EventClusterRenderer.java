package io.renderapps.balizinha.ui.event.custom;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.v4.content.ContextCompat;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.google.maps.android.ui.SquareTextView;

import io.renderapps.balizinha.R;
import io.renderapps.balizinha.model.Event;

public class EventClusterRenderer extends DefaultClusterRenderer<Event> {
    private Context mContext;
    private IconGenerator mIconGenerator;
    private ShapeDrawable mColoredCircleBackground;
    private final float mDensity;

    public EventClusterRenderer(Context context, GoogleMap map, ClusterManager<Event> clusterManager) {
        super(context, map, clusterManager);
        mContext = context;
        mDensity = context.getResources().getDisplayMetrics().density;

        mIconGenerator = new IconGenerator(context);
        mIconGenerator.setContentView(makeSquareTextView(context));
        mIconGenerator.setTextAppearance(R.style.amu_ClusterIcon_TextAppearance);
        mIconGenerator.setBackground(makeClusterBackground());
    }

    @Override
    protected void onBeforeClusterItemRendered(Event event, MarkerOptions markerOptions) {
        markerOptions.title(event.getName());
    }

    @Override
    protected void onBeforeClusterRendered(Cluster<Event> cluster, MarkerOptions markerOptions) {
        // Note: this method runs on the UI thread. Don't spend too much time in here (like in this example).
        mColoredCircleBackground.getPaint().setColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(mIconGenerator.makeIcon(getClusterText(cluster.getSize())));
        markerOptions.icon(descriptor);
    }

    private LayerDrawable makeClusterBackground() {
        mColoredCircleBackground = new ShapeDrawable(new OvalShape());
        ShapeDrawable outline = new ShapeDrawable(new OvalShape());
        outline.getPaint().setColor(0x80ffffff); // Transparent white.
        LayerDrawable background = new LayerDrawable(new Drawable[]{outline, mColoredCircleBackground});
        int strokeWidth = (int) (mDensity * 3);
        background.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth);
        return background;
    }

    private SquareTextView makeSquareTextView(Context context) {
        SquareTextView squareTextView = new SquareTextView(context);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        squareTextView.setLayoutParams(layoutParams);
        squareTextView.setId(R.id.amu_text);
        int twelveDpi = (int) (12 * mDensity);
        squareTextView.setPadding(twelveDpi, twelveDpi, twelveDpi, twelveDpi);
        return squareTextView;
    }
}
