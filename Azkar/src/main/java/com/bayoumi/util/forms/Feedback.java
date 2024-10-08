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
        META_DATA(788346873);

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
    private final String metaData;

    public Feedback(String type, String subject, String email, String details, String os, String metaData) {
        this.type = type;
        this.subject = subject;
        this.email = email;
        this.details = details;
        this.os = os;
        this.metaData = metaData;
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

    public String getMetaData() {
        return metaData;
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
        if (this.getMetaData() != null && !this.getMetaData().isEmpty()) {
            builder.put(FeedbackForm.META_DATA, this.getMetaData());
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
