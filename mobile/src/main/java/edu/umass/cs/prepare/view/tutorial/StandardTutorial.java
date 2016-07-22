package edu.umass.cs.prepare.view.tutorial;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.lovoo.tutorialbubbles.TutorialScreen;

import edu.umass.cs.prepare.R;

/**
 * A standard tutorial sequence, which contains a description of each view and a next button, which should
 * point to the next StandardTutorial object. A null value for the next tutorial reference marks the
 * end of the tutorial sequence. All tutorial listeners will be informed when the tutorial
 * sequence has been constructed (see {@link StandardTutorial.TutorialListener#onReady(StandardTutorial)})
 * and when the tutorial has been completed (see {@link StandardTutorial.TutorialListener#onFinish(StandardTutorial)}).
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
public class StandardTutorial {

    /**
     * Listens for tutorial events, e.g. when the tutorial has been constructed or the user
     * has finished the tutorial.
     */
    public interface TutorialListener {
        void onReady(StandardTutorial tutorial);
        void onFinish(StandardTutorial tutorial);
    }

    /**
     * The tutorial handle.
     */
    private TutorialScreen tutorial;

    /**
     * Listens for relevant tutorial events.
     */
    private TutorialListener tutorialListener;

    /**
     * Sets the tutorial listener for catching relevant events.
     * @param tutorialListener the handle to the tutorial listener
     */
    public void setTutorialListener(TutorialListener tutorialListener){
        this.tutorialListener = tutorialListener;
    }

    /**
     * Initializes a tutorial sequence.
     * @param UI the user interface where the tutorial should be displayed
     * @param view
     * @param description
     * @param next the following tutorial in the sequence, i.e. the remaining sequence
     */
    public StandardTutorial(final Activity UI, final View view, final String description, final String nextButtonText, final StandardTutorial next){
        view.post(new Runnable() { //run on a separate thread associated with the view
            @Override
            public void run() {
                tutorial = new TutorialScreen.TutorialBuilder(R.layout.standard_tutorial_layout, view)
                        .setParentLayout(UI.getWindow().getDecorView())    // parent layout is necessary for layout approach, use decorView or a root relative layout
                        .setDismissible(false)                      // set if this bubble can be dismissed by clicking somewhere outside of its context
                        .addHighlightView(view, false)      // sets the view that should be explained
                        .setOnTutorialLayoutInflatedListener(new TutorialScreen.OnTutorialLayoutInflatedListener() {
                            // you can use this callback to bind the bubble layout and apply logic to it
                            @Override
                            public void onLayoutInflated(View view) {
                                final TextView tutorialText = (TextView) view.findViewById(R.id.tutorial_text);
                                tutorialText.setText(description);
                                Button nextButton = (Button) view.findViewById(R.id.tutorial_next_button);
                                nextButton.setText(nextButtonText);
                                nextButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        tutorial.dismissTutorial();
                                        if (next != null) {
                                            next.setTutorialListener(new TutorialListener() {
                                                @Override
                                                public void onReady(StandardTutorial tutorial) {
                                                    //DO NOTHING
                                                }

                                                @Override
                                                public void onFinish(StandardTutorial tutorial) {
                                                    if (tutorialListener != null)
                                                        tutorialListener.onFinish(StandardTutorial.this);
                                                }
                                            });
                                            next.showTutorial();
                                        } else {
                                            if (tutorialListener != null)
                                                tutorialListener.onFinish(StandardTutorial.this);
                                        }
                                    }
                                });
                            }
                        })
                        .build();
                if (tutorialListener != null)
                    tutorialListener.onReady(StandardTutorial.this);
            }
        });
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
    }
}
