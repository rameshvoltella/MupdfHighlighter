package com.artifex.mupdfdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Iterator;



interface TextProcessor {
    void onStartLine();

    void onWord(TextWord word);

    void onEndLine();
}

public abstract class PageView extends ViewGroup {
    private static final float ITEM_SELECT_BOX_WIDTH = 4.0f;
    private static final int HIGHLIGHT_COLOR = 0x80ade1f6;
    private int LINK_COLOR;
    private static final int BOX_COLOR = -9868951;
    private int INK_COLOR;
    private float INK_THICKNESS;
    private float current_scale;
    private static final int BACKGROUND_COLOR = -1;
    private static final int PROGRESS_DIALOG_DELAY = 200;
    protected final Context mContext;
    protected int mPageNumber;
    private Point mParentSize;
    protected Point mSize;
    protected float mSourceScale;
    private ImageView mEntire;
    private Bitmap mEntireBm;
    private Matrix mEntireMat;
    private AsyncTask<Void, Void, TextWord[][]> mGetText;
    private AsyncTask<Void, Void, LinkInfo[]> mGetLinkInfo;
    private CancellableAsyncTask<Void, Void> mDrawEntire;
    private Point mPatchViewSize;
    private Rect mPatchArea;
    private ImageView mPatch;
    private Bitmap mPatchBm;
    private CancellableAsyncTask<Void, Void> mDrawPatch;
    private RectF[] mSearchBoxes;
    protected LinkInfo[] mLinks;
    public RectF mSelectBox;
    public RectF selectedText;
    private TextWord[][] mText;
    private RectF mItemSelectBox;
    protected ArrayList<ArrayList<PointF>> mDrawing;
    private View mSearchView;
    private boolean mIsBlank;
    private boolean mHighlightLinks;
    private ProgressBar mBusyIndicator;
    private final Handler mHandler;
    int once=-1;
    private PdfTextSelectionHelper textSelectionHelper = new PdfTextSelectionHelper();

    public PageView(final Context c, final Point parentSize, final Bitmap sharedHqBm) {
        super(c);
        this.LINK_COLOR = -2130749662;
        this.INK_COLOR = -16777216;
        this.INK_THICKNESS = 10.0f;
        this.mHandler = new Handler();
        this.mContext = c;
        this.mParentSize = parentSize;
        this.setBackgroundColor(-1);
        this.mEntireBm = Bitmap.createBitmap(parentSize.x, parentSize.y, Bitmap.Config.ARGB_8888);
        this.mPatchBm = sharedHqBm;
        this.mEntireMat = new Matrix();
    }

    protected abstract CancellableTaskDefinition<Void, Void> getDrawPageTask(final Bitmap p0, final int p1, final int p2, final int p3, final int p4, final int p5, final int p6);

    protected abstract CancellableTaskDefinition<Void, Void> getUpdatePageTask(final Bitmap p0, final int p1, final int p2, final int p3, final int p4, final int p5, final int p6);

    protected abstract LinkInfo[] getLinkInfo();

    protected abstract TextWord[][] getText();

    protected abstract void addMarkup(final PointF[] p0, final Annotation.Type p1 , int color);

    private void reinit() {
        if (this.mDrawEntire != null) {
            this.mDrawEntire.cancelAndWait();
            this.mDrawEntire = null;
        }
        if (this.mDrawPatch != null) {
            this.mDrawPatch.cancelAndWait();
            this.mDrawPatch = null;
        }
        if (this.mGetLinkInfo != null) {
            this.mGetLinkInfo.cancel(true);
            this.mGetLinkInfo = null;
        }
        if (this.mGetText != null) {
            this.mGetText.cancel(true);
            this.mGetText = null;
        }
        this.mIsBlank = true;
        this.mPageNumber = 0;
        if (this.mSize == null) {
            this.mSize = this.mParentSize;
        }
        if (this.mEntire != null) {
            this.mEntire.setImageBitmap((Bitmap) null);
            this.mEntire.invalidate();
            Log.d("INVALIDATEunda","1111");
        }
        if (this.mPatch != null) {
            this.mPatch.setImageBitmap((Bitmap) null);
            this.mPatch.invalidate();
            Log.d("INVALIDATEunda","22222");

        }
        this.mPatchViewSize = null;
        this.mPatchArea = null;
        this.mSearchBoxes = null;
        this.mLinks = null;
        this.mSelectBox = null;
        this.mText = null;
        this.mItemSelectBox = null;

        Log.d("nullified","yes1");
    }

    public void releaseResources() {
        this.reinit();
        if (this.mBusyIndicator != null) {
            this.removeView((View) this.mBusyIndicator);
            this.mBusyIndicator = null;
        }
    }

