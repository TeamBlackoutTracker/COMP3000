package com.example.robsh.comp3000.keyboard;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.util.Log;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;
import java.util.List;

//import com.example.android.dictionary.Dictionary;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.InputType;
import android.text.method.MetaKeyKeyListener;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import android.text.InputType;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.widget.ImageView;

import github.ankushsachdeva.emojicon.EmojiconGridView.OnEmojiconClickedListener;
import github.ankushsachdeva.emojicon.EmojiconsPopup;
import github.ankushsachdeva.emojicon.EmojiconsPopup.OnEmojiconBackspaceClickedListener;
import github.ankushsachdeva.emojicon.EmojiconsPopup.OnSoftKeyboardOpenCloseListener;
import github.ankushsachdeva.emojicon.emoji.Emojicon;

import com.example.robsh.comp3000.R;


/**
 * Created by robsh on 2016-12-01.
 */

public class MyKeyboard extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener, SpellCheckerSession.SpellCheckerSessionListener, OnClickListener {




    static final boolean PROCESS_HARD_KEYS = true;

    private static final String LOG_TAG = "softkeyboard";

    // private static final String TAG = "Update";

    private InputMethodManager mInputMethodManager;

    // private EmojiKeyboardView mInputView;
    private CandidateView mCandidateView;
    private CompletionInfo[] mCompletions;

    private StringBuilder mComposing = new StringBuilder();
    private boolean mPredictionOn;
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private long mLastShiftTime;
    private long mMetaState;

    private boolean mSave = false;

    private ArrayList<String> mCandidateList = new ArrayList<String>();

    private boolean mFlag = false;

    private boolean mPick = false;

    private String mSaveWord = null;

    private int mIndex = -1;

    private boolean mSpace = false;

    private View mCandidatesParent;

    private KeyboardView mInputView;
    private static Dictionary mDictionary = null;

    public static ImageView imageview;

    // private onEmojiClickListener onEmoji;
    // private EmojiKeyboard emojiKeyboard;

    private InputConnection inputConnection;

    private EmojiconsPopup popupWindow;

    private Keyboard mSymbolsKeyboard;
    private Keyboard mSymbolsShiftedKeyboard;
    private Keyboard mQwertyKeyboard;

    private Keyboard mCurKeyboard;

    private String mWordSeparators;

    private SpellCheckerSession mScs;
    private List<String> mSuggestions;



    private KeyboardView keyView;
//    private  mCandidateView;
    private Keyboard keyboard;
    private Keyboard shifted_keyboard;

    private boolean capsLock = false;

    @Override
    public View onCreateInputView() {
        keyView = (KeyboardView) getLayoutInflater().inflate(R.layout.input, null);
        keyboard = new Keyboard(this, R.xml.qwerty_keyboard);
        shifted_keyboard = new Keyboard(this, R.xml.shifted_symbols);
        keyView.setKeyboard(keyboard);
        keyView.setOnKeyboardActionListener(this);

        return keyView;
    }

    @Override
    public View onCreateCandidatesView() {
        LayoutInflater li = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // mCandidateView.setBackgroundColor(Color.rgb(75, 170, 201));
        ViewGroup wordBar = (ViewGroup) li.inflate(R.layout.candidates, null);

        imageview = (ImageView) wordBar.findViewById(R.id.close_suggestions_strip_icon);

        // Log.i("buttonHeight", button.getWidth() + )
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        setCandidatesViewShown(true);
        imageview.setOnClickListener( this);

        wordBar.addView(mCandidateView);
        return wordBar;

    }



    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() > 0) {

            if (mPredictionOn && mSuggestions != null && index >= 0) {
                mComposing.replace(0, mComposing.length(), mSuggestions.get(index));
            }
            commitTyped(getCurrentInputConnection());

        }
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
            updateCandidates();
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null
                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mInputView.setShifted(mCapsLock || caps != 0);
        }
    }

    private void playClick(int keyCode){
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        switch (keyCode) {
            case 32:
                audio.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR);
                break;

            case 10:
                audio.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN);
                break;

            case Keyboard.KEYCODE_DELETE:
                audio.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE);
                break;

            default:
                audio.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
                break;

        }
    }

