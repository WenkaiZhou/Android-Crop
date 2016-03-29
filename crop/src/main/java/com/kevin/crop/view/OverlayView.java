package com.kevin.crop.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import com.kevin.crop.R;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 * <p/>
 * This view is used for drawing the overlay on top of the image. It may have frame, crop guidelines and dimmed area.
 * This must have LAYER_TYPE_SOFTWARE to draw itself properly.
 */
public class OverlayView extends View {
    // 默认显示裁剪边框
    public static final boolean DEFAULT_SHOW_CROP_FRAME = true;
    // 默认显示裁剪网格
    public static final boolean DEFAULT_SHOW_CROP_GRID = true;
    // 默认周围阴影非椭圆(即矩形)
    public static final boolean DEFAULT_OVAL_DIMMED_LAYER = false;
    // 默认网格的行数
    public static final int DEFAULT_CROP_GRID_ROW_COUNT = 2;
    // 默认网格的列数
    public static final int DEFAULT_CROP_GRID_COLUMN_COUNT = 2;
    // 裁剪区域
    private final RectF mCropViewRect = new RectF();

    // 网格的行数
    private int mCropGridRowCount;
    // 网格的列数
    private int mCropGridColumnCount;
    // 裁剪目标区域宽高比
    private float mTargetAspectRatio;
    private float[] mGridPoints = null;
    // 是否显示裁剪边框
    private boolean mShowCropFrame;
    // 是否显示裁剪网格
    private boolean mShowCropGrid;
    // 周围阴影是否为椭圆(如果false则为矩形)
    private boolean mOvalDimmedLayer;
    // 周围阴影颜色
    private int mDimmedColor;
    private Path mCircularPath = new Path();
    // 周围阴影画笔
    private Paint mDimmedStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // 裁剪网格画笔
    private Paint mCropGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // 裁剪边框画笔
    private Paint mCropFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    // 当前控件的宽度
    protected int mThisWidth;
    // 当前控件的高度
    protected int mThisHeight;

    public OverlayView(Context context) {
        this(context, null);
    }