    public void releaseBitmaps() {
        this.reinit();
        if (this.mEntireBm != null) {
            this.mEntireBm.recycle();
        }
        this.mEntireBm = null;
        if (this.mPatchBm != null) {
            this.mPatchBm.recycle();
        }
        this.mPatchBm = null;
    }

    public void blank(final int page) {
        this.reinit();
        this.mPageNumber = page;
        if (this.mBusyIndicator == null) {
            (this.mBusyIndicator = new ProgressBar(this.mContext)).setIndeterminate(true);
            this.addView((View) this.mBusyIndicator);
        }
        this.setBackgroundColor(-1);
    }

    /* TODO: PageView::  this methods basically use to highlight pdf */
    public void setPage(final int page, final PointF size) {
        if (this.mDrawEntire != null) {
            this.mDrawEntire.cancelAndWait();
            this.mDrawEntire = null;
        }
        this.mIsBlank = false;
        if (this.mSearchView != null) {
            this.mSearchView.invalidate();
            Log.d("INVALIDATEunda","4444");

        }
        this.mPageNumber = page;
        if (this.mEntire == null) {
            (this.mEntire = (ImageView) new OpaqueImageView(this.mContext)).setScaleType(ImageView.ScaleType.MATRIX);
            this.addView((View) this.mEntire);
        }
        this.mSourceScale = Math.min(this.mParentSize.x / size.x, this.mParentSize.y / size.y);
        this.mSize = new Point((int) (size.x * this.mSourceScale), (int) (size.y * this.mSourceScale));
        this.mEntire.setImageBitmap((Bitmap) null);
        this.mEntire.invalidate();
        Log.d("INVALIDATEunda","5555");

        (this.mGetLinkInfo = new AsyncTask<Void, Void, LinkInfo[]>() {
            protected LinkInfo[] doInBackground(final Void... v) {
                return PageView.this.getLinkInfo();
            }

            protected void onPostExecute(final LinkInfo[] v) {
                PageView.this.mLinks = v;
                if (PageView.this.mSearchView != null) {
                    PageView.this.mSearchView.invalidate();
                    Log.d("INVALIDATEunda","31");

                }
            }
        }).execute(new Void[0]);
        (this.mDrawEntire = new CancellableAsyncTask<Void, Void>(this.getDrawPageTask(this.mEntireBm, this.mSize.x, this.mSize.y, 0, 0, this.mSize.x, this.mSize.y)) {
            @Override
            public void onPreExecute() {
                PageView.this.setBackgroundColor(-1);
                PageView.this.mEntire.setImageBitmap((Bitmap) null);
                PageView.this.mEntire.invalidate();
                Log.d("INVALIDATEunda","30");

                if (PageView.this.mBusyIndicator == null) {
                    PageView.this.mBusyIndicator = new ProgressBar(PageView.this.mContext);
                    PageView.this.mBusyIndicator.setIndeterminate(true);
                    PageView.this.addView((View) PageView.this.mBusyIndicator);
                    PageView.this.mBusyIndicator.setVisibility(4);
                    PageView.this.mHandler.postDelayed((Runnable) new Runnable() {
                        @Override
                        public void run() {
                            if (PageView.this.mBusyIndicator != null) {
                                PageView.this.mBusyIndicator.setVisibility(0);
                            }
                        }
                    }, 200L);
                }
            }

            @Override
            public void onPostExecute(final Void result) {
                PageView.this.removeView((View) PageView.this.mBusyIndicator);
                PageView.this.mBusyIndicator = null;
                PageView.this.mEntire.setImageBitmap(PageView.this.mEntireBm);
                PageView.this.mEntire.invalidate();
                Log.d("INVALIDATEunda","29");

                PageView.this.setBackgroundColor(0);
            }
        }).execute(new Void[0]);
        if (this.mSearchView == null) {
            this.addView(this.mSearchView = new View(this.mContext) {
//                private Float left = null;
//                private Float top = null;
//                private Float right = null;
//                private Float bottom = null;
//                private RectF rectF = null;
                RectF rectMain,rectMain2;
                private RectF lastCircleRect = null; // Track the last circle's position
                float initialHandleY2 = -1;  // Initialize it to an invalid value

                protected void onDraw(final Canvas canvas) {
                    super.onDraw(canvas);
                    final float scale = PageView.this.mSourceScale * this.getWidth() / PageView.this.mSize.x;
                    PageView.this.current_scale = scale;
                    final Paint paint = new Paint();
                    final Paint circlePaint = new Paint();
                    paint.setColor(Color.RED);
                    circlePaint.setColor(Color.BLUE);  // Circle color
                    circlePaint.setStyle(Paint.Style.FILL);
                    final RectF[] firstRect = {null};

                    // Draw the selection rectangle
                    if (PageView.this.mSelectBox != null && PageView.this.mText != null) {
                        int color = getInkColor();
                        paint.setColor(Color.argb(123, Color.red(color), Color.green(color), Color.blue(color)));
                        paint.setColor(HIGHLIGHT_COLOR);

                        final RectF[] lastLineRect = {null};

                        PageView.this.processSelectedText(new TextProcessor() {
                            RectF rect;
                            @Override
                            public void onStartLine() {
                                this.rect = new RectF();
                                if (firstRect[0] == null) {
                                    firstRect[0] = new RectF();
                                }
                            }

                            @Override
                            public void onWord(final TextWord word) {
                                this.rect.union((RectF) word);
                                if (firstRect[0].isEmpty()) {
                                    firstRect[0] = new RectF((RectF) word);  // Store the first word rect
                                }
                            }

                            @Override
                            public void onEndLine() {
                                if (!this.rect.isEmpty()) {
                                    // Store the current rect as the last rect
                                    lastLineRect[0] = new RectF(this.rect);

                                    // Draw the selection rectangle
                                    canvas.drawRect(this.rect.left * scale, this.rect.top * scale, this.rect.right * scale, this.rect.bottom * scale, paint);
                                }
                            }
                        });

                        // Draw a circle at the start of the selection
                        if (firstRect[0] != null) {
                            float startX = firstRect[0].left * scale;
                            float startY = (firstRect[0].top + firstRect[0].bottom) / 2 * scale;  // Midpoint of the first word's height
//                            canvas.drawCircle(startX, startY, 20f, circlePaint);  // Adjust the radius as needed
//                            textSelectionHelper.drawStartHandle();
                            float handleX = firstRect[0].left;  // You can adjust this to position on the left or right side
                            float handleY = (firstRect[0].top + firstRect[0].bottom) / 2;  // Midpoint of the top and bottom

                            // Call the drawStartHandle method with the calculated values
                            textSelectionHelper.drawStartHandle(canvas, handleX, handleY, scale);
                        }

                        // Draw a circle at the end of the selection
                        if (lastLineRect[0] != null) {
                            float endX = lastLineRect[0].right * scale;
                            float endY = (lastLineRect[0].top + lastLineRect[0].bottom) / 2 * scale;  // Midpoint of the last word's height
//                            canvas.drawCircle(endX, endY, 20f, circlePaint);  // Adjust the radius as needed
                            float handleRightX = lastLineRect[0].right;
                            float handleRightY = (lastLineRect[0].top + lastLineRect[0].bottom) / 2;
                            textSelectionHelper.drawEndHandle(canvas, handleRightX, handleRightY, scale);
                        }
                    }
                }

/*
                protected void onDraw(final Canvas canvas) {
                    super.onDraw(canvas);
                    final float scale = PageView.this.mSourceScale * this.getWidth() / PageView.this.mSize.x;
                    PageView.this.current_scale = scale;
                    final Paint paint = new Paint();
                    final Paint paint2 = new Paint();
                    paint.setColor(Color.RED);

                    if (!PageView.this.mIsBlank && PageView.this.mSearchBoxes != null) {
                        paint.setColor(HIGHLIGHT_COLOR);
                        for (final RectF rect : PageView.this.mSearchBoxes) {
                            Log.d("chakka","yaaaa1111");

                            canvas.drawRect(rect.left * scale, rect.top * scale, rect.right * scale, rect.bottom * scale, paint);
                        }
                    }
                    if (!PageView.this.mIsBlank && PageView.this.mLinks != null && PageView.this.mHighlightLinks) {
                        paint.setColor(PageView.this.LINK_COLOR);
                        for (final LinkInfo link : PageView.this.mLinks) {
                            Log.d("chakka","yaaaa2222");

                            canvas.drawRect(link.rect.left * scale, link.rect.top * scale, link.rect.right * scale, link.rect.bottom * scale, paint);
                        }
                    }
                    if (PageView.this.mSelectBox != null && PageView.this.mText != null) {
                        int color = getInkColor();
                        paint.setColor(Color.argb(123, Color.red(color), Color.green(color), Color.blue(color)));
                        paint.setColor(HIGHLIGHT_COLOR);
                        Log.d("chakka","yaaaa33333");
                        final RectF[] lastLineRect = {null};
                        PageView.this.processSelectedText(new TextProcessor() {
                            RectF rect;
                            RectF lastRect; // Store the last line's RectF
                            @Override
                            public void onStartLine() {


                                this.rect = new RectF();
                                rectMain2=new RectF();
                                RectF lastRect; // Store the last line's RectF
                                if(once==0)
                                {
                                    rectMain=new RectF();

                                }

                            }
                            // Method to clear the handle from the previous location
                            private void clearPreviousHandle(Canvas canvas, RectF lastLineRect, float scale) {
                                if (lastLineRect != null) {
                                    float handleX = lastLineRect.right;
                                    float handleY = (lastLineRect.top + lastLineRect.bottom) / 2;
                                    float radius = 20f * scale; // Assuming the same radius used in drawStartHandle

                                    // Clear the area where the previous handle was drawn (use background color)
                                    Paint clearPaint = new Paint();
                                    clearPaint.setColor(Color.RED); // Set this to the canvas background color
                                    clearPaint.setStyle(Paint.Style.FILL);

                                    // Draw a circle over the previous handle to erase it
                                    canvas.drawCircle(handleX * scale, handleY * scale, radius, clearPaint);
                                }
                            }

                            @Override
                            public void onWord(final TextWord word) {
                                this.rect.union((RectF) word);
//                                if(once==0) {
                                    rectMain=((RectF) word);
//                                    once = 1;
//                                }
                            }

                            @Override
                            public void onEndLine() {
                                if (!this.rect.isEmpty()) {
                                    Log.d("chakka","yaaa4444");
                                    // Store the current rect as the last rect
                                    Log.d("check","mSelectBoxthis.rect>"+this.rect.left* scale+"<>"+this.rect.bottom* scale);

                                    lastLineRect[0] = new RectF(this.rect);
                                    canvas.drawRect(this.rect.left * scale, this.rect.top * scale, this.rect.right * scale, this.rect.bottom * scale, paint);
                                    canvas.drawCircle(rectMain.left * scale, (rectMain.top + rectMain.bottom) / 2 * scale, 20f, paint2);

//                                    textSelectionHelper.drawStartHandle(canvas,);
//                                    left = this.rect.left * scale;
//                                    top = this.rect.top * scale;
//                                    right = this.rect.right * scale;
//                                    bottom = this.rect.bottom * scale;
//                                    rectF = new RectF(left, top, right, bottom);
                                    // Define the radius of the circles
*/
/*                                    float circleRadius = 30f; // Adjust the size as needed

// Draw the left circle
                                    canvas.drawCircle(rectMain.left * scale, (rectMain.top + rectMain.bottom) / 2 * scale, circleRadius, paint2);

// Draw the right circle
                                    canvas.drawCircle(this.rect.right * scale, (this.rect.top + this.rect.bottom) / 2 * scale, circleRadius, paint2);*//*

//                                    if (lastRect != null) {
//                                        float circleRadius = 30f; // Adjust the size as needed
//
//                                        // Draw the circle only at the right of the last line
//                                        canvas.drawCircle(lastRect.right * scale, (lastRect.top + lastRect.bottom) / 2 * scale, circleRadius, paint2);
//
//                                        // Optionally, draw the circle at the left side of the first word in the first line
//                                        canvas.drawCircle(rectMain.left * scale, (rectMain.top + rectMain.bottom) / 2 * scale, circleRadius, paint2);
//                                    }
                                    // After processing all the lines, draw the circle only on the last line
//                                    if (lastLineRect[0] != null) {
//                                        float circleRadius = 30f; // Adjust the size as needed
//
//                                        // Draw the circle only on the right of the last line
//                                        canvas.drawCircle(lastLineRect[0].right * scale, (lastLineRect[0].top + lastLineRect[0].bottom) / 2 * scale, circleRadius, paint2);
//
//                                        // Optionally, draw the circle on the left side of the first word in the first line
//                                        canvas.drawCircle(rectMain.left * scale, (rectMain.top + rectMain.bottom) / 2 * scale, circleRadius, paint2);
//
//                                        // Update lastCircleRect with the current circle's position
//                                        lastCircleRect = new RectF(lastLineRect[0]);
//                                    }
//                                    if (lastCircleRect != null) {
//                                        float circleRadius = 30f;
//                                        canvas.drawCircle(lastCircleRect.right * scale, (lastCircleRect.top + lastCircleRect.bottom) / 2 * scale, circleRadius, paint);
//                                    }
//                                    selectedText = rect;
//                                    PageView.this.setItemSelectBox(rect);
                                    // Calculate the x and y positions for the handle
                                    */
/*float handleX = this.rect.left;  // You can adjust this to position on the left or right side
                                    float handleY = (this.rect.top + this.rect.bottom) / 2;  // Midpoint of the top and bottom

                                    float handleX2 = mSelectBox.left;  // You can adjust this to position on the left or right side
                                    float handleY2 = (mSelectBox.top + mSelectBox.bottom) / 2;  // Midpoint of the top and bottom
// If this is the first time drawing the handle, store the initial value
                                    if (initialHandleY2 == -1) {
                                        initialHandleY2 = (mSelectBox.top + mSelectBox.bottom) / 2;  // Calculate the midpoint
                                    }

// Use the stored initial value for handleY2, so it doesn't change on dragging
                                    handleY2 = initialHandleY2;
                                    // Call the drawStartHandle method with the calculated values
                                    textSelectionHelper.drawStartHandle(canvas, handleX2, handleY2, scale);  // Adjust the scale to act as zoom

                                    // Optionally draw the handle on the right of the last line
                                    if (lastLineRect[0] != null) {
                                        float handleRightX = lastLineRect[0].right;
                                        float handleRightY = (lastLineRect[0].top + lastLineRect[0].bottom) / 2;
                                        textSelectionHelper.drawEndHandle(canvas, handleRightX, handleRightY, scale);
                                    }*//*


                                }
                            }
                        });
                    }
                    if (PageView.this.mItemSelectBox != null) {
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(4.0f);
                        paint.setColor(BOX_COLOR);
                        Log.d("chakka","yaaaa55555");

                        canvas.drawRect(PageView.this.mItemSelectBox.left * scale, PageView.this.mItemSelectBox.top * scale, PageView.this.mItemSelectBox.right * scale, PageView.this.mItemSelectBox.bottom * scale, paint);
                    }
                    if (PageView.this.mDrawing != null) {
                        final Path path = new Path();
                        paint.setAntiAlias(true);
                        paint.setDither(true);
                        paint.setStrokeJoin(Paint.Join.ROUND);
                        paint.setStrokeCap(Paint.Cap.ROUND);
                        paint.setStyle(Paint.Style.FILL);
                        paint.setStrokeWidth(PageView.this.INK_THICKNESS * scale);
                        paint.setColor(PageView.this.INK_COLOR);
                        for (final ArrayList<PointF> arc : PageView.this.mDrawing) {
                            if (arc.size() >= 2) {
                                final Iterator<PointF> iit = arc.iterator();
                                PointF p = iit.next();
                                float mX = p.x * scale;
                                float mY = p.y * scale;
                                path.moveTo(mX, mY);
                                while (iit.hasNext()) {
                                    p = iit.next();
                                    final float x = p.x * scale;
                                    final float y = p.y * scale;
                                    path.quadTo(mX, mY, (x + mX) / 2.0f, (y + mY) / 2.0f);
                                    mX = x;
                                    mY = y;
                                }
                                path.lineTo(mX, mY);
                            } else {
                                final PointF p = arc.get(0);
                                canvas.drawCircle(p.x * scale, p.y * scale, PageView.this.INK_THICKNESS * scale / 2.0f, paint);
                            }
                        }
                        paint.setStyle(Paint.Style.STROKE);
                        canvas.drawPath(path, paint);
                        Log.d("chakka","yaaaa66666");

                    }
                }
*/
            });
        }
        this.requestLayout();
    }

