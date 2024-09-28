//
// Decompiled by Procyon v0.5.36
//

package com.artifex.mupdfdemo;

import static com.artifex.mupdfdemo.AnnotationHandlerPopUpKt.showPopupAtCenterTopOfRect;

import android.os.Build;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;
import android.text.method.TransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
//import com.lonelypluto.pdfviewerdemo.R;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.content.Context;
import android.graphics.PointF;
import android.widget.EditText;
import android.app.AlertDialog;
import android.graphics.RectF;
import android.os.AsyncTask;

public class MuPDFPageView extends PageView implements MuPDFView
{
    private final MuPDFCore mCore;
    private AsyncTask<Void, Void, PassClickResult> mPassClick;
    private RectF[] mWidgetAreas;
    private Annotation[] mAnnotations;
    private int mSelectedAnnotationIndex;
    private AsyncTask<Void, Void, RectF[]> mLoadWidgetAreas;
    private AsyncTask<Void, Void, Annotation[]> mLoadAnnotations;
    private AlertDialog.Builder mTextEntryBuilder;
    private AlertDialog.Builder mChoiceEntryBuilder;
    private AlertDialog.Builder mSigningDialogBuilder;
    private AlertDialog.Builder mSignatureReportBuilder;
    private AlertDialog.Builder mPasswordEntryBuilder;
    private EditText mPasswordText;
    private AlertDialog mTextEntry;
    private AlertDialog mPasswordEntry;
    private EditText mEditText;
    private AsyncTask<String, Void, Boolean> mSetWidgetText;
    private AsyncTask<String, Void, Void> mSetWidgetChoice;
    private AsyncTask<Object, Void, Void> mAddStrikeOut;
    private AsyncTask<Object, Void, Void> mAddInk;
    private AsyncTask<Integer, Void, Void> mDeleteAnnotation;
    private AsyncTask<Void, Void, String> mCheckSignature;
    private AsyncTask<Void, Void, Boolean> mSign;
    private Runnable changeReporter;

