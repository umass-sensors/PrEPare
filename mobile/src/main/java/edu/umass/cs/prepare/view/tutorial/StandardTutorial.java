package edu.umass.cs.prepare.view.tutorial;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.lovoo.tutorialbubbles.TutorialScreen;

import edu.umass.cs.prepare.R;

/**
 * A standard tutorial sequence, which contains a description of each view and a next button, which should
 * point to the next StandardTutorial object. A null value for the next tutorial reference marks the
 * end of the tutorial sequence. All tutorial listeners will be informed when the tutorial
 * sequence has been constructed (see {@link StandardTutorial.TutorialListener#onReady(StandardTutorial)})
 * and when the tutorial has been completed (see {@link StandardTutorial.TutorialListener#onComplete(StandardTutorial)}).
 *
 * @author Sean Noran
 * @affiliation University of Massachusetts Amherst
 *
 * @see TutorialScreen
 * @see StandardTutorial.TutorialListener
 * @see View
 * @see TutorialScreen.TutorialBuilder
 *
 */
public class StandardTutorial implements TutorialScreen.OnTutorialLayoutInflatedListener {

    /**
     * Listens for tutorial events, e.g. when the tutorial has been constructed or the user
     * has finished the tutorial.
     */
    public interface TutorialListener {
        void onReady(StandardTutorial tutorial);
        void onComplete(StandardTutorial tutorial);
    }

    /**
     * The tutorial handle.
     */
    private TutorialScreen tutorial;

    /**
     * The next tutorial in the sequence.
     */
    private StandardTutorial next;

    /**
     * Listens for relevant tutorial events.
     */
    private TutorialListener tutorialListener;

    private boolean isReady = false;

    private View view;

    private int layoutId;

    private String description;

    private String buttonText;

    private boolean buttonEnabled;

    protected Activity UI;

    /**
     * Sets the tutorial listener for catching relevant events.
     * @param tutorialListener the handle to the tutorial listener
     */
    public StandardTutorial setTutorialListener(TutorialListener tutorialListener){
        this.tutorialListener = tutorialListener;
        if (isReady)
            this.tutorialListener.onReady(this);
        return this;
    }

    /**
     * Initializes a tutorial sequence.
     * @param UI the user interface where the tutorial should be displayed
     */
    public StandardTutorial(final Activity UI, @NonNull final View view){
        this.UI = UI;
        this.view = view;
        layoutId = R.layout.tutorial_standard;
        buttonText = UI.getString(R.string.tutorial_finish);
        buttonEnabled = true;
        next = null; // indicates no next
    }

    public StandardTutorial setView(@NonNull final View view){
        this.view = view;
        return this;
    }

    public StandardTutorial setLayout(final int layoutId){
        this.layoutId = layoutId;
        return this;
    }

    public StandardTutorial setDescription(String description){
        this.description = description;
        return this;
    }

    public StandardTutorial setButtonText(String buttonText){
        this.buttonText = buttonText;
        return this;
    }

    public StandardTutorial enableButton(boolean enabled){
        this.buttonEnabled = enabled;
        return this;
    }

    public StandardTutorial setNext(@Nullable StandardTutorial next){
        this.next = next;
        return this;
    }

    @Override
    public void onLayoutInflated(View view){
        final TextView tutorialText = (TextView) view.findViewById(R.id.tutorial_text);
        final ImageView nextButton = (ImageView) view.findViewById(R.id.tutorial_next_button);

        tutorialText.setText(description);
        if (!buttonEnabled){
            nextButton.setVisibility(View.GONE);
        }else {
            nextButton.setVisibility(View.VISIBLE);
            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tutorial.dismissTutorial();
                    onDismissed();
                }
            });
            nextButton.setContentDescription(buttonText);
        }
    }

    public StandardTutorial build(){
        view.post(new Runnable() { //run on a separate thread associated with the view
            @Override
            public void run() {
                tutorial = new TutorialScreen.TutorialBuilder(layoutId, view)
                        .setTutorialBackgroundColor(ContextCompat.getColor(UI, R.color.colorNotification))
                        .setParentLayout(UI.getWindow().getDecorView())    // parent layout is necessary for layout approach, use decorView or a root relative layout
                        .setDismissible(false)                      // set if this bubble can be dismissed by clicking somewhere outside of its context
                        .addHighlightView(view, false)      // sets the view that should be explained
                        .setOnTutorialLayoutInflatedListener(StandardTutorial.this)
                        .build();
                if (tutorialListener != null)
                    tutorialListener.onReady(StandardTutorial.this);
                isReady = true; //in case onReady is not called, we can use this for a tutorial listener added afterward
            }
        });
        return this;
    }

    public static void buildSequence(StandardTutorial... tutorials){
        for (int i = 0; i < tutorials.length-1; i++){
            tutorials[i].setNext(tutorials[i+1]);
            tutorials[i].build();
        }
        tutorials[tutorials.length-1].build();
    }

    private void onDismissed(){
        if (next != null) {
            next.setTutorialListener(new TutorialListener() {
                @Override
                public void onReady(StandardTutorial tutorial) {
                    //DO NOTHING
                }

                @Override
                public void onComplete(StandardTutorial tutorial) {
                    if (tutorialListener != null)
                        tutorialListener.onComplete(StandardTutorial.this);
                }
            });
            next.showTutorial();
        } else {
            if (tutorialListener != null)
                tutorialListener.onComplete(StandardTutorial.this);
        }
        isReady = false;
    }

    /**
     * Shows the tutorial sequence
     */
    public void showTutorial(){
        tutorial.showTutorial();
    }

    /**
     * Dismisses the tutorial sequence
     */
    public void dismiss() {
        tutorial.dismissTutorial();
        onDismissed();
    }
}
