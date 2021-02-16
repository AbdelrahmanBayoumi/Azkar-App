package com.bayoumi.util.forms;

import io.github.stepio.jgforms.Configuration;
import io.github.stepio.jgforms.Submitter;
import io.github.stepio.jgforms.answer.Builder;
import io.github.stepio.jgforms.question.MetaData;

import java.net.MalformedURLException;
import java.net.URL;

public class Feedback {
    private String name;
    private String mail;
    private int rate;
    private String content;

    public Feedback(String name, String mail, int rate, String content) {
        this.name = name;
        this.mail = mail; // TODO Validate mail + required
        this.rate = rate; // TODO validate rate
        this.content = content;
    }

    private URL buildFeedbackURL() throws MalformedURLException {
        Builder builder = Builder.formKey("1FAIpQLSdsHSZDURzLRCf1bxrMQ_onBRJ7BZvryU1_xxrXvr0Igh0SGg");
        if (this.getName() != null && !this.getName().equals("")) {
            builder.put(FeedbackForm.NAME, this.getName());
        }
        if (this.getMail() != null && !this.getMail().equals("")) {
            builder.put(FeedbackForm.EMAIL_ADDRESS, this.getMail());
        }
        if (this.getRate() != 0) {
            builder.put(FeedbackForm.RATE, this.getRate());
        }
        if (this.getContent() != null && !this.getContent().equals("")) {
            builder.put(FeedbackForm.CONTENT, this.getContent());
        }
        return builder.toUrl();
    }

    public void submitFeedback() throws MalformedURLException {
        URL url = buildFeedbackURL();
        System.out.println(url);
        Submitter submitter = new Submitter(
                new Configuration()
        );
        submitter.submitForm(url);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    private enum FeedbackForm implements MetaData {

        NAME(2005620554),
        EMAIL_ADDRESS(1045781291),
        RATE(1166974658),
        CONTENT(839337160);

        private final long id;

        FeedbackForm(long l) {
            this.id = l;
        }

        @Override
        public long getId() {
            return this.id;
        }
    }
}
