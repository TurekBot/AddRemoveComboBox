package tech.ugma.customcomponents;
/*
 * Created by Bradley Turek, on 5/10/17 9:07 AM.
 * Copyright (c) 2017 All rights reserved.
 *
 *  Last modified 5/10/17 9:07 AM
 */

import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;

import java.text.Collator;
import java.util.Comparator;
import java.util.Objects;


/**
 * A ComboBox you can add and remove items from.
 * <p>
 * Items are removed by clicking the 'x' next to the item to be removed. It's important
 * that the developer implement the removalAction by calling setRemovalAction; otherwise nothing will
 * happen. See setRemovalAction's documentation for a tip on removing the item who's 'x' was clicked.
 * <p>
 * Items are added by clicking the '+' at the bottom of the list. This plus button, or add button as
 * I'll call it will be automatically kept at the bottom of the list, but (for now) needs to be added manually.
 * You add the "add button" by including `AddRemoveButton.ADD_CELL` in your item list.
 * <p>
 * It's important that the developer implement the additionAction by
 * calling setAdditionAction; otherwise nothing will happen.
 * <p>
 * Removable cells are implemented using a custom class called
 * AddRemoveListCell (accessible via AddRemoveComboBox.AddRemoveListCell) which is conveniently
 * contained herein.
 */
@SuppressWarnings("WeakerAccess")
public class AddRemoveComboBox extends ComboBox<String> {

    /**
     * For convenience in adding the "Add Button" to the list of choices.
     */
    public static final String ADD_CELL = "Add";

