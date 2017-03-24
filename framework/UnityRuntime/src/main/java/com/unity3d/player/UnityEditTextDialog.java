package com.unity3d.player;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

public final class UnityEditTextDialog extends Dialog implements TextWatcher, View.OnClickListener {
    private Context mContext = null;
    private UnityPlayer mUnitPlayer = null;
    private static int mTextColor = -570425344;
    private static int d = 1627389952;
    private static int e = -1;

    public UnityEditTextDialog(Context context, UnityPlayer unityPlayer, String text, int index, boolean isAutoCorrect, boolean isSupportMutiLines, boolean isValidPassword, String hint) {
        super(context);
        this.mContext = context;
        this.mUnitPlayer = unityPlayer;
        this.getWindow().setGravity(80);
        this.getWindow().requestFeature(1);
        this.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        this.setContentView(this.createSoftInputView());
        this.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        this.getWindow().clearFlags(2);
        EditText editText = (EditText)this.findViewById(1057292289);
        Button button = (Button)this.findViewById(1057292290);
        this.init(editText, text, index, isAutoCorrect, isSupportMutiLines, isValidPassword, hint);
        button.setOnClickListener(this);
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener(){

            public final void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    UnityEditTextDialog.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
    }

    private void init(EditText editText, String context, int index, boolean isAutoCorrect, boolean isSupportMutiLines, boolean isValidPassword, String hint) {
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setText(context);
        editText.setHint(hint);
        editText.setHintTextColor(d);
        editText.setInputType(UnityEditTextDialog.getInputType(index, isAutoCorrect, isSupportMutiLines, isValidPassword));
        editText.addTextChangedListener(this);
        editText.setClickable(true);
        if (!isSupportMutiLines) {
            editText.selectAll();
        }
    }

    public final void afterTextChanged(Editable editable) {
        this.mUnitPlayer.reportSoftInputStr(editable.toString(), 0, false);
    }

    public final void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public final void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    private static int getInputType(int index, boolean isAutoCorrect, boolean isSupportMutiLines, boolean isValidPassword) {
        int type = (isAutoCorrect ? InputType.TYPE_TEXT_FLAG_AUTO_CORRECT : InputType.TYPE_TEXT_VARIATION_NORMAL)
                | (isSupportMutiLines ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : InputType.TYPE_TEXT_VARIATION_NORMAL)
                | (isValidPassword ? InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_TEXT_VARIATION_NORMAL);
        if (index < 0 || index > 7) {
            return type;
        }
        int[] arrn = new int[]{InputType.TYPE_CLASS_TEXT,
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_CLASS_TEXT,
                InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_MASK_VARIATION | InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_CLASS_NUMBER,
                InputType.TYPE_DATETIME_VARIATION_DATE | InputType.TYPE_CLASS_TEXT,
                InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE,
                InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_CLASS_TEXT,
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_CLASS_TEXT};
        return type | arrn[index];
    }

    private void clearEditText(String content, boolean isCanceled) {
        Selection.removeSelection((Spannable)((EditText)this.findViewById(1057292289)).getEditableText());
        this.mUnitPlayer.reportSoftInputStr(content, 1, isCanceled);
    }

    public void onClick(View view) {
        this.clearEditText(this.getContent(), false);
    }

    public void onBackPressed() {
        this.clearEditText(this.getContent(), true);
    }

    protected View createSoftInputView() {
        RelativeLayout relativeLayout = new RelativeLayout(this.mContext);
        relativeLayout.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        relativeLayout.setBackgroundColor(e);
        EditText editText = new EditText(this.mContext){

            public final boolean onKeyPreIme(int keyCode, KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    UnityEditTextDialog.this.clearEditText(UnityEditTextDialog.this.getContent(), true);
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_SEARCH) {
                    return true;
                }
                return super.onKeyPreIme(keyCode, keyEvent);
            }

            public final void onWindowFocusChanged(boolean bl) {
                super.onWindowFocusChanged(bl);
                if (bl) {
                    ((InputMethodManager)UnityEditTextDialog.this.mContext.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput((View)this, 0);
                }
            }
        };
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_BASELINE);
        layoutParams.addRule(RelativeLayout.LEFT_OF, 1057292290);
        editText.setLayoutParams(layoutParams);
        editText.setTextColor(mTextColor);
        editText.setId(1057292289);
        relativeLayout.addView(editText);
        Button button = new Button(this.mContext);
        button.setText(this.mContext.getResources().getIdentifier("ok", "string", "android"));
        layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        button.setLayoutParams(layoutParams);
        button.setId(1057292290);
        button.setBackgroundColor(0);
        button.setTextColor(mTextColor);
        relativeLayout.addView(editText);
        ((EditText)relativeLayout.findViewById(1057292289)).setOnEditorActionListener(new TextView.OnEditorActionListener(){

            public final boolean onEditorAction(TextView textView, int n2, KeyEvent keyEvent) {
                if (n2 == 6) {
                    UnityEditTextDialog.this.clearEditText(UnityEditTextDialog.this.getContent(), false);
                }
                return false;
            }
        });
        editText.setPadding(16, 16, 16, 16);
        return editText;
    }

    private String getContent() {
        EditText editText = (EditText)this.findViewById(1057292289);
        if (editText == null) {
            return null;
        }
        return editText.getText().toString().trim();
    }

    public void selectText(String content) {
        EditText editText = (EditText)this.findViewById(1057292289);
        if (editText != null) {
            editText.setText(content);
            editText.setSelection(content.length());
        }
    }

}

