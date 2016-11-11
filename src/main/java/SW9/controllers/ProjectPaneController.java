package SW9.controllers;

import SW9.HUPPAAL;
import SW9.abstractions.Component;
import SW9.presentations.FilePresentation;
import SW9.utility.UndoRedoStack;
import com.jfoenix.controls.JFXRippler;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;

public class ProjectPaneController implements Initializable {

    private final HashMap<Component, FilePresentation> componentPresentationMap = new HashMap<>();
    public StackPane root;
    public AnchorPane toolbar;
    public Label toolbarTitle;
    public ScrollPane scrollPane;
    public VBox filesList;
    public JFXRippler createComponent;
    public JFXRippler saveProject;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        HUPPAAL.getProject().getComponents().addListener(new ListChangeListener<Component>() {
            @Override
            public void onChanged(final Change<? extends Component> c) {
                while (c.next()) {
                    c.getAddedSubList().forEach(o -> handleAddedComponent(o));
                    c.getRemoved().forEach(o -> handleRemovedComponent(o));
                }
            }
        });

        HUPPAAL.getProject().getComponents().forEach(this::handleAddedComponent);
    }

    private void handleAddedComponent(final Component component) {
        final FilePresentation filePresentation = new FilePresentation(component);
        filesList.getChildren().add(filePresentation);
        componentPresentationMap.put(component, filePresentation);
    }

    private void handleRemovedComponent(final Component component) {
        filesList.getChildren().remove(componentPresentationMap.get(component));
        componentPresentationMap.remove(component);
    }

    @FXML
    private void saveProjectClicked() {
        System.out.println("saveProjectClicked");
    }

    @FXML
    private void createComponentClicked() {
        final Component newComponent = new Component();

        UndoRedoStack.push(() -> { // Perform
            HUPPAAL.getProject().getComponents().add(newComponent);
        }, () -> { // Undo
            HUPPAAL.getProject().getComponents().remove(newComponent);
        }, "Created new component: " + newComponent.getName(), "add-circle");
    }

}