    public void setSearchBoxes(final RectF[] searchBoxes) {
        this.mSearchBoxes = searchBoxes;
        if (this.mSearchView != null) {
            this.mSearchView.invalidate();
            Log.d("INVALIDATEunda","27");

        }
    }

    public void setLinkHighlighting(final boolean f) {
        this.mHighlightLinks = f;
        if (this.mSearchView != null) {
            this.mSearchView.invalidate();
            Log.d("INVALIDATEunda","26");

        }
    }

    public void setLinkHighlightColor(final int color) {
        this.LINK_COLOR = color;
        if (this.mHighlightLinks && this.mSearchView != null) {
            this.mSearchView.invalidate();
            Log.d("INVALIDATEunda","25");

        }
    }

    public void deselectText() {
        this.mSelectBox = null;
        Log.d("nullified","yes2");

        this.mSearchView.invalidate();
        Log.d("INVALIDATEunda","24");

    }
    private float rectSize = 50f; // Size of the RectF
    private RectF startPoint;
    public void  selectorFirstPoint(final float x, final float y)
    {
        startPoint = new RectF(
                x - rectSize / 2, // left
                y - rectSize / 2, // top
                x + rectSize / 2, // right
                y + rectSize / 2  // bottom
        );
        once=0;
    }

    public void resetSelection()
    {
        once=-1;
    }