    /**
     * This will keep the addOption (the little '+') at the bottom of the list
     */
    private Comparator<String> addOptionComparator;
    /**
     * This string is set by the user; any option within the AddRemoveComboBox's list that matches
     * this string will be turned into a "add-a-cell" list cell.
     */
    private String addOption = "Add";
    /**
     * Happens when the user removes something from the list.
     * <p>
     * Needs to be set by the user before the AddRemoveComboBox is used.
     */
    private EventHandler<ActionEvent> removalAction = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            throw new UnsupportedOperationException("The Removal Action needs to be implemented first!" +
                    " Try calling buttonedComboBox.setRemovalAction(new EventHandler<ActionEvent>() { ... }); " +
                    "before using the AddRemoveComboBox");
        }
    };
    /**
     * Happens when the user adds something to the list.
     * <p>
     * Needs to be set by the user before the AddRemoveComboBox is used.
     */
    private EventHandler<ActionEvent> additionAction = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            throw new UnsupportedOperationException("The Addition Action needs to be implemented first!" +
                    " Try calling buttonedComboBox.setAdditionAction(new EventHandler<ActionEvent>() { ... }); " +
                    "before using the AddRemoveComboBox");
        }
    };
    /**
     * Keeps track of whether the list was just sorted or not. If it was, the listener on the
     * item list will make sure not to sort it again. See tech.ugma.customcomponents.AddRemoveComboBox#initAddOptionManager()
     */
    private boolean wasJustSorted;

    /**
     * Controls whether or not the AddRemoveComboBox should sort the rest of the items (exluding the
     * addOption) alphabetically.
     *
     * I've got it set to false by default, because normal ComboBox behavior is to *not* sort items.
     */
    private boolean sortAlphabetically = false;

    /**
     * Just calls the other constructor, but with an empty list.
     */
    public AddRemoveComboBox() {
        this(FXCollections.emptyObservableList());
    }

    /**
     * Don't forget to implement the removalAction and additionAction (otherwise nothing will happen
     * when you click the '+' or 'x'.
     *
     * @param list of items you want the AddRemoveComboBox to contain
     */
    public AddRemoveComboBox(ObservableList<String> list) {
        super(list);

        //Make the comparator that will keep the addOption at the bottom of the list
        addOptionComparator = initAddOptionComparator();

        //Listen for every time the list is changed; when it is, re-sort the items,
        //making sure the addOption is at the bottom.
        list.addListener(initAddOptionManager());

        //Make and give the cell factory to the combo box. The cell factory
        this.setCellFactory(initCellFactory());

        //Without this, the ButtonedComboBx will hide before the click registers to the button.
        this.setSkin(initCustomSkin());

        //Take care of clicks that by definition don't mean anything
        this.getSelectionModel().selectedItemProperty().addListener(initBadClickListener());

        //By default, select first option
        this.getSelectionModel().selectFirst();

    }

    /**
     * As a rule, it is assumed that the addOption is desired at the bottom of the list.
     * The comparator that this returns should hopefully ensure just that.
     *
     * @return a comparator that will always consider the addOption as smaller
     */
    private Comparator<String> initAddOptionComparator() {
        return new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {

                if (Objects.equals(addOption, o1)) {
                    //If the addOption is o1 (the first argument), return positive, telling the
                    //comparator that o1 is greater.
                    return Integer.MAX_VALUE;
                } else if (Objects.equals(addOption, o2)) {

                    //If the addOption is o2 (the second argument), return negative, telling the
                    //comparator that addOption is lesser.
                    return Integer.MIN_VALUE;
                } else {

                    if (sortAlphabetically) {
                        //Otherwise, sort things in alphabetical order.
                        return Collator.getInstance().compare(o1, o2);
                    } else {
                        return 0;
                    }
                }
            }
        };
    }

    /**
     * Makes an invalidation listener that keeps the addOption at the bottom of the list.
     *
     * @return an invalidation listener that keeps the addOption at the bottom of the list.
     */
    private InvalidationListener initAddOptionManager() {
        return new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (wasJustSorted) {
                    //If the list was just sorted, we won't sort the list again.

                    //Just set it to false for the next time when we will want to sort the list.
                    wasJustSorted = false;
                } else {

                    //It's important that this, despite it's name, be changed *before* the list
                    //is sorted. This is true because once the list is sorted, the InvalidationListener
                    //is immediately fired before continuing on to the next line.
                    wasJustSorted = true;

                    //sort list
                    AddRemoveComboBox.this.getItems().sort(addOptionComparator);
                }
            }


        };
    }

    /**
     * Instead of just putting the String in a plain ol' ListCell, like normal, we'll put the String
     * into a custom subclass of ListCell that has a place for the string as well as a button to click.
     */
    private Callback<ListView<String>, ListCell<String>> initCellFactory() {

        return new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {

                //This class is what will contain each entry in the list; it has
                //two parts, the label and a button.
                AddRemoveListCell customCell = new AddRemoveListCell();

                //Make sure the combo box hides itself after the cell is clicked.
                //This is necessary because we disabled the hide on click with the custom skin.
                customCell.setOnMousePressed(event -> {
                    AddRemoveComboBox.this.hide();
                });

                //Gives the user the ability to remove carrier choices
                customCell.setRemoveButtonAction(removalAction);


                //Give user the ability to add carriers
                customCell.setAddButtonAction(additionAction);


                //Finally, return the customCell, all gussied up.
                return customCell;
            }
        };
    }

    /**
     * Makes a custom skin.
     * <p>
     * We have to make a custom skin, otherwise the ButtonedComboBx disappears before the click on the
     * add/removal button is registered.
     *
     * @return a custom skin that will stop the ButtonedComboBx from hiding.
     */
    private ComboBoxListViewSkin<String> initCustomSkin() {

        // We have to make a custom skin, otherwise the ButtonedComboBx disappears before the click on the
        // add/removal button is registered.

        return new ComboBoxListViewSkin<String>(AddRemoveComboBox.this) {
            @Override
            protected boolean isHideOnClickEnabled() {
                return false;
            }
        };
    }

    /**
     * When the user selects the add-button cell, we don't really want to do anything. (If you
     * do want to do something, go ahead and change this accordingly). We want to wait until they
     * click the little plus sign. It should be at least a little intuitive because their cursor
     * changes to a hand when they mouseover the button.
     *
     * @return a change listener that takes care of clicks we don't want
     */
    private ChangeListener<? super String> initBadClickListener() {

        return (observable, oldValue, selectedValue) -> {

            if (Objects.equals(selectedValue, addOption)) {
                //This will happen if the user selects the add-button cell, without clicking
                // the add-button.

                //The following needs to be in a runLater because
                // you cannot change the selection while a selection change
                // is still being processed
                Platform.runLater(() -> {
                    AddRemoveComboBox.this.getSelectionModel().clearSelection();
                    AddRemoveComboBox.this.valueProperty().set(null);
                });
                return;
            }

            //Otherwise, it's a fine click and you can select the item.
            if (selectedValue != null) {
                AddRemoveComboBox.this.getSelectionModel().select(selectedValue);
            }
        };
    }

    /**
     * Here you provide the ButtonedComboBx with instructions as to what it should do when a user
     * presses the remove button (the 'x').
     * <p>
     * To get to the Removable List Cell, try something like
     * <pre>
     * <code>Button button = (Button) event.getSource();
     * HBox hBox = (HBox) button.getParent();
     * AddRemoveComboBox.AddRemoveListCell cell = (AddRemoveComboBox.AddRemoveListCell) hBox.getParent();</code>
     * </pre>
     * It's important that you set this up before using the ButtonedComboBx.
     *
     * @param removalAction any other actions that should be taken when an item is removed.
     */
    public void setRemovalAction(EventHandler<ActionEvent> removalAction) {
        this.removalAction = removalAction;
    }

    /**
     * Here you provide the ButtonedComboBx with instructions as to what it should do when a user
     * presses the add button (the '+').
     * <p>
     * It's important that you set this up before using the ButtonedComboBx.
     *
     * @param additionAction any other actions that should be taken when an item is added.
     */
    public void setAdditionAction(EventHandler<ActionEvent> additionAction) {
        this.additionAction = additionAction;
    }

    /**
     * By default, set to "Add". Any option within the AddRemoveComboBox's list that matches
     * this string will be turned into a "add-a-cell" list cell.
     *
     * @return the current add option.
     */
    public String getAddOption() {
        return addOption;
    }

    /**
     * By default, set to "Add". Any option within the AddRemoveComboBox's list that matches
     * this string will be turned into a "add-a-cell" list cell.
     */
    public void setAddOption(String addOption) {
        this.addOption = addOption;
    }

    /**
     * Controls whether or not the list is sorted alphabetically. (This excludes the addOption,
     * which is always relegated to the bottom.)
     * @return true if the list *will* be sorted alphabetically, false otherwise.
     */
    public boolean isSortAlphabetically() {
        return sortAlphabetically;
    }


    /**
     * Set this to true if you want the list to be sorted alphabetically. The addOption will
     * still get put at the bottom, just the rest of the items will be put in alphabetical order.
     * @param sortAlphabetically true = do sort alphabetically; false = do not sort alphabetically
     */
    public void setSortAlphabetically(boolean sortAlphabetically) {
        this.sortAlphabetically = sortAlphabetically;
    }

    ///////////////////////////////
    ///REMOVABLE LIST CELL CLASS///
    ///////////////////////////////

    /**
     * A list cell with both a label and a button.
     * <p>
     * Used inside of AddRemoveComboBox to display list entries with both their label as well as a button.
     * <p>
     * Code adapted from code by 'sillyfly'. http://stackoverflow.com/a/36145822/5432315
     */
    public class AddRemoveListCell extends ListCell<String> {

        /**
         * This is the node that will display the text and the remove-button.
         */
        private HBox box;

        /**
         * Each item in the combo boxes item list will be put here as a label
         */
        private Label label = new Label();

        /**
         * An image button displayed to the far right of the cell.
         * This is formatted with internal CSS to be either an add- or remove- button.
         */
        private Button button = new Button();

        /**
         * Will happen when the user clicks a remove button; must be implemented by developer before use.
         */
        private EventHandler<ActionEvent> removeEvent;


        /**
         * Will happen when the user clicks the add button; must be implemented by the developer before use.
         */
        private EventHandler<ActionEvent> addEvent;


        /**
         * Gives the button a little '+'; makes it transparent, etc.
         * <p>
         * This style will be applied to the add button.
         * <p>
         * I wanted to do in the external style sheet, but I couldn't find a way to do that
         * with data binding. Buttons have an idProperty, but no classProperty.
         */
        private String addButtonStyle = "-fx-background-color: transparent;\n" +
                "        -fx-graphic:  url('Plus-Math-15.png');\n" +
                "        -fx-opacity: 0.2;\n" +
                "        -fx-padding: 0.333 .333 .333 .333;\n" +
                "        -fx-cursor: hand;";

        /**
         * Gives the button a little 'x'; makes it transparent, etc.
         * <p>
         * This style will be applied to all the remove buttons.
         * <p>
         * I wanted to do in the external style sheet, but I couldn't find a way to do that
         * with data binding. Buttons have an idProperty, but no classProperty.
         */
        private String removeButtonStyle = "-fx-background-color: transparent;\n" +
                "        -fx-graphic:  url('Delete-15.png');\n" +
                "        -fx-opacity: 0.2;\n" +
                "        -fx-padding: 0.333 .333 .333 .333;\n" +
                "        -fx-cursor: hand;";


        /**
         * Constructor
         * Gets things sit'chiated
         */
        public AddRemoveListCell() {

            // Bind the label text to the item property. If your ButtonedComboBx items are not Strings
            // you should use a converter.
            label.textProperty().bind(itemProperty());

            // Set max width to infinity so the button is all the way to the right.
            label.setMaxWidth(Double.POSITIVE_INFINITY);

            //If the item is the addOption, style it like an add button;
            // other cells should have remove buttons.
            button.styleProperty().bind(
                    Bindings.when(this.itemProperty().isEqualTo(addOption))
                            .then(addButtonStyle)
                            .otherwise(removeButtonStyle)
            );

            //Also, if it's the addOption, it's label should have no text;
            // other cells, however, should show the item.
            label.textProperty().bind(
                    Bindings.when(this.itemProperty().isEqualTo(addOption))
                            .then("")
                            .otherwise(this.itemProperty().asString())
            );


            //When the item is changed, if you need to, change what the button does.
            itemProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    if (Objects.equals(newValue, addOption)) {
                        //Make this button an add button

                        //And make it add instead of remove
                        if (addEvent != null) {
                            button.setOnAction(addEvent);
                        } else {
                            throw new NullPointerException("Cell's Add-Event is null! oldValue: " +
                                    oldValue + "; newValue: " + newValue);
                        }
                    } else {
                        //Make this button a REMOVE button

                        //Give the button it's remove event-handler
                        if (removeEvent != null) {
                            button.setOnAction(removeEvent);
                        } else {
                            throw new NullPointerException("Cell's Remove-Event is null! oldValue: " +
                                    oldValue + "; newValue: " + newValue);
                        }
                    }
                }
            });


            // Arrange controls in a HBox, and set display to graphic only
            // (the text is included in the box in this implementation).
            box = new HBox(label, button);
            HBox.setHgrow(label, Priority.ALWAYS);
            box.setAlignment(Pos.CENTER);

            //Give button a little bit of margin from the edge
            box.setPadding(new Insets(0, 3, 0, 0));
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        }


        /**
         * Gives the cells their custom graphic.
         * We set the graphic as our hBox—it contains both the item's text and either
         * an add or remove button.
         *
         * @param item  the list item to be put in the dropdown
         * @param empty whether or not it's empty?
         */
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                //Text is contained within graphic (called label), so none is set here
                setText(null);

                //We set the graphic as our hBox—it contains both the item's text and either
                // an add or remove button.
                setGraphic(box);
            }
        }


        /**
         * Allows the developer to provide an action event from the outside where
         * s/he has access to needful things.
         * <p>
         * In order to remove the cell whose 'X' was clicked, try something akin to:
         * //Get to the heart of the button
         * Button button = (Button) event.getSource();
         * HBox hBox = (HBox) button.getParent();
         * AddRemoveListCell cell = (AddRemoveListCell) hBox.getParent();
         * yourComboBox.getItems().remove(cell.getItem());
         * <p>
         * Your approach can be more streamlined.
         *
         * @param removeEvent the event desired to execute when a remove button is clicked.
         */
        public void setRemoveButtonAction(EventHandler<ActionEvent> removeEvent) {
            this.removeEvent = removeEvent;
        }

        /**
         * Allows the developer to provide an action event from the outside where
         * s/he has access to needful things.
         * <p>
         * This should only need to be set once. This same action event will be called
         * whenever the add button is clicked.
         *
         * @param addEvent
         */
        public void setAddButtonAction(EventHandler<ActionEvent> addEvent) {
            this.addEvent = addEvent;
        }


    }//End of AddRemoveListCell Class

}//End of AddRemoveComboBox Class
