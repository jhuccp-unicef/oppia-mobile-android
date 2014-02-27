/* 
 * This file is part of OppiaMobile - http://oppia-mobile.org/
 * 
 * OppiaMobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * OppiaMobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with OppiaMobile. If not, see <http://www.gnu.org/licenses/>.
 */

package org.digitalcampus.oppia.widgets;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.mobile.quiz.InvalidQuizException;
import org.digitalcampus.mobile.quiz.Quiz;
import org.digitalcampus.mobile.quiz.model.QuizQuestion;
import org.digitalcampus.mobile.quiz.model.questiontypes.Description;
import org.digitalcampus.mobile.quiz.model.questiontypes.Essay;
import org.digitalcampus.mobile.quiz.model.questiontypes.Matching;
import org.digitalcampus.mobile.quiz.model.questiontypes.MultiChoice;
import org.digitalcampus.mobile.quiz.model.questiontypes.MultiSelect;
import org.digitalcampus.mobile.quiz.model.questiontypes.Numerical;
import org.digitalcampus.mobile.quiz.model.questiontypes.ShortAnswer;
import org.digitalcampus.oppia.activity.CourseActivity;
import org.digitalcampus.oppia.adapter.QuizFeedbackAdapter;
import org.digitalcampus.oppia.application.DbHelper;
import org.digitalcampus.oppia.model.Activity;
import org.digitalcampus.oppia.model.Course;
import org.digitalcampus.oppia.model.QuizFeedback;
import org.digitalcampus.oppia.widgets.quiz.DescriptionWidget;
import org.digitalcampus.oppia.widgets.quiz.EssayWidget;
import org.digitalcampus.oppia.widgets.quiz.MatchingWidget;
import org.digitalcampus.oppia.widgets.quiz.MultiChoiceWidget;
import org.digitalcampus.oppia.widgets.quiz.MultiSelectWidget;
import org.digitalcampus.oppia.widgets.quiz.NumericalWidget;
import org.digitalcampus.oppia.widgets.quiz.QuestionWidget;
import org.digitalcampus.oppia.widgets.quiz.ShortAnswerWidget;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class FeedbackWidget extends WidgetFactory {

	private static final String TAG = FeedbackWidget.class.getSimpleName();
	private ViewGroup container;
	private Quiz feedback;
	private String feedbackContent;
	public Button prevBtn;
	public Button nextBtn;
	private TextView qText;
	private LinearLayout questionImage;
	private boolean isOnResultsPage = false;
	private QuestionWidget qw;
	
	public static FeedbackWidget newInstance(Activity activity, Course course, boolean isBaseline) {
		FeedbackWidget myFragment = new FeedbackWidget();

		Bundle args = new Bundle();
		args.putSerializable(Activity.TAG, activity);
		args.putSerializable(Course.TAG, course);
		args.putBoolean(CourseActivity.BASELINE_TAG, isBaseline);
		myFragment.setArguments(args);

		return myFragment;
	}
	
	public FeedbackWidget() {

	}
	
	@SuppressWarnings("unchecked")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		prefs = PreferenceManager.getDefaultSharedPreferences(super.getActivity());
		View vv = super.getLayoutInflater(savedInstanceState).inflate(R.layout.widget_quiz, null);
		this.container = container;
		course = (Course) getArguments().getSerializable(Course.TAG);
		activity = ((Activity) getArguments().getSerializable(Activity.TAG));
		this.setIsBaseline(getArguments().getBoolean(CourseActivity.BASELINE_TAG));
		feedbackContent = ((Activity) getArguments().getSerializable(Activity.TAG)).getContents(prefs.getString(
				super.getActivity().getString(R.string.prefs_language), Locale.getDefault().getLanguage()));

		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		vv.setLayoutParams(lp);
		vv.setId(activity.getActId());
		if ((savedInstanceState != null) && (savedInstanceState.getSerializable("widget_config") != null)){
			setWidgetConfig((HashMap<String, Object>) savedInstanceState.getSerializable("widget_config"));
		}
		return vv;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable("widget_config", getWidgetConfig());
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		prevBtn = (Button) getView().findViewById(R.id.mquiz_prev_btn);
		nextBtn = (Button) getView().findViewById(R.id.mquiz_next_btn);
		qText = (TextView) getView().findViewById(R.id.question_text);
		questionImage = (LinearLayout) getView().findViewById(R.id.question_image);
	}
	
	@Override
	public void onResume(){
		super.onResume();
		if (this.feedback == null) {
			this.feedback = new Quiz();
			this.feedback.load(feedbackContent);
		}
		if (this.isOnResultsPage) {
			this.showResults();
		} else {
			this.showQuestion();
		}
	}
	
	public void showQuestion() {
		QuizQuestion q = null;
		try {
			q = this.feedback.getCurrentQuestion();
		} catch (InvalidQuizException e) {
			Toast.makeText(super.getActivity(), super.getActivity().getString(R.string.error_quiz_no_questions), Toast.LENGTH_LONG).show();
			e.printStackTrace();
			return;
		}
		qText.setVisibility(View.VISIBLE);
		// convert in case has any html special chars
		qText.setText(Html.fromHtml(q.getTitle()).toString());

		if (q.getProp("image") == null) {
			questionImage.setVisibility(View.GONE);
		} else {
			String fileUrl = course.getLocation() + q.getProp("image");
			// File file = new File(fileUrl);
			Bitmap myBitmap = BitmapFactory.decodeFile(fileUrl);
			File file = new File(fileUrl);
			ImageView iv = (ImageView) getView().findViewById(R.id.question_image_image);
			iv.setImageBitmap(myBitmap);
			iv.setTag(file);
		}

		if (q instanceof MultiChoice) {
			qw = new MultiChoiceWidget(super.getActivity(), getView(), container);
		} else if (q instanceof Essay) {
			qw = new EssayWidget(super.getActivity(), getView(),container);
		} else {
			Log.d(TAG, "Class for question type not found");
			return;
		}
		qw.setQuestionResponses(q.getResponseOptions(), q.getUserResponses());
		this.setProgress();
		this.setNav();
	}
	
	private void setNav() {
		nextBtn.setVisibility(View.VISIBLE);
		prevBtn.setVisibility(View.VISIBLE);

		if (this.feedback.hasPrevious()) {
			prevBtn.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// save answer
					saveAnswer();

					if (FeedbackWidget.this.feedback.hasPrevious()) {
						FeedbackWidget.this.feedback.movePrevious();
						showQuestion();
					}
				}
			});
			prevBtn.setEnabled(true);
		} else {
			prevBtn.setEnabled(false);
		}

		nextBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// save answer
				if (saveAnswer()) {
					String feedback = "";
					try {
						feedback = FeedbackWidget.this.feedback.getCurrentQuestion().getFeedback();
					} catch (InvalidQuizException e) {
						e.printStackTrace();
					}
					if (!feedback.equals("") && !isBaseline) {
						showFeedback(feedback);
					} else if (FeedbackWidget.this.feedback.hasNext()) {
						FeedbackWidget.this.feedback.moveNext();
						showQuestion();
					} else {
						showResults();
					}
				} else {
					CharSequence text = FeedbackWidget.super.getActivity().getString(R.string.widget_quiz_noanswergiven);
					int duration = Toast.LENGTH_SHORT;
					Toast toast = Toast.makeText(FeedbackWidget.super.getActivity(), text, duration);
					toast.show();
				}
			}
		});

		// set label on next button
		if (feedback.hasNext()) {
			nextBtn.setText(super.getActivity().getString(R.string.widget_quiz_next));
		} else {
			nextBtn.setText(super.getActivity().getString(R.string.widget_quiz_getresults));
		}
	}
	
	public void showResults() {

		// log the activity as complete
		isOnResultsPage = true;
		this.saveTracker();

		// save results ready to send back to the quiz server
		String data = feedback.getResultObject().toString();
		DbHelper db = new DbHelper(super.getActivity());
		db.insertQuizResult(data, course.getModId());
		db.close();
		Log.d(TAG, data);
		
		// load new layout
		View C = getView().findViewById(R.id.quiz_progress);
	    ViewGroup parent = (ViewGroup) C.getParent();
	    int index = parent.indexOfChild(C);
	    parent.removeView(C);
	    C = super.getActivity().getLayoutInflater().inflate(R.layout.widget_quiz_results, parent, false);
	    parent.addView(C, index);
		
		TextView title = (TextView) getView().findViewById(R.id.quiz_results_score);
		
		if (this.isBaseline) {
			TextView baselineExtro = (TextView) getView().findViewById(R.id.quiz_results_baseline);
			baselineExtro.setVisibility(View.VISIBLE);
			baselineExtro.setText(super.getActivity().getString(R.string.widget_quiz_baseline_completed));
		} 
		
		// TODO add TextView here to give overall feedback if it's in the quiz
		
		// Show restart or continue button
		Button restartBtn = (Button) getView().findViewById(R.id.quiz_results_button);
		
		restartBtn.setText(super.getActivity().getString(R.string.widget_quiz_baseline_goto_course));
		restartBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				FeedbackWidget.this.getActivity().finish();
			}
		});
	
	}
	
	private void setProgress() {
		TextView progress = (TextView) getView().findViewById(R.id.quiz_progress);
		try {
			if (this.feedback.getCurrentQuestion().responseExpected()) {
				progress.setText(super.getActivity().getString(R.string.widget_quiz_progress, feedback.getCurrentQuestionNo(),
						this.feedback.getTotalNoQuestions()));
			} else {
				progress.setText("");
			}
		} catch (InvalidQuizException e) {
			e.printStackTrace();
		}

	}
	
	
	private boolean saveAnswer() {
		try {
			List<String> answers = qw.getQuestionResponses(feedback.getCurrentQuestion().getResponseOptions());
			if (answers != null) {
				feedback.getCurrentQuestion().setUserResponses(answers);
				return true;
			}
			if (!feedback.getCurrentQuestion().responseExpected()) {
				return true;
			}
		} catch (InvalidQuizException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private void showFeedback(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(super.getActivity());
		builder.setTitle(super.getActivity().getString(R.string.feedback));
		builder.setMessage(msg);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface arg0, int arg1) {
				if (FeedbackWidget.this.feedback.hasNext()) {
					FeedbackWidget.this.feedback.moveNext();
					showQuestion();
				} else {
					showResults();
				}
			}
		});
		builder.show();
	}
	
	@Override
	protected boolean getActivityCompleted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void saveTracker() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getContentToRead() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HashMap<String, Object> getWidgetConfig() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setWidgetConfig(HashMap<String, Object> config) {
		// TODO Auto-generated method stub
		
	}

}