    public void selectTeext(final float x0, final float y0, final float x1, final float y1) {
        final float scale = this.mSourceScale * this.getWidth() / this.mSize.x;
        final float docRelX0 = (x0 - this.getLeft()) / scale;
        final float docRelY0 = (y0 - this.getTop()) / scale;
        final float docRelX2 = (x1 - this.getLeft()) / scale;
        final float docRelY2 = (y1 - this.getTop()) / scale;

        // Create a new RectF for the new selection
        RectF newSelectBox;
        if (docRelY0 <= docRelY2) {
            newSelectBox = new RectF(docRelX0, docRelY0, docRelX2, docRelY2);
        } else {
            newSelectBox = new RectF(docRelX2, docRelY2, docRelX0, docRelY0);
        }

        // Perform the union only if the new box is below or above the existing mSelectBox
        if (this.mSelectBox != null) {
            boolean isBelow = newSelectBox.top > this.mSelectBox.bottom;
            boolean isAbove = newSelectBox.bottom < this.mSelectBox.top;

            if (isBelow || isAbove) {
                this.mSelectBox.union(newSelectBox);  // Merge only if it's below or above
            }
        } else {
            this.mSelectBox = newSelectBox;  // First selection
        }

        Log.d("check", "mSelectBox>" + mSelectBox.left + "<>" + mSelectBox.bottom);
        this.mSearchView.invalidate();
        Log.d("INVALIDATEunda", "23");

        if (this.mGetText == null) {
            (this.mGetText = new AsyncTask<Void, Void, TextWord[][]>() {
                protected TextWord[][] doInBackground(final Void... params) {
                    return PageView.this.getText();
                }

                protected void onPostExecute(final TextWord[][] result) {
                    PageView.this.mText = result;
                    PageView.this.mSearchView.invalidate();
                    Log.d("INVALIDATEunda", "22");
                }
            }).execute(new Void[0]);
        }
    }


