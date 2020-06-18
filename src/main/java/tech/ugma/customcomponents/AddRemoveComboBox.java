package tech.ugma.customcomponents;
/*
 * Created by Bradley Turek, on 5/10/17 9:07 AM.
 * Copyright (c) 2017 All rights reserved.
 *
 *  Last modified 5/10/17 9:07 AM
 */

import javafx.scene.control.skin.ComboBoxListViewSkin;
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
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.text.Collator;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;


/**
 * A ComboBox you can add and remove items from.
 * <p>
 * Items are removed by clicking the 'x' next to the item to be removed.
 * <p>
 * Items are added by clicking the '+' at the bottom of the list. This plus button, or add button as
 * I'll call it will be automatically kept at the bottom of the list, but (for now) needs to be added manually.
 * (This lets you only allow removal, but not addition.)
 * You add the "add button" by including `AddRemoveButton.ADD_CELL_PLACEHOLDER` in your item list.
 * <p>
 * Removable cells are implemented using a custom class called
 * AddRemoveListCell (accessible via AddRemoveComboBox.AddRemoveListCell) which is conveniently
 * contained herein.
 */
@SuppressWarnings({"WeakerAccess", "Convert2Lambda"})
public class AddRemoveComboBox extends ComboBox<String> {

    /**
     * For convenience in adding the "Add Button" to the list of choices. Hopefully no one ever
     * has this exact text in their list.
     */
    public static final String ADD_CELL_PLACEHOLDER = "ADD_AN_ADD_CELL_RIGHT_HERE_RIGHT_NOW";

    /**
     * This will keep the add cell (the little '+') at the bottom of the list.
     */
    private Comparator<String> addCellRelegator;

