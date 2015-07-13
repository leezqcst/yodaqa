package cz.brmlab.yodaqa.flow.dashboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import cz.brmlab.yodaqa.flow.dashboard.snippet.AnsweringSnippet;

/** A stateful question.  This question has been asked, can be referred to
 * and may or may not have been answered already.
 *
 * Important: Multiple threads can look and poke at a Question at once.
 * Be careful, always deal with it only through synchronized methods. */
public class Question {
	protected String id;
	protected String text;
	protected QuestionSummary summary = null;
	protected HashMap<Integer, AnswerSource> sources = new HashMap<>();
	protected List<QuestionAnswer> answers = new ArrayList<>();
	protected Map<Integer, AnsweringSnippet> snippets = new HashMap<>(); //key = ID of passage, value = Passage String
	protected boolean finished = false;
	/* Generation counts for various fields above, incremented every
	 * time they are modified. */
	protected int gen_sources = 0;
	protected int gen_answers = 0;

	protected static Gson gson = new Gson();

	public Question(String id, String text) {
		this.id = id;
		this.text = text;
	}

	/** @return the id */
	public synchronized String getId() { return id; }
	/** @return the text */
	public synchronized String getText() { return text; }

	/** @return the summary */
	public synchronized QuestionSummary getSummary() { return summary; }
	/** @param summary the summary to set */
	public synchronized void setSummary(QuestionSummary summary) {
		this.summary = summary;
	}

	/** @return the sources */
	public synchronized List<AnswerSource> getSources() { return new ArrayList<AnswerSource>(sources.values()); }
	public synchronized void addSource(AnswerSource source) {
		this.sources.put(source.getSourceID(),source);
		gen_sources ++;
	}

	public synchronized void setSourceState(int sourceID, int state) {
		sources.get(sourceID).setState(state);
				gen_sources++;
	}

	public synchronized boolean containsSource(int sourceID) {
		return sources.containsKey(sourceID);
	}

	/** @return the answer */
	public synchronized List<QuestionAnswer> getAnswers() { return answers; }
	public synchronized void addAnswer(QuestionAnswer qa) {
		/* Do trivial text-based deduplication. */
		QuestionAnswer dupe = null;
		for (QuestionAnswer qa2 : answers) {
			if (qa2.getText().equals(qa.getText())) {
				dupe = qa2;
				break;
			}
		}
		if (dupe != null)
			answers.remove(dupe);

		// pre-pend this answer, so that the latest are at the top
		this.answers.add(0, qa);
		gen_answers ++;
	}
	/** Reset the answer list, typically when have scored them.
	 * @param answer the answer to set */
	public synchronized void setAnswers(List<QuestionAnswer> answers) {
		this.answers = answers;
		gen_answers ++;
	}

	/** @return the finished */
	public synchronized boolean isFinished() { return finished; }
	/** @param finished the finished to set */
	protected synchronized void setFinished(boolean finished) {
		this.finished = finished;
	}

	public synchronized String toJson() {
		for (QuestionAnswer qa : answers ) {
			for (Integer snippetID : qa.getSnippetIDs()) {
				snippets.put(snippetID, QuestionDashboard.getInstance().getSnippet(snippetID));
			}
		}
		return gson.toJson(this);
	}
};
