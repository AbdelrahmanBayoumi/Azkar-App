package com.bayoumi.util.forms;

import io.github.stepio.jgforms.Configuration;
import io.github.stepio.jgforms.Submitter;
import io.github.stepio.jgforms.answer.Builder;
import io.github.stepio.jgforms.question.MetaData;

import java.net.MalformedURLException;
import java.net.URL;

public class Feedback {

    private enum FeedbackForm implements MetaData {
        TYPE(814257748),
        SUBJECT(1244495847),
        EMAIL(1620011711),
        OS(1968101612),
        DETAILS(1071993344),
        LOG_PATH(788346873);

        private final long id;

        FeedbackForm(long l) {
            this.id = l;
        }

        @Override
        public long getId() {
            return this.id;
        }
    }

    private final String type;
    private final String subject;
    private final String email;
    private final String details;
    private final String os;
    private final String logPath;

    public Feedback(String type, String subject, String email, String details, String os, String logPath) {
        this.type = type;
        this.subject = subject;
        this.email = email;
        this.details = details;
        this.os = os;
        this.logPath = logPath;
    }

    public String getType() {
        return type;
    }

    public String getSubject() {
        return subject;
    }

    public String getEmail() {
        return email;
    }

    public String getDetails() {
        return details;
    }

    public String getOS() {
        return os;
    }

    public String getLogPath() {
        return logPath;
    }

    private URL buildFeedbackURL() throws MalformedURLException {
        Builder builder = Builder.formKey("1FAIpQLSdgpuszSwl_sGLbGHgFGZo1zB0tJ05QSYlmxCZQMaMIuElrRw");
        if (this.getType() != null && !this.getType().isEmpty()) {
            builder.put(FeedbackForm.TYPE, this.getType());
        }
        if (this.getSubject() != null && !this.getSubject().isEmpty()) {
            builder.put(FeedbackForm.SUBJECT, this.getSubject());
        }
        if (this.getEmail() != null && !this.getEmail().isEmpty()) {
            builder.put(FeedbackForm.EMAIL, this.getEmail());
        }
        if (this.getOS() != null && !this.getOS().isEmpty()) {
            builder.put(FeedbackForm.OS, this.getOS());
        }
        if (this.getDetails() != null && !this.getDetails().isEmpty()) {
            builder.put(FeedbackForm.DETAILS, this.getDetails());
        }
        if (this.getLogPath() != null && !this.getLogPath().isEmpty()) {
            builder.put(FeedbackForm.LOG_PATH, this.getLogPath());
        }
        return builder.toUrl();
    }

    public void submitFeedback() throws MalformedURLException {
        URL url = buildFeedbackURL();
        Submitter submitter = new Submitter(
                new Configuration()
        );
        submitter.submitForm(url);
    }


}