    /**
     * The default action for when when the user removes something from the list.
     * <p>
     * If you don't like the way it's done here, feel free to set your own by calling
     * setRemovalAction().
     */
    private EventHandler<ActionEvent> removalAction = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {

            //Get the cell whose button was clicked
            Button button = (Button) event.getSource();
            HBox hBox = (HBox) button.getParent();
            AddRemoveComboBox.AddRemoveListCell cell = (AddRemoveComboBox.AddRemoveListCell) hBox.getParent();

            AddRemoveComboBox.this.getItems().remove(cell.getItem());

            //I can't quite figure out how to resize the ListView once an item is removed.
            //However, the ComboBox seems to know how to do it, itself; so I'll just open
            //and close it and hope no one notices.
            AddRemoveComboBox.this.hide();
            AddRemoveComboBox.this.show();
        }
    };
    /**
     * The default action for when when the user adds something from the list.
     * <p>
     * If you don't like the way it's done here, feel free to set your own by calling
     * setAdditionAction().
     */
    private EventHandler<ActionEvent> additionAction = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            //Get the dialog ready
            Dialog<String> addDialog = new TextInputDialog();
            addDialog.setTitle("New Item");
            addDialog.setHeaderText(null);
            addDialog.setContentText("Please enter the new item: ");

            //Get the scene and tell the dialog that the scene is its owner.
            //This will ensure that this dialog's icon matches the scene's
            Parent parent = AddRemoveComboBox.this.getParent();
            while (parent.getParent() != null) {
                parent = parent.getParent();
            }
            addDialog.initOwner(parent.getScene().getWindow());

            //Show the dialog and then wait for the result
            Optional<String> result = addDialog.showAndWait();

            //If the result comes back, then:
            result.ifPresent(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    //Add the new item
                    AddRemoveComboBox.this.getItems().add(s);
                    //Select the new item
                    AddRemoveComboBox.this.getSelectionModel().select(s);
                }
            });

        }
    };

    /**
     * Keeps track of whether the list was just sorted or not. If it was, the listener on the
     * item list will make sure not to sort it again. See tech.ugma.customcomponents.AddRemoveComboBox#initAddCellManager()
     */
    private boolean wasJustSorted;

    /**
     * Controls whether or not the AddRemoveComboBox should sort the rest of the items (excluding the
     * add cell) alphabetically.
     * <p>
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

        //Make the comparator that will keep the add cell at the bottom of the list
        addCellRelegator = initAddCellRelegator();

        //Listen for every time the list is changed; when it is, re-sort the items,
        //making sure the add cell is at the bottom.
        if (list != null) {
            list.addListener(initAddCellManager());
        } else {
            throw new NullPointerException("List passed to constructor is null.");
        }

        //Make and give the cell factory to the combo box. The cell factory makes the cells that are
        //displayed in the dropdown area in a ListView
        this.setCellFactory(initCellFactory());

        // Wait till we have our Skin and then configure it a little.
        this.skinProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Skin<?>> observable, Skin<?> oldValue, Skin<?> newValue) {
                if (newValue != null) {
                    // Without this, the ButtonedComboBox will hide before the click registers to the button.
                    ((ComboBoxListViewSkin<?>) newValue).setHideOnClick(false);
                }
            }
        });


        //Don't show the big ugly constant that holds the place of the add cell
        this.getSelectionModel().selectedItemProperty().addListener(initHideUglyConstantListener());

        //By default, select first option
        this.getSelectionModel().selectFirst();

    }

    /**
     * As a rule, it is assumed that the add cell is desired at the bottom of the list.
     * The comparator that this returns should hopefully ensure just that.
     *
     * @return a comparator that will always consider the add cell as the smaller of the two.
     */
    private Comparator<String> initAddCellRelegator() {
        return new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {

                if (Objects.equals(ADD_CELL_PLACEHOLDER, o1)) {
                    //If the add cell is o1 (the first argument), return positive, telling the
                    //comparator that o1 is greater.
                    return Integer.MAX_VALUE;
                } else if (Objects.equals(ADD_CELL_PLACEHOLDER, o2)) {

                    //If the add cell is o2 (the second argument), return negative, telling the
                    //comparator that add cell is lesser.
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
     * Makes an invalidation listener that keeps the add cell at the bottom of the list.
     *
     * @return an invalidation listener that keeps the add cell at the bottom of the list.
     */
    private InvalidationListener initAddCellManager() {
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
                    AddRemoveComboBox.this.getItems().sort(addCellRelegator);
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

                //Gives the user the ability to remove carrier choices
                customCell.setRemoveButtonAction(removalAction);


                //Give user the ability to add carriers
                customCell.setAddButtonAction(additionAction);

                //If the cell clicked is the add cell (even if they don't click the button), we want something to happen.
                customCell.setOnMousePressed(clickAddButton -> {
                    //Only if the click was on the add cell, simulate a button click.
                    if (Objects.equals(customCell.getItem(), ADD_CELL_PLACEHOLDER)) {
                        customCell.button.fire();
                    }

                    //No matter what, hide the box after the click.
                    //This is necessary because we disabled the hide on click with the custom skin.
                    AddRemoveComboBox.this.hide();
                });


                //Finally, return the customCell, all gussied up.
                return customCell;
            }
        };
    }

    /**
     * When the user selects the add-button cell, we don't really want to show anything in the drop down's button
     * area.
     *
     * @return a change listener that doesn't allow the big ugly constant to show in the button area
     */
    private ChangeListener<? super String> initHideUglyConstantListener() {

        return (observable, oldValue, selectedValue) -> {

            if (Objects.equals(selectedValue, ADD_CELL_PLACEHOLDER)) {

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
     * Controls whether or not the list is sorted alphabetically. (This excludes the add cell,
     * which is always relegated to the bottom.)
     *
     * @return true if the list *will* be sorted alphabetically, false otherwise.
     */
    public boolean isSortAlphabetically() {
        return sortAlphabetically;
    }


    /**
     * Set this to true if you want the list to be sorted alphabetically. The add cell will
     * still get put at the bottom, just the rest of the items will be put in alphabetical order.
     *
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
         * Each item in the combo boxes item list will be put here as a label
         */
        private Label label = new Label();

        /**
         * An image button displayed to the far right of the cell.
         * This is formatted with internal CSS to be either an add- or remove- button.
         */
        private Button button = new Button();

        /**
         * This is the node that will display the text and the remove-button.
         */
        private HBox box = new HBox(button, label);

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

            // Set max width to infinity so the label takes up the rest of the space.
            label.setMaxWidth(Double.POSITIVE_INFINITY);

            //If the item is the add cell, style it like an add button;
            // other cells should have remove buttons.
            button.styleProperty().bind(
                    Bindings.when(this.itemProperty().isEqualTo(ADD_CELL_PLACEHOLDER))
                            .then(addButtonStyle)
                            .otherwise(removeButtonStyle)
            );


            //Also, if it's the add cell, it's label should have no text;
            // other cells, however, should show the item.
            label.textProperty().bind(
                    Bindings.when(this.itemProperty().isEqualTo(ADD_CELL_PLACEHOLDER))
                            .then("")
                            .otherwise(this.itemProperty().asString())
            );


            //When the item is changed, if you need to, change what the button does.
            itemProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    if (Objects.equals(newValue, ADD_CELL_PLACEHOLDER)) {
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


            // Set display to graphic only
            // (the text is included in the box (graphic) in this implementation).
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            //Put some space between the button and the label
            box.setSpacing(3);


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
         * @param addEvent your favorite way to get a new item from the user and add it to the list.
         */
        public void setAddButtonAction(EventHandler<ActionEvent> addEvent) {
            this.addEvent = addEvent;
        }


    }//End of AddRemoveListCell Class

}//End of AddRemoveComboBox Class