//    @Override public View onCreateCandidatesView() {
//        mCandidateView = new CandidateView(this);
//        mCandidateView.setService(this);
//        return mCandidateView;
//    }

    @Override
    public void onPress(int primaryCode) {

    }

    public void onMailPress(int primaryCode){

    }

    @Override
    public void onRelease(int primaryCode) {

    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection input = getCurrentInputConnection();

        if (primaryCode == 32) {
            LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            View popupView = layoutInflater.inflate(R.layout.emojiview, null);
            popupWindow = new EmojiconsPopup(popupView, this);
            // final PopupWindow popupWindow = new PopupWindow();
            popupWindow.setSizeForSoftKeyboard();
            popupWindow.setSize(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            popupWindow.showAtLocation(mInputView.getRootView(), Gravity.BOTTOM, 0, 0);

            // Bring soft keyboard up : NOT WORKING
            final InputMethodManager mInputMethodManager = (InputMethodManager) getBaseContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);

            mInputMethodManager.showSoftInput(popupView, 0);

            // If the text keyboard closes, also dismiss the emoji popup
            popupWindow.setOnSoftKeyboardOpenCloseListener(new OnSoftKeyboardOpenCloseListener() {

                @Override
                public void onKeyboardOpen(int keyBoardHeight) {

                }

                @Override
                public void onKeyboardClose() {
                    if (popupWindow.isShowing())
                        popupWindow.dismiss();
                }
            });

            popupWindow.setOnEmojiconClickedListener(new OnEmojiconClickedListener() {

                @Override
                public void onEmojiconClicked(Emojicon emojicon) {
                    mComposing.append(emojicon.getEmoji());
                    commitTyped(getCurrentInputConnection());

                    customToast("" + emojicon.getEmoji());
                }
            });

            popupWindow.setOnEmojiconBackspaceClickedListener(new OnEmojiconBackspaceClickedListener() {

                @Override
                public void onEmojiconBackspaceClicked(View v) {
                    KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
                    customToast(" " + event);
                    handleBackspace();
                }
            });

            playClick(primaryCode);
            switch (primaryCode) {
                case Keyboard.KEYCODE_DELETE:
                    input.deleteSurroundingText(1, 0);
                    break;
                case Keyboard.KEYCODE_SHIFT:
                    capsLock = !capsLock;
                    keyboard.setShifted(capsLock);
                    keyView.invalidateAllKeys();
                    break;
                case Keyboard.KEYCODE_DONE:
                    input.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                    break;
                case Keyboard.KEYCODE_MODE_CHANGE:

                    Keyboard current = keyView.getKeyboard();
                    if (current == shifted_keyboard) {
                        keyView.setKeyboard(keyboard);
                    } else {
                        keyView.setKeyboard(shifted_keyboard);
                    }
                    break;

                default:
                    char code = (char) primaryCode;
                    if (Character.isLetter(code) && capsLock) {
                        code = Character.toUpperCase(code);
                    }
                    input.commitText(String.valueOf(code), 1);

            }
        }
    }
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    private void showEmoji() {
        LayoutInflater layoutInflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = layoutInflater.inflate(R.id.root_view, null);

        popupWindow = new EmojiconsPopup(popupView, this);
        // final PopupWindow popupWindow = new PopupWindow();
        popupWindow.setSizeForSoftKeyboard();
        popupWindow.setSize(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        popupWindow.showAtLocation(mInputView.getRootView(), Gravity.BOTTOM, 0, 0);

        // Bring soft keyboard up : NOT WORKING
        final InputMethodManager mInputMethodManager = (InputMethodManager) getBaseContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        mInputMethodManager.showSoftInput(popupView, 0);

        // If the text keyboard closes, also dismiss the emoji popup
        popupWindow.setOnSoftKeyboardOpenCloseListener(new OnSoftKeyboardOpenCloseListener() {

            @Override
            public void onKeyboardOpen(int keyBoardHeight) {

            }

            @Override
            public void onKeyboardClose() {
                if (popupWindow.isShowing())
                    popupWindow.dismiss();
            }
        });

        popupWindow.setOnEmojiconClickedListener(new OnEmojiconClickedListener() {

            @Override
            public void onEmojiconClicked(Emojicon emojicon) {
                mComposing.append(emojicon.getEmoji());
                commitTyped(getCurrentInputConnection());

                customToast("" + emojicon.getEmoji());
            }
        });

        popupWindow.setOnEmojiconBackspaceClickedListener(new OnEmojiconBackspaceClickedListener() {

            @Override
            public void onEmojiconBackspaceClicked(View v) {
                KeyEvent event = new KeyEvent(0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
                customToast(" " + event);
                handleBackspace();
            }
        });

    }

    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateCandidates();
        } else if (length > 0) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
            updateCandidates();
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
    }


    private void customToast(String tag) {
        Toast.makeText(getApplicationContext(), tag, Toast.LENGTH_SHORT).show();
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        if (!mCompletionOn) {
            if (mComposing.length() > 0) {
                ArrayList<String> list = new ArrayList<String>();
                //list.add(mComposing.toString());
                Log.d("SoftKeyboard", "REQUESTING: " + mComposing.toString());
                mScs.getSentenceSuggestions(new TextInfo[] {new TextInfo(mComposing.toString())}, 5);
                setSuggestions(list, true, true);
            } else {
                setSuggestions(null, false, false);
            }
        }
    }


    public void setSuggestions(List<String> suggestions, boolean completions,
                               boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        mSuggestions = suggestions;
        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
    }

    @Override
    public void onText(CharSequence text) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }


    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }

    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {

    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {

    }

    @Override
    public void onClick(View v) {
        customToast("Image Emoji");
        showEmoji();
    }
}