    public void selectText(final float x0, final float y0, final float x1, final float y1) {
        final float scale = this.mSourceScale * this.getWidth() / this.mSize.x;
        final float docRelX0 = (x0 - this.getLeft()) / scale;
        final float docRelY0 = (y0 - this.getTop()) / scale;
        final float docRelX2 = (x1 - this.getLeft()) / scale;
        final float docRelY2 = (y1 - this.getTop()) / scale;
        RectF newSelectBox;
        if (docRelY0 <= docRelY2) {
            newSelectBox = new RectF(docRelX0, docRelY0, docRelX2, docRelY2);
        } else {
            newSelectBox = new RectF(docRelX2, docRelY2, docRelX0, docRelY0);
        }


        // If mSelectBox is already defined, merge it with the new select box
        if (this.mSelectBox != null) {
            boolean isBelow = newSelectBox.top > this.mSelectBox.bottom;  // new selection is below the old one
            boolean isAbove = newSelectBox.bottom < this.mSelectBox.top;  // new selection is above the old one
//Log.d("kokokoko","isAbove"+isAbove+"isbelow"+isBelow);

            float mSelectBoxLeft = mSelectBox.left;
            float newSelectBoxLeft = newSelectBox.left;

            Log.d("kplp","mSelectBoxLeft>"+mSelectBoxLeft+"newSelectBoxLeft>"+newSelectBoxLeft);
            if (newSelectBox.bottom < mSelectBox.top) {
                Log.d("kokokoko", "before");
            }
            else if (mSelectBox.bottom < newSelectBox.top) {
                Log.d("kokokoko", "after");
            }
            else if (newSelectBox.bottom == mSelectBox.top || mSelectBox.bottom == newSelectBox.top) {
                Log.d("kokokoko", "adjacent");
            }
            else {
                Log.d("kokokoko", "between/overlap");
            }
            this.mSelectBox.union(newSelectBox);  // Union combines the new selection with the previous one
            Log.d("checkopz","mSelectBox>"+mSelectBox.left+"<bottom>"+mSelectBox.bottom+"<top>"+mSelectBox.top+"<right>"+mSelectBox.right);
            Log.d("checkopz","newSelectBox>"+newSelectBox.left+"<bottom>"+newSelectBox.bottom+"<top>"+newSelectBox.top+"<right>"+newSelectBox.right);

        } else {

            this.mSelectBox = newSelectBox;  // First selection
            float mSelectBoxLeft = mSelectBox.left;
            float newSelectBoxLeft = newSelectBox.left;
            Log.d("kplp","mSelectBoxLeft>"+mSelectBoxLeft+"newSelectBoxLeft>"+newSelectBoxLeft);

            Log.d("checkopz","mSelectBox>"+mSelectBox.left+"<bottom>"+mSelectBox.bottom+"<top>"+mSelectBox.top+"<right>"+mSelectBox.right);
            Log.d("checkopz","newSelectBox>"+newSelectBox.left+"<bottom>"+newSelectBox.bottom+"<top>"+newSelectBox.top+"<right>"+newSelectBox.right);

        }
//        if (docRelY0 <= docRelY2) {
//            this.mSelectBox = new RectF(docRelX0, docRelY0, docRelX2, docRelY2);
//        } else {
//            this.mSelectBox = new RectF(docRelX2, docRelY2, docRelX0, docRelY0);
//        }
        this.mSearchView.invalidate();
        Log.d("INVALIDATEunda","23");

        if (this.mGetText == null) {
            (this.mGetText = new AsyncTask<Void, Void, TextWord[][]>() {
                protected TextWord[][] doInBackground(final Void... params) {
                    return PageView.this.getText();
                }

                protected void onPostExecute(final TextWord[][] result) {
                    PageView.this.mText = result;
                    PageView.this.mSearchView.invalidate();
                    Log.d("INVALIDATEunda","22");


                }
            }).execute(new Void[0]);
        }
    }

