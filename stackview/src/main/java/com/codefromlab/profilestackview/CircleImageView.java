package com.codefromlab.profilestackview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;


public class CircleImageView extends ImageView {

    private static final ScaleType SCALE_TYPE = ScaleType.CENTER_CROP;

    private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.ARGB_8888;
    private static final int COLORDRAWABLE_DIMENSION = 2;

    private static final int DEFAULT_IMAGE_PADDING = 0;
    private static final int DEFAULT_BORDER_WIDTH = 0;
    private static final Boolean DEFAULT_ICON_BOTTOM = false;
    private static final int DEFAULT_BORDER_COLOR = Color.WHITE;

    private final RectF mDrawableRect = new RectF();
    private final RectF mBorderRect = new RectF();

    private final Matrix mShaderMatrix = new Matrix();
    private final Paint mBitmapPaint = new Paint();
    private final Paint mBorderPaint = new Paint();

    private int mBorderColor = DEFAULT_BORDER_COLOR;
    private int mBorderWidth = DEFAULT_BORDER_WIDTH;

    private Bitmap mBitmap;
    private BitmapShader mBitmapShader;
    private int mBitmapWidth;
    private int mBitmapHeight;

    private float mDrawableRadius;
    private float mBorderRadius;

    private ColorFilter mColorFilter;

    private boolean mReady;
    private boolean mSetupPending;
    private Drawable icon;
    private int imagePadding;

    public CircleImageView(Context context) {
        super(context);

        init();
    }

