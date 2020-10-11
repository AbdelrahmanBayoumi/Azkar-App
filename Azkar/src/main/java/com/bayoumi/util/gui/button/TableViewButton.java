/*
 * Code Clinic Solutions
 * PS-Restaurant System  * 
 */
package com.bayoumi.util.gui.button;

import com.jfoenix.controls.JFXButton;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;

/**
 *
 * @author Bayoumi
 */
public class TableViewButton extends JFXButton {
    
    public TableViewButton() {
        super();
        addDecoration();
    }
    
    public TableViewButton(String text) {
        super(text);
        addDecoration();
    }
    
    public TableViewButton(String text, Node graphic) {
        super(text, graphic);
        addDecoration();
    }
    
    private void addDecoration() {
        this.getStyleClass().add("table-btn");
        this.setFocusTraversable(false);
        if (this.getGraphic() != null) {
            this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }
    }
}
