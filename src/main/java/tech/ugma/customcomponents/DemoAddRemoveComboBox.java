package tech.ugma.customcomponents;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import tech.ugma.customcomponents.AddRemoveComboBox;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * For demonstrating how to use AddRemoveComboBox
 */
public class DemoAddRemoveComboBox extends Application{
    @Override
    public void start(Stage primaryStage) throws Exception {
        BorderPane borderPane = new BorderPane();

        ObservableList<String> dummyList =
                FXCollections.observableArrayList("Dummy", "List", AddRemoveComboBox.ADD_CELL);
        AddRemoveComboBox addRemoveComboBox = new AddRemoveComboBox(dummyList);
        addRemoveComboBox.setSortAlphabetically(false);

        borderPane.setTop(addRemoveComboBox);


        addRemoveComboBox.setAdditionAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Dialog<String> addDialog = new TextInputDialog();
                addDialog.setHeaderText(null);
                addDialog.setContentText("Please enter the new item: ");

                Optional<String> result = addDialog.showAndWait();

                result.ifPresent(new Consumer<String>() {
                    @Override
                    public void accept(String s) {
                       dummyList.add(s);
                    }
                });

            }
        });



        primaryStage.setScene(new Scene(borderPane, 400, 300));
        primaryStage.show();
    }
}