    public MuPDFPageView(final Context c, final MuPDFCore core, final Point parentSize, final Bitmap sharedHqBm) {
        super(c, parentSize, sharedHqBm);
        this.mSelectedAnnotationIndex = -1;
        this.mCore = core;
        (this.mTextEntryBuilder = new AlertDialog.Builder(c)).setTitle((CharSequence)this.getContext().getString(R.string.fill_out_text_field));
        final LayoutInflater inflater = (LayoutInflater)c.getSystemService("layout_inflater");
        this.mEditText = (EditText)inflater.inflate(R.layout.edittext_entry, (ViewGroup)null);
        this.mTextEntryBuilder.setView((View)this.mEditText);
        this.mTextEntryBuilder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener)new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.dismiss();
            }
        });
        this.mTextEntryBuilder.setPositiveButton(R.string.okay, (DialogInterface.OnClickListener)new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                MuPDFPageView.this.mSetWidgetText = new AsyncTask<String, Void, Boolean>() {
                    protected Boolean doInBackground(final String... arg0) {
                        return MuPDFPageView.this.mCore.setFocusedWidgetText(MuPDFPageView.this.mPageNumber, arg0[0]);
                    }

                    protected void onPostExecute(final Boolean result) {
                        MuPDFPageView.this.changeReporter.run();
                        if (!result) {
                            MuPDFPageView.this.invokeTextDialog(MuPDFPageView.this.mEditText.getText().toString());
                        }
                    }
                };
                MuPDFPageView.this.mSetWidgetText.execute(new String[] { MuPDFPageView.this.mEditText.getText().toString() });
            }
        });
        this.mTextEntry = this.mTextEntryBuilder.create();
        (this.mChoiceEntryBuilder = new AlertDialog.Builder(c)).setTitle((CharSequence)this.getContext().getString(R.string.choose_value));
        (this.mSigningDialogBuilder = new AlertDialog.Builder(c)).setTitle((CharSequence)this.getContext().getString(R.string.select_certificate_and_sign));
        this.mSigningDialogBuilder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener)new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.dismiss();
            }
        });
        this.mSigningDialogBuilder.setPositiveButton(R.string.okay, (DialogInterface.OnClickListener)new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
            }
        });
        (this.mSignatureReportBuilder = new AlertDialog.Builder(c)).setTitle((CharSequence)this.getContext().getString(R.string.signature_checked));
        this.mSignatureReportBuilder.setPositiveButton(R.string.okay, (DialogInterface.OnClickListener)new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.dismiss();
            }
        });
        (this.mPasswordText = new EditText(c)).setInputType(128);
        this.mPasswordText.setTransformationMethod((TransformationMethod)new PasswordTransformationMethod());
        (this.mPasswordEntryBuilder = new AlertDialog.Builder(c)).setTitle(R.string.enter_password);
        this.mPasswordEntryBuilder.setView((View)this.mPasswordText);
        this.mPasswordEntryBuilder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener)new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.dismiss();
            }
        });
        this.mPasswordEntry = this.mPasswordEntryBuilder.create();
    }

    public MuPDFCore getMuPdfCore() {
        return mCore;
    }

    private void signWithKeyFile(final Uri uri) {
        this.mPasswordEntry.getWindow().setSoftInputMode(5);
        this.mPasswordEntry.setButton(-1, (CharSequence)"Sign", (DialogInterface.OnClickListener)new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.dismiss();
                MuPDFPageView.this.signWithKeyFileAndPassword(uri, MuPDFPageView.this.mPasswordText.getText().toString());
            }
        });
        this.mPasswordEntry.show();
    }

    private void signWithKeyFileAndPassword(final Uri uri, final String password) {
        (this.mSign = new AsyncTask<Void, Void, Boolean>() {
            protected Boolean doInBackground(final Void... params) {
                return MuPDFPageView.this.mCore.signFocusedSignature(Uri.decode(uri.getEncodedPath()), password);
            }

            protected void onPostExecute(final Boolean result) {
                if (result) {
                    MuPDFPageView.this.changeReporter.run();
                }
                else {
                    MuPDFPageView.this.mPasswordText.setText((CharSequence)"");
                    MuPDFPageView.this.signWithKeyFile(uri);
                }
            }
        }).execute(new Void[0]);
    }

    @Override
    public LinkInfo hitLink(final float x, final float y) {
        final float scale = this.mSourceScale * this.getWidth() / this.mSize.x;
        final float docRelX = (x - this.getLeft()) / scale;
        final float docRelY = (y - this.getTop()) / scale;
        for (final LinkInfo l : this.mLinks) {
            if (l.rect.contains(docRelX, docRelY)) {
                return l;
            }
        }
        return null;
    }

    private void invokeTextDialog(final String text) {
        this.mEditText.setText((CharSequence)text);
        this.mTextEntry.getWindow().setSoftInputMode(5);
        this.mTextEntry.show();
    }

    private void invokeChoiceDialog(final String[] options) {
        this.mChoiceEntryBuilder.setItems((CharSequence[])options, (DialogInterface.OnClickListener)new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int which) {
                MuPDFPageView.this.mSetWidgetChoice = new AsyncTask<String, Void, Void>() {
                    protected Void doInBackground(final String... params) {
                        final String[] sel = { params[0] };
                        MuPDFPageView.this.mCore.setFocusedWidgetChoiceSelected(sel);
                        return null;
                    }

                    protected void onPostExecute(final Void result) {
                        MuPDFPageView.this.changeReporter.run();
                    }
                };
                MuPDFPageView.this.mSetWidgetChoice.execute(new String[] { options[which] });
            }
        });
        final AlertDialog dialog = this.mChoiceEntryBuilder.create();
        dialog.show();
    }

    private void invokeSignatureCheckingDialog() {
        (this.mCheckSignature = new AsyncTask<Void, Void, String>() {
            protected String doInBackground(final Void... params) {
                return MuPDFPageView.this.mCore.checkFocusedSignature();
            }

            protected void onPostExecute(final String result) {
                final AlertDialog report = MuPDFPageView.this.mSignatureReportBuilder.create();
                report.setMessage((CharSequence)result);
                report.show();
            }
        }).execute(new Void[0]);
    }

    private void invokeSigningDialog() {
        final AlertDialog dialog = this.mSigningDialogBuilder.create();
        dialog.show();
    }

    private void warnNoSignatureSupport() {
        final AlertDialog dialog = this.mSignatureReportBuilder.create();
        dialog.setTitle((CharSequence)"App built with no signature support");
        dialog.show();
    }

    @Override
    public void setChangeReporter(final Runnable reporter) {
        this.changeReporter = reporter;
    }

    @Override
    public Hit passClickEvent(final float x, final float y) {
        final float scale = this.mSourceScale * this.getWidth() / this.mSize.x;
        final float docRelX = (x - this.getLeft()) / scale;
        final float docRelY = (y - this.getTop()) / scale;
        boolean hit = false;
        if (this.mAnnotations != null) {
            int i;
            for (i = 0; i < this.mAnnotations.length; ++i) {
                if (this.mAnnotations[i].contains(docRelX, docRelY)) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                switch (this.mAnnotations[i].type) {
                    case HIGHLIGHT:
                    case UNDERLINE:
                    case SQUIGGLY:
                    case STRIKEOUT:
                    case INK: {
                        this.mSelectedAnnotationIndex = i;
                        this.setItemDeleteBox(this.mAnnotations[i]);
                        this.setItemSelectBox(this.mAnnotations[i]);
                        float recalculatedX0 = (mAnnotations[i].left * scale) + getLeft();
                        float recalculatedY0 = (mAnnotations[i].top * scale) + getTop();
                        float recalculatedX1 = (mAnnotations[i].right * scale) + getLeft();
                        float recalculatedY1 = (mAnnotations[i].bottom * scale) + getTop();
                        showPopupAtCenterTopOfRect(getContext(), new RectF(recalculatedX0,recalculatedY0,recalculatedX1,recalculatedY1),getRootView(),selectedListner);

                        return Hit.Annotation;
                    }
                }
            }
        }
        this.mSelectedAnnotationIndex = -1;
        this.setItemSelectBox(null);
        if (!this.mCore.javascriptSupported()) {
            return Hit.Nothing;
        }
        if (this.mWidgetAreas != null) {
            for (int i = 0; i < this.mWidgetAreas.length && !hit; ++i) {
                if (this.mWidgetAreas[i].contains(docRelX, docRelY)) {
                    hit = true;
                }
            }
        }
        if (hit) {
            (this.mPassClick = new AsyncTask<Void, Void, PassClickResult>() {
                protected PassClickResult doInBackground(final Void... arg0) {
                    return MuPDFPageView.this.mCore.passClickEvent(MuPDFPageView.this.mPageNumber, docRelX, docRelY);
                }

                protected void onPostExecute(final PassClickResult result) {
                    if (result.changed) {
                        MuPDFPageView.this.changeReporter.run();
                    }
                    result.acceptVisitor(new PassClickResultVisitor() {
                        @Override
                        public void visitText(final PassClickResultText result) {
                            MuPDFPageView.this.invokeTextDialog(result.text);
                        }

                        @Override
                        public void visitChoice(final PassClickResultChoice result) {
                            MuPDFPageView.this.invokeChoiceDialog(result.options);
                        }

                        @Override
                        public void visitSignature(final PassClickResultSignature result) {
                            switch (result.state) {
                                case NoSupport: {
                                    MuPDFPageView.this.warnNoSignatureSupport();
                                    break;
                                }
                                case Unsigned: {
                                    MuPDFPageView.this.invokeSigningDialog();
                                    break;
                                }
                                case Signed: {
                                    MuPDFPageView.this.invokeSignatureCheckingDialog();
                                    break;
                                }
                            }
                        }
                    });
                }
            }).execute(new Void[0]);
            return Hit.Widget;
        }
        return Hit.Nothing;
    }

    @TargetApi(11)
    @Override
    public boolean copySelection() {
        final StringBuilder text = new StringBuilder();
        this.processSelectedText(new TextProcessor() {
            StringBuilder line;

            @Override
            public void onStartLine() {
                this.line = new StringBuilder();
            }

            @Override
            public void onWord(final TextWord word) {
                if (this.line.length() > 0) {
                    this.line.append(' ');
                }
                this.line.append(word.w);
            }

            @Override
            public void onEndLine() {
                if (text.length() > 0) {
                    text.append('\n');
                }
                Log.d("ffnet", "onEndLine: "+line);
                text.append((CharSequence)this.line);
            }

            @Override
            public void onEndText() {

            }
        });
        if (text.length() == 0) {
            return false;
        }
        final ClipboardManager cm = (ClipboardManager)this.mContext.getSystemService("clipboard");
        cm.setPrimaryClip(ClipData.newPlainText((CharSequence)"MuPDF", (CharSequence)text));
        this.deselectText();
        return true;
    }

    /* Todo: MuPDFPageView:: it use to copy text after user press comment from popup window in pdf screen */
    public boolean isTextSelected() {
        final StringBuilder text = new StringBuilder();
        processSelectedText(new TextProcessor() {
            StringBuilder line;

            public void onStartLine() {
                line = new StringBuilder();
            }

            public void onWord(TextWord word) {
                if (line.length() > 0)
                    line.append(' ');
                line.append(word.w);
            }

            public void onEndLine() {
                if (text.length() > 0)
                    text.append('\n');
                text.append(line);
            }

            @Override
            public void onEndText() {

            }
        });
        if (text.length() == 0)
            return false;
        android.content.ClipboardManager cm = (android.content.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("MuPDF", text));
        return true;
    }
    @Override
    public boolean markupFromDbSelection(final Annotation.Type type, List<PointF> quadPoints) {
        /*ArrayList<PointF> quadPoints = new ArrayList<>();
        quadPoints.add(new PointF(68.448f, 852.0528f));
        quadPoints.add(new PointF(1128.5419f, 852.0528f));
        quadPoints.add(new PointF(1128.5419f, 827.5517f));
        quadPoints.add(new PointF(68.448f, 827.5517f));
        quadPoints.add(new PointF(68.448f, 876.5928f));
        quadPoints.add(new PointF(1107.5895f, 876.5928f));
        quadPoints.add(new PointF(1107.5895f, 852.0917f));
        quadPoints.add(new PointF(68.448f, 852.0917f));
        quadPoints.add(new PointF(68.448f, 900.3528f));
        quadPoints.add(new PointF(1077.5247f, 900.3528f));
        quadPoints.add(new PointF(1077.5247f, 875.8517f));
        quadPoints.add(new PointF(68.448f, 875.8517f));
        quadPoints.add(new PointF(68.448f, 924.83276f));
        quadPoints.add(new PointF(1058.3f, 924.83276f));
        quadPoints.add(new PointF(1058.3f, 900.33167f));
        quadPoints.add(new PointF(68.448f, 900.33167f));*/
        // Adding the points from your log
     /*   quadPoints.add(new PointF(327.30228f, 1349.8127f));
        quadPoints.add(new PointF(392.0764f, 1349.8127f));
        quadPoints.add(new PointF(392.0764f, 1325.3116f));
        quadPoints.add(new PointF(327.30228f, 1325.3116f));*/
//        quadPoints.add(new PointF(68.448f, 395.39285f));
//        quadPoints.add(new PointF(1099.8542f, 395.39285f));
//        quadPoints.add(new PointF(1099.8542f, 370.89175f));
//        quadPoints.add(new PointF(68.448f, 370.89175f));
//        quadPoints.add(new PointF(68.448f, 419.15286f));
//        quadPoints.add(new PointF(1081.1471f, 419.15286f));
//        quadPoints.add(new PointF(1081.1471f, 394.65176f));
//        quadPoints.add(new PointF(68.448f, 394.65176f));
//        quadPoints.add(new PointF(68.448f, 443.63284f));
//        quadPoints.add(new PointF(152.016f, 443.63284f));
//        quadPoints.add(new PointF(152.016f, 419.13174f));
//        quadPoints.add(new PointF(68.448f, 419.13174f));
//        2024-09-23 16:13:29.712 27245-27245 poinsyoyo               com.rameshvoltella.pdfeditorpro      D  [PointF(327.30228, 1349.8127), PointF(392.0764, 1349.8127), PointF(392.0764, 1325.3116), PointF(327.30228, 1325.3116)]

        (this.mAddStrikeOut = new AsyncTask<Object, Void, Void>() {
            protected Void doInBackground(final Object... params) {
                MuPDFPageView.this.addMarkup(((PointF[][]) params[0])[0], type, (int)params[1]);
                return null;
            }

            protected void onPostExecute(final Void result) {
                MuPDFPageView.this.loadAnnotations();
                MuPDFPageView.this.update();
            }
        }).execute(new PointF[][] { quadPoints.toArray(new PointF[quadPoints.size()]) }, getInkColor());
        this.deselectText();
        return true;
    }
    @Override
    public ArrayList<PointF> markupSelection(final Annotation.Type type) {
        final ArrayList<PointF> quadPoints = new ArrayList<PointF>();
        this.processSelectedText(new TextProcessor() {
            RectF rect;

            @Override
            public void onStartLine() {
                this.rect = new RectF();
            }

            @Override
            public void onWord(final TextWord word) {
                this.rect.union((RectF)word);
            }

            @Override
            public void onEndLine() {
                if (!this.rect.isEmpty()) {
                    quadPoints.add(new PointF(this.rect.left, this.rect.bottom));
                    quadPoints.add(new PointF(this.rect.right, this.rect.bottom));
                    quadPoints.add(new PointF(this.rect.right, this.rect.top));
                    quadPoints.add(new PointF(this.rect.left, this.rect.top));

                    Log.d("poinsyoyo",""+quadPoints);
                }
            }

            @Override
            public void onEndText() {

            }
        });
        if (quadPoints.size() == 0) {
            return quadPoints;
        }
        (this.mAddStrikeOut = new AsyncTask<Object, Void, Void>() {
            protected Void doInBackground(final Object... params) {
                MuPDFPageView.this.addMarkup(((PointF[][]) params[0])[0], type, (int)params[1]);
                return null;
            }

            protected void onPostExecute(final Void result) {
                MuPDFPageView.this.loadAnnotations();
                MuPDFPageView.this.update();
            }
        }).execute(new PointF[][] { quadPoints.toArray(new PointF[quadPoints.size()]) }, getInkColor());
        this.deselectText();
        return quadPoints;
    }

    @Override
    public void deleteSelectedAnnotation() {
        if (this.mSelectedAnnotationIndex != -1) {
            if (this.mDeleteAnnotation != null) {
                this.mDeleteAnnotation.cancel(true);
            }
            (this.mDeleteAnnotation = new AsyncTask<Integer, Void, Void>() {
                protected Void doInBackground(final Integer... params) {
                    MuPDFPageView.this.mCore.deleteAnnotation(MuPDFPageView.this.mPageNumber, params[0]);
                    return null;
                }

                protected void onPostExecute(final Void result) {
                    MuPDFPageView.this.loadAnnotations();
                    MuPDFPageView.this.update();
                }
            }).execute(new Integer[] { this.mSelectedAnnotationIndex });
            this.mSelectedAnnotationIndex = -1;
            this.setItemSelectBox(null);
        }
    }

    @Override
    public void deselectAnnotation() {
        this.mSelectedAnnotationIndex = -1;
        this.setItemSelectBox(null);
    }

    @Override
    public boolean saveDrawFromDb(PointF[][] points) {
        if (this.mAddInk != null) {
            this.mAddInk.cancel(true);
            this.mAddInk = null;
        }
        (this.mAddInk = new AsyncTask<Object, Void, Void>() {
            protected Void doInBackground(final Object... params) {
                MuPDFPageView.this.mCore.addInkAnnotation(MuPDFPageView.this.mPageNumber, (PointF[][])params[0], (int)params[1], (float)params[2]);
                return null;
            }

            protected void onPostExecute(final Void result) {
                MuPDFPageView.this.update();
                MuPDFPageView.this.loadAnnotations();
            }
        }).execute(new Object[] { points, this.getInkColor(), this.getInkThickness() });
        this.cancelDraw();
        return true;
    }

    @Override
    public PointF[][] saveDraw() {
        final PointF[][] path = this.getDraw();
        if (path == null) {
            return null;
        }
        if (this.mAddInk != null) {
            this.mAddInk.cancel(true);
            this.mAddInk = null;
        }
        (this.mAddInk = new AsyncTask<Object, Void, Void>() {
            protected Void doInBackground(final Object... params) {
                MuPDFPageView.this.mCore.addInkAnnotation(MuPDFPageView.this.mPageNumber, (PointF[][])params[0], (int)params[1], (float)params[2]);
                return null;
            }

            protected void onPostExecute(final Void result) {
                MuPDFPageView.this.update();
                MuPDFPageView.this.loadAnnotations();
            }
        }).execute(new Object[] { this.getDraw(), this.getInkColor(), this.getInkThickness() });
        this.cancelDraw();
        return path;
    }

    @Override
    protected CancellableTaskDefinition<Void, Void> getDrawPageTask(final Bitmap bm, final int sizeX, final int sizeY, final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
        return new MuPDFCancellableTaskDefinition<Void, Void>(this.mCore) {
            @Override
            public Void doInBackground(final MuPDFCore.Cookie cookie, final Void... params) {
                if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT < 14) {
                    bm.eraseColor(0);
                }
                MuPDFPageView.this.mCore.drawPage(bm, MuPDFPageView.this.mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
                return null;
            }
        };
    }

    @Override
    protected CancellableTaskDefinition<Void, Void> getUpdatePageTask(final Bitmap bm, final int sizeX, final int sizeY, final int patchX, final int patchY, final int patchWidth, final int patchHeight) {
        return new MuPDFCancellableTaskDefinition<Void, Void>(this.mCore) {
            @Override
            public Void doInBackground(final MuPDFCore.Cookie cookie, final Void... params) {
                if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT < 14) {
                    bm.eraseColor(0);
                }
                MuPDFPageView.this.mCore.updatePage(bm, MuPDFPageView.this.mPageNumber, sizeX, sizeY, patchX, patchY, patchWidth, patchHeight, cookie);
                return null;
            }
        };
    }

    @Override
    protected LinkInfo[] getLinkInfo() {
        return this.mCore.getPageLinks(this.mPageNumber);
    }

    @Override
    protected TextWord[][] getText() {
        return this.mCore.textLines(this.mPageNumber);
    }

    @Override
    protected void addMarkup(final PointF[] quadPoints, final Annotation.Type type , int color) {
        Log.d("addingpoints",quadPoints+"");


        this.mCore.addMarkupAnnotation(this.mPageNumber, quadPoints, type , color);
    }

    private void loadAnnotations() {
        this.mAnnotations = null;
        if (this.mLoadAnnotations != null) {
            this.mLoadAnnotations.cancel(true);
        }
        (this.mLoadAnnotations = new AsyncTask<Void, Void, Annotation[]>() {
            protected Annotation[] doInBackground(final Void... params) {
                return MuPDFPageView.this.mCore.getAnnoations(MuPDFPageView.this.mPageNumber);
            }

            protected void onPostExecute(final Annotation[] result) {
                MuPDFPageView.this.mAnnotations = result;
            }
        }).execute(new Void[0]);
    }

    @Override
    public void setPage(final int page, final PointF size) {
        this.loadAnnotations();
        (this.mLoadWidgetAreas = new AsyncTask<Void, Void, RectF[]>() {
            protected RectF[] doInBackground(final Void... arg0) {
                return MuPDFPageView.this.mCore.getWidgetAreas(page);
            }

            protected void onPostExecute(final RectF[] result) {
                MuPDFPageView.this.mWidgetAreas = result;
            }
        }).execute(new Void[0]);
        super.setPage(page, size);
    }

    @Override
    public void setScale(final float scale) {
    }

    @Override
    public void releaseResources() {
        if (this.mPassClick != null) {
            this.mPassClick.cancel(true);
            this.mPassClick = null;
        }
        if (this.mLoadWidgetAreas != null) {
            this.mLoadWidgetAreas.cancel(true);
            this.mLoadWidgetAreas = null;
        }
        if (this.mLoadAnnotations != null) {
            this.mLoadAnnotations.cancel(true);
            this.mLoadAnnotations = null;
        }
        if (this.mSetWidgetText != null) {
            this.mSetWidgetText.cancel(true);
            this.mSetWidgetText = null;
        }
        if (this.mSetWidgetChoice != null) {
            this.mSetWidgetChoice.cancel(true);
            this.mSetWidgetChoice = null;
        }
        if (this.mAddStrikeOut != null) {
            this.mAddStrikeOut.cancel(true);
            this.mAddStrikeOut = null;
        }
        if (this.mDeleteAnnotation != null) {
            this.mDeleteAnnotation.cancel(true);
            this.mDeleteAnnotation = null;
        }
        super.releaseResources();
    }

    /* Todo: PageView:: this method is basically use to show popup on specific position after user touch up */
    @Override
    public void showCopyRect(float x, float y) {
//        final MuPDFView pageView = (MuPDFView) this.getDisplayedView();
//        if (pageView != null) {
            if (getBoolean()) {
                if (Ext.Companion.getOnClick() != null) {
                    Ext.Companion.getOnClick().invoke(x, y, selectedText); //before
//                    mSelectBox = RectF(x, y, x, y);)
                }

            }
//
//        }
    }

    public boolean getBoolean() {
        if (mSelectBox != null && selectedText != null) {
            return true;
        } else {
            return false;
        }
    }
    SelectionListener selectedListner=null;
    public void setSelectionListener(SelectionListener selectedListner) {
        this.selectedListner=selectedListner;

    }


}