    public void startDraw(final float x, final float y) {
        final float scale = this.mSourceScale * this.getWidth() / this.mSize.x;
        final float docRelX = (x - this.getLeft()) / scale;
        final float docRelY = (y - this.getTop()) / scale;
        if (this.mDrawing == null) {
            this.mDrawing = new ArrayList<ArrayList<PointF>>();
        }
        final ArrayList<PointF> arc = new ArrayList<PointF>();
        arc.add(new PointF(docRelX, docRelY));
        this.mDrawing.add(arc);
        this.mSearchView.invalidate();
        Log.d("INVALIDATEunda","21");

    }

    public void continueDraw(final float x, final float y) {
        final float scale = this.mSourceScale * this.getWidth() / this.mSize.x;
        final float docRelX = (x - this.getLeft()) / scale;
        final float docRelY = (y - this.getTop()) / scale;
        if (this.mDrawing != null && this.mDrawing.size() > 0) {
            final ArrayList<PointF> arc = this.mDrawing.get(this.mDrawing.size() - 1);
            arc.add(new PointF(docRelX, docRelY));
            this.mSearchView.invalidate();
            Log.d("INVALIDATEunda","20");

        }
    }

    public void cancelDraw() {
        this.mDrawing = null;
        this.mSearchView.invalidate();
        Log.d("INVALIDATEunda","15");

    }

