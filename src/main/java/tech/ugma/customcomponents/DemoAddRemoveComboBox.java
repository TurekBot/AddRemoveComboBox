package tech.ugma.customcomponents;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * For demonstrating how to use AddRemoveComboBox
 */
public class DemoAddRemoveComboBox extends Application {

    private AddRemoveComboBox addRemoveComboBox;

    private Text submission;


    @Override
    public void start(Stage primaryStage) throws Exception {

        primaryStage.getIcons().addAll(new Image("Delete-15.png"), new Image("Plus-Math-15.png"));

        BorderPane borderPane = new BorderPane();
        ObservableList<String> dummyList =
                FXCollections.observableArrayList("Dummy", "List", AddRemoveComboBox.ADD_CELL_PLACEHOLDER);

        addRemoveComboBox = new AddRemoveComboBox(dummyList);
        addRemoveComboBox.setSortAlphabetically(false);


        borderPane.setTop(addRemoveComboBox);


        Button submit = new Button("Submit");
        submit.setOnAction(this::submit);
        borderPane.setCenter(submit);


        submission = new Text();
        borderPane.setBottom(submission);


        primaryStage.setScene(new Scene(borderPane, 400, 300));
        primaryStage.show();


//        ScenicView.show(primaryStage.getScene());
    }

    private void submit(ActionEvent actionEvent) {
        String value = addRemoveComboBox.getValue();
        if (value == null) {
            value = "null";
        }
        submission.setText(value);
    }
}