    public OverlayView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * Setter for {@link #mOvalDimmedLayer} variable.
     *
     * @param ovalDimmedLayer - set it to true if you want dimmed layer to be an oval
     */
    public void setOvalDimmedLayer(boolean ovalDimmedLayer) {
        mOvalDimmedLayer = ovalDimmedLayer;
    }

    /**
     * Setter for crop grid rows count.
     * Resets {@link #mGridPoints} variable because it is not valid anymore.
     */
    public void setCropGridRowCount(@IntRange(from = 0) int cropGridRowCount) {
        mCropGridRowCount = cropGridRowCount;
        mGridPoints = null;
    }

    /**
     * Setter for crop grid columns count.
     * Resets {@link #mGridPoints} variable because it is not valid anymore.
     */
    public void setCropGridColumnCount(@IntRange(from = 0) int cropGridColumnCount) {
        mCropGridColumnCount = cropGridColumnCount;
        mGridPoints = null;
    }

    /**
     * Setter for {@link #mShowCropFrame} variable.
     *
     * @param showCropFrame - set to true if you want to see a crop frame rectangle on top of an image
     */
    public void setShowCropFrame(boolean showCropFrame) {
        mShowCropFrame = showCropFrame;
    }

    /**
     * Setter for {@link #mShowCropGrid} variable.
     *
     * @param showCropGrid - set to true if you want to see a crop grid on top of an image
     */
    public void setShowCropGrid(boolean showCropGrid) {
        mShowCropGrid = showCropGrid;
    }

    /**
     * Setter for {@link #mDimmedColor} variable.
     *
     * @param dimmedColor - desired color of dimmed area around the crop bounds
     */
    public void setDimmedColor(@ColorInt int dimmedColor) {
        mDimmedColor = dimmedColor;
    }

    /**
     * Setter for crop frame stroke width
     */
    public void setCropFrameStrokeWidth(@IntRange(from = 0) int width) {
        mCropFramePaint.setStrokeWidth(width);
    }

    /**
     * Setter for crop grid stroke width
     */
    public void setCropGridStrokeWidth(@IntRange(from = 0) int width) {
        mCropGridPaint.setStrokeWidth(width);
    }

    /**
     * Setter for crop frame color
     */
    public void setCropFrameColor(@ColorInt int color) {
        mCropFramePaint.setColor(color);
    }

    /**
     * Setter for crop grid color
     */
    public void setCropGridColor(@ColorInt int color) {
        mCropGridPaint.setColor(color);
    }

    /**
     * This method sets aspect ratio for crop bounds.
     * 设置裁剪目标的宽高比
     *
     * @param targetAspectRatio - aspect ratio for image crop (e.g. 1.77(7) for 16:9)
     */
    public void setTargetAspectRatio(float targetAspectRatio) {
        mTargetAspectRatio = targetAspectRatio;
        setupCropBounds();
    }

    /**
     * This method setups crop bounds rectangles for given aspect ratio and view size.
     * {@link #mCropViewRect} is used to draw crop bounds - uses padding.
     */
    public void setupCropBounds() {
        int height = (int) (mThisWidth / mTargetAspectRatio);
        if (height > mThisHeight) {
            int width = (int) (mThisHeight * mTargetAspectRatio);
            int halfDiff = (mThisWidth - width) / 2;
            mCropViewRect.set(getPaddingLeft() + halfDiff, getPaddingTop(),
                    getPaddingLeft() + width + halfDiff, getPaddingTop() + mThisHeight);
        } else {
            int halfDiff = (mThisHeight - height) / 2;
            mCropViewRect.set(getPaddingLeft(), getPaddingTop() + halfDiff,
                    getPaddingLeft() + mThisWidth, getPaddingTop() + height + halfDiff);
        }

        mGridPoints = null;
        mCircularPath.reset();
        mCircularPath.addOval(mCropViewRect, Path.Direction.CW);
    }

    protected void init() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            left = getPaddingLeft();
            top = getPaddingTop();
            right = getWidth() - getPaddingRight();
            bottom = getHeight() - getPaddingBottom();
            mThisWidth = right - left;
            mThisHeight = bottom - top;
            setupCropBounds();
        }
    }

    /**
     * Along with image there are dimmed layer, crop bounds and crop guidelines that must be drawn.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawDimmedLayer(canvas);
        drawCropGrid(canvas);
    }

    /**
     * This method draws dimmed area around the crop bounds.
     * 绘制外部阴影部分
     *
     * @param canvas - valid canvas object
     */
    protected void drawDimmedLayer(@NonNull Canvas canvas) {
        canvas.save();
        if (mOvalDimmedLayer) {
            canvas.clipPath(mCircularPath, Region.Op.DIFFERENCE);
        } else {
            canvas.clipRect(mCropViewRect, Region.Op.DIFFERENCE);
        }
        canvas.drawColor(mDimmedColor);
        canvas.restore();

        if (mOvalDimmedLayer) { // Draw 1px stroke to fix antialias
            canvas.drawOval(mCropViewRect, mDimmedStrokePaint);
        }
    }

    /**
     * This method draws crop bounds (empty rectangle)
     * and crop guidelines (vertical and horizontal lines inside the crop bounds) if needed.
     * 绘制裁剪网格
     *
     * @param canvas - valid canvas object
     */
    protected void drawCropGrid(@NonNull Canvas canvas) {
        if (mShowCropGrid) {
            if (mGridPoints == null && !mCropViewRect.isEmpty()) {

                mGridPoints = new float[(mCropGridRowCount) * 4 + (mCropGridColumnCount) * 4];

                int index = 0;
                for (int i = 0; i < mCropGridRowCount; i++) {
                    mGridPoints[index++] = mCropViewRect.left;
                    mGridPoints[index++] = (mCropViewRect.height() * (((float) i + 1.0f) / (float) (mCropGridRowCount + 1))) + mCropViewRect.top;
                    mGridPoints[index++] = mCropViewRect.right;
                    mGridPoints[index++] = (mCropViewRect.height() * (((float) i + 1.0f) / (float) (mCropGridRowCount + 1))) + mCropViewRect.top;
                }

                for (int i = 0; i < mCropGridColumnCount; i++) {
                    mGridPoints[index++] = (mCropViewRect.width() * (((float) i + 1.0f) / (float) (mCropGridColumnCount + 1))) + mCropViewRect.left;
                    mGridPoints[index++] = mCropViewRect.top;
                    mGridPoints[index++] = (mCropViewRect.width() * (((float) i + 1.0f) / (float) (mCropGridColumnCount + 1))) + mCropViewRect.left;
                    mGridPoints[index++] = mCropViewRect.bottom;
                }
            }

            if (mGridPoints != null) {
                canvas.drawLines(mGridPoints, mCropGridPaint);
            }
        }

        if (mShowCropFrame) {
            canvas.drawRect(mCropViewRect, mCropFramePaint);
        }
    }

    /**
     * This method extracts all needed values from the styled attributes.
     * Those are used to configure the view.
     * 设置裁剪参数
     */
    @SuppressWarnings("deprecation")
    protected void processStyledAttributes(@NonNull TypedArray a) {
        mOvalDimmedLayer = a.getBoolean(R.styleable.ucrop_UCropView_ucrop_oval_dimmed_layer, DEFAULT_OVAL_DIMMED_LAYER);
        mDimmedColor = a.getColor(R.styleable.ucrop_UCropView_ucrop_dimmed_color,
                getResources().getColor(R.color.ucrop_color_default_dimmed));
        mDimmedStrokePaint.setColor(mDimmedColor);
        mDimmedStrokePaint.setStyle(Paint.Style.STROKE);
        mDimmedStrokePaint.setStrokeWidth(1);

        initCropFrameStyle(a);
        mShowCropFrame = a.getBoolean(R.styleable.ucrop_UCropView_ucrop_show_frame, DEFAULT_SHOW_CROP_FRAME);

        initCropGridStyle(a);
        mShowCropGrid = a.getBoolean(R.styleable.ucrop_UCropView_ucrop_show_grid, DEFAULT_SHOW_CROP_GRID);
    }

    /**
     * This method setups Paint object for the crop bounds.
     * 设置裁剪边框画笔参数
     */
    @SuppressWarnings("deprecation")
    private void initCropFrameStyle(@NonNull TypedArray a) {
        int cropFrameStrokeSize = a.getDimensionPixelSize(R.styleable.ucrop_UCropView_ucrop_frame_stroke_size,
                getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_frame_stoke_width));
        int cropFrameColor = a.getColor(R.styleable.ucrop_UCropView_ucrop_frame_color,
                getResources().getColor(R.color.ucrop_color_default_crop_frame));
        mCropFramePaint.setStrokeWidth(cropFrameStrokeSize);
        mCropFramePaint.setColor(cropFrameColor);
        mCropFramePaint.setStyle(Paint.Style.STROKE);
    }

    /**
     * This method setups Paint object for the crop guidelines.
     * 设置裁剪网格画笔参数
     */
    @SuppressWarnings("deprecation")
    private void initCropGridStyle(@NonNull TypedArray a) {
        int cropGridStrokeSize = a.getDimensionPixelSize(R.styleable.ucrop_UCropView_ucrop_grid_stroke_size,
                getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_grid_stoke_width));
        int cropGridColor = a.getColor(R.styleable.ucrop_UCropView_ucrop_grid_color,
                getResources().getColor(R.color.ucrop_color_default_crop_grid));
        mCropGridPaint.setStrokeWidth(cropGridStrokeSize);
        mCropGridPaint.setColor(cropGridColor);

        mCropGridRowCount = a.getInt(R.styleable.ucrop_UCropView_ucrop_grid_row_count, DEFAULT_CROP_GRID_ROW_COUNT);
        mCropGridColumnCount = a.getInt(R.styleable.ucrop_UCropView_ucrop_grid_column_count, DEFAULT_CROP_GRID_COLUMN_COUNT);
    }

}