    protected PointF[][] getDraw() {
        if (this.mDrawing == null) {
            return null;
        }
        final PointF[][] path = new PointF[this.mDrawing.size()][];
        for (int i = 0; i < this.mDrawing.size(); ++i) {
            final ArrayList<PointF> arc = this.mDrawing.get(i);
            path[i] = arc.toArray(new PointF[arc.size()]);
        }
        return path;
    }

    public void setInkColor(final int color) {
        this.INK_COLOR = color;
    }

    public void setPaintStrockWidth(final float inkThickness) {
        this.INK_THICKNESS = inkThickness;
    }

    protected float getInkThickness() {
        if (this.current_scale == 0.0f) {
            return 4.537815f;
        }
        return this.INK_THICKNESS / 2.0f;
    }

    public float getCurrentScale() {
        if (this.current_scale == 0.0f) {
            return 9.07563f;
        }
        return this.current_scale;
    }

    protected float[] getColor() {
        return this.changeColor(this.INK_COLOR);
    }

    protected int getInkColor() {
        return this.INK_COLOR;
    }

    private float[] changeColor(int color) {
        int red = (color & 0xff0000) >> 16;
        int green = (color & 0x00ff00) >> 8;
        int blue = (color & 0x0000ff);
        float colors[] = new float[3];
        colors[0] = red / 255f;
        colors[1] = green / 255f;
        colors[2] = blue / 255f;
        return colors;
    }

    private float[] changeAnnotationColor(int color) {

        int red = (color & 0xff0000) >> 16;
        int green = (color & 0x00ff00) >> 8;
        int blue = (color & 0x0000ff);
        float colors[] = new float[3];
        colors[0] = red / 255f;
        colors[1] = green / 255f;
        colors[2] = blue / 255f;

        return colors;
    }


    protected void processSelectedText(TextProcessor tp) {
        (new TextSelector(mText, mSelectBox)).select(tp);
    }