    public CircleImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView, defStyle, 0);

        mBorderWidth = a.getDimensionPixelSize(R.styleable.CircleImageView_border_width, DEFAULT_BORDER_WIDTH);
        mBorderColor = a.getColor(R.styleable.CircleImageView_border_color, DEFAULT_BORDER_COLOR);
        icon = a.getDrawable(R.styleable.CircleImageView_image_icon);
        icon_bottom = a.getBoolean(R.styleable.CircleImageView_icon_bottom, DEFAULT_ICON_BOTTOM);
        imagePadding = a.getDimensionPixelSize(R.styleable.CircleImageView_image_padding, DEFAULT_IMAGE_PADDING);
        a.recycle();

        init();
    }

    private void init() {
        super.setScaleType(SCALE_TYPE);
        mReady = true;

        if (mSetupPending) {
            setup();
            mSetupPending = false;
        }
    }

    @Override
    public ScaleType getScaleType() {
        return SCALE_TYPE;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        if (scaleType != SCALE_TYPE) {
            throw new IllegalArgumentException(String.format("ScaleType %s not supported.", scaleType));
        }
    }

    @Override
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        if (adjustViewBounds) {
            throw new IllegalArgumentException("adjustViewBounds not supported.");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.i("press", "" + event.getAction());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                paintBackground.setColor(Color.TRANSPARENT);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                paintBackground.setColor(Color.TRANSPARENT);
                invalidate();
                break;

        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.i("seen", "called");
        if (getDrawable() == null) {
            return;
        }

        paintBackground.setStyle(Paint.Style.FILL);


        float r = 0;
        if (isIcon) {
            r = iconRadius / 2;
        }

        if (imagePadding > 0) {
            // mBorderPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, mBorderRadius - r, paintBackground);
            //  mBorderPaint.setStyle(Paint.Style.STROKE);
        }

        canvas.drawCircle(getWidth() / 2, getHeight() / 2, mDrawableRadius - r, paintBackground);

        canvas.drawCircle(getWidth() / 2, getHeight() / 2, mDrawableRadius - r, mBitmapPaint);
        if (mBorderWidth != 0) {
            mBorderPaint.setStrokeWidth(mBorderWidth);
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, mBorderRadius - r, mBorderPaint);
        }


        if (isIcon) {
            paintBackground.setColor(Color.WHITE);
            canvas.drawCircle(xfactor, yfactor, (iconRadius), paintBackground);
            canvas.drawCircle(xfactor, yfactor, iconRadius, paintIcon);
            mBorderPaint.setStrokeWidth(mBorderWidth / 2);
            canvas.drawCircle(xfactor, yfactor, (iconRadius), mBorderPaint);
        }

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setup();
    }

    public int getBorderColor() {
        return mBorderColor;
    }

    public void setBorderColor(int borderColor) {
        if (borderColor == mBorderColor) {
            return;
        }

        mBorderColor = borderColor;
        mBorderPaint.setColor(mBorderColor);
        invalidate();
    }

    public int getBorderWidth() {
        return mBorderWidth;
    }

    public void setBorderWidth(int borderWidth) {
        if (borderWidth == mBorderWidth) {
            return;
        }

        mBorderWidth = borderWidth;
        setup();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        mBitmap = bm;
        setup();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        mBitmap = getBitmapFromDrawable(drawable);
        setup();
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        mBitmap = getBitmapFromDrawable(getDrawable());

        setup();
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        mBitmap = getBitmapFromDrawable(getDrawable());
        setup();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (cf == mColorFilter) {
            return;
        }

        mColorFilter = cf;
        mBitmapPaint.setColorFilter(mColorFilter);
        invalidate();
    }

    public static Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        try {
            Bitmap bitmap;

            if (drawable instanceof ColorDrawable) {
                bitmap = Bitmap.createBitmap(COLORDRAWABLE_DIMENSION, COLORDRAWABLE_DIMENSION, BITMAP_CONFIG);
            } else {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), BITMAP_CONFIG);
            }

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    private void setup() {
        if (!mReady) {
            mSetupPending = true;
            return;
        }

        if (mBitmap == null) {
            return;
        }

        mBitmapShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paintBackground.setAntiAlias(true);
        paintBackground.setColor(Color.TRANSPARENT);

        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setShader(mBitmapShader);

        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setColor(mBorderColor);
        mBorderPaint.setStrokeWidth(mBorderWidth);
        mBitmapHeight = mBitmap.getHeight();
        mBitmapWidth = mBitmap.getWidth();

        mBorderRect.set(0, 0, getWidth(), getHeight());
        mBorderRadius = Math.min((mBorderRect.height() - mBorderWidth) / 2, (mBorderRect.width() - mBorderWidth) / 2);

        mDrawableRect.set(mBorderWidth + imagePadding, mBorderWidth + imagePadding, mBorderRect.width() - mBorderWidth - imagePadding, mBorderRect.height() - mBorderWidth - imagePadding);
        mDrawableRadius = Math.min(mDrawableRect.height() / 2, mDrawableRect.width() / 2);


        updateShaderMatrix();
        if (icon != null) {
            setIcon(getBitmapFromDrawable(icon));
        }
        invalidate();
    }

    private void updateShaderMatrix() {
        float scale;
        float dx = 0;
        float dy = 0;

        mShaderMatrix.set(null);

        if (mBitmapWidth * mDrawableRect.height() > mDrawableRect.width() * mBitmapHeight) {
            scale = mDrawableRect.height() / (float) mBitmapHeight;
            dx = (mDrawableRect.width() - mBitmapWidth * scale) * 0.5f;
        } else {
            scale = mDrawableRect.width() / (float) mBitmapWidth;
            dy = (mDrawableRect.height() - mBitmapHeight * scale) * 0.5f;
        }
        Log.i("testing", "::" + scale + ":" + mBitmapHeight + ": " + mBitmapWidth + "radius:" + mDrawableRadius);
        Log.i("testing2", "::" + dx + ":" + dy);
        mShaderMatrix.setScale(scale, scale);
        mShaderMatrix.postTranslate((int) (dx + 0.5f) + mBorderWidth + imagePadding, (int) (dy + 0.5f) + mBorderWidth + imagePadding);

        mBitmapShader.setLocalMatrix(mShaderMatrix);


    }

    private BitmapShader mBitmapIconShader;
    private float iconRadius;
    Paint paintIcon = new Paint();
    Matrix mShaderIconMatrix = new Matrix();
    RectF mDrawableRectIcon = new RectF();
    int mBitmapWidthIcon;
    int mBitmapHeightIcon;
    public boolean isIcon = false;
    float xfactor;
    float yfactor;
    float iconPadding;
    private Boolean icon_bottom;
    Paint paintBackground = new Paint();

    public void setIconFromResource(Drawable resid) {
        icon = resid;
        if (icon != null)
            setIcon(getBitmapFromDrawable(icon));
    }

    void setIcon(String url) {

    }

    private void setIcon(Bitmap bitmapIcon) {
        xfactor = getWidth() - mDrawableRadius / 3;
        if (icon_bottom) {
            yfactor = getHeight() - mDrawableRadius / 3;//(float)(mDrawableRadius*(1-(1/Math.sqrt(2))));
        } else {
            yfactor = mDrawableRadius / 3;
        }
        iconRadius = mDrawableRadius / 4;
        iconPadding = iconRadius / 4;

        paintIcon.setAntiAlias(true);
        paintIcon.setShader(mBitmapIconShader);

        mBitmapHeightIcon = bitmapIcon.getHeight();
        mBitmapWidthIcon = bitmapIcon.getWidth();

        isIcon = true;

        mBitmapIconShader = new BitmapShader(bitmapIcon, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        mDrawableRectIcon.set(xfactor - (iconRadius) + getBorderWidth() / 2 + iconPadding, yfactor - iconRadius + getBorderWidth() / 2 + iconPadding, xfactor + iconRadius - getBorderWidth() / 2 - iconPadding, yfactor + iconRadius - +getBorderWidth() / 2 - iconPadding);
        updateIconMatrix();

        paintIcon.setShader(mBitmapIconShader);
        invalidate();
    }

    private void updateIconMatrix() {
        float scale;
        float dx = 0;
        float dy = 0;

        mShaderIconMatrix.set(null);
        if (mBitmapWidthIcon * mDrawableRectIcon.height() > mDrawableRectIcon.width() * mBitmapHeightIcon) {
            scale = mDrawableRectIcon.height() / (float) mBitmapHeightIcon;
            dx = (mDrawableRectIcon.width() - mBitmapWidthIcon * scale) * 0.5f;
        } else {
            scale = mDrawableRectIcon.width() / (float) mBitmapWidthIcon;
            dy = (mDrawableRectIcon.height() - mBitmapHeightIcon * scale) * 0.5f;
        }

        mShaderIconMatrix.setScale(scale, scale);
        mShaderIconMatrix.postTranslate((int) (dx + 0.5f) + xfactor - (iconRadius) + getBorderWidth() / 2 + iconPadding, (int) (dy + 0.5f) + yfactor - iconRadius + getBorderWidth() / 2 + iconPadding);

        mBitmapIconShader.setLocalMatrix(mShaderIconMatrix);
    }

}