package com.bayoumi.util.forms;

public class GoogleFormsUtil {

    public static void main(String[] args) {
        try {
            Feedback feedback = new Feedback(null, "abdo@gmail.com", 0, "content here\nanother line here");
            feedback.submitFeedback();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


}