    public void setItemSelectBox(final RectF rect) {
        this.mItemSelectBox = rect;
        if (this.mSearchView != null) {
            this.mSearchView.invalidate();
            Log.d("INVALIDATEunda","14");

        }
    }

    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        int x = 0;
        switch (MeasureSpec.getMode(widthMeasureSpec)) {
            case 0: {
                x = this.mSize.x;
                break;
            }
            default: {
                x = MeasureSpec.getSize(widthMeasureSpec);
                break;
            }
        }
        int y = 0;
        switch (MeasureSpec.getMode(heightMeasureSpec)) {
            case 0: {
                y = this.mSize.y;
                break;
            }
            default: {
                y = MeasureSpec.getSize(heightMeasureSpec);
                break;
            }
        }
        this.setMeasuredDimension(x, y);
        if (this.mBusyIndicator != null) {
            final int limit = Math.min(this.mParentSize.x, this.mParentSize.y) / 2;
            this.mBusyIndicator.measure(Integer.MIN_VALUE | limit, Integer.MIN_VALUE | limit);
        }
    }

    protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom) {
        final int w = right - left;
        final int h = bottom - top;
        if (this.mEntire != null) {
            if (this.mEntire.getWidth() != w || this.mEntire.getHeight() != h) {
                this.mEntireMat.setScale(w / (float) this.mSize.x, h / (float) this.mSize.y);
                this.mEntire.setImageMatrix(this.mEntireMat);
                this.mEntire.invalidate();
                Log.d("INVALIDATEunda","13");

            }
            this.mEntire.layout(0, 0, w, h);
        }
        if (this.mSearchView != null) {
            this.mSearchView.layout(0, 0, w, h);
        }
        if (this.mPatchViewSize != null) {
            if (this.mPatchViewSize.x != w || this.mPatchViewSize.y != h) {
                this.mPatchViewSize = null;
                this.mPatchArea = null;
                if (this.mPatch != null) {
                    this.mPatch.setImageBitmap((Bitmap) null);
                    this.mPatch.invalidate();
                    Log.d("INVALIDATEunda","12");

                }
            } else {
                this.mPatch.layout(this.mPatchArea.left, this.mPatchArea.top, this.mPatchArea.right, this.mPatchArea.bottom);
            }
        }
        if (this.mBusyIndicator != null) {
            final int bw = this.mBusyIndicator.getMeasuredWidth();
            final int bh = this.mBusyIndicator.getMeasuredHeight();
            this.mBusyIndicator.layout((w - bw) / 2, (h - bh) / 2, (w + bw) / 2, (h + bh) / 2);
        }
    }

    public void updateHq(final boolean update) {
        final Rect viewArea = new Rect(this.getLeft(), this.getTop(), this.getRight(), this.getBottom());
        if (viewArea.width() == this.mSize.x || viewArea.height() == this.mSize.y) {
            if (this.mPatch != null) {
                this.mPatch.setImageBitmap((Bitmap) null);
                this.mPatch.invalidate();
                Log.d("INVALIDATEunda","eleven");

            }
        } else {
            final Point patchViewSize = new Point(viewArea.width(), viewArea.height());
            final Rect patchArea = new Rect(0, 0, this.mParentSize.x, this.mParentSize.y);
            if (!patchArea.intersect(viewArea)) {
                return;
            }
            patchArea.offset(-viewArea.left, -viewArea.top);
            final boolean area_unchanged = patchArea.equals(this.mPatchArea) && patchViewSize.equals(this.mPatchViewSize);
            if (area_unchanged && !update) {
                return;
            }
            final boolean completeRedraw = !area_unchanged;
            if (this.mDrawPatch != null) {
                this.mDrawPatch.cancelAndWait();
                this.mDrawPatch = null;
            }
            if (this.mPatch == null) {
                (this.mPatch = (ImageView) new OpaqueImageView(this.mContext)).setScaleType(ImageView.ScaleType.MATRIX);
                this.addView((View) this.mPatch);
                this.mSearchView.bringToFront();
            }
            CancellableTaskDefinition<Void, Void> task;
            if (completeRedraw) {
                task = this.getDrawPageTask(this.mPatchBm, patchViewSize.x, patchViewSize.y, patchArea.left, patchArea.top, patchArea.width(), patchArea.height());
            } else {
                task = this.getUpdatePageTask(this.mPatchBm, patchViewSize.x, patchViewSize.y, patchArea.left, patchArea.top, patchArea.width(), patchArea.height());
            }
            (this.mDrawPatch = new CancellableAsyncTask<Void, Void>(task) {
                @Override
                public void onPostExecute(final Void result) {
                    PageView.this.mPatchViewSize = patchViewSize;
                    PageView.this.mPatchArea = patchArea;
                    PageView.this.mPatch.setImageBitmap(PageView.this.mPatchBm);
                    PageView.this.mPatch.invalidate();
                    Log.d("INVALIDATEunda","ten");

                    PageView.this.mPatch.layout(PageView.this.mPatchArea.left, PageView.this.mPatchArea.top, PageView.this.mPatchArea.right, PageView.this.mPatchArea.bottom);
                }
            }).execute(new Void[0]);
        }
    }

    public void update() {
        if (this.mDrawEntire != null) {
            this.mDrawEntire.cancelAndWait();
            this.mDrawEntire = null;
        }
        if (this.mDrawPatch != null) {
            this.mDrawPatch.cancelAndWait();
            this.mDrawPatch = null;
        }
        (this.mDrawEntire = new CancellableAsyncTask<Void, Void>(this.getUpdatePageTask(this.mEntireBm, this.mSize.x, this.mSize.y, 0, 0, this.mSize.x, this.mSize.y)) {
            @Override
            public void onPostExecute(final Void result) {
                PageView.this.mEntire.setImageBitmap(PageView.this.mEntireBm);
                PageView.this.mEntire.invalidate();
                Log.d("INVALIDATEunda","99999");

            }
        }).execute(new Void[0]);
        this.updateHq(true);
    }

    public void removeHq() {
        if (this.mDrawPatch != null) {
            this.mDrawPatch.cancelAndWait();
            this.mDrawPatch = null;
        }
        this.mPatchViewSize = null;
        this.mPatchArea = null;
        if (this.mPatch != null) {
            this.mPatch.setImageBitmap((Bitmap) null);
            this.mPatch.invalidate();
            Log.d("INVALIDATEunda","88888");

        }
    }

    public int getPage() {
        return this.mPageNumber;
    }

    public boolean isOpaque() {
        return true;
    }
}
